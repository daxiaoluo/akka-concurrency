package akk.concurrency.chapter8

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * Created by taoluo on 10/20/15.
 */
object IsolatedLifeCycleSupervisor {
  case object WaitForStart
  case object Started
}


trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  override def receive: Receive = {
    case WaitForStart =>
      sender() ! Started
    case m =>
      throw new Exception(s"Don't call ${self.path.name} directly ($m).")
  }

  def childStarter(): Unit

  final override def preStart() {
    childStarter()
  }

  final override def postRestart(reason: Throwable): Unit = {

  }

  final override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

  }
}