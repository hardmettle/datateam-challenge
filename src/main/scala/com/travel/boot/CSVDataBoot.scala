package com.travel.boot

import com.travel.util.CSVReader
import com.typesafe.config.Config
import kantan.csv._


import scala.reflect.runtime.universe._
/**
  * Created by harsh on 18/03/17.
  */
trait CSVDataBoot {
  def loadData[T](config: Config)(implicit  name: String,rowDecoder: RowDecoder[T]):Either[List[String],List[T]] = {
    val key = s"travel-app.$name.filename"
    val fileName = config.getString(key)
    CSVReader[T](fileName).validateData()
  }
}
