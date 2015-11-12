package akka.concurrency

/**
 * Created by taoluo on 11/11/15.
 */

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor, ActorRef}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Passenger {
  case object FastenSeatbelts
  case object UnfastenSeatbelts

  val SeatAssignment = """([\w\s_]+)-(\d+)-([A-Z])""".r
}

trait DrinkRequestProbability {
  val askThreshold = 0.9f
  val requestMin = 20.minutes
  val requestUpper = 30.minutes

  def randomishTime(): FiniteDuration = {
    requestMin + scala.util.Random.nextInt(
      requestUpper.toMillis.toInt).millis
  }
}

trait PassengerProvider {
  def newPassenger(callButton: ActorRef): Actor =
    new Passenger(callButton) with DrinkRequestProbability
}

class Passenger(callButton: ActorRef) extends Actor with ActorLogging {
  this: DrinkRequestProbability =>
  import Passenger._
  import FlightAttendant.{GetDrink, Drink}
  import scala.collection.JavaConverters._

  val r = scala.util.Random

  case object CallForDrink

  val SeatAssignment(myname, _, _) = self.path.name.replaceAllLiterally("_", " ")

  val drinks = context.system.settings.config.getStringList(
    "zzz.akka.avionics.drinks").asScala.toIndexedSeq

  val scheduler = context.system.scheduler

  override def preStart() {
    self ! CallForDrink
  }

  def maybeSendDrinkRequest(): Unit = {
    if(r.nextFloat() > askThreshold) {
      val drinkname = drinks(r.nextInt(drinks.length))
      callButton ! GetDrink(drinkname)
    }
    scheduler.scheduleOnce(randomishTime(), self, CallForDrink)
  }

  override def receive: Receive = {
    case CallForDrink =>
      maybeSendDrinkRequest()
    case Drink(drinkname) =>
      log.info(s"$myname received a $drinkname - Yum")
    case FastenSeatbelts =>
      log.info(s"$myname fastening seatbelt")
    case UnfastenSeatbelts =>
      log.info(s"$myname unfastening seatbelt")
  }
}

