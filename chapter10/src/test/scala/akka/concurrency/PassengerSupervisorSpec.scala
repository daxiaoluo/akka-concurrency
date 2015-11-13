package akka.concurrency

import akka.actor.{Props, ActorSystem, Actor, ActorRef}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{MustMatchers, BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

/**
 * Created by taoluo on 11/12/15.
 */

object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString(
    """
    zzz.akka.avionics.passengers = [
        [ "Kelly Franqui", "23", "A" ],
        [ "Tyrone Dotts", "23", "B" ],
        [ "Malinda Class", "23", "C" ],
        [ "Kenya Jolicoeur", "24", "A" ],
        [ "Christian Piche", "24", "B" ]
    ]
    """)
}

trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

class PassengerSupervisorSpec extends TestKit(ActorSystem("PassengerSupervisorSpec",
  PassengerSupervisorSpec.config))
with ImplicitSender
with WordSpecLike
with BeforeAndAfterAll
with MustMatchers {
  import PassengerSupervisor._
  override def afterAll(): Unit = {
    system.terminate()
  }

  "PassengerSupervisor" should {
    "work" in {
      val a = system.actorOf(Props(new PassengerSupervisor(testActor)
        with TestPassengerProvider))

      a ! GetPassengerBroadcaster

      val broadcaster = expectMsgPF() {
        case PassengerBroadcaster(b) =>
          b ! "Hithere"
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectNoMsg(100.milliseconds)
          b
      }

      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }
}
