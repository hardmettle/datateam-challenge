package com.travel.boot

import akka.actor.ActorSystem
import com.travel.component.{Airport, User}
import com.travel.event.{GeoDistanceCalculation, IndexInRedis}
import com.typesafe.config.ConfigFactory
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{Failure, Success}

/**
  * Created by harsh on 18/03/17.
  */
//Main entry point of the application
object MainBoot extends App with RedisBoot with CSVDataBoot with DataProcessingEngine {

  //All implicit values required for the application are loaded here
  implicit val configFactory = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("travel-system")
  implicit val executionContext = actorSystem.dispatcher
  implicit val futureTimeout = Timeout(configFactory.getInt("travel-app.future-timeout") minutes)
  implicit val redisClient = new RedisBoot {}.getRedisClient(configFactory,executionContext)

  val processEngine = DataProcessingEngineActor.props(redisClient,configFactory,executionContext,actorSystem)

  //Load initially validated User data from CSV input file
  val userData = new CSVDataBoot {}.loadData[User](configFactory)(User.toString.toLowerCase,User.decoder)

  // Ugly, but validation for missing id and duplicate entries is done here
  // since 3rd party api doesnt provide any support for it.
  var validatedUsers = List.empty[User]
  try{
    userData match {
      case Right(r) =>
          val userWithValidatedField = r.map(_.validateFields)
          if(userWithValidatedField.count(_.isLeft) > 0)
            throw new IllegalArgumentException(s"One or more of the user field missing id " +
              s"${userWithValidatedField.filter(_.isLeft).mkString("\n")}")
          else{
            val duplicates = r.par.groupBy(_.id.get).collect{case (x,List(_,_,_*)) =>x}
            if(duplicates.nonEmpty){
              throw new IllegalArgumentException(s"Duplicate entries in user data for keys:" +
                s"${duplicates.mkString("\n")}")
            }else
            validatedUsers = r
          }
      case Left(l) =>
        println("There is corrupt data in user file : ")
        throw new IllegalArgumentException(s"${l.mkString("\n")}")
    }
  }catch {
    case ex:Exception =>
      println(s"Validation failed for user data : ${ex.getMessage}")
      //Choose to exit in case of failed validation
      //System.exit(0)
  }

  //Load initial validated Airport data from CSV input file
  val airPortData = new CSVDataBoot {}.loadData[Airport](configFactory)(Airport.toString.toLowerCase,Airport.decoder)

  // Ugly, but validation for missing id and duplicate entries is done here
  // since 3rd party api doesnt provide any support for it.
  var validatedAirports = List.empty[Airport]
  try{
    airPortData match {
      case Right(r) =>
        val airportWithValidatedField = r.map(_.validateFields)
        if (airportWithValidatedField.count(_.isLeft) > 0)
          throw new IllegalArgumentException("One or more of the airport field missing id" +
            s"${airportWithValidatedField.filter(_.isLeft).mkString("\n")}")
        else {
          val duplicates = r.groupBy(_.id.get).collect { case (x, List(_, _, _*)) => x }
          if (duplicates.nonEmpty) {
            throw new IllegalArgumentException(s"Duplicate entries in airport data for keys:" +
              s"${duplicates.mkString("\n")}")
          } else
            validatedAirports = r
        }

      case Left(l) =>
        println("There is corrupt data in airport file : ")
        throw new IllegalArgumentException(s"${l.mkString("\n")}")
    }
  }catch {
      case ex:Exception => println(s"Validation failed for airport data : ${ex.getMessage}")
      //Choose to exit in case of failed validation
      //System.exit(0)
    }
  //Ask processing engine to index airport data in Redis.
  try{
      (processEngine ? IndexInRedis(validatedAirports)).mapTo[Option[Int]] onComplete{f => f match {
          case Success(s) => s match {
            case Some(i) => println(s"Number of airports indexed without duplicates $i")
            case None => throw new IllegalStateException("None of the airport got indexed.")
          }
        case Failure(fa) => throw new IllegalStateException(s"Redis I/O exception: ${fa.getMessage}")
      }
    }
  }catch {
    case is:IllegalStateException =>
      println(s"Exception while caching :${is.getMessage}")
      //Choose to exit in case of failed indexing.
      //System.exit(0)
  }
  //Ask processing engine to calculate and write to file the nearest airport for input user data.
  (processEngine ? GeoDistanceCalculation(validatedUsers)).mapTo[Int].onComplete(int => int match {
    case Success(i) => println(s"Successfully written nearest distances for all users.")
    case Failure(f) => println(s"Couldn't write nearest distances for users")
  })
}
