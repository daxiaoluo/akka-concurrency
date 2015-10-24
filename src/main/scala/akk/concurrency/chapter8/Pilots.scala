package akk.concurrency.chapter8

import akk.concurrency.chapter8.Plane.GiveMeControl
import akka.actor._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * Created by taoluo on 10/18/15.
 */
object Pilots {

  case object ReadyToGo

  case object RelinquishControl

}

class Pilot(plane: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging{

  import Pilots._
  import Plane._

//  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorSelection = ActorSelection(context.system.deadLetters, "")
  var autopilot: ActorSelection = ActorSelection(context.system.deadLetters, "")
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      copilot = context.actorSelection("../" + copilotName)
      log.info(copilot.pathString)
      autopilot = context.actorSelection("../AutoPilot")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class CoPilot(plane: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging{

  import Pilots._
  implicit val timeout = Timeout(5.seconds)


  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  override def receive: Receive = {
    case ReadyToGo =>
      val pilot = Await.result(context.actorSelection("../" + pilotName).resolveOne(), timeout.duration)
      context.watch(pilot)

    case Terminated(_) =>
      plane ! GiveMeControl
  }
}

trait PilotProvider {
  def newPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, controls, altimeter)

  def newCoPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, controls, altimeter)
}