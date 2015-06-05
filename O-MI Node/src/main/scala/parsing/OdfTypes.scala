package parsing
package Types

import xmlGen._
import xml.XML
import java.sql.Timestamp
import java.lang.Iterable
import scala.collection.JavaConversions.asJavaIterable
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList


object OdfTypes{

  sealed trait OdfElement

  case class OdfObjects(
    objects:              Iterable[OdfObject] = asJavaIterable(Seq.empty[OdfObject]),
    version:              Option[String] = None
  ) extends OdfElement

  def OdfObjectsAsObjectsType( objects: OdfObjects ) : ObjectsType ={
    ObjectsType(
      Object = objects.objects.map{
        obj: OdfObject => 
        OdfObjectAsObjectType( obj )
      }.toSeq,
      objects.version
    )
  }
  
  case class OdfObject(
    path:                 Path,
    infoItems:            Iterable[OdfInfoItem],
    objects:              Iterable[OdfObject],
    desctription:         Option[OdfDesctription] = None,
    typeValue:            Option[String] = None
  ) extends OdfElement

  def OdfObjectAsObjectType(obj: OdfObject) : ObjectType = {
    ObjectType(
      Seq( QlmID(
          obj.path.last,
          attributes = Map.empty
      )),
      InfoItem = obj.infoItems.map{ 
        info: OdfInfoItem =>
          OdfInfoItemAsInfoItemType( info )
        }.toSeq,
      Object = obj.objects.map{ 
        subobj: OdfObject =>
        OdfObjectAsObjectType( subobj )
      }.toSeq,
      attributes = Map.empty
    )
  }

  case class OdfInfoItem(
    path:                 Types.Path,
    values:               Iterable[OdfValue],
    desctription:         Option[OdfDesctription] = None,
    metaData:             Option[OdfMetaData] = None
  ) extends OdfElement

  def OdfInfoItemAsInfoItemType(info: OdfInfoItem) : InfoItemType = {
    InfoItemType(
      name = info.path.last,
      value = info.values.map{ 
        value : OdfValue =>
        ValueType(
          value.value,
          value.typeValue,
          unixTime = Some(value.timestamp.get.getTime/1000),
          attributes = Map.empty
        )
      },
      MetaData = 
        if(info.metaData.nonEmpty)
          Some( scalaxb.fromXML[MetaData]( XML.loadString( info.metaData.get.data ) ) )
        else 
          None
      ,
      attributes = Map.empty
    )
  }
  case class OdfMetaData(
    data:                 String
  ) extends OdfElement

  case class OdfValue(
    value:                String,
    typeValue:            String = "",
    timestamp:            Option[Timestamp] = None
  ) extends OdfElement

  case class OdfDesctription(
    value:                String,
    lang:                 Option[String]
  ) extends OdfElement
  
  type  OdfParseResult = Either[Iterable[ParseError], OdfObjects]
  def getObjects( odf: OdfParseResult ) : Iterable[OdfObject] = 
    odf match{
      case Right(objs: OdfObjects) => objs.objects
      case _ => asJavaIterable(Seq.empty[OdfObject])
    }
  def getErrors( odf: OdfParseResult ) : Iterable[ParseError] = 
    odf match {
      case Left(pes: Iterable[ParseError]) => pes
      case _ => asJavaIterable(Seq.empty[ParseError])
    }

}
