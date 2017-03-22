package com.travel.component

import kantan.csv.RowDecoder


/**
  * Created by harsh on 18/03/17.
  */

/**
  * Component wrapping objects involved like user and airports in the application.
  * Uses id as identification.Had to use it as an option due to validation check
  * as the 3rd party api to read csv doesn't return a failure for missing first column which is a string.
  *
  * Components inherit validateFields which helps in detecting missing ids.
  * Returns either error message for the component or the component itself in case of no error.
  *
  * @tparam T Generic type parameter,which can represent Airport or User.
  */
sealed trait TravelComponents[T]{
  val id:Option[String]

  def validateFields:Either[String,T]
}
//Coordinate class to encapsulate coordinates of components i.e latitude and longitudes.
case class Coordinates(longitude:Float,latitude:Float)
//Airport class to model airport component
case class Airport(id:Option[String],coordinates: Coordinates) extends TravelComponents[Airport]{
  val long = coordinates.longitude
  val lat = coordinates.latitude

  override def validateFields =
    if(this.id.isEmpty)
      Left(s"Missing ID for ${this}")
    else
      Right(this)
}
//Helping companion object to invoke implicit row decoder for the csv reader when needed.
case object Airport{
  val decoder:RowDecoder[Airport] =
    RowDecoder.ordered((s: Option[String], la:Float, lo: Float) => Airport(s, Coordinates(lo, la)))
}
//User class to model user component
case class User(id:Option[String],coordinates: Coordinates) extends TravelComponents[User]{

  val long = coordinates.longitude
  val lat = coordinates.latitude

  override def validateFields =
    if(this.id.isEmpty)
      Left(s"Missing ID for ${this}")
    else
      Right(this)
}
//Helping companion object to invoke implicit row decoder for the csv reader when needed.
case object User{
  val decoder:RowDecoder[User] =
    RowDecoder.ordered((s: Option[String], la:Float, lo: Float) => User(s, Coordinates(lo, la)))
}
