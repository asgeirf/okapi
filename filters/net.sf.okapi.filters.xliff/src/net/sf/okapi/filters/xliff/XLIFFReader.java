package net.sf.okapi.filters.xliff;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.CodeFragment;
import net.sf.okapi.common.resource.Container;
import net.sf.okapi.common.resource.ExtractionItem;
import net.sf.okapi.common.resource.GroupResource;
import net.sf.okapi.common.resource.IContainer;
import net.sf.okapi.common.resource.IExtractionItem;
import net.sf.okapi.common.resource.IFragment;
import net.sf.okapi.common.resource.ISkeletonResource;
import net.sf.okapi.common.resource.SkeletonResource;

public class XLIFFReader {

	public static final int       STATUS_NOTRANS      = 0;
	public static final int       STATUS_UNUSED       = 1;
	public static final int       STATUS_TOTRANS      = 2;
	public static final int       STATUS_TOEDIT       = 3;
	public static final int       STATUS_TOREVIEW     = 4;
	public static final int       STATUS_OK           = 5;

	public static final int       RESULT_ENDINPUT          = 0;
	public static final int       RESULT_STARTFILE         = 1;
	public static final int       RESULT_ENDFILE           = 2;
	public static final int       RESULT_STARTGROUP        = 3;
	public static final int       RESULT_ENDGROUP          = 4;
	public static final int       RESULT_STARTTRANSUNIT    = 5;
	public static final int       RESULT_ENDTRANSUNIT      = 6;
	public static final int       RESULT_SKELETON          = 7;
	

	protected Resource            resource;
	protected GroupResource       fileRes;
	protected Stack<GroupResource> groupResStack;
	protected IExtractionItem     item;

	private SkeletonResource      sklBefore;
	private SkeletonResource      sklAfter;
	private SkeletonResource      currentSkl;
	private int                   itemID;
	private int                   sklID;
	private boolean               sourceDone;
	private boolean               targetDone;
	private XMLStreamReader       reader; 
	private IContainer            content;
	private int                   nextAction;
	private Pattern               pattern;
	
//	private String                elemTransUnit;
//	private String                elemFile;
//	private String                elemNote;
	

	public XLIFFReader () {
		resource = new Resource();
		sklBefore = new SkeletonResource();
		groupResStack = new Stack<GroupResource>();
	}
	
	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void open (InputStream input) {
		try {
			close();
			XMLInputFactory fact = XMLInputFactory.newInstance();
			//fact.setProperty(XMLInputFactory.IS_COALESCING, false);
			fact.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, true);
			reader = fact.createXMLStreamReader(input);
			nextAction = -1;
			sklID = 0;
			itemID = 0;
			sklAfter = new SkeletonResource();
			if ( resource.params.useStateValues ) {
				pattern = Pattern.compile(resource.params.stateValues);
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the last item read.
	 * @return The last item read.
	 */
	public IExtractionItem getItem () {
		return item;
	}

	/**
	 * Gets the last skeleton part read.
	 * @return
	 */
	public ISkeletonResource getSkeleton () {
		return currentSkl;
	}
	
	/**
	 * Reads the next part of the input.
	 * @return One of the RESULT_* values.
	 */
	public int readItem () {
		try {
			switch ( nextAction ) {
			case RESULT_STARTTRANSUNIT:
				nextAction = RESULT_ENDTRANSUNIT;
				return RESULT_STARTTRANSUNIT;
			case RESULT_ENDTRANSUNIT:
				nextAction = -1;
				return RESULT_ENDTRANSUNIT;
			case RESULT_STARTGROUP:
				nextAction = -1;
				return RESULT_STARTGROUP;
			case RESULT_ENDGROUP:
				nextAction = -2;
				return RESULT_ENDGROUP;
			case RESULT_ENDINPUT:
				nextAction = -1;
				return RESULT_ENDINPUT;
			case -2: // Pop group
				nextAction = -1;
				groupResStack.pop();
				break;
			}
			sourceDone = targetDone = false;
			sklBefore.data = new StringBuilder(sklAfter.toString());
			sklBefore.setID(String.format("s%d", ++sklID));
			currentSkl = sklBefore;
			resource.needTargetElement = true;

			int eventType;
			while ( reader.hasNext() ) {
				eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.START_ELEMENT:
					String name = reader.getLocalName();
					if ( "file".equals(name) ) {
						processFile();
					}
					else if ( "trans-unit".equals(name) ) {
						return processStartTransUnit();
					}
					//else if ( "group".equals(name) ) {
					//	return processStartGroup();
					//}
					else storeStartElement();
					break;
				case XMLStreamConstants.END_ELEMENT:
					//if ( "group".equals(reader.getLocalName()) ) {
					//	return processEndGroup();
					//}
					//else 
					storeEndElement();
					break;
				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.CHARACTERS:
					currentSkl.data.append(Util.escapeToXML(reader.getText(),
						0, resource.params.escapeGT));
					break;
				case XMLStreamConstants.COMMENT:
					currentSkl.data.append("<!--"+ reader.getText() + "-->");
					break;
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					currentSkl.data.append("<?"+ reader.getPITarget() + " "
						+ reader.getPIData() + "?>");
					break;
				case XMLStreamConstants.DTD:
					processDTD();
					break;
				case XMLStreamConstants.ENTITY_REFERENCE:
					//TODO: handle entity references
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
		nextAction = RESULT_ENDINPUT; 
		return RESULT_SKELETON;
	}

	private void processDTD () {
		//TODO: handle DTD declaration
//		List notations = (List)reader.getProperty("javax.xml.stream.notations");
//		List entities = (List)reader.getProperty("javax.xml.stream.entities");
//		int i = notations.size();
	}
	
	private int processStartGroup () {
		storeStartElement();
		groupResStack.push(new GroupResource());
		nextAction = RESULT_STARTGROUP;
		return RESULT_SKELETON;
	}
	
	private int processEndGroup () {
		// Pop is done in main loop after the call comes back
		storeEndElement();
		nextAction = RESULT_ENDGROUP;
		return RESULT_SKELETON;
	}
	
	private void resetItem () {
		item = new ExtractionItem();
	}
	
	private int processFile () {
		fileRes = new GroupResource();
		storeStartElement();
		String tmp = reader.getAttributeValue("", "original");
		if ( tmp == null ) throw new RuntimeException("Missing attribute 'original'.");
		else fileRes.setName(tmp);
		//TODO: check lang, etc.
		return RESULT_STARTFILE;
	}
	
	private void storeStartElement () {
		String prefix = reader.getPrefix();
		if (( prefix == null ) || ( prefix.length()==0 )) {
			currentSkl.data.append("<"+reader.getLocalName());
		}
		else {
			currentSkl.data.append("<"+prefix+":"+reader.getLocalName());
		}

		int count = reader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = reader.getNamespacePrefix(i);
			currentSkl.data.append(String.format(" xmlns%s=\"%s\"",
				((prefix.length()>0) ? ":"+prefix : ""),
				reader.getNamespaceURI(i)));
		}
		count = reader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = reader.getAttributePrefix(i); 
			currentSkl.data.append(String.format(" %s%s=\"%s\"",
				(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
				reader.getAttributeLocalName(i),
				reader.getAttributeValue(i)));
		}
		currentSkl.data.append(">");
	}
	
	private void storeEndElement () {
		String ns = reader.getPrefix();
		if (( ns == null ) || ( ns.length()==0 )) {
			currentSkl.data.append("</"+reader.getLocalName()+">");
		}
		else {
			currentSkl.data.append("</"+ns+":"+reader.getLocalName()+">");
		}
	}
	
	private void checkTarget () {
		if ( !sourceDone ) return;
		if ( targetDone ) return;
		currentSkl = sklAfter;
		targetDone = true;
	}
	
	private int processStartTransUnit () {
		try {
			resetItem();
			item.setID(String.format("%d", ++itemID));
			storeStartElement();

			String tmp = reader.getAttributeValue("", "translate");
			if ( tmp != null ) item.setIsTranslatable(tmp.equals("yes"));
		
			tmp = reader.getAttributeValue("", "resname");
			if ( tmp != null ) item.setName(tmp);
			else if ( resource.params.fallbackToID ) {
				tmp = reader.getAttributeValue("", "id");
				if ( tmp == null ) throw new RuntimeException("Missing attribute 'id'.");
				item.setName(tmp);
			}

			tmp = reader.getAttributeValue("", "restype");
			if ( tmp != null ) item.setType(tmp);
			
			// Get the content
			int eventType;
			while ( reader.hasNext() ) {
				eventType = reader.next();
				String name;
				switch ( eventType ) {
				case XMLStreamConstants.START_ELEMENT:
					name = reader.getLocalName();
					if ( "source".equals(name) ) {
						storeStartElement();
						processSource();
						storeEndElement();
					}
					else if ( "target".equals(name) ) {
						storeStartElement();
						processTarget();
						checkTarget();
						storeEndElement();
					}
					else if ( "note".equals(name) ) {
						checkTarget();
						storeStartElement();
						processNote();
						storeEndElement();
					}
					else {
						checkTarget();
						storeStartElement();
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					name = reader.getLocalName();
					checkTarget();
					if ( "trans-unit".equals(name) ) {
						storeEndElement();
						nextAction = RESULT_STARTTRANSUNIT;
						currentSkl = sklBefore;
						return RESULT_SKELETON;
					}
					else storeEndElement();
					break;
				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.CHARACTERS:
					if ( !targetDone ) {
						// Faster that separating XMLStreamConstants.SPACE
						// from other data in the all process
						tmp = reader.getText();
						for ( int i=0; i<tmp.length(); i++ ) {
							if ( !Character.isWhitespace(tmp.charAt(i)) ) {
								checkTarget();
								break;
							}
						}
					}
					currentSkl.data.append(Util.escapeToXML(reader.getText(),
						0, resource.params.escapeGT));
					break;
				case XMLStreamConstants.COMMENT:
					checkTarget();
					currentSkl.data.append("<!--"+ reader.getText() + "-->");
					break;
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					checkTarget();
					currentSkl.data.append("<?"+ reader.getPITarget() + " "
						+ reader.getPIData() + "?>");
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
		return RESULT_ENDINPUT;
	}
	
	private void processNote () {
		try {
			StringBuilder tmp = new StringBuilder();
			if ( item.hasNote() ) {
				tmp.append(item.getNote());
				tmp.append("\n---\n");
			}
			int eventType;
			while ( reader.hasNext() ) {
				eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					currentSkl.data.append(Util.escapeToXML(reader.getText(),
						0, resource.params.escapeGT));
					tmp.append(reader.getText());
					break;
				case XMLStreamConstants.END_ELEMENT:
					String name = reader.getLocalName();
					if ( name.equals("note") ) {
						//TODO: Handle 'annotates', etc.
						item.setNote(tmp.toString());
						return;
					}
					// Else: This should be an error as note are text only.
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Processes a segment content. Set the content and set inCode variables 
	 * before calling this method with <source> or <target>.
	 * @param tagName The name of the element content that is being processed.
	 * @param store True if the data must be stored in the skeleton.
	 * This is used to merge later on.
	 * @param content The object where to put the code.
	 * @param inlineCodes Array where to save the in-line codes. Do not save if this parameter
	 * is set to null.
	 */
	private void processContent (String tagName,
		boolean store,
		IContainer content,
		ArrayList<IFragment> inlineCodes)
	{
		try {
			int id = 0;
			Stack<Integer> idStack = new Stack<Integer>();
			idStack.push(id);
			int eventType;
			String name;
			
			if ( inlineCodes != null ) {
				inlineCodes.clear();
			}
			
			while ( reader.hasNext() ) {
				eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					content.append(reader.getText());
					if ( store )
						currentSkl.data.append(Util.escapeToXML(reader.getText(),
							0, resource.params.escapeGT));
					break;
		
				case XMLStreamConstants.END_ELEMENT:
					name = reader.getLocalName();
					if ( name.equals(tagName) ) {
						return;
					}
					else if ( name.equals("g") || name.equals("mrk") ) {
						if ( store ) storeEndElement();
						int tmpID = idStack.pop();
						content.append(
							new CodeFragment(IContainer.CODE_CLOSING, tmpID, name));
						if ( inlineCodes != null ) {
							String tmp = reader.getPrefix();
							if (( tmp != null ) && ( tmp.length()>0 )) {
								tmp = tmp+":";
							}
							inlineCodes.add(
								new CodeFragment(IContainer.CODE_CLOSING, tmpID,
									"</"+tmp+name+">"));
						}
					}
					break;
					
				case XMLStreamConstants.START_ELEMENT:
					if ( store ) storeStartElement();
					name = reader.getLocalName();
					if ( name.equals("g") || name.equals("mrk") ) {
						idStack.push(++id);
						content.append(
							new CodeFragment(IContainer.CODE_OPENING, id, name));
						if ( inlineCodes != null ) {
							String prefix = reader.getPrefix();
							StringBuilder tmpg = new StringBuilder();
							if (( prefix == null ) || ( prefix.length()==0 )) {
								tmpg.append("<"+reader.getLocalName());
							}
							else {
								tmpg.append("<"+prefix+":"+reader.getLocalName());
							}
							int count = reader.getNamespaceCount();
							for ( int i=0; i<count; i++ ) {
								prefix = reader.getNamespacePrefix(i);
								tmpg.append(String.format(" xmlns:%s=\"%s\"",
									((prefix.length()>0) ? ":"+prefix : ""),
									reader.getNamespaceURI(i)));
							}
							count = reader.getAttributeCount();
							for ( int i=0; i<count; i++ ) {
								if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
								prefix = reader.getAttributePrefix(i); 
								tmpg.append(String.format(" %s%s=\"%s\"",
									(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
									reader.getAttributeLocalName(i),
									reader.getAttributeValue(i)));
							}
							tmpg.append(">");
							inlineCodes.add(
								new CodeFragment(IContainer.CODE_OPENING, id, tmpg.toString()));
						}
					}
					else if ( name.equals("x") ) {
						appendCode(IContainer.CODE_ISOLATED, ++id, name, store, content, inlineCodes);
					}
					else if ( name.equals("bpt") ) {
						idStack.push(++id);
						appendCode(IContainer.CODE_OPENING, id, name, store, content, inlineCodes);
					}
					else if ( name.equals("ept") ) {
						appendCode(IContainer.CODE_CLOSING, idStack.pop(), name, store, content, inlineCodes);
					}
					else if ( name.equals("ph") ) {
						appendCode(IContainer.CODE_ISOLATED, ++id, name, store, content, inlineCodes);
					}
					else if ( name.equals("it") ) {
						appendCode(IContainer.CODE_ISOLATED, ++id, name, store, content, inlineCodes);
					}
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Appends a code, using the content of the node. Do not use for <g>-type tags.
	 * @param type The type of in-line code.
	 * @param id The id of the code to add.
	 * @param tagName The tag name of the in-line element to process.
	 * @param store True if we need to store the data in the skeleton.
	 * @param content The object where to put the code.
	 * @param inlineCodes Array where to save the original in-line code.
	 * Do not save if this parameter is null.
	 */
	private void appendCode (int type,
		int id,
		String tagName,
		boolean store,
		IContainer content,
		ArrayList<IFragment> inlineCodes)
	{
		try {
			StringBuilder tmp = new StringBuilder();
			StringBuilder outerCode = null;
			if ( inlineCodes != null ) {
				outerCode = new StringBuilder();
				outerCode.append("<"+tagName);
				int count = reader.getAttributeCount();
				String prefix;
				for ( int i=0; i<count; i++ ) {
					if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
					prefix = reader.getAttributePrefix(i); 
					outerCode.append(String.format(" %s%s=\"%s\"",
						(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
						reader.getAttributeLocalName(i),
						reader.getAttributeValue(i)));
				}
				outerCode.append(">");
			}
			
/*			if ( useInlineIDs ) { // Try to use existing id of inline code
				String ilID = reader.getAttributeValue("", "id");
				if (( ilID != null ) && ( ilID.length()>0 )) {
					
				}
			}
*/				
			int eventType;
			while ( reader.hasNext() ) {
				eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.START_ELEMENT:
					if ( store ) storeStartElement();
					if ( inlineCodes != null ) {
						String prefix = reader.getPrefix();
						StringBuilder tmpg = new StringBuilder();
						if (( prefix == null ) || ( prefix.length()==0 )) {
							tmpg.append("<"+reader.getLocalName());
						}
						else {
							tmpg.append("<"+prefix+":"+reader.getLocalName());
						}
						int count = reader.getNamespaceCount();
						for ( int i=0; i<count; i++ ) {
							prefix = reader.getNamespacePrefix(i);
							tmpg.append(String.format(" xmlns:%s=\"%s\"",
								((prefix.length()>0) ? ":"+prefix : ""),
								reader.getNamespaceURI(i)));
						}
						count = reader.getAttributeCount();
						for ( int i=0; i<count; i++ ) {
							if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
							prefix = reader.getAttributePrefix(i); 
							tmpg.append(String.format(" %s%s=\"%s\"",
								(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
								reader.getAttributeLocalName(i),
								reader.getAttributeValue(i)));
						}
						tmpg.append(">");
						outerCode.append(tmpg.toString());
					}
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					if ( store ) storeEndElement();
					if ( tagName.equals(reader.getLocalName()) ) {
						if ( inlineCodes != null ) {
							outerCode.append("</"+tagName+">");
							inlineCodes.add(
								new CodeFragment(type, id, outerCode.toString()));
						}
						content.append(new CodeFragment(type, id, tmp.toString()));
						return;	
					}
					break;
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					tmp.append(reader.getText());
					if ( inlineCodes != null )
						outerCode.append(Util.escapeToXML(reader.getText(),
							0, resource.params.escapeGT));
					if ( store )
						currentSkl.data.append(Util.escapeToXML(reader.getText(),
							0, resource.params.escapeGT));
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void processSource () {
		if ( sourceDone ) {
			// Case where this entry is not the main one, but from an alt-trans
			Container tmpCont = new Container();
			processContent("source", true, tmpCont, null);
			return;
		}
		content = new Container();
		processContent("source", true, content, resource.srcCodes);
		item.setSource(content);
		sklAfter.data = new StringBuilder();
		sourceDone = true;
	}
	
	private void processTarget () {
		if ( targetDone ) {
			// Case where this entry is not the main one, but from an alt-trans
			Container tmpCont = new Container();
			processContent("target", true, tmpCont, null);
			return;
		}
		
		item.setTarget(new Container());
		//String tmp = reader.getAttributeValue("", "state");
		/*if ( tmp != null ) {
			item.getTarget().setProperty("state", tmp);
			if ( tmp.equals("needs-translation") ) resource.status = STATUS_TOTRANS;
			else if ( tmp.equals("final") ) resource.status = STATUS_OK;
			else if ( tmp.equals("translated") ) resource.status = STATUS_TOEDIT;
			else if ( tmp.equals("needs-review-translation") ) resource.status = STATUS_TOREVIEW;
		}*/
		content = new Container();
		processContent("target", false, content, resource.trgCodes);
		resource.needTargetElement = false;
		if ( !item.isEmpty() && !content.isEmpty() )
			item.setTarget(content);
		else
			item.setTarget(null);
	}
	
	private boolean isExtractable () {
		if ( !resource.params.useStateValues ) return true;
		if ( !item.hasTarget() ) return true;
		
		String state = (String)item.getTarget().getProperty("state");
		if (( state == null ) || ( state.length() == 0 )) {
			return resource.params.extractNoState;
		}
		return pattern.matcher(state).find();
	}
	
}
