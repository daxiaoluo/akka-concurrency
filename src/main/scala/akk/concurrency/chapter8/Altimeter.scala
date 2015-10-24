package akk.concurrency.chapter8

import akka.actor.{Actor, Props, ActorLogging}

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
    case Tick => try {
      val tick = System.currentTimeMillis()
      val roc = (rateOfClimb * rateOfClimb) / rateOfClimb
      altitude = altitude + ((tick - lastTick) / 60000.0) * roc
      lastTick = tick
      sendEvent(AltitudeUpdate(altitude))
    } catch {
      case e: ArithmeticException =>
        throw new ArithmeticException(
          "Altimeter: " + e.getMessage)
    }
  }


  override def postStop() = ticker.cancel()
}

trait AltimeterProvider {
  def newAltimeter: Actor = new Altimeter
}