package com.travel.boot

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.redis.RedisClient
import com.travel.component.{Airport, Coordinates, User}
import com.travel.event.{GeoDistanceCalculation, IndexInRedis}

import scala.util.{Failure, Success}
import Airport.customAirportFormat
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by harsh on 18/03/17.
  */
trait DataProcessingEngine

class DataProcessingEngineActor(redisClient: RedisClient,conf:Config,executionContext: ExecutionContext) extends Actor with DataProcessingEngine{
  implicit val timeout = Timeout(conf.getInt("travel-app.redis.timeout") seconds )
  implicit val client = redisClient
  implicit val config = conf
  implicit val econtext = executionContext
  def receive = {
    case IndexInRedis(airports) => DataProcessingEngineActor.indexInRedis(airports)

    case GeoDistanceCalculation(user) => sender() ! DataProcessingEngineActor.calculateNearestAirportDistance (user)

    case _ => println(s"Request not identified.")
  }
}

object DataProcessingEngineActor {

  def props(implicit redisClient: RedisClient,conf:Config,executionContext: ExecutionContext,
            actorSystem: ActorRefFactory): ActorRef =
    actorSystem.actorOf(Props(new DataProcessingEngineActor(redisClient,conf,executionContext)), "processing-actor")

  def indexInRedis(airports: List[Airport])(implicit redisClient: RedisClient,timeout: Timeout,executionContext: ExecutionContext) = {
    airports.foreach { airport =>
      redisClient.set(airport.id.get, airport)(timeout).onComplete {
        case Success(s) => println(s"Loaded $airport")
        case Failure(f) => println(s"Failed to load $airport due to ${f.getMessage}")
      }
    }
  }

  def calculateNearestAirportDistance(user: User)(implicit redisClient: RedisClient,timeout: Timeout,executionContext: ExecutionContext):Future[Either[String,Airport]] = {
    val keys = redisClient.keys("*")(timeout)

    def getNearest(user: User,airportKeys: List[String]):Future[Either[String,Airport]] = {

      val distances = airportKeys.map(a => {
        redisClient.get[Airport](a)(timeout,Airport.customAirportFormat).map(ac => ac match {
          case Some(s) =>
            Right((s,getDistance(user.coordinates,s.coordinates)))
          case None =>
            Left(s"Fetch failed for airport with IATA $a")
        })
      })
      Future.sequence(distances).map(f => {
        val lefts = f.filter(_.isLeft)
        if(lefts.nonEmpty)
          Left(s"Calculation of distance failed for $user because \n " + lefts.map(_.left.get).mkString("\n"))
        else
          Right(f.map(_.right.get).sortWith((a1,a2) => a1._2 > a2._2).head._1)
      })
    }

    def getDistance(c1:Coordinates,c2:Coordinates):Float ={
      val l = Math.pow(c2.longitude - c1.longitude,2.0)
      val r = Math.pow(c2.latitude - c1.latitude,2.0)
      Math.pow(l+r,2.0).toFloat
    }
    keys flatMap ( getNearest(user,_))
  }
}
