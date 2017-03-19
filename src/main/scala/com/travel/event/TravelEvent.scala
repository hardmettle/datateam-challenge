package com.travel.event


import com.redis.serialization.Format
import com.travel.component.{Airport, User}

/**
  * Created by harsh on 19/03/17.
  */
sealed trait TravelEvent
case class IndexInRedis(airports: List[Airport])
case class GeoDistanceCalculation(user: User)


