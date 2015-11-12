package akka.concurrency

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.duration._
/**
 * Created by taoluo on 11/8/15.
 */
object HeadingIndicator {
  case class BankChange(amount: Float)
  case class HeadingUpdate(heading: Float)

  def apply() = new HeadingIndicator with ProductionEventSource
}

trait HeadingIndicator extends Actor with ActorLogging {
  this: EventSource =>
  import HeadingIndicator._
  import context._

  case object Tick

  val maxDegPerSec = 5

  val ticker = system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  var lastTick: Long = System.currentTimeMillis()

  var rateOfBank = 0f

  var heading = 0f

  def headingIndicatorReceive: Receive = {
    case BankChange(amount) =>
      rateOfBank = amount.min(1.0f).max(-1.0f)

    case Tick =>
      val tick = System.currentTimeMillis()
      val timeDelta = (tick - lastTick) / 1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      sendEvent(HeadingUpdate(heading))
  }

  override def receive = eventSourceReceive.orElse(headingIndicatorReceive)

  override def postStop() = ticker.cancel()

}

trait HeadingIndicatorProvider{
  def newHeadingIndicator: Actor = HeadingIndicator()
}