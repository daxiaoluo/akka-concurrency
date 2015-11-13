package akka.concurrency

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor, ActorSystem}
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by taoluo on 11/13/15.
 */

object Waiter {
  case object GetPassengerSupervisor
}

class Waiter extends Actor with ActorLogging {
  import FlightAttendant._
  import Waiter._
  import PassengerSupervisor._

  override def receive: Receive = {
    case GetDrink(drinkname) =>
      val senderPath = sender().path.toString
      log.info(s"($senderPath) ask $drinkname")
      sender() ! Drink(drinkname)
    case GetPassengerSupervisor =>
      log.info("Get passengerSupervisor")
      context.actorSelection("PassengerSupervisor") ! GetPassengerBroadcaster
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    context.actorOf(Props(PassengerSupervisor(self)), "PassengerSupervisor")
  }
}

object Passangers {
  implicit val timeout = Timeout(5.seconds)
  import Waiter._
  val system = ActorSystem("PassangerSimulation")

  val waiter = system.actorOf(Props[Waiter], "waiter")

  def main(args: Array[String]): Unit = {
    system.scheduler.scheduleOnce(1.seconds) {
      waiter ! GetPassengerSupervisor
    }

    system.scheduler.scheduleOnce(3.seconds) {
      waiter ! GetPassengerSupervisor
    }

    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.terminate()
    }
  }
}
