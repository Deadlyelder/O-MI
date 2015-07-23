package agentSystem

import akka.actor.{ Actor, Props  }
import akka.io.{ IO, Tcp  }
import akka.actor.ActorLogging
import java.net.InetSocketAddress

object InternalAgentCLICmds
{
  case class ReStartCmd(agent: String)
  case class StartCmd(agent: String)
  case class StopCmd(agent: String)
}

import InternalAgentCLICmds._
class InternalAgentCLI(
    sourceAddress: InetSocketAddress
  ) extends Actor with ActorLogging {

  import Tcp._
  def receive = {
    case Received(data) =>{ 
      val dataString = data.decodeString("UTF-8")

      val args = dataString.split(" ")
      args match {
        case Array("start", agent) =>
          log.debug(s"Got start command from $sender for $agent")
          context.parent ! StartCmd(agent.dropRight(1))
        case Array("re-start", agent) =>
          log.debug(s"Got re-start command from $sender for $agent")
          context.parent ! ReStartCmd(agent.dropRight(1))
        case Array("stop", agent) => 
          log.debug(s"Got stop command from $sender for $agent")
          context.parent ! StopCmd(agent.dropRight(1))
        case cmd: Array[String] => log.warning(s"Unknown command from $sender: "+ cmd.mkString(" "))
        case a => log.warning(s"Unknown message from $sender: "+ a) 
      }
    }
  case PeerClosed =>{
    log.info(s"InternalAgent CLI disconnected from $sourceAddress")
    context stop self
  }
  }
}
