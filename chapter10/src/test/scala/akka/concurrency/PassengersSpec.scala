package akka.concurrency

/**
 * Created by taoluo on 11/12/15.
 */

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{WordSpecLike, MustMatchers}

import scala.concurrent.duration._

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0.milliseconds
  override val requestUpper = 2.milliseconds
}
class PassengersSpec extends TestKit(ActorSystem("PassengersSpec")) with ImplicitSender with WordSpecLike with MustMatchers{
  import akka.event.Logging.Info
  import akka.testkit.TestProbe
  import Passenger._

  var seatNumber = 9

  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor)
      with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-B")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString must include ("fastening seatbelt")
      }
    }
  }
}
