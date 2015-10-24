package akk.concurrency.chapter8

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{OneForOneStrategy, SupervisorStrategy, SupervisorStrategyConfigurator}

/**
 * Created by taoluo on 10/19/15.
 */
class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator{
  override def create(): SupervisorStrategy = {
    OneForOneStrategy() {
      case _ => Resume
    }
  }
}
