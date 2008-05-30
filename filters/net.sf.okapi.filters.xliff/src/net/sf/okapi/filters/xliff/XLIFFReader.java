package net.sf.okapi.filters.xliff;

import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.okapi.common.resource.CodeFragment;
import net.sf.okapi.common.resource.Container;
import net.sf.okapi.common.resource.ExtractionItem;
import net.sf.okapi.common.resource.IContainer;
import net.sf.okapi.common.resource.IExtractionItem;

public class XLIFFReader {

	// Same as in Borneo database TSTATUS_* values
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

	protected Resource            resource;
	protected FileResource        fileRes;
	protected IExtractionItem     sourceItem;
	protected IExtractionItem     targetItem;
	
	private IContainer       content;
	private Node             node;
	private int              inCode;
	private Stack<Boolean>   firstChildDoneFlags;
	private int              lastResult;
	private boolean          backTrack;
	private boolean          fallbackToID;
	

	public XLIFFReader () {
		resource = new Resource();
	}
	
	public void open (InputStream input,
		boolean fallbackToID)
	{
		try {
			this.fallbackToID = fallbackToID;
			DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			fact.setValidating(false);
			resource.doc = fact.newDocumentBuilder().parse(input);
			firstChildDoneFlags = new Stack<Boolean>();
			firstChildDoneFlags.push(true); // For #document root
			node = resource.doc.getDocumentElement();
			firstChildDoneFlags.push(false);
			lastResult = -1;
			processXliff();
		}
		catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the last source item read.
	 * @return The last source item read.
	 */
	public IExtractionItem getSourceItem () {
		return sourceItem;
	}

	/**
	 * Gets the last target item read.
	 * @return The last target item read, or null if there are no target item.
	 */
	public IExtractionItem getTargetItem () {
		return targetItem;
	}

	/**
	 * Reads the next part of the input.
	 * @return One of the RESULT_* values.
	 */
	//TODO: Change the parsing to get start/end elements
	public int readItem () {
		// If needed, resume parsing based on the last result
		switch ( lastResult ) {
		case RESULT_STARTTRANSUNIT:
			lastResult = processEndTransUnit();
			return lastResult;
		}
		
		// Move on to the next node
		while ( true ) {
			if ( !nextNode() ) {
				return RESULT_ENDINPUT; // Document is done
			}
			String name = getName();
			//TOFO: groups, etc.
			if ( name.equals("trans-unit") ) {
				resetItem();
				lastResult = processStartTransUnit();
				return lastResult;
			}
			else if ( name.equals("file") ) {
				if ( backTrack ) {
					lastResult = RESULT_ENDFILE;
					return lastResult;
				}
				else {
					lastResult = processFile();
					return lastResult;
				}
			}
		}
	}
	
	/**
	 * Gets the name of the current node or an empty string.
	 * @return The name, or an empty string if the node is null or not an element.
	 */
	private String getName () {
		if ( node == null ) return "";
		if ( node.getNodeType() != Node.ELEMENT_NODE ) return "";
		return node.getNodeName();
	}
	
	private boolean nextNode () {
		if ( node != null ) {
			backTrack = false;
			if ( !firstChildDoneFlags.peek() && node.hasChildNodes() ) {
				// Change the flag for the current node
				firstChildDoneFlags.push(!firstChildDoneFlags.pop());
				// Get the new node and push its flag
				node = node.getFirstChild();
				firstChildDoneFlags.push(false);
			}
			else {
				Node TmpNode = node.getNextSibling();
				if ( TmpNode == null ) {
					node = node.getParentNode();
					firstChildDoneFlags.pop();
					backTrack = true;
				}
				else {
					node = TmpNode;
					firstChildDoneFlags.pop(); // Remove flag for previous sibling 
					firstChildDoneFlags.push(false); // Set new flag for new sibling
				}
			}
		}
		return (node != null);
	}
	
	private void resetItem () {
		sourceItem = new ExtractionItem();
		targetItem = null;
		resource.srcElem = null;
		resource.trgElem = null;
		resource.status = STATUS_TOTRANS;
	}
	
	private void processXliff () {
		//TODO: check version, root, etc.
	}
	
	private int processFile () {
		fileRes = new FileResource();
		Element Elem = (Element)node;
		String tmp = Elem.getAttribute("original");
		if ( tmp.length() == 0 ) throw new RuntimeException("Missing attribute 'original'.");
		else fileRes.setName(tmp);
		return RESULT_STARTFILE;
	}
	
	private int processStartTransUnit () {
		Element Elem = (Element)node;
		String sTmp = Elem.getAttribute("translate");
		if ( sTmp.length() > 0 ) sourceItem.setIsTranslatable(sTmp.equals("yes"));
		sTmp = Elem.getAttribute("id");
		if ( sTmp.length() == 0 ) throw new RuntimeException("Missing attribute 'id'.");
		else sourceItem.setID(sTmp);
		sTmp = Elem.getAttribute("resname");
		if ( sTmp.length() > 0 ) sourceItem.setName(sTmp);
		else if ( fallbackToID ) sourceItem.setName(sourceItem.getID());
		sTmp = Elem.getAttribute("restype");
		if ( sTmp.length() > 0 ) sourceItem.setType(sTmp);
		
		return RESULT_STARTTRANSUNIT;
	}
	
	private int processEndTransUnit () {
		// Process the content
		if ( !node.hasChildNodes() ) {
			return RESULT_ENDTRANSUNIT; // Empty trans-unit (should not exist, but just in case...)
		}
		while ( nextNode() ) {
			String sName = getName();
			if ( sName.equals("trans-unit") ) {
				return RESULT_ENDTRANSUNIT; // End of the trans-unit element
			}
			if ( sName.equals("source") ) {
				resource.srcElem = (Element)node;
				content = new Container();
				inCode = 0;
				processContent(sName);
				sourceItem.setContent(content);
			}
			else if ( sName.equals("target") ) {
				processTarget();
			}
			else if ( sName.equals("note") ) {
				if ( !backTrack ) sourceItem.setNote(node.getTextContent());
			}
		}
		return RESULT_ENDTRANSUNIT; // Should not get here
	}
	
	/**
	 * Processes a segment content. Set m_CurrentFI and set m_nInCode to zero before
	 * calling this method with <source> or <target>.
	 * @param p_sContainer The name of the element content that is processed.
	 */
	private void processContent (String container)
	{
		// Is this an empty <source> or <target>?
		if ( !node.hasChildNodes() ) {
			return;
		}

		// For now use a stack for tracking the id
		// Not great: Assumes all openings have closings.
		//TODO: improve id setting mechanism for inline codes
		int id = 0;
		Stack<Integer> idStack = new Stack<Integer>();
		idStack.push(id);
		
		while ( nextNode() ) {
			switch ( node.getNodeType() ) {
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				if ( inCode == 0 )
					content.append(node.getTextContent());
				break;
	
			case Node.ELEMENT_NODE:
				String sName = node.getNodeName();
				if ( sName.equals(container) ) {
					return;
				}
				if ( backTrack ) {
					if ( sName.equals("bpt") ) inCode--;
					else if ( sName.equals("ept") ) inCode--;
					else if ( sName.equals("ph") ) inCode--;
					else if ( sName.equals("g") ) {
						content.append(
							new CodeFragment(IContainer.CODE_CLOSING, idStack.pop(), sName));
					}
					continue; // Move on the next node
				}
				
				// Else: It's a start of element
				if ( sName.equals("g") ) {
					idStack.push(++id);
					content.append(
						new CodeFragment(IContainer.CODE_OPENING, id, sName));
				}
				else if ( sName.equals("x") ) {
					appendCode(IContainer.CODE_ISOLATED, ++id);
				}
				else if ( sName.equals("bpt") ) {
					idStack.push(++id);
					appendCode(IContainer.CODE_OPENING, id);
					inCode++;
				}
				else if ( sName.equals("ept") ) {
					appendCode(IContainer.CODE_CLOSING, idStack.pop());
					inCode++;
				}
				else if ( sName.equals("ph") ) {
					appendCode(IContainer.CODE_ISOLATED, ++id);
					inCode++;
				}
				else if ( sName.equals("it") ) {
					appendCode(IContainer.CODE_ISOLATED, ++id);
					inCode++;
				}
				break;
			}
		}
	}

	/**
	 * Appends a code, using the content of the node. Do not use for <g>-type tags.
	 * @param type The type of in-line code.
	 * @param id The id of the code to add.
	 */
	private void appendCode (int type,
		int id)
	{
		String inside = node.getTextContent(); // No support for <sub>
		NamedNodeMap attrs = node.getAttributes();
		StringBuilder tmp = new StringBuilder();
		for ( int i=0; i<attrs.getLength(); i++ ) {
			tmp.append(String.format("%s%s=\"%s\"", (tmp.length()>0 ? "" : " "), attrs.item(i).getNodeName(),
				attrs.item(i).getNodeValue()));
		}
		content.append(new CodeFragment(type, 1, inside, node));
	}
	
	private void processTarget () {
		targetItem = new ExtractionItem();
		resource.trgElem = (Element)node;
		String tmp = resource.trgElem.getAttribute("state");
		if ( tmp.length() > 0 ) {
			targetItem.setProperty("state", tmp);
			if ( tmp.equals("needs-translation") ) resource.status = STATUS_TOTRANS;
			else if ( tmp.equals("final") ) resource.status = STATUS_OK;
			else if ( tmp.equals("translated") ) resource.status = STATUS_TOEDIT;
			else if ( tmp.equals("needs-review-translation") ) resource.status = STATUS_TOREVIEW;
		}

		content = new Container();
		inCode = 0;
		processContent("target");
		targetItem.setContent(content);

		if ( !sourceItem.isEmpty() && !targetItem.isEmpty() ) {
			sourceItem.setHasTarget(true);
		}
	}
	
}
