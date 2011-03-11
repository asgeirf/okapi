
package net.sf.okapi.steps.rainbowkit.ontram.xini.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for TD complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TD">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;sequence maxOccurs="unbounded" minOccurs="0">
 *           &lt;element name="Seg" type="{}Seg"/>
 *           &lt;element name="Trans" type="{}Trans" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;/sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="NoContent" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="CustomerTextID" type="{}TokenMaxLen255" />
 *       &lt;attribute name="Label" type="{}TokenMaxLen255" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TD", propOrder = {
    "segAndTrans"
})
public class TD {

    @XmlElements({
        @XmlElement(name = "Seg", type = Seg.class),
        @XmlElement(name = "Trans", type = Trans.class)
    })
    protected List<TextContent> segAndTrans;
    @XmlAttribute(name = "NoContent")
    protected Boolean noContent;
    @XmlAttribute(name = "CustomerTextID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String customerTextID;
    @XmlAttribute(name = "Label")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String label;

    /**
     * Gets the value of the segAndTrans property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the segAndTrans property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSegAndTrans().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Seg }
     * {@link Trans }
     * 
     * 
     */
    public List<TextContent> getSegAndTrans() {
        if (segAndTrans == null) {
            segAndTrans = new ArrayList<TextContent>();
        }
        return this.segAndTrans;
    }

	public List<Seg> getSeg() {
		List<Seg> segs = new ArrayList<Seg>();
		for (TextContent tc : getSegAndTrans()) {
			if (tc instanceof Seg) {
				segs.add((Seg) tc);
			}
		}
		return segs;
	}

    /**
     * Gets the value of the noContent property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNoContent() {
        return noContent;
    }

    /**
     * Sets the value of the noContent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNoContent(Boolean value) {
        this.noContent = value;
    }

    /**
     * Gets the value of the customerTextID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustomerTextID() {
        return customerTextID;
    }

    /**
     * Sets the value of the customerTextID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomerTextID(String value) {
        this.customerTextID = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabel(String value) {
        this.label = value;
    }

}
