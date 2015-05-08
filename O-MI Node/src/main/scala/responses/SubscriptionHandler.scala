package responses

import akka.actor.Actor
import akka.event.LoggingAdapter
import akka.actor.ActorLogging
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.ActorLogging

import responses._
import database._
import parsing.Types.Path
import parsing.Types.OmiTypes.{ SubLike, SubDataRequest }
import OMISubscription.SubscriptionResponseGen
import CallbackHandlers._

import scala.collection.mutable.PriorityQueue

import java.sql.Timestamp
import System.currentTimeMillis
import scala.math.Ordering
import scala.util.{ Success, Failure }
import scala.collection.mutable.{ Map, HashMap }

import xml._
import scala.concurrent.duration._
import scala.concurrent._

// MESSAGES
case object HandleIntervals
case class NewSubscription(id: Int)
case class RemoveSubscription(id: Int)

/**
 * Handles interval counting and event checking for subscriptions
 */
class SubscriptionHandler extends Actor with ActorLogging {
  import ExecutionContext.Implicits.global
  import context.system

  implicit val timeout = Timeout(5.seconds)

  implicit val dbConnection: DB = new SQLiteConnection

  val subscriptionResponseGen = new SubscriptionResponseGen

  sealed trait SavedSub {
    val sub: DBSub
    val id: Int
  }

  case class TimedSub(sub: DBSub, id: Int, nextRunTime: Timestamp)
    extends SavedSub

  case class EventSub(sub: DBSub, id: Int, lastValue: String)
    extends SavedSub

  object TimedSubOrdering extends Ordering[TimedSub] {
    def compare(a: TimedSub, b: TimedSub) =
      a.nextRunTime.getTime compare b.nextRunTime.getTime
  }

  private var intervalSubs: PriorityQueue[TimedSub] = {
    PriorityQueue()(TimedSubOrdering.reverse)
  }
  def getIntervalSubs = intervalSubs

  //var eventSubs: Map[Path, EventSub] = HashMap()
  private var eventSubs: Map[String, EventSub] = HashMap()
  def getEventSubs = eventSubs

  // Attach to db events
  database.attachSetHook(this.checkEventSubs _)

  // load subscriptions at startup
  override def preStart() = {
    val subs = dbConnection.getAllSubs(Some(true))
    for (sub <- subs) loadSub(sub.id, sub)

  }

  private def loadSub(id: Int): Unit = {
    dbConnection.getSub(id) match {
      case Some(dbsub) =>
        loadSub(id, dbsub)
      case None =>
        log.error(s"Tried to load nonexistent subscription: $id")
    }
  }
  private def loadSub(id: Int, dbsub: DBSub): Unit = {
    log.debug(s"Adding sub: $id")

    require(dbsub.callback.isDefined, "SubscriptionHandlerActor is not for buffered messages")

    if (dbsub.isIntervalBased) {
      intervalSubs += TimedSub(
        dbsub,
        id,
        new Timestamp(currentTimeMillis()))

      handleIntervals()
      log.debug(s"Added sub as TimedSub: $id")

    } else if (dbsub.isEventBased) {

      for (path <- dbsub.paths)
        dbConnection.get(path).foreach{
          case sensor: DBSensor =>
            eventSubs += path.toString -> EventSub(dbsub, id, sensor.value)
          case x =>
            log.warning(s"$x not implemented in SubscriptionHandlerActor for Interval=-1")
        }

      log.debug(s"Added sub as EventSub: $id")
    }

  }

  /**
   * @param id The id of subscription to remove
   * @return true on success
   */
  private def removeSub(id: Int): Boolean = {
    dbConnection.getSub(id) match {
      case Some(dbsub) => removeSub(dbsub)
      case None => false
    }
  }
  
  /**
   * @param sub The subscription to remove
   * @return true on success
   */
  private def removeSub(sub: DBSub): Boolean = {
    if (sub.isEventBased) {
      sub.paths.foreach { path =>
        eventSubs -= path.toString
      }
    } else {
      //remove from intervalSubs
      intervalSubs = intervalSubs.filterNot(sub.id == _.id)
    }
    dbConnection.removeSub(sub.id)
  }

  override def receive = {

    case HandleIntervals => handleIntervals()

    case NewSubscription(requestId) => loadSub(requestId)

    case RemoveSubscription(requestId) => sender() ! removeSub(requestId)
  }

  def checkEventSubs(paths: Seq[Path]): Unit = {

    for (path <- paths) {
      var newestValue: Option[String] = None

      eventSubs.get(path.toString) match {

        case Some(EventSub(subscription, id, lastValue)) => {

          if (hasTTLEnded(subscription, currentTimeMillis())) {
            removeSub(subscription)
          } else {
            if (newestValue.isEmpty)
              newestValue = dbConnection.get(path).map{
                case DBSensor(_, v, _) => v
                case _ => ""// noop, already logged at loadSub
              }
            
            if (lastValue != newestValue.getOrElse("")){

              //def failed(reason: String) =
              //  log.warning(s"Callback failed; subscription id:$id  reason: $reason")

              val addr = subscription.callback 
              if (addr == None) return

              subscriptionResponseGen.runRequest(SubDataRequest(subscription))

            }
          }
        }

        case None => // noop
      }
    }
  }

  private def hasTTLEnded(sub: DBSub, timeMillis: Long): Boolean = {
    val removeTime = sub.startTime.getTime + sub.ttlToMillis

    if (removeTime <= timeMillis && !sub.isImmortal) {
      log.debug(s"TTL ended for sub: id:${sub.id} ttl:${sub.ttlToMillis} delay:${timeMillis - removeTime}ms")
      true
    } else
      false
  }

  def handleIntervals(): Unit = {
    // failsafe
    if (intervalSubs.isEmpty) {
      log.error("handleIntervals shouldn't be called when there is no intervalSubs!")
      return
    }

    val checkTime = currentTimeMillis()

    while (intervalSubs.headOption.exists(_.nextRunTime.getTime <= checkTime)) {

      val TimedSub(sub, id, time) = intervalSubs.dequeue()

      log.debug(s"handleIntervals: delay:${checkTime - time.getTime}ms currentTime:$checkTime targetTime:${time.getTime} id:$id")

      // Check if ttl has ended, comparing to original check time
      if (hasTTLEnded(sub, checkTime)) {

        dbConnection.removeSub(id)

      } else {
        val numOfCalls = ((checkTime - sub.startTime.getTime) / sub.intervalToMillis).toInt

        val newTime = new Timestamp(sub.startTime.getTime.toLong + sub.intervalToMillis * (numOfCalls + 1))
        //val newTime = new Timestamp(time.getTime + sub.intervalToMillis) // OLD VERSION

        intervalSubs += TimedSub(sub, id, newTime)


        log.debug(s"generateOmi for id:$id")
        subscriptionResponseGen.runRequest(SubDataRequest(sub))
        val interval = sub.interval
        val callbackAddr = sub.callback.get
        log.info(s"Sending in progress; Subscription id:$id addr:$callbackAddr interval:$interval")


          /*
          def failed(reason: String) =
            log.warning(
              s"Callback failed; subscription id:$id interval:$interval  reason: $reason")


            case Success(CallbackSuccess) =>
              log.info(s"Callback sent; subscription id:$id addr:$callbackAddr interval:$interval")

            case Success(fail: CallbackFailure) =>
              failed(fail.toString)

            case Failure(e) =>
              failed(e.getMessage)
          }
        }.onFailure {
          case err: Throwable =>
            log.error(s"Error in callback handling of sub $id: ${err.getStackTrace.mkString("\n")}")
        }
        */
      }
    }

    // Schedule for next
    intervalSubs.headOption foreach { next =>

      val nextRun = next.nextRunTime.getTime - currentTimeMillis()
      system.scheduler.scheduleOnce(nextRun.milliseconds, self, HandleIntervals)

      log.debug(s"Next subcription handling scheluded after ${nextRun}ms")
    }
  }

}
