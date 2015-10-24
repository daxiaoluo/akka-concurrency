package akk.concurrency.chapter7

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
   val plane = system.actorOf(Props[Plane], "Plane")

   def main(args: Array[String]): Unit = {
     println(plane.path)
     val control = Await.result(
       (plane ? Plane.GiveMeControl).mapTo[ActorRef],
     5.seconds
     )

     // Shut down
     system.scheduler.scheduleOnce(5.seconds) {
       system.terminate()
     }


   }
 }
