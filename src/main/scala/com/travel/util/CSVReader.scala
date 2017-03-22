package com.travel.util

import java.net.URL

import kantan.csv._
import kantan.csv.ops._

/**
  * Created by harsh on 18/03/17.
  */

/**
  * [[CSVReader]] Generic class to read CSVs.Uses 3rd party application kantan.csv
  * @param filePath Location of csv
  * @param rowDecoder implicit deserializer for csv
  * @tparam T Generic type to be read.
  */
case class CSVReader[T](filePath:String)(implicit rowDecoder: RowDecoder[T]) {
  /**
    * Method to get csv file url which is to be read.
    * @return file url to be read.
    */
  private def getFileURL:URL = getClass.getResource(s"/${this.filePath}")

  /**
    * Uses kantan csv api to read csv file generically.
    * @return reader of the file containing rows as results
    */
  def readCSVFile():CsvReader[ReadResult[T]] = {
    getFileURL.asCsvReader[T](rfc.withHeader)
  }

  /**
    * Method to check validity of the file.The 3rd party api returns a wrapper Failure
    * @return Either list of each errors for respective rows or list of well read records
    */
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
