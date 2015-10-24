package akk.concurrency.chapter7

import akka.actor.{Actor, ActorLogging, ActorRef}

/**
 * Created by taoluo on 10/15/15.
 */
object ControlSurfaces {
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
}

class ControlSurfaces(altimeter: ActorRef) extends Actor  with ActorLogging {
  import Altimeter._
  import ControlSurfaces._
  override def receive: Receive = {
    case StickBack(amount) =>
      altimeter ! RateChange(amount)
    case StickForward(amount) =>
      altimeter ! RateChange(-1 * amount)
  }
}