package akka.concurrency

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by taoluo on 10/15/15.
 */
object Altimeter {
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
}

class Altimeter extends Actor with ActorLogging with EventSource{
  import Altimeter._
  val ceiling = 43000
  val maxRateOfClimb = 5000
  var rateOfClimb: Float = 0
  var altitude: Double = 0
  var lastTick = System.currentTimeMillis
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis,
    self, Tick)

  case object Tick

  override def receive = eventSourceReceive orElse altimeterReceive

  def altimeterReceive: Receive = {
    case RateChange(amount) =>
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log.info(s"Altimeter changed rate of climb to $rateOfClimb.")
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
      lastTick = tick
      sendEvent(AltitudeUpdate(altitude))
  }


  override def postStop() = ticker.cancel()
}