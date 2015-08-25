package responses

import org.specs2.mutable._
import org.specs2.matcher.XmlMatchers._
import org.specs2.specification.Scope
import scala.io.Source

import responses._
//import responses.Common._
import parsing._
import types._
import types.Path._
import types.OmiTypes._
import database._
import parsing.OdfParser._

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone
import java.text.SimpleDateFormat;
import java.sql.Timestamp

import scala.xml.Utility.trim
import scala.xml.XML

import akka.actor._
import akka.testkit.{ TestKit, TestActorRef, EventFilter }
import akka.testkit.TestEvent.{Mute, UnMute}
import akka.pattern.ask
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Try
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.Iterable
import scala.collection.JavaConversions.asJavaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.iterableAsScalaIterable

import testHelpers.{ BeforeAfterAll, SubscriptionHandlerTestActor, DeactivatedTimeConversions }

// For eclipse:
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
//

@RunWith(classOf[JUnitRunner])
class SubscriptionTest extends Specification with BeforeAfterAll with DeactivatedTimeConversions {
  sequential
//change akka.loglevel to DEBUG  and to see more info
  implicit val system = ActorSystem("on-core", ConfigFactory.parseString(
    """
            akka.loggers = ["akka.testkit.TestEventListener"]
            akka.stdout-loglevel = INFO
            akka.loglevel = WARNING
            akka.log-dead-letters-during-shutdown = off
            akka.jvm-exit-on-fatal-error = off
            """))
  implicit val dbConnection = new TestDB("subscription-response-test")
  implicit val timeout = akka.util.Timeout.apply(5000)
  //  {
  //    def setVal(path: Path) = runSync(this.addObjectsI(path, true))
  //  }

  def newTimestamp(time: Long = -1L): Timestamp = {
    if (time == -1) {
      new Timestamp(new java.util.Date().getTime)
    } else {
      new java.sql.Timestamp(time)
    }
  }

  val subscriptionHandlerRef = system.actorOf((Props(new SubscriptionHandler()(dbConnection)))) //[SubscriptionHandler]

  val requestHandler = new RequestHandler(subscriptionHandlerRef)(dbConnection)

  val calendar = Calendar.getInstance()
  // try to fix bug with travis
  val timeZone = TimeZone.getTimeZone("Etc/GMT+2")
  calendar.setTimeZone(timeZone)
  val date = calendar.getTime
  val testtime = new java.sql.Timestamp(date.getTime)

  def beforeAll = {
    //    dbConnection.clearDB()
    val testData = Map(
      Path("Objects/ReadTest/Refrigerator123/PowerConsumption") -> "0.123",
      Path("Objects/ReadTest/Refrigerator123/RefrigeratorDoorOpenWarning") -> "door closed",
      Path("Objects/ReadTest/Refrigerator123/RefrigeratorProbeFault") -> "Nothing wrong with probe",
      Path("Objects/ReadTest/RoomSensors1/Temperature/Inside") -> "21.2",
      Path("Objects/ReadTest/RoomSensors1/CarbonDioxide") -> "too much",
      Path("Objects/ReadTest/RoomSensors1/Temperature/Outside") -> "12.2",
      Path("Objects/ReadTest/SmartCar/Fuel") -> "30")

    val intervaltestdata = List(
      "100",
      "102",
      "105",
      "109",
      "115",
      "117")

    for ((path, value) <- testData) {
      //      dbConnection.remove(path)
      dbConnection.set(path, testtime, value)
    }

    var count = 1000000

    //    dbConnection.remove(Path("Objects/ReadTest/SmartOven/Temperature"))
    for (value <- intervaltestdata) {
      dbConnection.set(Path("Objects/ReadTest/SmartOven/Temperature"), new java.sql.Timestamp(date.getTime + count), value)
      count = count + 1000
    }

    //  lazy val simpletestfile = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequest.xml")).getLines.mkString("\n")
    //  val parserlist = OmiParser.parse(simpletestfile)

    //  val (requestID, xmlreturn) = OMISubscription.setSubscription(parserlist.head.asInstanceOf[SubscriptionRequest])

    //  lazy val simpletestfilecallback = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequestWithCallback.xml")).getLines.mkString("\n")
    //  val parserlistcallback = OmiParser.parse(simpletestfilecallback)

    //  val (requestIDcallback, xmlreturncallback) = OMISubscription.setSubscription(parserlistcallback.head.asInstanceOf[SubscriptionRequest])
  }
  def afterAll = {
    system.shutdown()
    dbConnection.destroy()
  }

  "Subscription response" should {
    "Return with just a requestID when subscribed" in {
//      println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW\n"* 4)
//      println(System.getProperty("user.dir"))
//      println(getClass.getClassLoader.getResource("SubscriptionRequest.xml"))
//      println(getClass.getClassLoader.getResourceAsStream("SubscriptionRequest.xml"))
//      lazy val simpletestfile = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequest.xml")).getLines().mkString("\n")//.fromURL(getClass.getClassLoader.getResource("SubscriptionRequest.xml")).getLines.mkString("\n")
      val parserlist = OmiParser.parse(subscriptionRequest)
      //      parserlist.isRight === true
      val requestOption = parserlist.right.toOption.flatMap(x => x.headOption.collect({ case y: SubscriptionRequest => y }))
      val requestReturn = requestOption.map(x => requestHandler.handleRequest(x))
      //      val (xmlreturn, requestID) = requestHandler.handleRequest(parserlist.right.get.head.asInstanceOf[SubscriptionRequest])

      val correctxml = requestReturn map (x => {
        <omi:omiEnvelope xmlns:omi="omi.xsd" xmlns="odf.xsd" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ttl="1.0" version="1.0">
          <omi:response>
            <omi:result>
              <omi:return description="Successfully started subscription" returnCode="200"/>
              <omi:requestID>{ x._1.\\("requestID").text }</omi:requestID>
            </omi:result>
          </omi:response>
        </omi:omiEnvelope>
      })

      //      trim(xmlreturn.head).toString() === trim(correctxml).toString()
      requestReturn must beSome.which(requestReturn => correctxml must beSome.which(correct => requestReturn._1 must beEqualToIgnoringSpace(correct)))
      //      xmlreturn must beEqualToIgnoringSpace(correctxml)
    }

    "Return a value when interval is larger than time elapsed and no callback given" in {
//      lazy val simpletestfile = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequestWithLargeInterval.xml")).getLines.mkString("\n")
      val parserlist = OmiParser.parse(subscriptionRequestWithLargeInterval)
      parserlist.isRight === true
      val requestOption = parserlist.right.toOption.flatMap(x => x.headOption.collect({ case y: SubscriptionRequest => y }))
      val requestReturn = requestOption.map(x => requestHandler.handleRequest(x))
      val requestID = Try(requestReturn.map(x => x._1.\\("requestID").text.toInt)).toOption.flatten
      //      dbConnection.getSub(requestID.get) must beSome
      val subxml = requestID.map(id => requestHandler.handleRequest((PollRequest(10.seconds, None, asJavaIterable(Seq(id))))))
      //might fail after change in namespaces
      val correctxml = requestID map (x => {
        <omi:omiEnvelope xmlns:omi="omi.xsd" xmlns="odf.xsd" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ttl="1.0" version="1.0">
          <omi:response>
            <omi:result msgformat="odf">
              <omi:return returnCode="200"/>
              <omi:requestID>{ x }</omi:requestID>
              <omi:msg>
                <Objects>
                  <Object>
                    <id>ReadTest</id>
                    <Object>
                      <id>Refrigerator123</id>
                      <InfoItem name="PowerConsumption">
                        <value unixTime={ (testtime.getTime / 1000).toInt.toString }>0.123</value>
                      </InfoItem>
                    </Object>
                  </Object>
                </Objects>
              </omi:msg>
            </omi:result>
          </omi:response>
        </omi:omiEnvelope>
      })

      //      trim(subxml.head) === trim(correctxml)
      subxml must beSome.which(requestReturn => correctxml must beSome.which(correct => requestReturn._1 must beEqualToIgnoringSpace(correct)))
      //      subxml.head must beEqualToIgnoringSpace(correctxml)

    }

    "Return with right values and requestID in subscription generation" in {
//      lazy val simpletestfilecallback = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequest.xml")).getLines.mkString("\n")
      val parserlistcallback = OmiParser.parse(subscriptionRequest)
      parserlistcallback.isRight === true
      val requestOption = parserlistcallback.right.toOption.flatMap(x => x.headOption.collect({ case y: SubscriptionRequest => y }))
      val requestReturn = requestOption.map(x => requestHandler.handleRequest(x))
      val requestID = Try(requestReturn.map(x => x._1.\\("requestID").text.toInt)).toOption.flatten
      //XXX: Stupid hack, for interval      
      Thread.sleep(1000)
      val subxml = requestID.map(id => requestHandler.handleRequest((PollRequest(10.seconds, None, asJavaIterable(Seq(id))))))

      //      val (requestIDcallback, xmlreturncallback) = requestHandler.handleRequest(parserlistcallback.right.get.head.asInstanceOf[SubscriptionRequest])

      //      val subxml = omiResponse(pollResponseGen.genResult(PollRequest(10.seconds, None, Seq(requestIDcallback))))

      //      val correctxml =
      /*
        <omi:omiEnvelope xmlns:omi="omi.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="omi.xsd omi.xsd" version="1.0" ttl="0.0">
          <omi:response>
            <omi:result msgformat="odf">
              <omi:return returnCode="200"></omi:return>
              <omi:requestID>1</omi:requestID>
              <omi:msg xsi:schemaLocation="odf.xsd odf.xsd" xmlns="odf.xsd">
                <Objects>
                  <Object>
                    <id>ReadTest</id>
                    <Object>
                      <id>Refrigerator123</id>
                      <InfoItem name="PowerConsumption">
                        <value dateTime="1970-01-17T12:56:15.723">0.123</value>
                      </InfoItem>
                    </Object>
                  </Object>
                </Objects>
              </omi:msg>
            </omi:result>
          </omi:response>
        </omi:omiEnvelope> */

      //      trim(subxml.head) === trim(correctxml)

      subxml must beSome.which(_._1.\\("value").headOption must beSome.which(_.text === "0.123")) //subxml.\\("value").head.text === "0.123"
    }

    "Return error code when asked for nonexisting infoitem" in {
//      lazy val simpletestfile = Source.fromURL(getClass.getClassLoader.getResource("BuggyRequest.xml")).getLines.mkString("\n")
      val parserlist = OmiParser.parse(buggyRequest)
      parserlist.isRight === true
      val requestOption = parserlist.right.toOption.flatMap(x => x.headOption.collect({ case y: SubscriptionRequest => y }))
      val requestReturn = requestOption.map(x => requestHandler.handleRequest(x))

      /* we don't want this to break every time we change the error message
      val correctxml =
        <omi:omiEnvelope xmlns:omi="omi.xsd" xmlns="odf.xsd" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ttl="1.0" version="1.0">
          <omi:response>
            <omi:result>
              <omi:return returnCode="400" description="Bad request: requirement failed: Invalid path, no such item found"></omi:return>
            </omi:result>
          </omi:response>
        </omi:omiEnvelope>
        */
      requestReturn must beSome.which {
        _._1.headOption must beSome.which { response =>
          response must not(\\("requestID"))
          response must \("response") \ ("result") \ ("return", "returnCode" -> "40[40]")
        }
      }
    }

    "Return with error when subscription doesn't exist" in {
      val rid = 1234L
      val xmlreturn = requestHandler.handleRequest((PollRequest(10.seconds, None, Seq(rid))))

      val correctxml =
        <omi:omiEnvelope xmlns:omi="omi.xsd" xmlns="odf.xsd" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ttl="1.0" version="1.0">
          <omi:response>
            <omi:result>
              <omi:return returnCode="404" description="A subscription with this id has expired or doesn't exist"/>
              <omi:requestID>{ rid }</omi:requestID>
            </omi:result>
          </omi:response>
        </omi:omiEnvelope>

      //      trim(xmlreturn.head) === trim(correctxml)
      xmlreturn._1.headOption must beSome.which(_ must beEqualToIgnoringSpace(correctxml))
    }
    "Return polled data only once" in {
      val testPath = Path("Objects/SubscriptionTest/intervalTest/SmartOven/pollingtest1")
      //      dbConnection.setVal(testPath)

      val testTime = new Date().getTime - 10000
      //      db.saveSub(NewDBSub(1.seconds, newTs, 0, None), Array(Path("/Objects/path/to/sensor1"), Path("/Objects/path/to/sensor2")))
      //      val testSub = dbConnection.saveSub(NewDBSub(1.seconds, newTimestamp(testTime), -1, None), Array(testPath))
      //      dbConnection.startBuffering(Path("Objects/SubscriptionTest/SmartOven/pollingtest"))

      //      dbConnection.remove(testPath)
      dbConnection.get(testPath) === None

      (0 to 10).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime + n * 1000), n.toString()))
      val testSub = dbConnection.saveSub(NewDBSub(1.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))
      val test = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1

      //omiResponse(pollResponseGen.genResult(PollRequest(10.seconds, None, Seq(testSub))))

      val dataLength = test.\\("value").length

      dataLength must be_>=(10)
      val test2 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      val newDataLength = test2.\\("value").length
      newDataLength must be_<=(dataLength) and be_<=(3)

      //      dbConnection.remove(testPath)

      //      dbConnection.stopBuffering(Path("Objects/SubscriptionTest/SmartOven/pollingtest"))
      dbConnection.removeSub(testSub)
    }

    //this test will be removed when db upgrade is ready
    "TTL should decrease by some multiple of interval" in {
      val testPath = Path("Objects/SubscriptionTest/intervalTest/SmartOven/pollingtest2")
      //      dbConnection.setVal(testPath)

      val testTime = new Date().getTime - 10000

      //      val ttlFirst = dbConnection.getSub(testSub.id).map(_.ttl)
      //      ttlFirst must beSome(60.0.seconds)
      (0 to 10).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime + n * 1000), n.toString()))

      val testSub = dbConnection.saveSub(NewDBSub(3.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))
      val ttlFirst = dbConnection.getSub(testSub.id).map(_.ttl)
      ttlFirst must beSome(60.0.seconds)

      requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))
      requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))

      val ttlEnd = dbConnection.getSub(testSub.id).map(_.ttl)
      ttlFirst must beSome.which(first =>
        ttlEnd must beSome.which(last =>
          (first.toUnit(SECONDS) - last.toUnit(SECONDS)) % 3 === 0)) //(ttlFirst - ttlEnd) % 3 === 0

      //      dbConnection.remove(testPath)
      dbConnection.removeSub(testSub)
    }
    "Event based subscription without callback should return all the new values when polled" in {
      val testPath = Path("Objects/SubscriptionTest/eventTest/SmartOven/pollingtest1")
      //      dbConnection.setVal(testPath)

      val testTime = new Date().getTime - 10000
      (0 to 10).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime - 4999 + n * 1000), n.toString()))

      val testSub = dbConnection.saveSub(NewDBSub(-1.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))

      val test = requestHandler.handleRequest(PollRequest(60.seconds, None, Seq(testSub.id)))._1
      //      dbConnection.remove(testPath)
      dbConnection.removeSub(testSub)
      test.\\("value").length === 6

    }
    "Event based subscription without callback should not return already polled data" in {
      val testPath = Path("Objects/SubscriptionTest/eventTest/SmartOven/pollingtest2")
      //      dbConnection.setVal(testPath)

      val testTime = new Date().getTime - 10000

      (0 to 10).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime - 5000 + n * 1000), n.toString()))

      val testSub = dbConnection.saveSub(NewDBSub(-1.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))
      val test = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test.\\("value").length === 6
      val test2 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test2.\\("value").length === 0
      dbConnection.set(testPath, new java.sql.Timestamp(new Date().getTime), "1234")
      val test3 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test3.\\("value").length === 1

      //      dbConnection.remove(testPath)
      dbConnection.removeSub(testSub)
    }
    "Event based subscription should return new values only when the value changes" in {
      val testPath = Path("Objects/SubscriptionTest/eventTest/SmartOven/pollingtest3")
      //      dbConnection.setVal(testPath)

      val testTime = new Date().getTime - 10000
      //      val testSub = dbConnection.saveSub(NewDBSub(-1.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))
      (0 to 10).zip(Array(1, 1, 1, 2, 3, 4, 3, 5, 5, 6, 7)).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime + n._1 * 900), n._2.toString()))

      val testSub = dbConnection.saveSub(NewDBSub(-1.seconds, newTimestamp(testTime), 60.0.seconds, None), Array(testPath))
      val test = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test.\\("value").length === 8

      val test2 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test2.\\("value").length === 0

      dbConnection.set(testPath, new java.sql.Timestamp(new Date().getTime), "1234")
      val test3 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test3.\\("value").length === 1

      //does not return same value twice
      val test4 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test4.\\("value").length === 0

      dbConnection.set(testPath, new java.sql.Timestamp(new Date().getTime), "1234")
      val test5 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub.id)))._1
      test5.\\("value").length === 0

      //      dbConnection.remove(testPath)
      dbConnection.removeSub(testSub)
    }
    "Having 2 subs on same data should work without problems" in {
      val testPath = Path("Objects/SubscriptionTest/eventTest/SmartOven/pollingtest4")
      val testTime = new Date().getTime - 20000
      (1 to 10).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime + n * 900), n.toString()))

      val testSub1 = dbConnection.saveSub(NewDBSub(-1 seconds, newTimestamp(testTime), Duration.Inf, None), Array(testPath))
      val testSub2 = dbConnection.saveSub(NewDBSub(-1 seconds, newTimestamp(testTime + 5000), Duration.Inf, None), Array(testPath))

      (11 to 20).foreach(n =>
        dbConnection.set(testPath, new java.sql.Timestamp(testTime + n * 900), n.toString()))

      val test1 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub1.id)))._1
      test1.\\("value").length === 20

      val test2 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub1.id)))._1
      test2.\\("value").length === 0

      dbConnection.set(testPath, newTimestamp(), "21")

      val test3 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub1.id)))._1
      test3.\\("value").length === 1

      val test4 = requestHandler.handleRequest(PollRequest(10.seconds, None, Seq(testSub2.id)))._1
      test4.\\("value").length === 16

    }

    "Subscriptions should be removed from database when their ttl expires" in {
//      val simpletestfile = Source.fromURL(getClass.getClassLoader.getResource("SubscriptionRequest.xml")).getLines.mkString("\n").replaceAll("""ttl="10.0"""", """ttl="1.0"""")
      val parserlist = OmiParser.parse(subscriptionRequest.replaceAll("""ttl="10.0"""", """ttl="1.0""""))
      parserlist.isRight === true
      val requestReturn = requestHandler.handleRequest(parserlist.right.get.head.asInstanceOf[SubscriptionRequest])._1
      val testSub = requestReturn.\\("requestID").text.toInt
      //      val temp = dbConnection.getSub(testSub).get
      dbConnection.getSub(testSub) must beSome
      //      Thread.sleep(3000) //NOTE this might need to be uncommented 
      dbConnection.getSub(testSub) must beNone.eventually(3, new org.specs2.time.Duration(1000))
    }

    /*
     * 
   case class SubscriptionRequest(
  ttl: Duration,
  interval: Duration,
  odf: OdfObjects ,
  newest: Option[ Int ] = None,
  oldest: Option[ Int ] = None,
  callback: Option[ String ] = None
) extends OmiRequest with SubLike with OdfRequest
     */

    "Subscription removing logic should work with large number of subscriptions" in {
      val testPath = Path("Objects/SubscriptionTest/eventTest/SmartOven/pollingtest4")
      val testTime = new Date().getTime - 3000
      dbConnection.set(testPath, new java.sql.Timestamp(testTime), "testValue")
      val testOdfObjects = dbConnection.getNBetween(dbConnection.get(testPath), None, None, None, None)
      
      system.eventStream.publish(Mute(EventFilter.debug(), EventFilter.info()))
//      system.eventStream.publish(Mute(EventFilter.info()))
      
      val start = System.currentTimeMillis()
      val subscriptions = ((1 until 100).toList ::: List(10000)).map { a =>
        //        println("saving sub with ttl " + a + " milliseconds")
        Await.result((subscriptionHandlerRef ? NewSubscription(SubscriptionRequest(a milliseconds, -1 seconds, testOdfObjects.get))).mapTo[Try[Long]], Duration.Inf).get //dbConnection.saveSub(NewDBSub(-1 seconds, newTimestamp(), a milliseconds, None), Array(testPath))
        //        println(s"got $id")
        //        id
      }
      
      system.eventStream.publish(UnMute(EventFilter.debug(),EventFilter.info()))
      val stop = System.currentTimeMillis()
      println("added 100 subs in: " + (stop-start) + "milliseconds")
//      //      }
//      println("sleepin 1000");
      Thread.sleep(1000)

      subscriptions.init.foreach { x =>
        val das = dbConnection.getSub(x)
        das must beNone
      }
      dbConnection.getSub(subscriptions.last) must beSome
    }
    
    
  }
  
  val subscriptionRequest = """<?xml version="1.0" encoding="UTF-8"?>
<omi:omiEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:omi="omi.xsd" xsi:schemaLocation="omi.xsd omi.xsd" version="1.0" ttl="10.0">
  <omi:read msgformat="odf" interval="1">
    <omi:msg xmlns="odf.xsd" xsi:schemaLocation="odf.xsd odf.xsd">
      <Objects>
        <Object>
          <id>ReadTest</id>
          <Object>
            <id>Refrigerator123</id>
              <InfoItem name="PowerConsumption">
              </InfoItem>
          </Object>
        </Object>
       </Objects>
    </omi:msg>
  </omi:read>
</omi:omiEnvelope>
"""
  
  val subscriptionRequestWithLargeInterval = """<?xml version="1.0" encoding="UTF-8"?>
<omi:omiEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:omi="omi.xsd" xsi:schemaLocation="omi.xsd omi.xsd" version="1.0" ttl="10.0">
  <omi:read msgformat="odf" interval="5">
    <omi:msg xmlns="odf.xsd" xsi:schemaLocation="odf.xsd odf.xsd">
      <Objects>
        <Object>
          <id>ReadTest</id>
          <Object>
            <id>Refrigerator123</id>
              <InfoItem name="PowerConsumption">
              </InfoItem>
          </Object>
        </Object>
       </Objects>
    </omi:msg>
  </omi:read>
</omi:omiEnvelope>
"""
  
  val buggyRequest = """<?xml version="1.0" encoding="UTF-8" ?>
<omi:omiEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:omi="omi.xsd" xsi:schemaLocation="omi.xsd omi.xsd" version="1.0" ttl="10">
  <omi:read msgformat="odf" interval="2">
    <omi:msg xmlns="odf.xsd" xsi:schemaLocation="odf.xsd odf.xsd">
      <Objects>
        <Object>
          <id>OMI-Service</id>
          <InfoItem name="NonexistingInfoItem">
          </InfoItem>
        </Object>
      </Objects>
    </omi:msg>
  </omi:read>
</omi:omiEnvelope>
"""


}





