package akka.concurrency

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

/**
 * Created by taoluo on 10/15/15.
 */
object Plane {

  case object GiveMeControl

  case class Controls(controlSurfaces: ActorRef)

}

class Plane extends Actor with ActorLogging {

  import Altimeter._
  import EventSource._
  import Plane._

  val altimeter = context.actorOf(Props[Altimeter], "Altimeter")

  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  val config = context.system.settings.config

  val pilot = context.actorOf(Props[Pilot],
    config.getString("zzz.akka.avionics.flightcrew.pilotName"))

  val copilot = context.actorOf(Props[CoPilot],
    config.getString("zzz.akka.avionics.flightcrew.copilotName"))

  val autopilot = context.actorOf(Props[AutoPilot], "AutoPilot")


  val flightAttendant = context.actorOf(Props(new LeadFlightAttendant with AttendantCreationPolicy),
    config.getString("zzz.akka.avionics.flightcrew.leadAttendantName"))


  override def receive: Receive = {
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! controls
  }

  override def preStart(): Unit = {
    altimeter ! RegisterListener(self)
    List(pilot, copilot) foreach { _ ! Pilots.ReadyToGo }
  }

}