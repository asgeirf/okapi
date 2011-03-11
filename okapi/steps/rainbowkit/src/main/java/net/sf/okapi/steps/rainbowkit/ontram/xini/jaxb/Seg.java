
package net.sf.okapi.steps.rainbowkit.ontram.xini.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Seg complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Seg">
 *   &lt;complexContent>
 *     &lt;extension base="{}TextContent">
 *       &lt;attribute name="SegID" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="SubSeg" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="TrailingSpacer" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="EmptyTranslation" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Seg")
public class Seg
    extends TextContent
{

    @XmlAttribute(name = "SegID", required = true)
    protected int segID;
    @XmlAttribute(name = "SubSeg")
    protected Integer subSeg;
    @XmlAttribute(name = "TrailingSpacer")
    protected String trailingSpacer;
    @XmlAttribute(name = "EmptyTranslation")
    protected Boolean emptyTranslation;

    /**
     * Gets the value of the segID property.
     * 
     */
    public int getSegID() {
        return segID;
    }

    /**
     * Sets the value of the segID property.
     * 
     */
    public void setSegID(int value) {
        this.segID = value;
    }

    /**
     * Gets the value of the subSeg property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSubSeg() {
        return subSeg;
    }

    /**
     * Sets the value of the subSeg property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSubSeg(Integer value) {
        this.subSeg = value;
    }

    /**
     * Gets the value of the trailingSpacer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrailingSpacer() {
        return trailingSpacer;
    }

    /**
     * Sets the value of the trailingSpacer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrailingSpacer(String value) {
        this.trailingSpacer = value;
    }

    /**
     * Gets the value of the emptyTranslation property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEmptyTranslation() {
        return emptyTranslation;
    }

    /**
     * Sets the value of the emptyTranslation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEmptyTranslation(Boolean value) {
        this.emptyTranslation = value;
    }

}
