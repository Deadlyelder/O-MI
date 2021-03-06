//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.06.24 at 01:40:38 PM MSK 
//


package parsing.xmlGen.omi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;element name="read" type="{omi.xsd}readRequest"/&gt;
 *           &lt;element name="write" type="{omi.xsd}writeRequest"/&gt;
 *           &lt;element name="response" type="{omi.xsd}responseListType"/&gt;
 *           &lt;element name="cancel" type="{omi.xsd}cancelRequest"/&gt;
 *         &lt;/choice&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="version" use="required"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="ttl" use="required"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}double"&gt;
 *             &lt;minInclusive value="-1"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "read",
    "write",
    "response",
    "cancel"
})
@XmlRootElement(name = "omiEnvelope")
public class OmiEnvelope {

    protected ReadRequest read;
    protected WriteRequest write;
    protected ResponseListType response;
    protected CancelRequest cancel;
    @XmlAttribute(name = "version", required = true)
    protected String version;
    @XmlAttribute(name = "ttl", required = true)
    protected double ttl;

    /**
     * Gets the value of the read property.
     * 
     * @return
     *     possible object is
     *     {@link ReadRequest }
     *     
     */
    public ReadRequest getRead() {
        return read;
    }

    /**
     * Sets the value of the read property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReadRequest }
     *     
     */
    public void setRead(ReadRequest value) {
        this.read = value;
    }

    /**
     * Gets the value of the write property.
     * 
     * @return
     *     possible object is
     *     {@link WriteRequest }
     *     
     */
    public WriteRequest getWrite() {
        return write;
    }

    /**
     * Sets the value of the write property.
     * 
     * @param value
     *     allowed object is
     *     {@link WriteRequest }
     *     
     */
    public void setWrite(WriteRequest value) {
        this.write = value;
    }

    /**
     * Gets the value of the response property.
     * 
     * @return
     *     possible object is
     *     {@link ResponseListType }
     *     
     */
    public ResponseListType getResponse() {
        return response;
    }

    /**
     * Sets the value of the response property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseListType }
     *     
     */
    public void setResponse(ResponseListType value) {
        this.response = value;
    }

    /**
     * Gets the value of the cancel property.
     * 
     * @return
     *     possible object is
     *     {@link CancelRequest }
     *     
     */
    public CancelRequest getCancel() {
        return cancel;
    }

    /**
     * Sets the value of the cancel property.
     * 
     * @param value
     *     allowed object is
     *     {@link CancelRequest }
     *     
     */
    public void setCancel(CancelRequest value) {
        this.cancel = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the ttl property.
     * 
     */
    public double getTtl() {
        return ttl;
    }

    /**
     * Sets the value of the ttl property.
     * 
     */
    public void setTtl(double value) {
        this.ttl = value;
    }

}
