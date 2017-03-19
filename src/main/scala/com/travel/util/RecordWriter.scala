package com.travel.util

import com.typesafe.config.Config
import kantan.csv.rfc
import kantan.csv.ops._
/**
  * Created by harsh on 20/03/17.
  */
case class RecordWriter() {
  def writeRecordToFile(records:List[(String,String)])(implicit config:Config):Unit = {
    val fileName = config.getString("travel-app.out-file")
    val out = java.io.File.createTempFile(fileName, "csv")
    val writer = out.asCsvWriter[(String,String)](rfc.withHeader("User ID", "IATA"))
    try{
      records.map(record => {
        writer.write(record)
      })
    }catch {
      case e:Exception => println(s"Exception occured while writing output ${e.getMessage}")
    }finally {
      writer.close()
    }
  }
}
