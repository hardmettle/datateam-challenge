package com.travel.util

import java.net.URL

import kantan.csv._
import kantan.csv.ops._

/**
  * Created by harsh on 18/03/17.
  */

case class CSVReader[T](filePath:String)(implicit rowDecoder: RowDecoder[T]) {

  private def getFileURL:URL = getClass.getResource(s"/${this.filePath}")
  println(getClass.getResource(s"/${this.filePath}"))
  def readCSVFile():CsvReader[ReadResult[T]] = {
    getFileURL.asCsvReader[T](rfc.withHeader)
  }
  def validateData():Either[List[String],List[T]] = {
    var listOfRecords = List.empty[T]
    var listOfErrors = List.empty[String]
    readCSVFile().toIterator.toList.zipWithIndex.foreach{record =>
      record._1 match {
        case Success(s) => listOfRecords = s :: listOfRecords
        case Failure(v) => listOfErrors = s"${v.getMessage} at ${record._2}" :: listOfErrors
      }
    }
    if(listOfErrors.nonEmpty)
      Left(listOfErrors)
    else Right(listOfRecords)
  }
}
/*case class B(i:Int,i2:Int)

case class A(s:String,b:B){
     implicit val aDecoder: RowDecoder[A] =
       RowDecoder.ordered((s: String, i: Int, i2: Int) => A(s, B(i, i2)))
     def printIt =
         """S1,1,2
      ,E,4""".asCsvReader[A](rfc).foreach(r => r match {
           case Success(s) => println(s)
           case Failure(v) => println(v.getMessage)
         })
   }*/

