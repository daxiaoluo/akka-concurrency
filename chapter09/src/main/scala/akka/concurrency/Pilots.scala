package akka.concurrency

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._


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

object Pilot {
  import FlyingBehaviour._
  import ControlSurfaces._

  val tipsyCalcElevator: Calculator  = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }

  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m
    }
  }

  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m
    }
  }

  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m
    }
  }

}

class Pilot(plane: ActorRef, autopilot: ActorRef, heading: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging{
  this: DrinkingProvider with FlyingProvider =>
  import Pilots._
  import Pilot._
  import Plane._
  import context._
  import Altimeter._
  import ControlSurfaces._
  import DrinkingBehaviour._
  import FlyingBehaviour._
  import FSM._

  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  override def preStart(): Unit = {
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading, altimeter), "FlyingBehaviour")
  }

  def bootstrap: Receive = {
    case ReadyToGo =>
//      val copilot = actorFor("../" + copilotName)
      val flyer = context.actorSelection("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
      become(sober(autopilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorSelection): Receive = {
    case FeelingSober =>
    case FeelingTipsy =>
      becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod =>
      becomeZaphod(copilot, flyer)
  }

  def tipsy(copilot: ActorRef, flyer: ActorSelection): Receive = {
    case FeelingSober =>
      becomeSober(copilot, flyer)
    case FeelingTipsy =>
    case FeelingLikeZaphod =>
      becomeZaphod(copilot, flyer)
  }

  def zaphod(copilot: ActorRef, flyer: ActorSelection): Receive = {
    case FeelingSober =>
      becomeSober(copilot, flyer)
    case FeelingTipsy =>
      becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod =>
  }

  def becomeSober(copilot: ActorRef, flyer: ActorSelection) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    become(sober(copilot, flyer))
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorSelection) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    become(tipsy(copilot, flyer))
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorSelection) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    become(zaphod(copilot, flyer))
  }

  def idle: Receive = {
    case _ =>
  }

  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_, _, Idle) =>
        become(idle)
      case Transition(_, _, _) =>
      case CurrentState(_, _) =>
      case m => super.unhandled(m)
    }
  }

  def receive = bootstrap
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
  def newPilot(plane: ActorRef, autopilot: ActorRef, heading: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, autopilot, heading, altimeter) with DrinkingProvider with FlyingProvider

  def newCoPilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, autopilot, altimeter)

  def newAutoPilot(plane: ActorRef): Actor = new Autopilot(plane)
}