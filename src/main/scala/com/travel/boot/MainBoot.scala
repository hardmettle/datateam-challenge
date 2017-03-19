package com.travel.boot

import akka.actor.ActorSystem
import com.travel.component.{Airport, User}
import com.travel.event.{GeoDistanceCalculation, IndexInRedis}
import com.typesafe.config.ConfigFactory

import scala.reflect.runtime.universe._
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import com.travel.util.{CSVReader, RecordWriter}

import scala.concurrent.{Await, Future}
/**
  * Created by harsh on 18/03/17.
  */
object MainBoot extends App with RedisBoot with CSVDataBoot with DataProcessingEngine {
  implicit val configFactory = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("travel-system")
  implicit val executionContext = actorSystem.dispatcher
  implicit val futureTimeout = Timeout(configFactory.getInt("travel-app.future-timeout") seconds)
  implicit val redisClient = new RedisBoot {}.getRedisClient(configFactory,actorSystem,executionContext)


  val processEngine = DataProcessingEngineActor.props(redisClient,configFactory,executionContext,actorSystem)
  val userData = new CSVDataBoot {}.loadData[User](configFactory)(User.toString.toLowerCase,User.decoder)
  var validatedUsers = List.empty[User]
  userData match {
    case Right(r) =>
      val userWithValidatedField = r.map(_.validateFields)
      if(userWithValidatedField.count(_.isLeft) > 0)
        throw new IllegalArgumentException("One of the user field missing id")
      else
        validatedUsers = r

    case Left(l) =>
      println("There is corrupt data in user file : ")
      l.foreach(println)
      throw new IllegalArgumentException("Inconsistent data in file")
  }
  val airPortData = new CSVDataBoot {}.loadData[Airport](configFactory)(Airport.toString.toLowerCase,Airport.decoder)
  var validatedAirports = List.empty[Airport]
  airPortData match {
    case Right(r) =>
      val userWithValidatedField = r.map(_.validateFields)
      if(userWithValidatedField.count(_.isLeft) > 0)
        throw new IllegalArgumentException("One of the airport field missing id")
      else
        validatedAirports = r

    case Left(l) =>
      println("There is corrupt data in airport file : ")
      l.foreach(println)
      throw new IllegalArgumentException("Inconsistent data in file")
  }
  processEngine ! IndexInRedis(validatedAirports)
  val output = validatedUsers.par.map(user => {
    println(s"Calculating distance for $user")
    val futureDistance = processEngine ? GeoDistanceCalculation(user)
    val d = futureDistance.flatMap(_.asInstanceOf[Future[Either[String,Airport]]])
    (user.id.get,Await.result(d,5 minute))
  })
  val records = output.map(o => (o._1,if(o._2.isLeft) o._2.left.get else o._2.right.get.id.get))
  RecordWriter().writeRecordToFile(records.toList)
}
