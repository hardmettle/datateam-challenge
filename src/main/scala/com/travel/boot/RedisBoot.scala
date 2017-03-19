package com.travel.boot

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.redis.RedisClient
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * Created by harsh on 18/03/17.
  */
trait RedisBoot {

  def getRedisClient(implicit config:Config,actorRefFactory: ActorRefFactory,executionContext:ExecutionContext):RedisClient = {
    implicit val timeout = Timeout(config.getInt("travel-app.redis.timeout") seconds)
    val host:String = config.getString("travel-app.redis.host")
    val port:Int = config.getInt("travel-app.redis.port")
    RedisClient(host, port)
  }
}
