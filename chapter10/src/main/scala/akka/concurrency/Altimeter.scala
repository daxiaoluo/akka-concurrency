package akka.concurrency

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by taoluo on 10/15/15.
 */
object Altimeter {
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)

  case class CalculateAltitude(lastTick: Long, tick: Long, roc: Double)
  case class AltitudeCalculated(altitude: Double)

  def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>
  import Altimeter._
  val ceiling = 43000
  val maxRateOfClimb = 5000
  var rateOfClimb: Float = 0
  var altitude: Double = 0
  var lastTick = System.currentTimeMillis
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis,
    self, Tick)

  override val supervisorStrategy = OneForOneStrategy(-1, Duration.Inf){case _ => Resume}

  val altitudeCalculator = context.actorOf(Props(new Actor {
    override def receive: Actor.Receive = {
      case CalculateAltitude(lastTick, tick, roc) =>
        if(roc == 0) throw new Exception("Divide by zero")
        val alt = ((tick - lastTick) / 60000.0) * (roc * roc) / roc
        sender() ! AltitudeCalculated(alt)
    }
  }), "AltitudeCalculator")

  case object Tick

  override def receive = eventSourceReceive orElse altimeterReceive

  def altimeterReceive: Receive = {
    case RateChange(amount) =>
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log.info(s"Altimeter changed rate of climb to $rateOfClimb.")
    case Tick =>
      val tick = System.currentTimeMillis()
      altitudeCalculator ! CalculateAltitude(lastTick, tick, rateOfClimb)
      lastTick = tick

    case AltitudeCalculated(altitudeDelta) =>
      sendEvent(AltitudeUpdate(altitudeDelta))

  }


  override def postStop() = ticker.cancel()
}

trait AltimeterProvider {
  def newAltimeter: Actor = Altimeter()
}