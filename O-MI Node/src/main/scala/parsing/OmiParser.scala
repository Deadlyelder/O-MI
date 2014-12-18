package parsing

import sensorDataStructure._
import scala.xml._

object OmiParser extends Parser{
  private def parseODF(msg:Node) = OdfParser.parse( new PrettyPrinter( 80, 2 ).format( msg ) )
  def parse(xml_msg: String): Seq[ ParseMsg ] ={
    val root = XML.loadString( xml_msg )
    if( root.prefix + ":" + root.label != "omi:Envelope" )
      return Seq( new ParseError( "XML's root isn't omi:Envelope" ))
      
    val request = root.child.tail    
    if( request.headOption.isEmpty )
      return Seq( new ParseError( "omi:Envelope doesn't contain request" ))

    val ttl = root \ "@ttl"
    if(ttl.isEmpty){
      return Seq( new ParseError("No ttl present in O-MI Envelope" ) )
    }
    parseNode(request.head, ttl.text) 
  }

  private def parseNode(node: Node, ttl: String ): Seq[ ParseMsg ]  = {
    //TODO: test for correct node.prefix ( node.prefix = "omi")
  node.label match {
      /*
        Write request 
      */
      case "write"  => {
        val msgformat = (node \ "@msgformat").headOption.getOrElse(
          return Seq( new ParseError( "No msgformat in write request" ) )  
        ).text
        msgformat match{ 
          case "odf" => {
            val msg = ( node \ "{omi}msg" ).headOption.getOrElse{
              return Seq( new ParseError( "No message node found in read node." ) ) 
            }
            val odf = parseODF( msg )
            val errorsandOdf = errorsAndOdf(odf)

            if ( errorsandOdf( true ).isEmpty ) {
              Seq( Write(ttl, errorsandOdf( false ).map( _.right.get ) ) ) 
            } else {
              errorsandOdf( true ).map( _.left.get )  
            }
          }
          case _ => Seq( new ParseError( "Unknown message format." ) ) 
        }        
      } 

      /*
        Read request 
      */
      case "read"  => {
        val msgformat = (node \ "@msgformat").headOption.getOrElse(
          return Seq( new ParseError( "No msgformat in read request" ) )  
        ).text

        msgformat match { 
          case "odf" => {
            val msg = ( node \ "{omi}msg" ).headOption.getOrElse{
              return Seq( new ParseError( "No message node found in read node." ) ) 
            }
            val interval = ( node \ "@interval" ).headOption
            val odf = parseODF( msg )
            val errorsandOdf = errorsAndOdf(odf)

            if ( errorsandOdf( true ).isEmpty ) {

              if ( interval.isEmpty ){
                Seq( OneTimeRead(ttl, errorsandOdf( false ).map( _.right.get ) ) ) 
              } else {
                Seq( Subscription( ttl, interval.get.text, errorsandOdf( false ).map( _.right.get ) ) ) 
              }

            } else {
              errorsandOdf( true ).map( _.left.get )  
            }
          }

          case _ => Seq( new ParseError( "Unknown message format." ) ) 
        }
      }
      
      /*
        Cancel request 
      */
      case "cancel"  => Seq( new ParseError( "Unimplemented O-MI node." ) )  
      
      /*
        Response 
      */
      case "response"  => {
        parseNode( ( node \ "{omi}result" ).headOption.getOrElse( 
          return Seq( new ParseError( "No result node in response node" ) ) 
        ), ttl ) 
      }
    
      /*
        Response's Result 
      */
      case "result" => {
        val msgformat = (node \ "@msgformat").headOption.getOrElse(
          return Seq( new ParseError( "No return node in result node" ) )  
        ).text
        val returnValue = ( node \ "{omi}return" ).headOption.getOrElse(
          return Seq( new ParseError( "No return node in result node" ) )  
        ).text
        val msgOp = ( node \ "{omi}msg" ).headOption
        if(msgOp.isEmpty)
          return  Seq( Result(returnValue, None) ) 
        else{
          msgformat match{
            case "odf" => {
              val odf = parseODF( msgOp.get )
              val errorsandOdf = errorsAndOdf(odf)

              if ( errorsandOdf( true ).isEmpty ) 
                return Seq( Result( returnValue, Some( errorsandOdf( false ).map( _.right.get ) ) ) ) 
              else
                return errorsandOdf( true ).map( _.left.get )

            }
            case _ => return Seq( new ParseError( "Unknown smgformat in result" ) ) 
          }
        }
      }
     
      /*
        Unknown node 
      */
      case _  => Seq( new ParseError( "Unknown node." ) )  
    }
  }

  private def errorsAndOdf( odf:Seq[ OdfParser.ParseResult ]) = odf.groupBy( _.isLeft )

}

abstract sealed trait ParseMsg
case class ParseError( msg:String ) extends ParseMsg
case class OneTimeRead( ttl:String, sensors: Seq[OdfParser.ODFNode]) extends ParseMsg
case class Write( ttl:String, sensors: Seq[OdfParser.ODFNode]) extends ParseMsg
case class Subscription( ttl:String, interval: String, sensors: Seq[OdfParser.ODFNode]) extends ParseMsg
case class Result(value: String ,parseMsgOp:Option[Seq[OdfParser.ODFNode]]) extends ParseMsg
