package akka.concurrency

/**
 * Created by taoluo on 10/18/15.
 */


import akka.actor.Actor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }
}
class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>
  import FlightAttendant._
  override def receive: Receive = {
    case GetDrink(drinkname) =>
      context.system.scheduler.scheduleOnce(responseDuration, sender,
        Drink(drinkname))
  }
}
