package akka.concurrency

import akka.actor.{Actor, ActorRef, Props}

/**
 * Created by taoluo on 10/18/15.
 */

trait AttendantCreationPolicy {
  val numberOfAttendants: Int = 8
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {

  case object GetFlightAttendant

  case class Attendant(a: ActorRef)

  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}


class LeadFlightAttendant extends Actor{
  this: AttendantCreationPolicy =>
  import LeadFlightAttendant._


  override def preStart() {
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList(
      "zzz.akka.avionics.flightcrew.attendantNames").asScala

    attendantNames take numberOfAttendants foreach {
      i => context.actorOf(Props(createAttendant), i)
    }
  }

  def randomAttendant(): ActorRef = {
    context.children.take(
      scala.util.Random.nextInt(numberOfAttendants) + 1).last
  }

  override def receive: Receive = {
    case GetFlightAttendant =>
      sender() ! Attendant(randomAttendant())
    case m =>
      randomAttendant() forward m
  }
}
