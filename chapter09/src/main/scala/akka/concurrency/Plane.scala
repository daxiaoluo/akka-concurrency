package akka.concurrency

import akka.actor.SupervisorStrategy.{Restart, Resume}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by taoluo on 10/15/15.
 */
object Plane {

  case object GiveMeControl

  case object LostControl

  case class Controls(controlSurfaces: ActorRef)


  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider with HeadingIndicatorProvider
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
    with PilotProvider
    with LeadFlightAttendantProvider
    with HeadingIndicatorProvider =>
  import Altimeter._
  import IsolatedLifeCycleSupervisor._
  import Plane._

  implicit val timeout = Timeout(5.seconds)

  val config = context.system.settings.config

  val pilotName = config.getString("zzz.akka.avionics.flightcrew.pilotName")
  val copilotName = config.getString("zzz.akka.avionics.flightcrew.copilotName")
  val attendantName = config.getString("zzz.akka.avionics.flightcrew.leadAttendantName")

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 30 seconds) {
    case e: ArithmeticException =>
      if (e.getMessage.startsWith("Altimeter")) Restart else Resume
  }
  override def receive: Receive = {
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! actorForControls("ControlSurfaces")
  }

  override def preStart(): Unit = {
    startControls()
    startPeople()
    context.actorSelection("Controls/Altimeter") ! EventSource.RegisterListener(self)
    context.actorSelection(s"Pilots/$pilotName") ! Pilots.ReadyToGo
    context.actorSelection(s"Pilots/$copilotName") ! Pilots.ReadyToGo
  }

  def actorForControls(name: String) = {
    var controls = context.system.deadLetters
    context.actorSelection("Controls/" + name).resolveOne().onComplete {
      case Success(cp) => controls = cp
      case Failure(e) => throw e
    }
    controls
  }

  def actorForPilots(name: String) ={
    var pilot = context.system.deadLetters
    context.actorSelection("Pilots/" + name).resolveOne().onComplete {
      case Success(cp) => pilot = cp
      case Failure(e) => throw e
    }
    pilot
  }

  def startControls(): Unit = {
    val controls = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        val alt = context.actorOf(Props(newAltimeter), "Altimeter")
        val head = context.actorOf(Props(newHeadingIndicator), "HeadingIndicator")
        context.actorOf(Props(newAutoPilot(self)), "AutoPilot")
        context.actorOf(Props(new ControlSurfaces(self,alt, head)), "ControlSurfaces")
      }
    }), "Controls")
    Await.result(controls ? WaitForStart, 1.second)
  }

  def startPeople() {
    val plane = self
    val control = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        context.actorOf(Props(newPilot(plane, autopilot, control, altimeter)), pilotName)
        context.actorOf(Props(newCoPilot(plane, autopilot, altimeter)), copilotName)
      }
    }), "Pilots")
    context.actorOf(Props(newFlightAttendant), attendantName)
    Await.result((people ? WaitForStart), 1.second)
  }

}