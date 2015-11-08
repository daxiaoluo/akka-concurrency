package akka.concurrency

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}

/**
 * Created by taoluo on 11/8/15.
 */


class PilotSpec extends TestKit(ActorSystem("PilotSpec"))
with ImplicitSender
with WordSpecLike
with MustMatchers {

  import DrinkingBehaviour._
  import FlyingBehaviour._

  def nilActor = system.actorOf(Props[NilActor])

  def makePilot() = {
    val a = TestActorRef[Pilot](
      new Pilot(nilActor, nilActor, nilActor, nilActor) with DrinkingProvider
                                                        with FlyingProvider)
    (a, a.underlyingActor)
  }

  "Pilot.becomeZaphod" should {
    "send new zaphodCalcElevator and zaphodCalcAilerons to FlyingBehaviour" in {
      val (ref, a) = makePilot()
//      a.context.become(a.sober(nilActor, testActor))
      ref ! FeelingLikeZaphod
      expectMsgAllOf(NewElevatorCalculator(Pilot.zaphodCalcElevator),
        NewBankCalculator(Pilot.zaphodCalcAilerons))
    }
  }

//  "Pilot.becomeTipsy" should {
//    "send new tipsyCalcElevator and tipsyCalcAilerons to FlyingBehavior" in {
//      val (ref, a) = makePilot()
//      a.context.become(a.sober(nilActor, testActor))
//      ref ! FeelingTipsy
//      expectMsgAllClassOf(classOf[NewElevatorCalculator],
//        classOf[NewBankCalculator]) foreach {
//        case NewElevatorCalculator(f) =>
//          f should be (Pilot.tipsyCalcElevator)
//        case NewBankCalculator(f) =>
//          f should be (Pilot.tipsyCalcAilerons)
//      }
//    }
//  }
}
