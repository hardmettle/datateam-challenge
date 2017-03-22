package com.travel.util

import java.io.IOException

import org.scalatest.FunSuite
import com.travel.helper.TestHelper._
import org.scalatest.Matchers._
/**
  * Created by harsh on 23/03/17.
  */

class CSVReaderSpec extends FunSuite{
  test("Reading file with invalid data"){
    assert(CSVReader[Util]("sample1.csv").readCSVFile().toList.count(_.isFailure) > 0)
  }
  test("Reading missing/invalid file"){
    assertThrows[IOException]{
      CSVReader[Util]("missing.csv").readCSVFile()
    }
  }
}
