package net.sf.okapi.filters.xliff;

import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
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

	public Resource          resource;
	public IExtractionItem   sourceItem;
	public IExtractionItem   targetItem;
	
	private IContainer       content;
	private Node             node;
	private int              inCode;
	private Stack<Boolean>   firstChildDoneFlags;


	public XLIFFReader () {
		resource = new Resource();
	}
	
	public void open (InputStream input)
		throws Exception
	{
		try {
			DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			fact.setValidating(false);
			resource.doc = fact.newDocumentBuilder().parse(input);
			firstChildDoneFlags = new Stack<Boolean>();
			firstChildDoneFlags.push(true); // For #document root
			node = resource.doc.getDocumentElement();
			firstChildDoneFlags.push(false);
			processXliff();
		}
		catch ( Exception e ) {
			throw e;
		}
	}

	public int readItem ()
		throws Exception
	{
		resetItem();
		while ( true ) {
			if ( !nextNode() ) {
				return 0; // Document is done
			}
			String sName = getName();
			//TOFO: groups, etc.
			if ( sName.equals("trans-unit") ) {
				processTransUnit();
				return 2;
			}
			else if ( sName.equals("file") ) {
				//TODO: handle multiple files
				processFile();
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
	
	private void processFile () {
		//TODO: check language, etc.
	}
	
	private void processTransUnit ()
		throws Exception
	{
		Element Elem = (Element)node;
		String sTmp = Elem.getAttribute("translate");
		if ( sTmp.length() > 0 ) sourceItem.setIsTranslatable(sTmp.equals("yes"));
		sTmp = Elem.getAttribute("id");
		if ( sTmp.length() == 0 ) throw new Exception("Missing attribute 'id'.");
		else {
			try {
				sourceItem.setID(Integer.valueOf(sTmp));
			}
			catch ( Exception E ) {
				throw new Exception("Invalid value for attribute 'id'.");
			}
		}
		
		// Process the content
		while ( nextNode() ) {
			String sName = getName();
			if ( sName.equals("trans-unit") ) {
				return; // End of the trans-unit element
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
		}
	}
	
	/**
	 * Processes a segment content. Set m_CurrentFI and set m_nInCode to zero before
	 * calling this method with <source> or <target>.
	 * @param p_sContainer The name of the element content that is processed.
	 */
	private void processContent (String p_sContainer)
	{
		// Is this an empty <source> or <target>?
		if ( !node.hasChildNodes() ) {
			return;
		}
		
		while ( nextNode() ) {
			switch ( node.getNodeType() ) {
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				if ( inCode == 0 )
					content.append(node.getTextContent());
				break;
	
			case Node.ELEMENT_NODE:
				String sName = node.getNodeName();
				if ( sName.equals(p_sContainer) ) {
					if ( sName.equals("bpt") ) inCode--;
					else if ( sName.equals("ept") ) inCode--;
					else if ( sName.equals("ph") ) inCode--;
					else if ( sName.equals("g") ) {
						//TODO: use real IDs. 1 will not work
						content.append(new CodeFragment(IContainer.CODE_CLOSING, 1, sName));
					}
					// End return in all cases
					return;
				}
				
				// Else: It's a start of element
				if ( sName.equals("g") ) {
					content.append(new CodeFragment(IContainer.CODE_OPENING, 1, sName));
				}
				else if ( sName.equals("x") ) {
					content.append(new CodeFragment(IContainer.CODE_ISOLATED, 1, sName));
				}
				else if ( sName.equals("bpt") ) {
					content.append(new CodeFragment(IContainer.CODE_OPENING, 1, sName));
					inCode++;
				}
				else if ( sName.equals("ept") ) {
					content.append(new CodeFragment(IContainer.CODE_CLOSING, 1, sName));
					inCode++;
				}
				else if ( sName.equals("ph") ) {
					content.append(new CodeFragment(IContainer.CODE_ISOLATED, 1, sName));
					inCode++;
				}
				else if ( sName.equals("it") ) {
					content.append(new CodeFragment(IContainer.CODE_ISOLATED, 1, sName));
					inCode++;
				}
				break;
			}
		}
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
