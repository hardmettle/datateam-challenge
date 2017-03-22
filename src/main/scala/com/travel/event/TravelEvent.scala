package com.travel.event

import com.travel.component.{Airport, User}

/**
  * Created by harsh on 19/03/17.
  */

//Events which can be triggered in the application.More can be added based on the requirement.
sealed trait TravelEvent

//Event to index airport data in redis.
case class IndexInRedis(airports: List[Airport])
//Event to calculate geodistance data for input user data.
case class GeoDistanceCalculation(user: List[User])



