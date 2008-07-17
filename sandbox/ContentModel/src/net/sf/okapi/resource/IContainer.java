package net.sf.okapi.resource;

import java.util.List;

public interface IContainer extends List<IContent>{

	/**
	 * Retrieves a String encoded representation of the content.
	 * 
	 * @return a String representation of the content
	 */
	public String getCodedText();
	
	/**
	 * 
	 * @param codedText 
	 * @return
	 */
	public void setCodedText(String codedText) throws CodedTextException;
}
