package akka.concurrency

import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.actor.{ActorInitializationException, ActorKilledException, SupervisorStrategy}

import scala.concurrent.duration.Duration

/**
 * Created by taoluo on 10/20/15.
 */
abstract class IsolatedResumeSupervisor(maxNrRetries: Int = -1, withinTimeRange: Duration = Duration.Inf)
      extends IsolatedLifeCycleSupervisor{
  this: SupervisionStrategyFactory =>
  override def supervisorStrategy: SupervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}
