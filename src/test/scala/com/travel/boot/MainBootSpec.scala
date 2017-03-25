package com.travel.boot

import java.io.File
import java.nio.file.Files

import com.redis.RedisConnectionException
import com.travel.component.{Airport, Coordinates, User}
import com.travel.event.{GeoDistanceCalculation, IndexInRedis}
import org.scalatest.FunSuite
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global
import com.travel.helper.TestHelper._

import scala.util.Success
/**
  * Created by harsh on 25/03/17.
  */
class MainBootSpec extends FunSuite{

  test("Redis connection"){
      assert(new RedisBoot{}.getRedisClient.ping.get == "PONG")
  }
  test("Engine actor"){
    redisClient.flushall
    val future = actorRef ? IndexInRedis(List(Airport(Some("A1"),Coordinates(1.2f,2.1f)),Airport(Some("A2"),Coordinates(9.2f,8.1f))))
    val  Success(result: Option[Int])  = future.mapTo[Option[Int]].value.get
    assert(result.isDefined && result.get == 2)
    val future2 = actorRef ? GeoDistanceCalculation(List(User(Some("U2"),Coordinates(1.1f,1.0f)) ,User(Some("U2"),Coordinates(8.9f,8.7f)) ))
    //val  Success(result2: Int)  = future.mapTo[Int].value.get
    val output = conf.getString("travel-app.out-loc")
    val outputName = conf.getString("travel-app.out-file")
    //TODO: To avoid confusion clear off the file created since it's being appended in the engine.
    val resultFile = new File(output+outputName)
    val content = Files.readAllLines(resultFile.toPath)
    val first = content.get(0).split(",")
    val second = content.get(1).split(",")
    println(s"Nearest airport for ${first(0)} is ${first(1)}")
    println(s"Nearest airport for ${second(0)} is ${second(1)}")
    assert(resultFile.exists() && first(1) == "A1" && second(1) == "A2")
  }
}
