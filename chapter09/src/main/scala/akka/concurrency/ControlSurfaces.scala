package akka.concurrency

import akka.actor.{Actor, ActorLogging, ActorRef}

/**
 * Created by taoluo on 10/15/15.
 */
object ControlSurfaces {
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)

  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)
}

class ControlSurfaces(plane: ActorRef, altimeter: ActorRef, heading: ActorRef) extends Actor  with ActorLogging {
  import Altimeter._
  import ControlSurfaces._
  import HeadingIndicator._

  override def receive: Receive = {
    case StickBack(amount) =>
      altimeter ! RateChange(amount)
    case StickForward(amount) =>
      altimeter ! RateChange(-1 * amount)
  }

  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if sender() == somePilot =>
      altimeter ! RateChange(amount)
    case StickForward(amount) if sender() == somePilot =>
      altimeter ! RateChange(-1 * amount)
    case StickLeft(amount) if sender() == somePilot =>
      heading ! BankChange(-1 * amount)
    case StickRight(amount) if sender() == somePilot =>
      heading ! BankChange(amount)
    case HasControl(entity) if sender() == plane =>
      context.become(controlledBy(entity))
  }
}