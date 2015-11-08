package akka.concurrency

import akka.actor._
import akka.actor.Actor.emptyBehavior
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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

  implicit val timeout = Timeout(5.seconds)


  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      context.actorSelection("../" + copilotName).resolveOne().onComplete {
        case Success(cp) => copilot = cp
        case Failure(e) => throw e
      }
      context.actorSelection("../AutoPilot").resolveOne().onComplete {
        case Success(cp) =>
          autopilot = cp
        case Failure(e) => throw e
      }
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class CoPilot extends Actor with ActorLogging{

  import Pilots._

  implicit val timeout = Timeout(5.seconds)


  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

  override def receive: Receive = {
    case ReadyToGo =>
      context.actorSelection("../" + pilotName).resolveOne().onComplete {
        case Success(cp) => pilot = cp
        case Failure(e) => throw e
      }
      context.actorSelection("../AutoPilot").resolveOne().onComplete {
        case Success(cp) => autopilot = cp
        case Failure(e) => throw e
      }
  }
}

class AutoPilot  extends Actor with ActorLogging{
  override def receive: Actor.Receive = emptyBehavior

}

trait PilotProvider {
  def pilot: Actor = new Pilot

  def copilot: Actor = new CoPilot

  def autopilot: Actor = new AutoPilot
}