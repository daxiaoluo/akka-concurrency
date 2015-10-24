package akk.concurrency.chapter8

import akka.actor.SupervisorStrategy.{Resume, Restart}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
/**
 * Created by taoluo on 10/15/15.
 */
object Plane {

  case object GiveMeControl

  case class Controls(controlSurfaces: ActorRef)

}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider =>
  import Altimeter._
  import EventSource._
  import Plane._
  import IsolatedLifeCycleSupervisor._

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
//      sender ! controls
  }

  override def preStart(): Unit = {
    startControls()
    startPeople()
    actorForControls("Altimeter") ! EventSource.RegisterListener(self)
    actorForPilots(pilotName) ! Pilots.ReadyToGo
    actorForControls(copilotName) ! Pilots.ReadyToGo
  }

  def actorForControls(name: String) = {
    Await.result(context.actorSelection("Controls/" + name).resolveOne(), timeout.duration)
  }

  def actorForPilots(name: String) ={
    Await.result(context.actorSelection("Pilots/" + name).resolveOne(), timeout.duration)
  }

  def startControls(): Unit = {
    val controls = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        val alt = context.actorOf(Props(newAltimeter), "Altimeter")
        context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
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
        context.actorOf(Props(newPilot(plane, control, altimeter)), pilotName)
        context.actorOf(Props(newCoPilot(plane, control, altimeter)), copilotName)
      }
    }), "Pilots")
    context.actorOf(Props(newFlightAttendant), attendantName)
    Await.result((people ? WaitForStart), 1.second)
  }

}