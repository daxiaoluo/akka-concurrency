package akk.concurrency.chapter8

import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.actor.{ActorKilledException, ActorInitializationException, SupervisorStrategy}

import scala.concurrent.duration.Duration

/**
 * Created by taoluo on 10/20/15.
 */
abstract class IsolatedStopSupervisor(maxNrRetries: Int = -1, withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor{
  this: SupervisionStrategyFactory =>
  override def supervisorStrategy: SupervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Stop
    case _ => Escalate
  }
}
