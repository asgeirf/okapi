package org.w3c.its;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ITSEngine implements IProcessor, ITraversal  
{
	public static final String    ITS_NS_URI     = "http://www.w3.org/2005/11/its";
	public static final String    ITS_NS_PREFIX  = "its";
	public static final String    XLINK_NS_URI   = "http://www.w3.org/1999/xlink";
	
	private static final String   FLAGNAME       = "\u00ff";
	private static final String   FLAGSEP        = "\u001c";

	private static final int      FP_TRANSLATE        = 0;
	private static final int      FP_DIRECTIONALITY   = 1;
	private static final int      FP_WITHINTEXT       = 2;
	private static final int      FP_TERMINOLOGY      = 3;

	private static final int      FP_TERMINOLOGY_DATA      = 0;
	
	private static final int      TERMINFOTYPE_POINTER     = 0;
	private static final int      TERMINFOTYPE_REF         = 1;
	private static final int      TERMINFOTYPE_REFPOINTER  = 2;

	private DocumentBuilderFactory fact; 
	private Document doc;
	private URI docURI;
	private NSContextManager nsContext;
	private XPathFactory xpFact;
	private XPath xpath;
	private ArrayList<ITSRule> rules;
	private Node node;
	private boolean startTraversal;
	private Stack<ITSTrace> trace;
	private boolean backTracking;
	
	public ITSEngine (Document doc,
		URI docURI)
	{
		this.doc = doc;
		this.docURI = docURI;
		node = null;
		rules = new ArrayList<ITSRule>();
		nsContext = new NSContextManager();
		nsContext.addNamespace(ITS_NS_PREFIX, ITS_NS_URI);
		xpFact = XPathFactory.newInstance();
		xpath = xpFact.newXPath();
		xpath.setNamespaceContext(nsContext);
	}

	public void addExternalRules (URI docURI) {
		try {
			if ( fact == null ) { 
				fact = DocumentBuilderFactory.newInstance();
				fact.setNamespaceAware(true);
				fact.setValidating(false);
			}
			Document rulesDoc = fact.newDocumentBuilder().parse(docURI.toString());
			addExternalRules(rulesDoc, docURI);
		}
		catch ( SAXException e ) {
			throw new RuntimeException(e);
		}
		catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public void addExternalRules (Document rulesDoc,
		URI docURI)
	{
		compileRules(rulesDoc, docURI, false);
	}
	
	private void compileRules (Document rulesDoc,
		URI docURI,
		boolean isInternal)
	{
		try {
			// Compile the namespaces
			XPathExpression expr = xpath.compile("//*[@selector]//namespace::*");
			NodeList nl = (NodeList)expr.evaluate(rulesDoc, XPathConstants.NODESET);
			for ( int i=0; i<nl.getLength(); i++ ) {
				String prefix = nl.item(i).getLocalName();
				if ( "xml".equals(prefix) ) continue; // Set by default
				String uri = nl.item(i).getNodeValue();
				nsContext.addNamespace(prefix, uri);
			}
			
			// Compile the rules
			// First: get the its:rules element(s)
			expr = xpath.compile("//"+ITS_NS_PREFIX+":rules");
			nl = (NodeList)expr.evaluate(rulesDoc, XPathConstants.NODESET);
			if ( nl.getLength() == 0 ) return; // Nothing to do
			
			// Process each its:rules element
			Element rulesElem;
			for ( int i=0; i<nl.getLength(); i++ ) {
				rulesElem = (Element)nl.item(i);
				//TODO: Check version

				//TODO: load linked rules
				// Check for link
				String href = rulesElem.getAttributeNS(XLINK_NS_URI, "href");
				if ( href.length() > 0 ) {
					//String id = null;
					int n = href.lastIndexOf('#');
					if ( n > -1 ) {
						//id = href.substring(n+1);
						href = href.substring(0, n);
					}

					// xlink:href allows the use of xml:base so we need to calculate it
					// The initial base is the folder of the current document
					String baseFolder = "";
					if ( docURI != null) baseFolder = getPartBeforeFile(docURI);
					
					// Then we look for the last xml:base specified
					Node node = rulesElem;
					while ( node != null ) {
						if ( node.getNodeType() == Node.ELEMENT_NODE ) {
							//TODO: Relative path with ../../ constructs
							String xmlBase = ((Element)node).getAttribute("xml:base");
							if ( xmlBase.length() > 0 ) {
								if ( xmlBase.endsWith("/") )
									xmlBase = xmlBase.substring(0, xmlBase.length()-1);
								if ( !baseFolder.startsWith("/") )
									baseFolder = xmlBase + "/" + baseFolder;
								else
									baseFolder = xmlBase + baseFolder;
							}
						}
						node = node.getParentNode(); // Back-track to parent
					}
					if ( baseFolder.length() > 0 ) {
						if ( baseFolder.endsWith("/") )
							baseFolder = baseFolder.substring(0, baseFolder.length()-1);
						if ( !href.startsWith("/") ) href = baseFolder + "/" + href;
						else href = baseFolder + href;
					}

					// Load the document and the rules
					URI linkedDoc = new URI(href);
					loadLinkedRules(linkedDoc, isInternal);
				}

				// Process each rule inside its:rules
				expr = xpath.compile("//"+ITS_NS_PREFIX+":*");
				NodeList nl2 = (NodeList)expr.evaluate(rulesElem, XPathConstants.NODESET);
				if ( nl2.getLength() == 0 ) break; // Nothing to do, move to next its:rules
				
				Element ruleElem;
				for ( int j=0; j<nl2.getLength(); j++ ) {
					ruleElem = (Element)nl2.item(j);
					if ( "translateRule".equals(ruleElem.getLocalName()) ) {
						compileTranslateRule(ruleElem, isInternal);
					}
					else if ( "withinTextRule".equals(ruleElem.getLocalName()) ) {
						compileWithinTextRule(ruleElem, isInternal);
					}
					else if ( "dirRule".equals(ruleElem.getLocalName()) ) {
						compileDirRule(ruleElem, isInternal);
					}
					else if ( "termRule".equals(ruleElem.getLocalName()) ) {
						compileTermRule(ruleElem, isInternal);
					}
				}
			}
		}
		catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
		catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private String getPartBeforeFile (URI uri) {
		String tmp = uri.toString();
		int n = tmp.lastIndexOf('/');
		if ( n == -1 ) return uri.toString();
		else return tmp.substring(0, n+1);
	}
	
	private void loadLinkedRules (URI docURI,
		boolean isInternal)
	{
		try {
			if ( fact == null ) { 
				fact = DocumentBuilderFactory.newInstance();
				fact.setNamespaceAware(true);
				fact.setValidating(false);
			}
			Document rulesDoc = fact.newDocumentBuilder().parse(docURI.toString());
			compileRules(rulesDoc, docURI, isInternal);
		}
		catch ( SAXException e ) {
			throw new RuntimeException(e);
		}
		catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private void compileTranslateRule (Element elem,
		boolean isInternal)
	{
		ITSRule rule = new ITSRule();
		rule.selector = elem.getAttribute("selector");
		String value = elem.getAttribute("translate");
		if ( "yes".equals(value) ) rule.translate = true;
		else if ( "no".equals(value) ) rule.translate = false;
		else throw new RuntimeException("Invalid value for 'translate'.");
		rule.ruleType = IProcessor.DC_TRANSLATE;
		rule.isInternal = isInternal;
		rules.add(rule);
	}

	private void compileDirRule (Element elem,
		boolean isInternal)
	{
		ITSRule rule = new ITSRule();
		rule.selector = elem.getAttribute("selector");
		String value = elem.getAttribute("dir");
		if ( "ltr".equals(value) ) rule.dir = DIR_LTR;
		else if ( "rtl".equals(value) ) rule.dir = DIR_RTL;
		else if ( "lro".equals(value) ) rule.dir = DIR_LRO;
		else if ( "rlo".equals(value) ) rule.dir = DIR_RLO;
		else throw new RuntimeException("Invalid value for 'dir'.");
		rule.ruleType = IProcessor.DC_DIRECTIONALITY;
		rule.isInternal = isInternal;
		rules.add(rule);
	}

	private void compileWithinTextRule (Element elem,
			boolean isInternal)
		{
			ITSRule rule = new ITSRule();
			rule.selector = elem.getAttribute("selector");
			String value = elem.getAttribute("withinText");
			if ( "yes".equals(value) ) rule.withinText = WITHINTEXT_YES;
			else if ( "no".equals(value) ) rule.withinText = WITHINTEXT_NO;
			else if ( "nested".equals(value) ) rule.withinText = WITHINTEXT_NESTED;
			else throw new RuntimeException("Invalid value for 'withinText'.");
			rule.ruleType = IProcessor.DC_WITHINTEXT;
			rule.isInternal = isInternal;
			rules.add(rule);
		}

	private void compileTermRule (Element elem,
			boolean isInternal)
		{
			ITSRule rule = new ITSRule();
			rule.selector = elem.getAttribute("selector");
			// term
			String value = elem.getAttribute("term");
			if ( "yes".equals(value) ) rule.term = true;
			else if ( "no".equals(value) ) rule.term = false;
			else throw new RuntimeException("Invalid value for 'term'.");
			
			value = elem.getAttribute("termInfoPointer");
			String value2 = elem.getAttribute("termInfoRef");
			String value3 = elem.getAttribute("termInfoRefPointer");
			
			if ( value.length() > 0 ) {
				rule.termInfoType = TERMINFOTYPE_POINTER;
				rule.termInfo = value;
				if (( value2.length() > 0 ) || ( value3.length() > 0 )) {
					throw new RuntimeException("Too many termInfoXXX attributes specified");
				}
			}
			else {
				if ( value2.length() > 0 ) {
					rule.termInfoType = TERMINFOTYPE_REF;
					rule.termInfo = value2;
					if ( value3.length() > 0 ) {
						throw new RuntimeException("Too many termInfoXXX attributes specified");
					}
				}
				else {
					if ( value3.length() > 0 ) {
						rule.termInfoType = TERMINFOTYPE_REFPOINTER;
						rule.termInfo = value3;
					}
					// Else: No associate information, rule.termInfo is null
				}
			}

			rule.ruleType = IProcessor.DC_TERMINOLOGY;
			rule.isInternal = isInternal;
			rules.add(rule);
		}

	public void applyRules (int dataCategories) {
		processGlobalRules(dataCategories);
		processLocalRules(dataCategories);
	}

	private void removeFlag (Node node) {
		//TODO: Any possible optimization, instead of using recursive calls
		if ( node == null ) return;
		node.setUserData(FLAGNAME, null, null);
		if ( node.hasChildNodes() )
			removeFlag(node.getFirstChild());
		if ( node.getNextSibling() != null )
			removeFlag(node.getNextSibling());
	}
	
	public void disapplyRules () {
		removeFlag(doc.getDocumentElement());
	}

	public boolean backTracking () {
		return backTracking;
	}

	public Node nextNode () {
		if ( startTraversal ) {
			startTraversal = false;
			// Set the initial trace with default behaviors
			ITSTrace startTrace = new ITSTrace();
			startTrace.translate = true;
			startTrace.isChildDone = true;
			trace.push(startTrace); // For #document root
			node = doc.getDocumentElement();
			trace.push(new ITSTrace(trace.peek(), false));
			// Overwrite any default behaviors if needed
			updateTraceData(node);
			return node;
		}
		if ( node != null ) {
			backTracking = false;
			if ( !trace.peek().isChildDone && node.hasChildNodes() ) {
				// Change the flag for the current node
				ITSTrace tmp = new ITSTrace(trace.peek(), true);
				trace.pop();
				trace.push(tmp);
				// Get the new node and push its flag
				node = node.getFirstChild();
				trace.push(new ITSTrace(trace.peek(), false));
			}
			else {
				Node TmpNode = node.getNextSibling();
				if ( TmpNode == null ) {
					node = node.getParentNode();
					trace.pop();
					backTracking = true;
				}
				else {
					node = TmpNode;
					trace.pop(); // Remove flag for previous sibling
					trace.push(new ITSTrace(trace.peek(), false)); // Set new flag for new sibling
				}
			}
		}
		updateTraceData(node);
		return node;
	}
	
/*	public Node OLD_nextNode () {
		// Check for start or end
		backTracking = false;
		if ( startTraversal ) {
			startTraversal = false;
			// Set the initial trace with default behaviors
			ITSTrace startTrace = new ITSTrace();
			startTrace.translate = true;
			startTrace.isChildDone = false;
			trace.push(startTrace);
			node = doc.getDocumentElement();
			// Overwrite any default behaviors if needed
			updateTraceData(node);
			return node;
		}
		if ( node == null ) {
			return node;
		}

		while ( true ) {
			// Check for child
			if ( !trace.peek().isChildDone && node.hasChildNodes() ) {
				trace.peek().isChildDone = true;
				trace.push(new ITSTrace(trace.peek(), false));
				node = node.getFirstChild();
				updateTraceData(node);
				return node;
			}
			
			// Check for sibling
			if ( node.getNextSibling() != null ) {
				trace.pop(); // Remove previous sibling
				trace.push(new ITSTrace(trace.peek(), false));
				node = node.getNextSibling();
				updateTraceData(node);
				return node;
			}

			// Else: move back to parent
			trace.pop();
			node = node.getParentNode();
			backTracking = true;
			if (( node == null ) || ( trace.empty() )) return null;
		}
	}*/
	
	/**
	 * Updates the trace stack.
	 * @param newNode Node to update 
	 */
	private void updateTraceData (Node newNode) {
		// Check if the node is null
		if ( newNode == null ) return;

		// Get the flag data
		String data = (String)newNode.getUserData(FLAGNAME);
		
		// If this node has no ITS flags, then we leave the current states
		// as they are. They have been set by inheritance.
		if ( data == null ) return;
		
		// Otherwise: see if there are any flags to change
		if ( data.charAt(FP_TRANSLATE) != '?' ) {
			trace.peek().translate = (data.charAt(FP_TRANSLATE) == 'y');
		}
		
		if ( data.charAt(FP_DIRECTIONALITY) != '?' ) {
			switch ( data.charAt(FP_DIRECTIONALITY) ) {
			case '0':
				trace.peek().dir = DIR_LTR;
				break;
			case '1':
				trace.peek().dir = DIR_RTL;
				break;
			case '2':
				trace.peek().dir = DIR_LRO;
				break;
			case '3':
				trace.peek().dir = DIR_LRO;
				break;
			}
		}
		
		if ( data.charAt(FP_WITHINTEXT) != '?' ) {
			switch ( data.charAt(FP_WITHINTEXT) ) {
			case '0':
				trace.peek().withinText = WITHINTEXT_NO;
				break;
			case '1':
				trace.peek().withinText = WITHINTEXT_YES;
				break;
			case '2':
				trace.peek().withinText = WITHINTEXT_NESTED;
				break;
			}
		}
		
		if ( data.charAt(FP_TERMINOLOGY) != '?' ) {
			trace.peek().term = (data.charAt(FP_TERMINOLOGY) == 'y');
		//TODO: terminfo data
		}
		

	}

	public void startTraversal () {
		node = null;
		trace = new Stack<ITSTrace>();
		//trace.push(new ITSTrace(true)); // For root #document
		startTraversal = true;
	}
	
	private void clearInternalGlobalRules () {
		for ( int i=0; i<rules.size(); i++ ) {
			if ( rules.get(i).isInternal ) {
				rules.remove(i);
				i--;
			}
		}
	}

	private void processGlobalRules (int dataCategories) {
		try {
			// Compile any internal global rules
			clearInternalGlobalRules();
			compileRules(doc, docURI, true);
			
			// Now apply the compiled rules
		    for ( ITSRule rule : rules ) {
		    	// Check if we should apply this type of rule
		    	if ( (dataCategories & rule.ruleType) == 0 ) continue;
		    	
		    	// Get the selected nodes for the rule
				XPathExpression expr = xpath.compile(rule.selector);
				NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
				
				// Apply the rule specific action on the selected nodes
				// Global rules are applies before local so they should 
				// always override existing flag. override should be set to false
				// only for default attributes.
				for ( int i=0; i<NL.getLength(); i++ ) {
					switch ( rule.ruleType ) {
					case IProcessor.DC_TRANSLATE:
						setFlag(NL.item(i), FP_TRANSLATE,
							(rule.translate ? 'y' : 'n'), true);
						break;
					case IProcessor.DC_DIRECTIONALITY:
						setFlag(NL.item(i), FP_DIRECTIONALITY,
							String.valueOf(rule.dir).charAt(0), true);
						break;
					case IProcessor.DC_WITHINTEXT:
						setFlag(NL.item(i), FP_WITHINTEXT,
							String.valueOf(rule.withinText).charAt(0), true);
						break;
					case IProcessor.DC_TERMINOLOGY:
						//TODO: add the termInfo string too
						setFlag(NL.item(i), FP_TERMINOLOGY,
							(rule.term ? 'y' : 'n'), true);
						break;
					}
				}
		    }
		}
		catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private void processLocalRules (int dataCategories) {
		try {
			if ( (dataCategories & IProcessor.DC_TRANSLATE) > 0 ) {
				XPathExpression expr = xpath.compile("//*/@"+ITS_NS_PREFIX+":translate|//"+ITS_NS_PREFIX+":span/@translate");
				NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
				Attr attr;
				for ( int i=0; i<NL.getLength(); i++ ) {
					attr = (Attr)NL.item(i);
					// Skip irrelevant nodes
					if ( ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
						&& "translateRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
					// Validate the value
					String value = attr.getValue();
					if (( !"yes".equals(value) ) && ( !"no".equals(value) )) {
						throw new RuntimeException("Invalid value for 'translate'.");
					}
					// Set the flag
					setFlag(attr.getOwnerElement(), FP_TRANSLATE, value.charAt(0), attr.getSpecified());
				}
			}
			
			if ( (dataCategories & IProcessor.DC_DIRECTIONALITY) > 0 ) {
				XPathExpression expr = xpath.compile("//*/@"+ITS_NS_PREFIX+":dir|//"+ITS_NS_PREFIX+":span/@dir");
				NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
				Attr attr;
				for ( int i=0; i<NL.getLength(); i++ ) {
					attr = (Attr)NL.item(i);
					// Skip irrelevant nodes
					if ( ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
						&& "dirRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
					// Set the flag on the others
					int n = DIR_LTR;
					if ( "rtl".equals(attr.getValue()) ) n = DIR_LTR; 
					else if ( "ltr".equals(attr.getValue()) ) n = DIR_RTL; 
					else if ( "rlo".equals(attr.getValue()) ) n = DIR_RLO; 
					else if ( "lro".equals(attr.getValue()) ) n = DIR_LRO;
					else throw new RuntimeException("Invalid value for 'dir'."); 
					setFlag(attr.getOwnerElement(), FP_DIRECTIONALITY,
						String.format("%d", n).charAt(0), attr.getSpecified());
				}
			}
			
			if ( (dataCategories & IProcessor.DC_TERMINOLOGY) > 0 ) {
				XPathExpression expr = xpath.compile("//*/@"+ITS_NS_PREFIX+":term|//"+ITS_NS_PREFIX+":span/@term"
					+"//*/@"+ITS_NS_PREFIX+":termInfoPointer|//"+ITS_NS_PREFIX+":span/@termInfoPointer");
				NodeList NL = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
				Attr attr;
				String localName;
				for ( int i=0; i<NL.getLength(); i++ ) {
					attr = (Attr)NL.item(i);
					localName = attr.getLocalName();
					// Skip irrelevant nodes
					if ( ITS_NS_URI.equals(attr.getOwnerElement().getNamespaceURI())
						&& "termRule".equals(attr.getOwnerElement().getLocalName()) ) continue;
					// term
					if ( localName.equals("term") ) {
						// Validate the value
						String value = attr.getValue();
						if (( !"yes".equals(value) ) && ( !"no".equals(value) )) {
							throw new RuntimeException("Invalid value for 'term'.");
						}
						// Set the flag
						setFlag(attr.getOwnerElement(), FP_TERMINOLOGY, value.charAt(0), attr.getSpecified());
					}
					else if ( localName.equals("termInfoPointer") ) {
						setFlag(attr.getOwnerElement(), FP_TERMINOLOGY_DATA, attr.getValue(), attr.getSpecified());
					}
				}
			}
		}
		catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the flag for a given node.
	 * @param node The node to flag.
	 * @param position The position for the data category.
	 * @param value The value to set.
	 * @param override True if the value should override an existing value.
	 * False should be used only for default attribute values.
	 */
	private void setFlag (Node node,
		int position,
		char value,
		boolean override)
	{
		StringBuilder data = new StringBuilder();
		if ( node.getUserData(FLAGNAME) == null )
			data.append("????"+FLAGSEP+FLAGSEP);
		else
			data.append((String)node.getUserData(FLAGNAME));
		// Set the new value (if not there yet or override requested)
		if ( override || ( data.charAt(position) != '?' )) 
			data.setCharAt(position, value);
		node.setUserData(FLAGNAME, data.toString(), null);
	}

	/**
	 * Sets the data for a flag for a given node.
	 * @param node The node where to set the data.
	 * @param position The position for the data category.
	 * @param value The value to set.
	 * @param override True if the value should override an existing value.
	 * False should be used only for default attribute values.
	 */
	private void setFlag (Node node,
		int position,
		String value,
		boolean override)
	{
		StringBuilder data = new StringBuilder();
		if ( node.getUserData(FLAGNAME) == null )
			data.append("????"+FLAGSEP+FLAGSEP);
		else
			data.append((String)node.getUserData(FLAGNAME));
		// Get the data
		int n1 = 0;
		int n2 = 0;
		int start = 0;
		for ( int i=0; i<=position; i++ ) {
			n1 = data.indexOf(FLAGSEP, start+1);
			n2 = data.indexOf(FLAGSEP, n1+1);
			start = n2;
		}
		// Set the new value (if not there yet or override requested)
		if ( override || ( n2>n1+1 )) {
			data.replace(n1+1, n2, value);
		}
		node.setUserData(FLAGNAME, data.toString(), null);
	}

	public boolean translate () {
		return trace.peek().translate;
	}
	
	public boolean translate (Attr attribute) {
		if ( attribute == null ) return false;
		String tmp;
		if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return false;
		// '?' and 'n' will return (correctly) false
		return (tmp.charAt(FP_TRANSLATE) == 'y');
	}
	
	public int getDirectionality () {
		return trace.peek().dir;
	}

	public int getDirectionality (Attr attribute) {
		if ( attribute == null ) return DIR_LTR;
		String tmp;
		if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return DIR_LTR;
		return Integer.valueOf(tmp.charAt(FP_DIRECTIONALITY));
	}
	
	public int getWithinText () {
		return trace.peek().withinText;
	}
	
	public boolean isTerm () {
		return trace.peek().term;
	}

	public boolean isTerm (Attr attribute) {
		if ( attribute == null ) return false;
		String tmp;
		if ( (tmp = (String)attribute.getUserData(FLAGNAME)) == null ) return false;
		// '?' and 'n' will return (correctly) false
		return (tmp.charAt(FP_TERMINOLOGY) == 'y');
	}
	
}
