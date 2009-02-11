/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.filters.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.its.IProcessor;
import org.w3c.its.ITSEngine;
import org.w3c.its.ITraversal;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.filters.IEncoder;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

public class XMLFilter implements IFilter {

	private String docName;
	private String encoding;
	private String srcLang;
	private String lineBreak;
	private Document doc;
	private ITraversal trav;
	private LinkedList<FilterEvent> queue;
	private int tuId;
	private int otherId;
	private int parseState;
	private TextFragment frag;
	private GenericSkeleton skel;
	private Stack<ContextItem> context;
	private boolean canceled;
	
	public void cancel () {
		canceled = true;
	}

	public void close () {
	}

	public ISkeletonWriter createSkeletonWriter () {
		return new GenericSkeletonWriter();
	}

	public String getName () {
		return "okf_xml";
	}

	public IParameters getParameters () {
		return null;
	}

	public boolean hasNext () {
		return (queue != null);
	}

	public FilterEvent next () {
		if ( canceled ) {
			queue = null;
			return new FilterEvent(FilterEventType.CANCELED);
		}
		if ( queue == null ) return null;

		// Process queue if it's not empty yet
		if ( queue.size() > 0 ) {
			return queue.poll();
		}

		// Process the next item, filling the queue
		if ( parseState != 1 ) {
			process();
			// Send next event after processing, if there is one
			if ( queue.size() > 0 ) {
				return queue.poll();
			}
		}

		// Else: we are done
		queue = null;
		return new FilterEvent(FilterEventType.FINISHED, null);
	}

	public void open (InputStream input) {
		commonOpen(0, input);
	}

	public void open (CharSequence inputText) {
		encoding = "UTF-16";
		InputSource is = new InputSource(new StringReader(inputText.toString()));
		commonOpen(2, is);
	}

	public void open (URL inputURL) {
		try {
			docName = inputURL.getPath();
			commonOpen(1, inputURL.toURI());
		}
		catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
	}

	public void setOptions (String sourceLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		setOptions(sourceLanguage, null, defaultEncoding, generateSkeleton);
	}

	public void setOptions (String sourceLanguage,
		String targetLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		srcLang = sourceLanguage;
		encoding = defaultEncoding;
	}

	public void setParameters (IParameters params) {
	}

	/**
	 * Shared open method for the public open() calls.
	 * @param type Indicates the type of obj: 0=InputStream, 1=URI, 2=InputSource.
	 * @param obj The object to read.
	 */
	private void commonOpen (int type,
		Object obj)
	{
		close();
		// Initializes the variables
		canceled = false;
		tuId = 0;
		otherId = 0;
		parseState = 0;
		lineBreak = System.getProperty("line.separator"); //TODO: Auto-detection of line-break type

		// Create the document builder
		DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		fact.setNamespaceAware(true);
		fact.setValidating(false);
		URI uri = null;
		
		// Load the document
		try {
			switch ( type ) {
			case 0: // InputStream
				doc = fact.newDocumentBuilder().parse((InputStream)obj);
				break;
			case 1: // URI
				uri = (URI)obj;
				doc = fact.newDocumentBuilder().parse(uri.toString());
				break;
			case 2: // InputSource
				doc = fact.newDocumentBuilder().parse((InputSource)obj);
				break;
			}
		}
		catch ( SAXException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}
		
		// Create the ITS engine
		ITSEngine itsEng;
		itsEng = new ITSEngine(doc, uri);
		
		// Apply the all rules (external and internal) to the document
		itsEng.applyRules(IProcessor.DC_TRANSLATE | IProcessor.DC_LANGINFO 
			| IProcessor.DC_LOCNOTE | IProcessor.DC_WITHINTEXT);
		
		trav = itsEng;
		trav.startTraversal();
		context = new Stack<ContextItem>();
		
		// Set the start event
		queue = new LinkedList<FilterEvent>();
		queue.add(new FilterEvent(FilterEventType.START));

		StartDocument startDoc = new StartDocument(String.valueOf(++otherId));
		startDoc.setName(docName);
		String realEnc = doc.getInputEncoding();
		if ( realEnc != null ) encoding = realEnc;
		startDoc.setEncoding(encoding);
		startDoc.setLanguage(srcLang);
		//TODO: startDoc.setFilterParameters(params);
		startDoc.setType("text/xml");
		startDoc.setMimeType("text/xml");

		// Add the XML declaration
		skel = new GenericSkeleton();
		skel.add("<?xml version=\"" + doc.getXmlVersion() + "\"");
		skel.add(" encoding=\"");
		skel.addValuePlaceholder(startDoc, IEncoder.PROP_ENCODING, "");
		skel.add("\"");
		startDoc.setProperty(new Property(IEncoder.PROP_ENCODING, encoding, false));
		
		if ( doc.getXmlStandalone() ) skel.add(" standalone=\"true\"");
		skel.add("?>"+lineBreak);
		startDoc.setSkeleton(skel);
		// Put the start document in the queue
		queue.add(new FilterEvent(FilterEventType.START_DOCUMENT, startDoc));
	}

	private void process () {
		Node node;
		frag = null;
		skel = new GenericSkeleton();
		
		if ( context.size() > 0 ) {
			// If we are within an element, reset the frag to append to it
			if ( trav.translate() ) { // The stack is up-to-date already
				frag = new TextFragment();
			}
		}
		
		while ( true ) {
			node = trav.nextNode();
			if ( node == null ) { // No more node: we stop
				Ending ending = new Ending(String.valueOf(++otherId));
				if (( skel != null ) && ( !skel.isEmpty() )) {
					ending.setSkeleton(skel);
				}
				queue.add(new FilterEvent(FilterEventType.END_DOCUMENT, ending));
				parseState = 1;
				return;
			}
			
			// Else: valid node
			switch ( node.getNodeType() ) {
			case Node.CDATA_SECTION_NODE:
				if ( frag == null ) {
					skel.append(buildCDATA(node));
				}
				else {
					if ( trav.translate() ) {
						frag.append(node.getNodeValue());
					}
					else {
						frag.append(TagType.PLACEHOLDER, null, buildCDATA(node));
					}
				}
				break;

			case Node.TEXT_NODE:
				if ( frag == null ) {//TODO: escape unsupported chars
					skel.append(Util.escapeToXML(node.getNodeValue(), 0, false, null));
				}
				else {
					if ( trav.translate() ) {
						frag.append(node.getNodeValue());
					}
					else {//TODO: escape unsupported chars
						frag.append(TagType.PLACEHOLDER, null, Util.escapeToXML(node.getNodeValue(), 0, false, null));
					}
				}
				break;
				
			case Node.ELEMENT_NODE:
				if ( processElementTag(node) ) return;
				break;
				
			case Node.PROCESSING_INSTRUCTION_NODE:
				if ( frag == null ) {
					skel.add(buildPI(node));
				}
				else {
					frag.append(TagType.PLACEHOLDER, null, buildPI(node));
				}
				break;
				
			case Node.COMMENT_NODE:
				if ( frag == null ) {
					skel.add(buildComment(node));
				}
				else {
					frag.append(TagType.PLACEHOLDER, null, buildComment(node));
				}
				break;
			}
		}
	}

	private String buildStartTag (Node node) {
		StringBuilder tmp = new StringBuilder();
		tmp.append("<"
			+ ((node.getPrefix()==null) ? "" : node.getPrefix()+":")
			+ node.getLocalName());
		if ( node.hasAttributes() ) {
			NamedNodeMap list = node.getAttributes();
			Attr attr;
			//TODO: Keep the original order... NamedNodeMap does not
			for ( int i=0; i<list.getLength(); i++ ) {
				attr = (Attr)list.item(i);
				if ( !attr.getSpecified() ) continue; // Skip auto-attributes
				tmp.append(" "
					+ ((attr.getPrefix()==null) ? "" : attr.getPrefix()+":")
					+ attr.getLocalName() //TODO: escape unsupported chars
					+ "=\"");
				//TODO: extract if needed
				if ( trav.translate(attr) ) {
					tmp.append("[["+Util.escapeToXML(attr.getNodeValue(), 3, false, null)
						+ "]]\"");
				}
				else {
					tmp.append(Util.escapeToXML(attr.getNodeValue(), 3, false, null)
						+ "\"");
				}
			}
		}
		if ( !node.hasChildNodes() ) tmp.append("/");
		tmp.append(">");
		return tmp.toString();
	}

	private String buildEndTag (Node node) {
		if ( node.hasChildNodes() ) {
			return "</"
				+ ((node.getPrefix()==null) ? "" : node.getPrefix()+":")
				+ node.getLocalName() + ">";
		}
		else { // Start tag was set as an empty element
			return "";
		}
	}
	
	private String buildPI (Node node) {
		// Do not escape PI content
		return "<?" + node.getNodeName() + " " + node.getNodeValue() + "?>";
	}
	
	private String buildCDATA (Node node) {
		// Do not escape CDATA content
		return "<![CDATA[" + node.getNodeValue() + "]]>";
	}
	
	private String buildComment (Node node) {
		// Do not escape comments
		return "<!--" + node.getNodeValue() + "-->";
	}

	/**
	 * Processes the start or end tag of an element node.
	 * @param node Node to process.
	 * @return True if we need to return, false to continue processing.
	 */
	private boolean processElementTag (Node node) {
		if ( trav.backTracking() ) {
			if ( frag == null ) { // Not an extraction: in skeleton
				skel.add(buildEndTag(node));
				if ( node.isSameNode(context.peek().node) ) context.pop();
				if ( isContextTranslatable() ) { // We are after non-translatable withinText='no', check parent again.
					frag = new TextFragment();
				}
			}
			else { // Else we are within an extraction
				if ( node.isSameNode(context.peek().node) ) { // End of possible text-unit
					return addTextUnit(node, true);
				}
				else { // Within text
					frag.append(TagType.CLOSING, node.getLocalName(), buildEndTag(node));
				}
			}
		}
		else { // Else: Start tag
			switch ( trav.getWithinText() ) {
			case ITraversal.WITHINTEXT_YES:
			case ITraversal.WITHINTEXT_NESTED: //TODO: deal with nested elements
				if ( frag == null ) { // Not yet in extraction
					// Strange case: inline without parent???
					//TODO: do something about this, warning?
					assert(false);
				}
				else { // Already in extraction
					frag.append(TagType.OPENING, node.getLocalName(), buildStartTag(node));					
				}
				break;
			default: // Not within text
				if ( frag == null ) { // Not yet in extraction
					skel.add(buildStartTag(node));
					if ( node.hasChildNodes() && trav.translate() ) {
						context.push(new ContextItem(node, trav.translate()));
						frag = new TextFragment();
					}
				}
				else { // Already in extraction
					// Queue the current item
					addTextUnit(node, false);
					skel.add(buildStartTag(node));
					// And create a new one
					if ( node.hasChildNodes() && trav.translate() ) {
						context.push(new ContextItem(node, trav.translate()));
						frag = new TextFragment();
					}
				}
				break;
			}
		}
		return false;
	}

	private boolean isContextTranslatable () {
		if ( context.size() == 0 ) return false;
		return context.peek().translate;
	}
	
	/**
	 * Adds a text unit to the queue if needed.
	 * @param node The current node.
	 * @param popStack True to pop the stack, false to leave the stack alone.
	 * @return True if a text unit was added to the queue, false otherwise.
	 */
	private boolean addTextUnit (Node node,
		boolean popStack)
	{
		if ( popStack ) context.pop();
		// Create a unit only if needed
		if ( !frag.hasCode() && !frag.hasText(false) ) {
			if ( !frag.isEmpty() ) { // Nothing but white spaces
				skel.add(frag.toString()); // Pass them as skeleton
			}
			frag = null;
			if ( popStack ) skel.add(buildEndTag(node));
			return false;
		}
		// Create the unit
		TextUnit tu = new TextUnit(String.valueOf(++tuId));
		tu.setMimeType("text/xml");
		tu.setSourceContent(frag);
		skel.addContentPlaceholder(tu);
		if ( popStack ) skel.add(buildEndTag(node));
		tu.setSkeleton(skel);
		queue.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
		frag = null;
		skel = new GenericSkeleton();
		return true;
	}
	
}
