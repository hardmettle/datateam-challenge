package com.travel.component

import com.redis.serialization.Format
import kantan.csv.RowDecoder


/**
  * Created by harsh on 18/03/17.
  */

/**
  * component wrapping objects involved like user and airports
  */

sealed trait TravelComponents[T]{
  val id:Option[String]

  def validateFields:Either[String,T]
}

case class Coordinates(longitude:Float,latitude:Float)

case class Airport(id:Option[String],coordinates: Coordinates) extends TravelComponents[Airport]{

  val long = coordinates.longitude
  val lat = coordinates.latitude

  override def validateFields =
    if(this.id.isEmpty)
      Left(s"Missing ID for ${this}")
    else
      Right(this)


}
case object Airport{
  implicit val customAirportFormat =
    new Format[Airport]{
      def read(str: String) = {
        val head :: rest = str.split('|').toList
        val id = Some(head)
        val long :: lati :: Nil = rest

        Airport(id, Coordinates(long.toFloat,lati.toFloat))
      }

      def write(airport: Airport) = {
        import airport._

        s"${id.get}|$long|$lat"
      }
    }
  val decoder:RowDecoder[Airport] =
    RowDecoder.ordered((s: Option[String], lo:Float, la: Float) => Airport(s, Coordinates(lo, la)))
}
case class User(id:Option[String],coordinates: Coordinates) extends TravelComponents[User]{

  val long = coordinates.longitude
  val lat = coordinates.latitude

  override def validateFields =
    if(this.id.isEmpty)
      Left(s"Missing ID for ${this}")
    else
      Right(this)
}
case object User{
  val decoder:RowDecoder[User] =
    RowDecoder.ordered((s: Option[String], lo:Float, la: Float) => User(s, Coordinates(lo, la)))
}
