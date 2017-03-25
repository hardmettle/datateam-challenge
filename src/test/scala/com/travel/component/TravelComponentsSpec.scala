package com.travel.component

import org.scalatest.FunSuite
import kantan.csv._
import kantan.csv.ops._
import com.travel.helper.TestHelper._
/**
  * Created by harsh on 25/03/17.
  */
class TravelComponentsSpec extends FunSuite{
  test("Validation of airport and user"){
    val passUser = User(Some("A-ID"),Coordinates(2.0f,1.0f))
    val failUser = User(None,Coordinates(2.0f,1.0f))

    assert(passUser.validateFields.isRight)
    assert(failUser.validateFields.isLeft)

    val passAirport = Airport(Some("A-ID"),Coordinates(2.0f,1.0f))
    val failAirport = Airport(None,Coordinates(2.0f,1.0f))

    assert(passAirport.validateFields.isRight)
    assert(failAirport.validateFields.isLeft)
  }
  test("Validation of airport and user with csvreader"){

    val data = List(""""ID-1",1.0,4.0""",""""ID-2",,4.0""").mkString("\n").asCsvReader[User](rfc).toList
    val head = data.head
    val tail = data.tail.head
    assert(head.isSuccess && tail.isFailure)
  }
}
