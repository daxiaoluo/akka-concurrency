package akka.concurrency

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Created by taoluo on 10/18/15.
 */
object Pilots {

  case object ReadyToGo

  case object RelinquishControl

  case object RequestCopilot
  case object TakeControl
  case class CopilotReference(copilot: ActorRef)

}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging{

  import Pilots._
  import Plane._

  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      plane ! Plane.GiveMeControl
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class CoPilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging{

  import Pilots._
  import Plane._
  implicit val timeout = Timeout(5.seconds)


  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  override def receive: Receive = {
    case ReadyToGo =>
//      var pliot = context.system.deadLetters
//      context.actorSelection(s"../$pilotName").resolveOne().onComplete {
//        case Success(cp) => pliot = cp
//        case Failure(e) => throw e
//      }
//      context.watch(pliot)
      context.watch(autopilot)

    case Terminated(_) =>
      plane ! GiveMeControl
  }
}

class Autopilot(plane: ActorRef) extends Actor {
  import Pilots._
  import Plane._
  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      plane ! GiveMeControl

    case CopilotReference(copilotFromPlane) =>
      copilot = copilotFromPlane
      context.watch(copilot)

    case Terminated(_) => plane ! GiveMeControl

    case TakeControl => plane ! GiveMeControl

    case Controls(controlSurfaces) => controls = controlSurfaces

  }
}

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, autopilot, controls, altimeter)

  def newCoPilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, autopilot, altimeter)

  def newAutoPilot(plane: ActorRef): Actor = new Autopilot(plane)
}