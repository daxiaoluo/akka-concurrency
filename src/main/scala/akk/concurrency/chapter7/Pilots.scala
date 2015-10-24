package akk.concurrency.chapter7

import akka.actor.{ActorLogging, ActorRef, ActorSelection, Actor}

/**
 * Created by taoluo on 10/18/15.
 */
object Pilots {

  case object ReadyToGo

  case object RelinquishControl

}

class Pilot extends Actor with ActorLogging{

  import Pilots._
  import Plane._

  var controls: ActorRef = context.system.deadLetters
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

class CoPilot extends Actor with ActorLogging{

  import Pilots._

  var controls: ActorSelection = ActorSelection(context.system.deadLetters, "")
  var pilot: ActorSelection = ActorSelection(context.system.deadLetters, "")
  var autopilot: ActorSelection = ActorSelection(context.system.deadLetters, "")
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  override def receive: Receive = {
    case ReadyToGo =>
      pilot = context.actorSelection("../" + pilotName)
      autopilot = context.actorSelection("../AutoPilot")
  }
}

trait PilotProvider {
  def pilot: Actor = new Pilot

  def copilot: Actor = new CoPilot
}