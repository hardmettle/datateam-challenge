package com.travel.boot

import java.io.{FileWriter, IOException}

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.redis.RedisClient
import com.travel.component.{Airport, User}
import com.travel.event.{GeoDistanceCalculation, IndexInRedis}
import scala.collection.parallel.ForkJoinTaskSupport
import com.typesafe.config.Config
import scala.concurrent.{ExecutionContext}
/**
  * Created by harsh on 18/03/17.
  */

/**
  * Framework of processing engine of the application.
  * Designed in a way that any other batch processing engine can take its place(eg: Spark etc)
  */
trait DataProcessingEngine

/**
  * Actor for the engine which runs asynchronously to execute jobs that are sent to it.
  * @param redisClient Has a reference to redis engine.
  * @param conf Uses the configuration to get all parameters for execution of various jobs
  * @param executionContext Execution context in which the application is running
  */
class DataProcessingEngineActor(redisClient: RedisClient,conf:Config,executionContext: ExecutionContext) extends Actor with DataProcessingEngine{

  implicit def client = redisClient
  implicit val config = conf
  implicit val econtext = executionContext
  implicit val parallel = conf.getInt("travel-app.parallel")

  def receive = {
    case IndexInRedis(airports) => sender() ! DataProcessingEngineActor.indexInRedis(airports)

    case GeoDistanceCalculation(user) => sender() ! DataProcessingEngineActor.calculateNearestAirport (user)

    case _ => println(s"Request not identified.")
  }
}
//Helper companion object which takes care of instantiation of actor with constructor parameters
// and encompasses business logic of execution of various request sent to the actor.
object DataProcessingEngineActor {

  //Instantiates actor.
  def props(implicit redisClient: RedisClient, conf:Config, executionContext: ExecutionContext,
            actorSystem: ActorRefFactory): ActorRef =
    actorSystem.actorOf(Props(new DataProcessingEngineActor(redisClient,conf,executionContext)), "processing-actor")

  //Takes a list of airports and indexes it into redis with geo commands.Returns option of number of records indexed
  def indexInRedis(airports: List[Airport])(implicit redisClient:RedisClient,
                                            executionContext: ExecutionContext):Option[Int] =
    //redisClient.withClient[Option[Int]](client => {
    redisClient.geoadd("Airports",airports.map(a => (a.coordinates.longitude.toString,a.coordinates.latitude.toString,a.id.get.toString)))
    //})


  //Helper method to calculate nearest airport for each user input.Returns the count of input processed successfully
  def calculateNearestAirport(users:List[User])(implicit parallel:Int,config: Config,redisClient: RedisClient):Int = {
    //FIXME: HUGE HUGE HUGE !! drawback here due to API restrictions.
    //FIXME: Idea was to use parallel collection in scala to do batch processing in parallel asynchronously
    //FIXME: But the API is blocking and has a bug which throws an exception.
    //FIXME: Similar api of same author available which is non blocking,but has missing geo commands.
    //FIXME: Another workaround was to use RedisClientPool but it has another bug so doesn't work asynchronously
    //FIXME: Finally had to settle with linear approach which does the job but takes ages to complete.
    //============================ This is of no use :( ===============================================
    val parUsers = users.par
    parUsers.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(parallel))
    //============//============//============//============//============//============//==============
    val output = config.getString("travel-app.out-loc")
    val outputName = config.getString("travel-app.out-file")
    val fw = new FileWriter(output+outputName, true)
    var count = 0
    //redisClients.withClient[Unit](client => {
    users.foreach(us => {
      println(s"Finding distance for $us")
      //Fun fact : 40,050 km used here since this is beyond the maximum circumference of earth
      // (40,030km polar circumference).
      val dist = redisClient.georadius("Airports",s"${us.coordinates.longitude}",
        s"${us.coordinates.latitude}","40050","km",false,false,false,Some(1),Some("ASC"),None,None)
      dist match {
        case Some(di) => try{
          di.head match {
            case Some(d) =>
              //If all is ok write output to file.
              fw.write(List(us.id.get,d.member.get).mkString(",")+"\n")
              fw.flush()
              count = count + 1
            case None => println(s"No nearest airport found for $us")
          }
        }catch {
          case ie:IOException =>
            println(s"I/O exception occurred while writing (${List(us.id.get,di.mkString(" ")).mkString(",")}) to file: ${ie.getMessage}")
          case _:Exception =>
            println(s"Exception occurred while writing (${List(us.id.get,di.mkString(" ")).mkString(",")}) to file")
        }
        case None => println(s"No nearest airport found for $us")
      }
    })
  //})
    count
  }
}
