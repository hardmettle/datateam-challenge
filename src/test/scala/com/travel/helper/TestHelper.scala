package com.travel.helper

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.travel.boot.{DataProcessingEngine, DataProcessingEngineActor, RedisBoot}
import com.typesafe.config.ConfigFactory
import kantan.csv.RowDecoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
/**
  * Created by harsh on 23/03/17.
  */
case object TestHelper {
  case class Util(s1:String,i:Int)
  case object Util{
    implicit val decoder:RowDecoder[Util] =
      RowDecoder.ordered((s1: String, i:Int) => Util(s1,i))
  }
  implicit val decoderForUser =  com.travel.component.User.decoder
  implicit val decoderForAirport =  com.travel.component.Airport.decoder
  implicit val conf = ConfigFactory.load()
  implicit val redisClient = new RedisBoot{}.getRedisClient
  implicit val system = ActorSystem("test-system")
  implicit val timeout = Timeout(conf.getInt("travel-app.future-timeout") seconds)
  val actorRef = TestActorRef(new DataProcessingEngineActor(redisClient,conf,scala.concurrent.ExecutionContext.Implicits.global))
}
