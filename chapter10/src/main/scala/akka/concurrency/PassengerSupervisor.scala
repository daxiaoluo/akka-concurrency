package akka.concurrency

import akka.actor.SupervisorStrategy.{Stop, Resume, Escalate}
import akka.actor._
import akka.routing.BroadcastGroup
import scala.collection.immutable._

/**
 * Created by taoluo on 11/12/15.
 */
object PassengerSupervisor {
  case object GetPassengerBroadcaster

  case class PassengerBroadcaster(broadcaster: ActorRef)

  def apply(callButton: ActorRef) = new PassengerSupervisor(callButton)
    with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef) extends Actor {
  this: PassengerProvider =>
  import PassengerSupervisor._

  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  case class GetChildren(forSomeone: ActorRef)


  case class Children(children: Iterable[ActorRef], childrenFor: ActorRef)

  override def preStart(): Unit = {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }
      override def preStart(): Unit = {
        import scala.collection.JavaConverters._
        import com.typesafe.config.ConfigList
        val passengers = config.getList("zzz.akka.avionics.passengers")
        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList].unwrapped(
          ).asScala.mkString("-").replaceAllLiterally(" ", "_")
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      override def receive: Actor.Receive = {
        case GetChildren(forSomeone: ActorRef) =>
          sender ! Children(context.children, forSomeone)
      }
    }), "PassengersSupervisor")
  }

  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      context.actorSelection("PassengersSupervisor") ! GetChildren(sender())
    case Children(passengers, destinedFor) =>
      val paths = passengers.map { p => p.path.toString }
      val router = context.actorOf(BroadcastGroup(paths).props(), "Passengers")

      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster =>
      sender ! PassengerBroadcaster(router)
  }

  override def receive: Receive = noRouter
}