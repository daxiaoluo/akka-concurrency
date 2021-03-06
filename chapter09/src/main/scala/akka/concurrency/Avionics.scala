package akka.concurrency

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by taoluo on 10/15/15.
  */
object Avionics {
   implicit val timeout = Timeout(5.seconds)
   val system = ActorSystem("PlaneSimulation")
   val plane = system.actorOf(Props(Plane()), "Plane")

   def main(args: Array[String]): Unit = {
     println(plane.path)
     val control = Await.result(
       (plane ? Plane.GiveMeControl).mapTo[ActorRef],
     5.seconds
     )

     system.scheduler.scheduleOnce(200.millis) {
       control ! ControlSurfaces.StickBack(1f)
     }

     system.scheduler.scheduleOnce(1.seconds) {
       control ! ControlSurfaces.StickBack(0f)
     }

     system.scheduler.scheduleOnce(3.seconds){
       control ! ControlSurfaces.StickBack(.5f)
     }

     system.scheduler.scheduleOnce(4.seconds) {
       control ! ControlSurfaces.StickBack(0f)
     }

     // Shut down
     system.scheduler.scheduleOnce(5.seconds) {
       system.terminate()
     }


   }
 }
