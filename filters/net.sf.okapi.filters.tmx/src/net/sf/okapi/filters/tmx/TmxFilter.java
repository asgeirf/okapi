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
============================================================================*/

package net.sf.okapi.filters.tmx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.BadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.InputResource;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import org.codehaus.stax2.XMLInputFactory2;

public class TmxFilter implements IFilter {

	private boolean hasNext;
	private boolean canceled;
	private XMLStreamReader reader;
	private String docName;
	private String encoding;	
	private GenericSkeleton skel;	
	private TextUnit tu;
	private Parameters params;
	private Stack<Integer> parentIds;
	private Queue<Event> queue;
	private String srcLang;
	private String trgLang;
	private int groupId;
	private int otherId; 
	private int tuId;
	int tuvType=0;	//--tracks tuv type: 1 for source, 2 for target, otherwise 0 language independent--

	public TmxFilter () {
		params = new Parameters();
	}
	
	public void cancel() {
		canceled = true;
	}

	public void close() {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
			hasNext = false;
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	public String getName() {
		return "okf_tmx";
	}
	
	public String getMimeType () {
		return "text/x-tmx"; //TODO: check
	}

	public IParameters getParameters () {
		return params;
	}
	
	public boolean hasNext() {
		return hasNext;		
	}
	
	public Event next () {
		try {		
			// Check for cancellation first
			if ( canceled ) {
				queue.clear();
				queue.add(new Event(EventType.CANCELED));
				hasNext = false;
			}
			
			// Parse next if nothing in the queue
			if ( queue.isEmpty() ) {
				if ( !read() ) {
					Ending ending = new Ending(String.valueOf(++otherId));
					ending.setSkeleton(skel);
					queue.add(new Event(EventType.END_DOCUMENT, ending));
				}
			}

			// Return the head of the queue
			if ( queue.peek().getEventType() == EventType.END_DOCUMENT ) {
				hasNext = false;
			}
			return queue.poll();		
		}
		catch ( XMLStreamException e ) {
			throw new OkapiIOException(e);
		}
	}

	private boolean read () throws XMLStreamException {
		skel = new GenericSkeleton();
		int eventType;
		while ( reader.hasNext() ) {
			eventType = reader.next();
			switch ( eventType ) {
			case XMLStreamConstants.START_ELEMENT:
				if( isStartGroupTag(reader.getLocalName())){
					return processStartGroup();
				} else if (isDocumentPartTag(reader.getLocalName())){
					boolean success = processDocumentPart();
					if(!success){
						throw new OkapiIOException("Invalid Xml.");
					}else{
						return true;
					}
				}else if (reader.getLocalName().equals("tu")){
					return processTranslationUnit();
				}else{
					storeStartElement();
					break;
				}
			
			case XMLStreamConstants.END_ELEMENT:
				storeEndElement();
				if( isStartGroupTag(reader.getLocalName())){
					return processEndGroup();
				}
				break;
			
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS: //TODO: Check if it's ok to not check for unsupported chars
				skel.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
				break;
				
			case XMLStreamConstants.COMMENT:
				skel.append("<!--"+ reader.getText() + "-->");
				break;

			case XMLStreamConstants.ENTITY_REFERENCE:
			case XMLStreamConstants.ENTITY_DECLARATION:
			case XMLStreamConstants.NAMESPACE:
			case XMLStreamConstants.NOTATION_DECLARATION:
			case XMLStreamConstants.ATTRIBUTE:
			case XMLStreamConstants.END_DOCUMENT:
			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
			case XMLStreamConstants.DTD:

				break;				
			}
		}
		return false;
	}
	
	public void open (InputResource input) {
		open(input, true);
	}
	
	public void open (InputResource input,
		boolean generateSkeleton)
	{
		setOptions(input.getSourceLanguage(), input.getTargetLanguage(),
			input.getEncoding(), generateSkeleton);
		if ( input.getInputCharSequence() != null ) {
			open(input.getInputCharSequence());
		}
		else if ( input.getInputURI() != null ) {
			open(input.getInputURI());
		}
		else if ( input.getInputStream() != null ) {
			open(input.getInputStream());
		}
		else {
			throw new BadFilterInputException("InputResource has no input defined.");
		}
	}
	
	private void open(URI inputURI) {
		try { 
			docName = inputURI.getPath();
			open(inputURI.toURL().openStream());
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	private void open(CharSequence inputText) {
		//TODO: Check for better solution, going from char to byte to read char is just not good
		open(new ByteArrayInputStream(inputText.toString().getBytes())); 		
	}
	
	private void open (InputStream input) {
		try {
			if ( srcLang == null ) throw new NullPointerException("Source language not set.");
			if ( trgLang == null ) throw new NullPointerException("Target language not set.");
			close();
			canceled = false;			
			
			XMLInputFactory fact = XMLInputFactory.newInstance();
			fact.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, true);
			fact.setProperty(XMLInputFactory2.IS_VALIDATING, false);
			reader = fact.createXMLStreamReader(input);

			//--initialize
			parentIds = new Stack<Integer>();
			parentIds.push(0);
			groupId = 0;		
			otherId = 0;
			tuId = 0;
			hasNext=true;

			//--attempt encoding detection--
			if(reader.getEncoding()!=null){
				encoding = reader.getEncoding();
			}
			
			// Set the start event
			queue = new LinkedList<Event>();
			
			StartDocument startDoc = new StartDocument(String.valueOf(++otherId));
			startDoc.setName(docName);
			startDoc.setEncoding(encoding, false); //TODO: UTF8 BOM detection
			startDoc.setLanguage(srcLang);
			startDoc.setFilterParameters(getParameters());
			startDoc.setType("text/x-tmx");
			startDoc.setMimeType("text/x-tmx");
			startDoc.setMultilingual(true);
			queue.add(new Event(EventType.START_DOCUMENT, startDoc));			
			
			// The XML declaration is not reported by the parser, so we need to
			// create it as a document part when starting			
			skel = new GenericSkeleton();
			startDoc.setProperty(new Property("encoding", encoding, false));
			skel.append("<?xml version=\"1.0\" encoding=\"");
			skel.addValuePlaceholder(startDoc, "encoding", "");
			skel.append("\"?>");
			startDoc.setSkeleton(skel);
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	public void setParameters(IParameters params) {
		this.params = (Parameters)params;
	}

	private void setOptions(String sourceLanguage,
		String targetLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		srcLang = sourceLanguage;
		trgLang = targetLanguage;
		encoding = defaultEncoding;
	}
	
	public ISkeletonWriter createSkeletonWriter() {
		return new GenericSkeletonWriter();
	}

	public IFilterWriter createFilterWriter () {
		return new GenericFilterWriter(createSkeletonWriter());
	}

	private boolean processStartGroup () {
		storeStartElement();
		
		StartGroup group = new StartGroup(parentIds.peek().toString(),String.valueOf(++groupId));
		group.setName(reader.getLocalName());
		group.setSkeleton(skel);
		parentIds.push(groupId);
		queue.add(new Event(EventType.START_GROUP, group));
		return true;
	}	
	
	private boolean processEndGroup () {
		// Pop and checks the value for this group
		int id = parentIds.pop();

		Ending ending = new Ending(String.valueOf(id));
		ending.setSkeleton(skel);
		queue.add(new Event(EventType.END_GROUP, ending));
		return true;
	}

	private void storeStartElement () {
		String prefix = reader.getPrefix();
		if (( prefix == null ) || ( prefix.length()==0 )) {
			skel.append("<"+reader.getLocalName());
		}
		else {
			skel.append("<"+prefix+":"+reader.getLocalName());
		}

		int count = reader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = reader.getNamespacePrefix(i);
			skel.append(String.format(" xmlns%s=\"%s\"",
				((prefix.length()>0) ? ":"+prefix : ""),
				reader.getNamespaceURI(i)));
		}
		count = reader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = reader.getAttributePrefix(i); 
			skel.append(String.format(" %s%s=\"%s\"",
				(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
				reader.getAttributeLocalName(i),
				reader.getAttributeValue(i)));
		}
		skel.append(">");
	}
	
	private void storeTuStartElement () {
		String prefix = reader.getPrefix();
		if (( prefix == null ) || ( prefix.length()==0 )) {
			skel.append("<"+reader.getLocalName());
		}
		else {
			skel.append("<"+prefix+":"+reader.getLocalName());
		}

		int count = reader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = reader.getNamespacePrefix(i);
			skel.append(String.format(" xmlns%s=\"%s\"",
				((prefix.length()>0) ? ":"+prefix : ""),
				reader.getNamespaceURI(i)));
		}
		count = reader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = reader.getAttributePrefix(i); 
			skel.append(String.format(" %s%s=\"%s\"",
				(((prefix==null)||(prefix.length()==0)) ? "" : prefix+":"),
				reader.getAttributeLocalName(i),
				reader.getAttributeValue(i)));
			
			//--set the properties depending on the tuvType--
			if(tuvType == 1){
				tu.setSourceProperty(new Property(reader.getAttributeLocalName(i), reader.getAttributeValue(i), true));
			}else if(tuvType == 2){
				tu.setTargetProperty(trgLang, new Property(reader.getAttributeLocalName(i),reader.getAttributeValue(i), true));
			}else if(tuvType == 0){
				tu.setProperty(new Property(reader.getAttributeLocalName(i),reader.getAttributeValue(i), true));
			}			
		}
		skel.append(">");
	}	
	
	private void storeEndElement () {
		String ns = reader.getPrefix();
		if (( ns == null ) || ( ns.length()==0 )) {
			skel.append("</"+reader.getLocalName()+">");
		}
		else {
			skel.append("</"+ns+":"+reader.getLocalName()+">");
		}
	}	
	
	private boolean processDocumentPart(){
		storeStartElement();
		DocumentPart dp = new DocumentPart(String.valueOf(++otherId), false);
		dp.setName(reader.getLocalName());
		String startElement = reader.getLocalName();
		try {
			while(reader.hasNext()){
				int eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS: //TODO: Check if it's ok to not check for unsupported chars
					skel.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));					
					break;
				case XMLStreamConstants.END_ELEMENT:
					if(reader.getLocalName().equals(startElement)){
						storeEndElement();
						dp.setSkeleton(skel);
						queue.add(new Event(EventType.DOCUMENT_PART, dp));
						return true;						
					}
					break;
				default: 
					//Todo--handle remaining cases
					break;
				}
			}
		} catch (XMLStreamException e) {
			throw new OkapiIOException(e);
		}
		return false;
	}
	
	private boolean processTuDocumentPart(){
		storeTuStartElement();		
		String startElement = reader.getLocalName();
		try {
			while(reader.hasNext()){
				int eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS:
					 //TODO: Check if it's ok to not check for unsupported chars
					skel.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
					//--set the properties depending on the tuvType--
					if(tuvType == 1){
						tu.setSourceProperty(new Property(startElement, reader.getText(), true));	
					}else if(tuvType == 2){
						tu.setTargetProperty(trgLang, new Property(startElement, reader.getText(), true));
					}else if(tuvType == 0){
						tu.setProperty(new Property(startElement, reader.getText(), true));
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					if(reader.getLocalName().equals(startElement)){
						storeEndElement();
						return true;
					}
					break;
				}
			}
			return false;
		} catch (XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}	
	
	/**
	 * Process a segment <seg>*</seg>, appending the skeleton to skel and adding the properties to nameable and reference to tu 
	 */			
	private boolean processSeg(TextContainer tc, TextUnit tu){
		storeTuStartElement();
		String startElement = reader.getLocalName();
		try {
			while(reader.hasNext()){
				int eventType = reader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS:
					if(tuvType == 1 || tuvType == 2){ //TODO: Check if it's ok to not check for unsupported chars
						tc.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
					}else if (tuvType == 0){ //TODO: Check if it's ok to not check for unsupported chars
						skel.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
					}
					break;
				case XMLStreamConstants.START_ELEMENT:		
					
					if(tuvType == 1 || tuvType == 2){
						String localName = reader.getLocalName().toLowerCase();
						
						if(localName.equals("ph") || localName.equals("it") || localName.equals("hi")){
							processInlineElement(tc, TagType.PLACEHOLDER);	
						}else if(localName.equals("bpt")){
							processInlineElement(tc, TagType.OPENING);	
						}else if(localName.equals("ept")){
							processInlineElement(tc, TagType.CLOSING);	
						}
						break;
					}else if (tuvType == 0){
						//TODO: handle remaining inline tags
					}
					
				case XMLStreamConstants.END_ELEMENT:
					if(reader.getLocalName().equals(startElement)){
						if(tuvType == 1){
							skel.addContentPlaceholder(tu, null);
						}else if(tuvType == 2){
							skel.addContentPlaceholder(tu, trgLang);
						}
						storeEndElement();
						return true;
					}
					break;
				}
			}
			return false;
		} catch (XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}
	
	/**
	 * Process an entire tu element
	 * @return FilterEvent
	 */		
	private boolean processTranslationUnit(){
		
		if ( !skel.isEmpty(true) ) {
			DocumentPart dp = new DocumentPart(String.valueOf(++otherId), false, skel);
			skel = new GenericSkeleton(); // And create a new skeleton for the next event
			queue.add(new Event(EventType.DOCUMENT_PART, dp));
		}
		
		tuvType=0;	//--1 for source, 2 for target, otherwise 0 language independent--

		tu = new TextUnit(String.valueOf(++tuId));
		TextContainer trgCont = tu.setTarget(trgLang, new TextContainer());
		
		storeTuStartElement();

		String localName;
		try {
			while(reader.hasNext()){
				
				int eventType = reader.next();
				switch ( eventType ) {

				case XMLStreamConstants.COMMENT:
					skel.append("<!--"+ reader.getText() + "-->");
					break;
					
				case XMLStreamConstants.CHARACTERS: //TODO: Check if it's ok to not check for unsupported chars
					skel.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
					break;
				case XMLStreamConstants.START_ELEMENT:

					localName = reader.getLocalName().toLowerCase(); 
					if(localName.equals("note") || localName.equals("prop")){
						//Todo: handle true/false
						processTuDocumentPart();
					}else if(reader.getLocalName().equals("tuv")){
						tuvType = tuvType();
						storeTuStartElement();
					}else if(reader.getLocalName().equals("seg")){
						if(tuvType==1){
							processSeg(tu.getSource(), tu);
						}if(tuvType==2){
							processSeg(trgCont, tu);
						}if(tuvType==0){
							processSeg(null, null);
						}
					}
					break;
					
				case XMLStreamConstants.END_ELEMENT:

					localName = reader.getLocalName().toLowerCase(); 

					if(localName.equals("tu")){
						storeEndElement();
						tu.setSkeleton(skel);
						tu.setMimeType("text/xml");
						queue.add(new Event(EventType.TEXT_UNIT, tu));
						return true;
					}else if(localName.equals("tuv")){
						storeEndElement();
						tuvType=0; 	//--reset tuvType to unspecified--
					}else{
						storeEndElement();
					} 	
					break;
				}
			}
		} catch (XMLStreamException e) {
			throw new OkapiIOException(e);
		}
		return false;		
	}
	
	/**
	 * Process an Inline Element, create a code and add to the passed TextContainer. 
	 * Include start tag, content, and end tag. 
	 */		
	private boolean processInlineElement(TextContainer tc, TagType tt){
		
		String startElement = reader.getLocalName();	//--store the start element--
		StringBuffer sb = new StringBuffer();			//--store the code--
		String type = startElement;						//--store the type--
		
		sb.append("<"+startElement);
		for(int i=0; i< reader.getAttributeCount(); i++){

			//--set the type if attribute type exists--
			if(reader.getAttributeLocalName(i).equals("type")){
				type=reader.getAttributeValue(i);
			}
			sb.append(" "+reader.getAttributeLocalName(i)+"=\""+reader.getAttributeValue(i)+"\"");
		}
		sb.append(">");
		try {
			while(reader.hasNext()){
				int eventType = reader.next();
				switch ( eventType ) {
				//--process the document note content--
				case XMLStreamConstants.CHARACTERS: //TODO: Check if it's ok to not check for unsupported chars
					sb.append(Util.escapeToXML(reader.getText(), 0, params.escapeGT, null));
					break;
				case XMLStreamConstants.END_ELEMENT:
					if(reader.getLocalName().equals(startElement)){
						sb.append("</"+reader.getLocalName()+">");
						tc.append(tt, type, sb.toString());
						return true;
					}
					break;
				}
			}
			return false;
		} catch (XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	private boolean isStartGroupTag(String elementName){
		return params.startGroupTags.contains(elementName.toLowerCase()) ? true: false;
	}
	
	private boolean isDocumentPartTag(String elementName){
		return params.documentPartTags.contains(elementName.toLowerCase()) ? true: false;
	}

	/**
	 * Checks the tuv lang attribute to see if it's a source tuv
	 * @return 	returns 0 for tuv that is neither source or target
	 * 			returns 1 for source tuv
	 * 			returns 2 for target tuv
	 */		
	private int tuvType(){
		//TODO: change this to check for the actual name of the attribute (not the index)
		if (reader.getAttributeValue(0).toLowerCase().equals(srcLang.toLowerCase())){
			return 1;
		}else if (reader.getAttributeValue(0).toLowerCase().equals(trgLang.toLowerCase())){
			return 2;
		}
		return 0;
	}
}
