package akk.concurrency.chapter8

import akka.actor.Props

/**
 * Created by taoluo on 10/18/15.
 */
object FlightAttendantPathChecker {
  def main(args: Array[String]) {
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val lead = system.actorOf(Props(
      new LeadFlightAttendant with AttendantCreationPolicy),
      "LeadFlightAttendant")
    Thread.sleep(2000)
    system.terminate()
  }
}
