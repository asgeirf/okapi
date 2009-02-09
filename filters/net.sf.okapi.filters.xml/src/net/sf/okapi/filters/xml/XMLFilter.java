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

import java.io.File;
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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.its.IProcessor;
import org.w3c.its.ITSEngine;
import org.w3c.its.ITraversal;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.Ending;
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
	private Document doc;
	private ITraversal trav;
	private LinkedList<FilterEvent> queue;
	private int tuId;
	private int otherId;
	private int parseState;
	private TextFragment frag;
	private GenericSkeleton skel;
	private Stack<Node> stack;
	
	public void cancel () {
		queue.clear();
		queue.add(new FilterEvent(FilterEventType.CANCELED));
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
			commonOpen(0, inputURL.openStream());
		}
		catch ( IOException e ) {
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
	 * @param type Indicates the type of obj: 0=InputStream, 1=File, 2=InputSource.
	 * @param obj The object to read.
	 */
	private void commonOpen (int type,
		Object obj)
	{
		close();
		// Initializes the variables
		tuId = 0;
		otherId = 0;
		parseState = 0;
		//lineBreak = System.getProperty("line.separator"); //TODO: Auto-detection of line-break type

		// Create the document builder
		DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		fact.setNamespaceAware(true);
		fact.setValidating(false);
		
		// Load the document
		try {
			switch ( type ) {
			case 0: // InputStream
				doc = fact.newDocumentBuilder().parse((InputStream)obj);
				break;
			case 1: // File
				doc = fact.newDocumentBuilder().parse((File)obj);
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
		try {
			itsEng = new ITSEngine(doc, new URI("http://test")); //doc.getDocumentURI()));
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		
		// Apply the all rules (external and internal) to the document
		itsEng.applyRules(IProcessor.DC_TRANSLATE | IProcessor.DC_LANGINFO 
			| IProcessor.DC_LOCNOTE | IProcessor.DC_WITHINTEXT);
		
		trav = itsEng;
		trav.startTraversal();
		stack = new Stack<Node>();
		
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
		queue.add(new FilterEvent(FilterEventType.START_DOCUMENT, startDoc));
	}

	private void process () {
		Node node;
		frag = null;
		skel = new GenericSkeleton();
		
		while ( true ) {
			node = trav.nextNode();
			if ( node == null ) { // No more node: we stop
				Ending ending = new Ending(String.valueOf(++otherId));
				queue.add(new FilterEvent(FilterEventType.END_DOCUMENT, ending));
				parseState = 1;
				return;
			}
			
			// Else: valid node
			switch ( node.getNodeType() ) {
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				if ( frag == null ) {
					skel.append(node.getNodeValue());
				}
				else {
					frag.append(node.getNodeValue());
				}
				break;
				
			case Node.ELEMENT_NODE:
				if ( processElement(node) ) return;
				break;
				
			case Node.PROCESSING_INSTRUCTION_NODE:
				//TODO: implement pi
				break;
				
			case Node.COMMENT_NODE:
				if ( frag == null ) {
					skel.add("<!--"+node.getNodeValue()+"-->");
				}
				else {
					frag.append(TagType.PLACEHOLDER, null, "<!--"+node.getNodeValue()+"-->");
				}
				break;
			}
		}
	}

	private String buildStartTag (Node node) {
		StringBuilder tmp = new StringBuilder();
		tmp.append("<"+node.getLocalName());
		if ( node.hasAttributes() ) {
			NamedNodeMap list = node.getAttributes();
			Node attr;
			for ( int i=0; i< list.getLength(); i++ ) {
				attr = list.item(i);
				tmp.append(" "+attr.getLocalName()+"=\""+attr.getNodeValue()+"\"");
			}
		}
		if ( !node.hasChildNodes() ) tmp.append("/");
		tmp.append(">");
		return tmp.toString();
	}

	private String buildEndTag (Node node) {
		if ( node.hasChildNodes() ) {
			return "</"+node.getLocalName()+">";
		}
		else {
			return "";
		}
	}

	/**
	 * Processes an element node.
	 * @param node Node to process.
	 * @return True if we need to return, false to continue processing.
	 */
	private boolean processElement (Node node) {
		if ( trav.backTracking() ) {
			if ( frag == null ) { // Not an extraction: in skeleton
				skel.add(buildEndTag(node));
			}
			else { // Else we are within an extraction
				if ( node == stack.peek() ) { // End of text-unit
					addTextUnit(node);
					return true;
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
				skel.add(buildStartTag(node));
				if ( frag == null ) { // Not yet in extraction
					if ( node.hasChildNodes() && trav.translate() ) {
						stack.push(node);
						frag = new TextFragment();
					}
				}
				else { // Already in extraction
					// Queue the current item
					addTextUnit(node);
					// And create a new one
					if ( node.hasChildNodes() && trav.translate() ) {
						stack.push(node);
						frag = new TextFragment();
					}
				}
				break;
			}
		}
		return false;
	}

	private void addTextUnit (Node node) {
		stack.pop();
		// Create a unit only if needed
		if ( !frag.hasCode() && !frag.hasText(false) ) {
			frag = null;
			return;
		}
		// Create the unit
		TextUnit tu = new TextUnit(String.valueOf(++tuId));
		tu.setSourceContent(frag);
		skel.addContentPlaceholder(tu);
		skel.add(buildEndTag(node));
		tu.setSkeleton(skel);
		queue.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
		frag = null;
	}
	
}
