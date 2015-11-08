package akka.concurrency

/**
 * Created by taoluo on 10/18/15.
 */


import akka.actor.{Cancellable, ActorRef, Actor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)

  case class Assist(passenger: ActorRef)
  case object Busy_?
  case object Yes
  case object No

  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }
}
class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>
  import FlightAttendant._

  case class DeliverDrink(drink: Drink)

  var pendingDelivery: Option[Cancellable] = None


  def scheduleDelivery(drinkname: String): Cancellable = {
    context.system.scheduler.scheduleOnce(responseDuration, self, DeliverDrink(Drink(drinkname)))
  }

  def assistInjuredPassenger: Receive = {
    case Assist(passenger) =>
      pendingDelivery.foreach(_.cancel())
      pendingDelivery = None
      passenger ! Drink("Magic Healing Potion")
  }

  def handleDrinkRequests: Receive = {
    case GetDrink(drinkname) =>
      pendingDelivery = Some(scheduleDelivery(drinkname))
      context.become(assistInjuredPassenger.orElse(handleSpecificPerson(sender())))

    case Busy_? =>
      sender ! No
  }

  def handleSpecificPerson(person: ActorRef): Receive = {
    case GetDrink(drinkname) if sender() == person =>
      pendingDelivery.foreach(_.cancel())
      pendingDelivery = Some(scheduleDelivery(drinkname))

    case DeliverDrink(drink) =>
      person ! drink
      pendingDelivery = None
      context.become(assistInjuredPassenger.orElse(handleDrinkRequests))

    case m: GetDrink =>
      context.parent.forward(m)

    case Busy_? =>
      sender() ! Yes
  }

  override def receive: Receive = assistInjuredPassenger.orElse(handleDrinkRequests)
}
