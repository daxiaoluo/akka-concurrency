package akka.concurrency

import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import akka.testkit.{TestFSMRef, ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}

/**
 * Created by taoluo on 11/8/15.
 */
class NilActor extends Actor {
  override def receive: Actor.Receive = {
    case _ =>
  }
}

class FlyingBehaviourSpec extends TestKit(ActorSystem("FlyingBehaviourSpec"))
with ImplicitSender
with WordSpecLike
with MustMatchers {
  import FlyingBehaviour._
  import HeadingIndicator._
  import Altimeter._
  import Plane._

  def nilActor = system.actorOf(Props[NilActor])
  val target = CourseTarget(0,0,0)

  def fsm(plane: ActorRef = nilActor,
          heading: ActorRef = nilActor,
          altimeter: ActorRef = nilActor) = {
    TestFSMRef(new FlyingBehaviour(plane, heading, altimeter))
  }

  "FlyingBehaviour" should {
    "start in the Idle state and with Uninitialized data" in {
      val a = fsm()
      a.stateName must be(Idle)
      a.stateData must be(Uninitialized)
    }
  }

  "PreparingToFly state" should {
    "stay in PreparingToFly state when only a HeadingUpdate is received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a.stateName must be(PreparingToFly)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.status.altitude must be(-1)
      sd.status.heading must be(20)
    }
  }

  "move to Flying state when all parts are received" in {
    val a = fsm()
    a ! Fly(target)
    a ! HeadingUpdate(20)
    a ! AltitudeUpdate(20)
    a ! Controls(testActor)
    a.stateName must be(Flying)
    val sd = a.stateData.asInstanceOf[FlightData]
    sd.controls must be(testActor)
    sd.status.altitude must be(20)
    sd.status.heading must be(20)
  }

  "transitioning to Flying state" should {
    "create the Adjustment timer" in {
      val a = fsm()
      a.setState(PreparingToFly)
      a.setState(Flying)
      a.isTimerActive("Adjustment") must be(true)
    }
  }
}

