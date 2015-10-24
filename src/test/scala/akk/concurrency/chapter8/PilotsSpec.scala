package akk.concurrency.chapter8

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.{MustMatchers, WordSpecLike}
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * Created by taoluo on 10/24/15.
 */

class FakePilot extends Actor {
  override def receive: Receive = {
    case _ =>
      throw new Exception("This exception is expected.")
  }
}

class NilActor extends Actor {
  override def receive: Actor.Receive = {
    case _ =>
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
    zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
    zzz.akka.avionics.flightcrew.pilotName = "$pilotName""""
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec",
  ConfigFactory.parseString(PilotsSpec.configStr)))
                with ImplicitSender
                with WordSpecLike
                with MustMatchers{
  import PilotsSpec._
  import Plane._

  def nilActor = system.actorOf(Props[NilActor])

  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def pilotsReadyToGo(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(Props(new IsolatedStopSupervisor
      with OneForOneStrategyFactory {
      override def childStarter(): Unit = {
        context.actorOf(Props[FakePilot], pilotName)
        context.actorOf(Props(new CoPilot(testActor, nilActor,
          nilActor)), copilotName)
      }
    }), "TestPilots")

    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    system.actorSelection(copilotPath) ! Pilots.ReadyToGo
    a
  }

  "CoPilot" should {
    "take control when the Pilot dies" in {
      implicit val timeout = Timeout(5.seconds)
      pilotsReadyToGo()
      system.actorSelection(pilotPath) ! "throw"

      expectMsg(GiveMeControl)
      val copilot = Await.result(system.actorSelection(copilotPath).resolveOne(), timeout.duration)
      lastSender must be (copilot)
    }
  }

}
