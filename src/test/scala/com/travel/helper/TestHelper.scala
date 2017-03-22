package com.travel.helper

import kantan.csv.RowDecoder

/**
  * Created by harsh on 23/03/17.
  */
case object TestHelper {
  case class Util(s1:String,i:Int)
  case object Util{
    implicit val decoder:RowDecoder[Util] =
      RowDecoder.ordered((s1: String, i:Int) => Util(s1,i))
  }
}
