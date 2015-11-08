package akka.concurrency

import akka.actor.{AllForOneStrategy, OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.Decider

import scala.concurrent.duration.Duration

/**
 * Created by taoluo on 10/20/15.
 */

trait SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int,
                   withinTimeRange: Duration)(decider: Decider): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int,
                   withinTimeRange: Duration)(decider: Decider): SupervisorStrategy =
  OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

trait AllForOneStrategyFactor extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int,
                   withinTimeRange: Duration)(decider: Decider): SupervisorStrategy =
  AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}
