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

package net.sf.okapi.filters.openoffice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.filters.openoffice.ElementData.WithinTextType;

/**
 * This class implements IFilter for XML documents in Open-Document format (ODF).
 * The expected input is the XML document itself. It can be used on ODF documents
 * that are not in Open-Office.org files (i.e. directly on the content.xml of the .odt).
 * For processing ODT, ODS, ODP or ODG zipped documents, use the OpenDocumentFilter class,
 * which calls this filter as needed.
 */
public class ODFFilter2 implements IFilter {

	protected static final String NSURI_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	protected static final String NSURI_XLINK = "http://www.w3.org/1999/xlink";

	private Hashtable<String, ElementData> rules;
	private Stack<ContextItem> ctx;
	private LinkedList<FilterEvent> queue;
	private String docName;
	private XMLStreamReader reader;
	private int otherId;
	private int tuId;
	private String language;
	private Parameters params;
	private GenericSkeleton skel;
	private TextFragment tf;
	private TextUnit tu;
	private boolean canceled;
	private boolean hasNext;

	public ODFFilter2 () {
		params = new Parameters();

		rules = new Hashtable<String, ElementData>();

		rules.put("text:p", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("text:h", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("dc:title", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("dc:description", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("dc:subject", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("meta:keyword", new ElementData(WithinTextType.EXTERNAL, true, false));
		rules.put("meta:user-defined", new ElementData(WithinTextType.EXTERNAL, true, false, "meta:name"));
		rules.put("text:index-title-template", new ElementData(WithinTextType.EXTERNAL, true, false));

		rules.put("text:note", new ElementData(WithinTextType.EMBEDDED, false, true));
		
		rules.put("text:initial-creator", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:creation-date", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:creation-time", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:description", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:user-defined", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:print-time", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:print-date", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:printed-by", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:title", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:subject", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:keywords", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:editing-cycles", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:editing-duration", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:modification-time", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:modification-date", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:creator", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:page-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:paragraph-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:word-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:character-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:table-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:image-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("text:object-count", new ElementData(WithinTextType.INTERNAL, false));
		rules.put("dc:date", new ElementData(WithinTextType.INTERNAL, false));
	}

	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
			hasNext = false;
		}
		catch ( XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	public void cancel () {
		canceled = true;
	}

	public boolean hasNext () {
		return hasNext;
	}

	public void open (InputStream input) {
		try {
			close();
			canceled = false;
			
			XMLInputFactory fact = XMLInputFactory.newInstance();
			fact.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			fact.setProperty(XMLInputFactory.IS_COALESCING, true);
			fact.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, true);
			fact.setProperty(XMLInputFactory2.P_AUTO_CLOSE_INPUT, true);
			reader = fact.createXMLStreamReader(input);

			ctx = new Stack<ContextItem>();
			ctx.push(new ContextItem(false, null, skel)); // Empty initial context
			otherId = 0;
			tuId = 0;

			queue = new LinkedList<FilterEvent>();
			queue.add(new FilterEvent(FilterEventType.START));
			hasNext = true;
			
			StartDocument startDoc = new StartDocument(String.valueOf(++otherId));
			startDoc.setLanguage(language);
			startDoc.setName(docName);
			startDoc.setMimeType("text/x-odf"); //TODO Use proper mime type value
			startDoc.setType(startDoc.getMimeType());
			queue.add(new FilterEvent(FilterEventType.START_DOCUMENT, startDoc));
		}
		catch ( XMLStreamException e ) {
			throw new RuntimeException(e);
		}
	}

	public void open (CharSequence input) {
		//TODO: Check for better solution, going from char to byte to read char is just not good
		try {
			open(new ByteArrayInputStream(input.toString().getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} 
	}

	public void open (URL inputUrl) {
		try {
			docName = inputUrl.getPath();
			open(inputUrl.openStream());
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public String getName () {
		return "ODFFilter";
	}

	public IParameters getParameters () {
		return params;
	}

	public FilterEvent next () {
		if ( canceled ) {
			queue.clear();
			queue.add(new FilterEvent(FilterEventType.CANCELED));
			hasNext = false;
		}
		
		if ( queue.isEmpty() ) {
			read();
		}

		// Update hasNext flag on last event
		if ( queue.peek().getEventType() == FilterEventType.FINISHED ) {
			hasNext = false;
		}
		// Return the head of the queue
		return queue.poll();
	}
	
	public void setParameters (IParameters params) {
		params = (Parameters)params;
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
		language = sourceLanguage;
	}

	private boolean read () {
		skel = new GenericSkeleton();
		tf = new TextFragment();
		try {
			while ( reader.hasNext() ) {
				switch ( reader.next() ) {
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					if ( ctx.peek().inText ) tf.append(reader.getText());
					else skel.append(reader.getText()); //TODO: need to escape
					break;
					
				case XMLStreamConstants.START_DOCUMENT:
					// Never called because of bug in STAX reader?
					//TODO set resource.setTargetEncoding(SET REAL ENCODING);
					skel.append("<?xml version=\"1.0\" "
						+ ((reader.getEncoding()==null) ? "" : "encoding=\""+reader.getEncoding()+"\"")
						+ "?>");
					break;
				
				case XMLStreamConstants.END_DOCUMENT:
					Ending ending = new Ending(String.valueOf(++otherId));
					ending.setSkeleton(skel);
					queue.add(new FilterEvent(FilterEventType.END_DOCUMENT, ending));
					queue.add(new FilterEvent(FilterEventType.FINISHED));
					return true;
				
				case XMLStreamConstants.START_ELEMENT:
					processStartElement();
					break;
				
				case XMLStreamConstants.END_ELEMENT:
					if ( processEndElement() ) return true; // Send an event
					break;
				
				case XMLStreamConstants.COMMENT:
					if ( ctx.peek().inText ) {
						tf.append(TagType.PLACEHOLDER, null, "<!--" + reader.getText() + "-->");
					}
					else {
						skel.append("<!--" + reader.getText() + "-->");
					}
					break;

				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					if ( ctx.peek().inText ) {
						tf.append(TagType.PLACEHOLDER, null, "<?" + reader.getPITarget() + " " + reader.getPIData() + "?>");
					}
					else {
						skel.append("<?" + reader.getPITarget() + " " + reader.getPIData() + "?>");
					}
					break;
				}
			} // End of main while		

		}
		catch ( XMLStreamException e ) {
			throw new RuntimeException(e);
		}
		return true;
	}

	private void gatherInfo (String name) {
		tu.setType("x-"+name);
		//lang?? 
		//id???
	}
	
	private String buildStartTag (String name) {
		StringBuilder tmp = new StringBuilder();
		// Tag name
		tmp.append("<" + name);
		
		// Namespaces
		String prefix;
		int count = reader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = reader.getNamespacePrefix(i);
			tmp.append(String.format(" xmlns%s=\"%s\"",
				((prefix.length()>0) ? ":"+prefix : ""),
				reader.getNamespaceURI(i)));
		}

		// Attributes
		count = reader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = reader.getAttributePrefix(i); 
			tmp.append(String.format(" %s%s=\"%s\"",
				(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
				reader.getAttributeLocalName(i),
				reader.getAttributeValue(i))); //TODO: Are quotes escaped???
		}

		tmp.append(">");
		return tmp.toString();
	}
	
	private String buildEndTag (String name) {
		return "</" + name + ">";
	}
	
	private String makePrintName () {
		String prefix = reader.getPrefix();
		if (( prefix == null ) || ( prefix.length()==0 )) {
			return reader.getLocalName();
		}
		// Else: with a prefix
		return prefix + ":" + reader.getLocalName();
	}
	
	//TODO: use element itself, and attribute-based conditions
	private ElementData getRule (String name) {
		return rules.get(name); // Null if not found
	}
	
	/**
	 * If needed, sends a DocumentPart event with the current skeleton.
	 */
	private void checkForDocumentPart () {
		if ( !skel.isEmpty(true) ) {
			DocumentPart dp = new DocumentPart(String.valueOf(++otherId), false);
			dp.setSkeleton(skel);
			queue.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));
			skel = new GenericSkeleton(); // Start new skeleton 
		}
	}
	
	private void closeTextUnit () {
		if ( tu == null ) return;
		
		skel.addContentPlaceholder(tu);
		tu.setSourceContent(tf);
		tu.setId(String.valueOf(++tuId));
		tu.setMimeType("text/x-odf");
		queue.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
	}
	
	private void startTextUnit (ElementData rule,
		String name)
	{
		checkForDocumentPart();
		skel.append(buildStartTag(name));
		//TODO: need a way to set the TextUnit's name/id/restype/etc.
		tu = new TextUnit(null); // ID set only if needed
		tu.setSkeleton(skel); 
		gatherInfo(name);
		tf = new TextFragment();
		tu.setSourceContent(tf);
	}
	
	private void pushTextUnit () {
		String id = String.valueOf(++tuId);
		Code code = tf.append(TagType.PLACEHOLDER, null, TextFragment.makeRefMarker(id));
		code.setHasReference(true);
		tu = new TextUnit(id);
		tu.setIsReferent(true);
		skel = new GenericSkeleton();
		tu.setSkeleton(skel);
		tf = new TextFragment();
		tu.setSourceContent(tf);
	}
	
	private void addStartTagAsInlineCode (String name) {
		//TODO: deal with extractable atributes
		tf.append(new Code(TagType.OPENING, name, buildStartTag(name)));
	}
	
	private void addStartTagAsSkeleton (String name) {
		//TODO: deal with extractable atributes
		skel.append(buildStartTag(name));
	}
	
	private void processStartElement () throws XMLStreamException {
		String name = makePrintName();
		ElementData rule = getRule(name);
		if ( rule == null ) { // No specific rule, use the defaults
			// Create a rule based on context
			rule = new ElementData(WithinTextType.EXTERNAL, true);
			if (( ctx.size() > 1 ) && ( !ctx.peek().inText )) rule.translate = false;
			//todo etc.
		}
		
		if ( ctx.peek().inText ) {
			switch ( rule.getCase() ) {
			case TRANS_EXTERNAL:
				closeTextUnit();
				addStartTagAsSkeleton(name);
				startTextUnit(rule, name);
				ctx.push(new ContextItem(true, tu, skel));
				break;
			case TRANS_INTERNAL:
			case NOTRANS_INTERNAL:
				addStartTagAsInlineCode(name);
				ctx.push(new ContextItem(true, tu, skel));
				break;
			case TRANS_EMBEDDED:
				pushTextUnit();
				addStartTagAsSkeleton(name);
				ctx.push(new ContextItem(true, tu, skel));
				break;
			case NOTRANS_EXTERNAL:
				closeTextUnit();
				addStartTagAsSkeleton(name);
				ctx.push(new ContextItem(false, null, skel));
				break;
			case NOTRANS_EMBEDDED:
				addStartTagAsInlineCode(name);
				ctx.push(new ContextItem(false, null, skel));
				break;
			}
		}
		else { // Not in text
			switch ( rule.getCase() ) {
			case TRANS_EXTERNAL:
				startTextUnit(rule, name);
				ctx.push(new ContextItem(true, tu, skel));
				break;
			case TRANS_EMBEDDED:
			case TRANS_INTERNAL:
				startTextUnit(rule, name);
				ctx.push(new ContextItem(true, tu, skel));
				break;
			case NOTRANS_EXTERNAL:
			case NOTRANS_INTERNAL:
			case NOTRANS_EMBEDDED:
				addStartTagAsSkeleton(name);
				ctx.push(new ContextItem(false, null, skel));
				break;
			}
		}
	}

	private boolean processEndElement () {
		String name = makePrintName();
		ElementData rule = getRule(name);
		if ( rule == null ) {
			// Create a rule based on context
			rule = new ElementData(WithinTextType.EXTERNAL, true);
			if (( ctx.size() > 1 ) && ( !ctx.peek().inText )) rule.translate = false;
			//todo etc.
		}
		
		if ( ctx.peek().inText ) {
			switch ( rule.withinText ) {
			case EXTERNAL:
				closeTextUnit();
				skel.append(buildEndTag(name)+"\n");
				ctx.pop();
				return true;
			case INTERNAL:
				tf.append(new Code(TagType.CLOSING, name, buildEndTag(name)));
				ctx.pop();
				break;
			case EMBEDDED:
				closeTextUnit();
				tf.append(new Code(TagType.CLOSING, name, buildEndTag(name)));
				ctx.pop();
				tu = ctx.peek().tu;
				skel = ctx.peek().skel;
				tf = tu.getSourceContent();
				break;
			}
		}
		else { // Not in text
			switch ( rule.withinText ) {
			case EXTERNAL:
				skel.append(buildEndTag(name)+"\n");
				ctx.pop();
				break;
			case INTERNAL:
			case EMBEDDED:
				tf.append(new Code(TagType.CLOSING, name, buildEndTag(name)));
				ctx.pop();
				break;
			}
		}
		
		return false;
	}
	
	private void processStartALink (String name) {
		String data = buildStartTag(name);
		String href = reader.getAttributeValue(NSURI_XLINK, "href");
		if ( href != null ) {
			//TODO: set the property, but where???
		}
		tf.append(new Code(TagType.OPENING, name, data));
	}
	
	private void processReadOnlyInlineElement (String name) throws XMLStreamException {
		StringBuilder tmp = new StringBuilder(buildStartTag(name));
		while ( true ) {
			switch ( reader.next() ) {
			case XMLStreamConstants.CHARACTERS:
				tmp.append(reader.getText());
				break;
			case XMLStreamConstants.START_ELEMENT:
				tmp.append(buildStartTag(makePrintName()));
				break;
			case XMLStreamConstants.END_ELEMENT:
				String tmpName = makePrintName();
				tmp.append(buildEndTag(tmpName));
				if ( tmpName.equals(name) ) {
					tf.append(new Code(TagType.PLACEHOLDER, name, tmp.toString()));
					return;
				}
				break;
			case XMLStreamConstants.COMMENT:
				tmp.append("<!--" + reader.getText() + "-->");
				break;
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				tmp.append("<?" + reader.getPITarget() + " "
					+ reader.getPIData() + "?>");
				break;
			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.END_DOCUMENT:
				// Should not occur
				throw new RuntimeException("Invalid start or end document detected while processing inline element.");
			}
		}		
	}
	
	/*
	// Return true when it's ready to send an event
	private boolean processEndElement () {
		String name = makePrintName();
		if ( toExtract.containsKey(name) ) {
			extract.pop();
			skel.addRef(tu);
			tu.setId(String.valueOf(++tuId));
			tu.setSourceContent(tf);
			tu.setSkeleton(skel);
			tu.setMimeType("text/x-odf");
			// Add line break because ODF files don't have any
			// They are needed for example in RTF output
			//TODO: Maybe have this as an options set through the parameters but not user-driven?
			// Note: we may keep adding extra lines if one exists already!
			//TODO: find a way to add \n only if needed
			skel.append(buildEndTag(name)+"\n");
			queue.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
			return true;
		}
		else {
			if ( extract.peek() ) {
				tf.append(new Code(TagType.CLOSING, name, buildEndTag(name)));
			}
			else {
				skel.append(buildEndTag(name));
				if ( name.equals("style:style")
					|| ( name.equals("text:list-style"))
					|| ( name.equals("draw:frame"))
					|| ( name.equals("text:list"))
					|| ( name.equals("text:list-item")) ) {
					skel.append("\n");
				}
			}
		}
		if ( name.equals("office:document-content") ) {
			Ending ending = new Ending(String.valueOf(++otherId));
			ending.setSkeleton(skel);
			queue.add(new FilterEvent(FilterEventType.END_DOCUMENT, ending));
			queue.add(new FilterEvent(FilterEventType.FINISHED));
			return true;
		}
		return false;
	}
*/
}
