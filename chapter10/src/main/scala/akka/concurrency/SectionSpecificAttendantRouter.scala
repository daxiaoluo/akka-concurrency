package akka.concurrency

import akka.actor.{SupervisorStrategy, ActorSystem}
import akka.dispatch.Dispatchers
import akka.routing._

import scala.collection.immutable.IndexedSeq

/**
 * Created by taoluo on 11/12/15.
 */
class SectionSpecificAttendantRoutingLogic(nbrCopies: Int) extends RoutingLogic  {
  this: LeadFlightAttendantProvider =>

  val roundRobin = RoundRobinRoutingLogic()
  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
    val targets = (1 to nbrCopies).map(_ => roundRobin.select(message, routees))
    SeveralRoutees(targets)
  }
}


