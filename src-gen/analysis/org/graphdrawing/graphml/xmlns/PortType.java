//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.04.01 at 08:06:02 AM CEST 
//


package org.graphdrawing.graphml.xmlns;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *       Complex type for the <port> element.
 *     
 * 
 * <p>Java class for port.type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="port.type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://graphml.graphdrawing.org/xmlns}desc" minOccurs="0"/>
 *         &lt;choice maxOccurs="unbounded" minOccurs="0">
 *           &lt;element ref="{http://graphml.graphdrawing.org/xmlns}data"/>
 *           &lt;element ref="{http://graphml.graphdrawing.org/xmlns}port"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://graphml.graphdrawing.org/xmlns}port.extra.attrib"/>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "port.type", propOrder = {
    "desc",
    "dataOrPort"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
public class PortType {

    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    protected String desc;
    @XmlElements({
        @XmlElement(name = "data", type = DataType.class),
        @XmlElement(name = "port", type = PortType.class)
    })
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    protected List<Object> dataOrPort;
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    protected String name;

    /**
     * Gets the value of the desc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    public String getDesc() {
        return desc;
    }

    /**
     * Sets the value of the desc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    public void setDesc(String value) {
        this.desc = value;
    }

    /**
     * Gets the value of the dataOrPort property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataOrPort property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataOrPort().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataType }
     * {@link PortType }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    public List<Object> getDataOrPort() {
        if (dataOrPort == null) {
            dataOrPort = new ArrayList<Object>();
        }
        return this.dataOrPort;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2014-04-01T08:06:02+02:00", comments = "JAXB RI v2.2.4-2")
    public void setName(String value) {
        this.name = value;
    }

}