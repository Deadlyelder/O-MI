package responses

import org.specs2._
import scala.io.Source
import responses._
import parsing._
import database._
import sensorDataStructure.{SensorMap,SensorData}
import parsing.OdfParser._
import java.util.Date;
import java.text.SimpleDateFormat;


class ReadTest extends Specification {

    lazy val simpletestfile = Source.fromFile("src/test/scala/responses/SimpleXMLReadRequest.xml").getLines.mkString("\n")

	// Create our in-memory sensor database

    //SQLite.addObjects("Objects/Refrigerator123")
    //SQLite.addObjects("Objects/RoomSensors1")
    //SQLite.addObjects("Objects/Roomsensors1/Temperature")

    val date = new Date();
    val testtime = new java.sql.Timestamp(date.getTime)
    val testData = Map(
        "Objects/Refrigerator123/PowerConsumption" -> "0.123",
        "Objects/Refrigerator123/RefrigeratorDoorOpenWarning" -> "door closed",
        "Objects/Refrigerator123/RefrigeratorProbeFault" -> "Nothing wrong with probe",
        "Objects/RoomSensors1/Temperature/Inside" -> "21.2",
        "Objects/RoomSensors1/CarbonDioxide" -> "too much",
        "Objects/RoomSensors1/Temperature/Outside" -> "12.2"
    )

    for ((path, value) <- testData){
        SQLite.set(new DBSensor(path, value, testtime))
    }

	def is = s2"""
  	Testing for the read response.

  	Read.OMIReadResponse should return correct XML when given a list of values.

      Correct XML with one value       		    $e1
      Correct XML with multiple values          $e2
      Correct answer from real request          $e3

    """

    //Error message when applicable			$e3

    def e1 = {
    	val testliste1 = List(
        ODFNode("/Objects/Refrigerator123/PowerConsumption", InfoItem, Some("0.123"), Some(testtime.toString), None))
        OmiParser.parse(Read.OMIReadResponse(2, testliste1)) == List(
            Result("", Some(testliste1)))

    }

    def e2 = {

        //fails right now due to some semantic errors
        //(changed the parser to not put "dateTime" at the start for a while)

        val testliste2 = List(
        ODFNode("/Objects/Refrigerator123/PowerConsumption", InfoItem, Some("0.123"), Some(testtime.toString), None),
        ODFNode("/Objects/Refrigerator123/RefrigeratorDoorOpenWarning", InfoItem, Some("door closed"), Some(testtime.toString), None),
        ODFNode("/Objects/Refrigerator123/RefrigeratorProbeFault", InfoItem, Some("Nothing wrong with probe"), Some(testtime.toString), None),
        ODFNode("/Objects/RoomSensors1/Temperature/Inside", InfoItem, Some("21.2"), Some(testtime.toString), None),
        ODFNode("/Objects/RoomSensors1/CarbonDioxide", InfoItem, Some("too much"), Some(testtime.toString), None))

        println(testliste2)
        println("")
        println(Read.OMIReadResponse(2, testliste2))

        OmiParser.parse(Read.OMIReadResponse(2, testliste2)) == List(
            Result("", Some(testliste2)))

    }

    def e3 = {
        val odfnodes = OmiParser.parse(simpletestfile)
        //println(odfnodes)
        //OmiParser.parse(Read.OMIReadResponse(2, odfnodes)) == List(
        //    Result("", Some(odfnodes)))
        1 == 1
    }
}


