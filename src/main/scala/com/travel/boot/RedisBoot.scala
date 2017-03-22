package com.travel.boot

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.redis.{RedisClient, RedisClientPool}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * Created by harsh on 18/03/17.
  */

//The framework to boot redis engine.
trait RedisBoot {

  //FIXME: Badly wanted to use RedisClientPool for parallel execution but it has bug and fails for asynchronus executions.
  //FIXME: There was another option to use non blocking library of same author but it had no geo command support :-(
  /** Check [[com.travel.boot.DataProcessingEngineActor.calculateNearestAirport()]]
    * for detailed explanation on limitations
    * Method to return redis client.
    * @param config configuration to aid various implicit and explicit values required in creation of client
    * //@param actorRefFactory
    * @param executionContext
    * @return RedisClient instance
    */
  def getRedisClient(implicit config:Config,executionContext:ExecutionContext): RedisClient = {
    implicit val timeout = Timeout(config.getInt("travel-app.redis.timeout") seconds)
    val host:String = config.getString("travel-app.redis.host")
    val port:Int = config.getInt("travel-app.redis.port")
    //new RedisClientPool("localhost", 6379)
    new RedisClient(host, port)
  }
}
