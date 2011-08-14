/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.oasisopen.xliff.v2.ICode;
import org.oasisopen.xliff.v2.IWithCandidates;
import org.oasisopen.xliff.v2.IWithNotes;
import org.oasisopen.xliff.v2.InlineType;

import net.sf.okapi.lib.xliff.XLIFFEvent.XLIFFEventType;

public class XLIFFReader {
	
	private XMLStreamReader reader;
	private boolean hasNext;
	private LinkedList<XLIFFEvent> queue;
	private DocumentData docData;
	private SectionData sectionData;
	private Stack<GroupData> groups;
	private Unit unit;
	private Segment segment;
	private Part ignorable;

	public void open (URI inputURI) {
		try {
			BufferedInputStream bis = new BufferedInputStream(inputURI.toURL().openStream());
			open(bis);
		}
		catch ( MalformedURLException e ) {
			throw new XLIFFReaderException("Cannot open the XLIFF stream. "+e.getMessage(), e);
		}
		catch ( IOException e ) {
			throw new XLIFFReaderException("Cannot open the XLIFF stream. "+e.getMessage(), e);
		}
	}
	
	public void open (String input) {
		open(new ByteArrayInputStream(input.getBytes()));
	}
	
	public void open (InputStream inputStream) {
		try {
			XMLInputFactory fact = XMLInputFactory.newInstance();
			fact.setProperty(XMLInputFactory.IS_COALESCING, true);
			fact.setProperty(XMLInputFactory.SUPPORT_DTD, false);

			reader = fact.createXMLStreamReader(inputStream);
			queue = new LinkedList<XLIFFEvent>();
			groups = new Stack<GroupData>();
			hasNext = true;
		}
		catch ( XMLStreamException e ) {
			throw new XLIFFReaderException("Cannot open the XLIFF stream. "+e.getMessage(), e);
		}
	}
	
	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
			hasNext = false;
		}
		catch ( XMLStreamException e ) {
			throw new XLIFFReaderException("Closing error. "+e.getMessage(), e);
		}
	}

	public boolean hasNext () {
		return hasNext;
	}
	
	public XLIFFEvent next () {
		if ( queue.isEmpty() ) {
			readNext();
		}
		if ( queue.peek().getType() == XLIFFEventType.END_DOCUMENT ) {
			hasNext = false;
		}
		return queue.poll();
	}
	
	private void readNext () {
		try {
			String tmp;
			while ( reader.hasNext() ) {
				int type = reader.next();
				switch ( type ) {
				case XMLStreamReader.START_DOCUMENT:
					// Not used. The start-document occurs when opening the <xliff> element.
					break;
				case XMLStreamReader.END_DOCUMENT:
					queue.add(new XLIFFEvent(XLIFFEventType.END_DOCUMENT, docData));
					return;
				case XMLStreamReader.START_ELEMENT:
					tmp = reader.getLocalName();
					if ( tmp.equals(Util.ELEM_UNIT) ) {
						processUnit();
						return;
					}
					if ( tmp.equals(Util.ELEM_GROUP) ) {
						processStartGroup();
						return;
					}
					if ( tmp.equals(Util.ELEM_SECTION) ) {
						processStartSection();
						return;
					}
					if ( tmp.equals(Util.ELEM_DOC) ) {
						processXliff();
						return;
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					tmp = reader.getLocalName();
					if ( tmp.equals(Util.ELEM_GROUP) ) {
						queue.add(new XLIFFEvent(XLIFFEventType.END_GROUP, groups.pop()));
						return;
					}
					if ( tmp.equals(Util.ELEM_SECTION) ) {
						queue.add(new XLIFFEvent(XLIFFEventType.END_SECTION, sectionData));
						return;
					}
					break;
				}
			}
			hasNext = false;
		}
		catch ( XMLStreamException e ) {
			throw new XLIFFReaderException("Reading error. "+e.getMessage(), e);
		}
	}

	private void processXliff () {
		String tmp = reader.getAttributeValue(null, "version");
		cannotBeNullOrEmpty("version", tmp);
		if ( !tmp.equals("2.0") ) {
			throw new XLIFFReaderException("Not a 2.0 XLIFF document.");
		}
		docData = new DocumentData(tmp);
		docData.setExtendedAttributes(gatherExtendedAttributes());
		
		queue.add(new XLIFFEvent(XLIFFEventType.START_DOCUMENT, docData));
	}

	private void processStartSection () {
		sectionData = new SectionData("sectionId"); //TODO: section id

		// Get original
		String tmp = reader.getAttributeValue(null, "original");
		if ( cannotBeEmpty("original", tmp) ) {
			sectionData.setOriginal(tmp);
		}
		
		// Get the source language
		tmp = reader.getAttributeValue(null, Util.ATTR_SOURCELANG);
		cannotBeNullOrEmpty(Util.ATTR_SOURCELANG, tmp);
		//TODO: basic validation of the value
		sectionData.setSourceLanguage(tmp);

		// Get the target language
		tmp = reader.getAttributeValue(null, Util.ATTR_TARGETLANG);
		if ( cannotBeEmpty(Util.ATTR_TARGETLANG, tmp) ) {
			sectionData.setTargetLanguage(tmp);
		}
	
		sectionData.setExtendedAttributes(gatherExtendedAttributes());
		
		// We are done
		queue.add(new XLIFFEvent(XLIFFEventType.START_SECTION, sectionData));
	}
	
	private void processStartGroup () {
		// Get id
		String tmp = reader.getAttributeValue(null, Util.ATTR_ID);
		cannotBeEmpty(Util.ATTR_ID, tmp);
		GroupData gd = new GroupData(tmp);
		
		// Get type
		tmp = reader.getAttributeValue(null, "type");
		if ( cannotBeEmpty("type", tmp) ) {
			gd.setType(tmp);
		}
		gd.setExtendedAttributes(gatherExtendedAttributes());
		
		// We are done
		groups.push(gd);
		queue.add(new XLIFFEvent(XLIFFEventType.START_SECTION, gd));
	}
	
	/**
	 * Throws an exception if the value is null or empty.
	 * @param name name of the attribute.
	 * @param value value being checked.
	 */
	private void cannotBeNullOrEmpty (String name,
		String value)
	{
		if ( Util.isNullOrEmpty(value) ) {
			throw new XLIFFReaderException(String.format("Missing or empty attribute '%s'", name));
		}
	}
	
	/**
	 * Throws an exception if the value is empty.
	 * @param name name of the attribute.
	 * @param value value to check.
	 * @return true if the value is not null. False for null value.
	 */
	private boolean cannotBeEmpty (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() ) {
			throw new XLIFFReaderException(String.format("Empty attribute '%s'", name));
		}
		return true;
	}
	
	private boolean mustBeYesOrNo (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() || ( !value.equals("yes") && !value.equals("no") )) {
			throw new XLIFFReaderException(String.format("Invalid attribute value for '%s' (must be 'yes' or 'no')", name));
		}
		return true;
	}
	
	private void processUnit ()
		throws XMLStreamException
	{
		// New unit
		String tmp = reader.getAttributeValue(null, Util.ATTR_ID);
		cannotBeNullOrEmpty(Util.ATTR_ID, tmp);
		unit = new Unit(tmp);
		unit.setExtendedAttributes(gatherExtendedAttributes());
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_SEGMENT) ) {
					processPart(true);
				}
				else if ( tmp.equals(Util.ELEM_IGNORABLE) ) {
					processPart(false);
				}
				else if ( tmp.equals(Util.ELEM_ORIGINALDATA) ) {
					processOriginalData(unit.getDataStore()); // unit-level original data
				}
				else if ( tmp.equals(Util.ELEM_CANDIDATE) ) {
					processCandidate(unit); // unit-level match
				}
				else if ( tmp.equals(Util.ELEM_NOTE) ) {
					processNote(unit);
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_UNIT) ) { // End of this unit
					queue.add(new XLIFFEvent(XLIFFEventType.EXTRACTION_UNIT, unit));
					return;
				}
				break;
			}
		}
	}
	
	private void processCandidate (IWithCandidates parent)
		throws XMLStreamException
	{
		// New candidate
		Candidate alt = new Candidate();
		Part part = new Part(alt.getDataStore());

		String tmp;
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_SOURCE) ) {
					processContent(part);
					alt.setSource(part.getSource());
				}
				else if ( tmp.equals(Util.ELEM_TARGET) ) {
					processContent(part);
					alt.setTarget(part.getTarget(false));
				}
				else if ( tmp.equals(Util.ELEM_ORIGINALDATA) ) {
					processOriginalData(alt.getDataStore()); // match-level original data
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_CANDIDATE) ) { // End of this candidate
					parent.addCandidate(alt);
					return;
				}
				break;
			}
		}
	}
	
	private void processNote (IWithNotes parent)
		throws XMLStreamException
	{
		Note.AppliesTo at = Note.AppliesTo.DEFAULT;
		String tmp = reader.getAttributeValue(null, Util.ATTR_APPLIESTO);
		if ( cannotBeEmpty(Util.ATTR_APPLIESTO, tmp) ) {
			if ( tmp.equals("source") ) {
				at = Note.AppliesTo.SOURCE;
			}
			else if ( tmp.equals("target") ) {
				at = Note.AppliesTo.TARGET;
			}
			else {
				throw new XLIFFReaderException(String.format("Invalid appliesTo value ('%s').", tmp));
			}
		}

		StringBuilder sb = new StringBuilder();
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				sb.append(reader.getText());
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_NOTE) ) { // End of this note
					Note note = new Note(sb.toString(), at);
					parent.addNote(note);
					return;
				}
				break;
			}
		}
	}
	
	private void processOriginalData (DataStore store)
		throws XMLStreamException
	{
		String tmp;
		Map<String, String> map = new HashMap<String, String>();
		StringBuilder content = new StringBuilder();
		String id = null;

		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				if ( id != null ) {
					content.append(reader.getText());
				}
				break;
				
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_DATA) ) {
					id = reader.getAttributeValue(null, Util.ATTR_ID);
					cannotBeNullOrEmpty(Util.ATTR_ID, id);
					if ( map.containsKey(id) ) {
						throw new XLIFFReaderException(String.format("Duplicated id ('%s') in original data table.", id));
					}
					content.setLength(0);
				}
				else if ( tmp.equals(Util.ELEM_CP) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_HEX);
					cannotBeNullOrEmpty(Util.ATTR_HEX, tmp);
					try {
						int cp = Integer.valueOf(tmp, 16);
						if ( cp > 0xFFFF ) {
							content.append(Character.toChars(cp));
						}
						else {
							content.append((char)cp);
						}
					}
					catch ( NumberFormatException e ) {
						throw new XLIFFReaderException(String.format("Invalid code-point value in '%s': '%s'", Util.ATTR_HEX, tmp));
					}
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_DATA) ) {
					map.put(id, content.toString());
					id = null;
				}
				else if ( tmp.equals(Util.ELEM_ORIGINALDATA) ) {
					store.setOutsideRepresentationMap(map);
					return;
				}
				// Else: could be end of ELEM_CP: nothing to do
				break;
			}
		}
	}
	
	private void processPart (boolean isSegment)
		throws XMLStreamException
	{
		String tmp;
		if ( isSegment ) {
			segment = unit.appendNewSegment();
			tmp = reader.getAttributeValue(null, Util.ATTR_ID);
			if ( cannotBeEmpty(Util.ATTR_ID, tmp) ) {
				segment.setId(tmp);
			}
			tmp = reader.getAttributeValue(null, Util.ATTR_TRANSLATABLE);
			if ( mustBeYesOrNo(Util.ATTR_TRANSLATABLE, tmp) ) {
				segment.setTranslatable(tmp.equals("yes"));
			}
			segment.setExtendedAttributes(gatherExtendedAttributes());
		}
		else {
			ignorable = unit.appendNewIgnorable();
			ignorable.setExtendedAttributes(gatherExtendedAttributes());
		}

		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_SOURCE) ) {
					if ( isSegment ) processContent(segment);
					else processContent(ignorable);
				}
				else if ( tmp.equals(Util.ELEM_TARGET) ) {
					if ( isSegment ) processContent(segment);
					else processContent(ignorable);
					if ( sectionData.getTargetLanguage() == null ) {
						throw new XLIFFReaderException("No target language defined in a file with a target entry.");
					}
				}
				else if ( tmp.equals(Util.ELEM_CANDIDATE) ) {
					processCandidate(segment); // segment-level match
				}
				else if ( tmp.equals(Util.ELEM_NOTE) ) {
					processNote(segment);
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_SEGMENT) ) {
					return;
				}
				if ( tmp.equals(Util.ELEM_IGNORABLE) ) {
					return;
				}
				break;
			}
		}
	}
	
	private void processContent (Part partToFill)
		throws XMLStreamException
	{
		Fragment frag = new Fragment(partToFill.getDataStore());
		String tmp;
		ICode code = null;
		StringBuilder content = new StringBuilder();
		String nid = null;
		Stack<ICode> pairs = new Stack<ICode>();
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				if ( code == null ) {
					frag.append(reader.getText());
				}
				else {
					content.append(reader.getText());
				}
				break;
				
			case XMLStreamReader.COMMENT:
			case XMLStreamReader.PROCESSING_INSTRUCTION:
				// Ignored
				//TODO: generate some type of warning
				break;
				
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_OPENINGCODE) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_ID);
					cannotBeNullOrEmpty(Util.ATTR_ID, tmp);
					code = frag.append(InlineType.OPENING, tmp, null);
					tmp = reader.getAttributeValue(null, Util.ATTR_TYPE);
					code.setEquiv(reader.getAttributeValue(null, Util.ATTR_EQUIV));
					code.setDisp(reader.getAttributeValue(null, Util.ATTR_DISP));
					content.setLength(0);
				}
				else if ( tmp.equals(Util.ELEM_CLOSINGCODE) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_RID);
					cannotBeNullOrEmpty(Util.ATTR_RID, tmp);
					code = frag.append(InlineType.CLOSING, tmp, null);
					code.setEquiv(reader.getAttributeValue(null, Util.ATTR_EQUIV));
					code.setDisp(reader.getAttributeValue(null, Util.ATTR_DISP));
					content.setLength(0);
				}
				else if ( tmp.equals(Util.ELEM_PLACEHOLDER) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_ID);
					cannotBeNullOrEmpty(Util.ATTR_ID, tmp);
					code = frag.append(InlineType.PLACEHOLDER, tmp, null);
					code.setEquiv(reader.getAttributeValue(null, Util.ATTR_EQUIV));
					code.setDisp(reader.getAttributeValue(null, Util.ATTR_DISP));
					content.setLength(0);
				}
				else if ( tmp.equals(Util.ELEM_PAIREDCODES) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_ID);
					cannotBeNullOrEmpty(Util.ATTR_ID, tmp);
					code = frag.append(InlineType.OPENING, tmp, null);
					code.setEquiv(reader.getAttributeValue(null, Util.ATTR_EQUIV));
					code.setDisp(reader.getAttributeValue(null, Util.ATTR_DISP));
					// Closing code
					ICode closing = new Code(InlineType.CLOSING, tmp, null);
					closing.setEquiv(reader.getAttributeValue(null, Util.ATTR_EQUIVEND));
					closing.setDisp(reader.getAttributeValue(null, Util.ATTR_DISPEND));
					pairs.push(closing);
					code = null;
					continue;
				}
				else if ( tmp.equals(Util.ELEM_CP) ) {
					tmp = reader.getAttributeValue(null, Util.ATTR_HEX);
					cannotBeNullOrEmpty(Util.ATTR_HEX, tmp);
					try {
						int cp = Integer.valueOf(tmp, 16);
						if ( cp > 0xFFFF ) {
							char[] chars = Character.toChars(cp);
							if ( code == null ) {
								frag.append(chars[0]);
								frag.append(chars[1]);
							}
							else {
								content.append(chars);
							}
						}
						else {
							if ( code == null ) {
								frag.append((char)cp);
							}
							else {
								content.append((char)cp);
							}
						}
					}
					catch ( NumberFormatException e ) {
						throw new XLIFFReaderException(String.format("Invalid code-point value in '%s': '%s'", Util.ATTR_HEX, tmp));
					}
					continue;
				}
				// Common attributes
				if ( code != null ) {
					// Try to see if there are outside data defined
					tmp = reader.getAttributeValue(null, Util.ATTR_NID);
					if ( cannotBeEmpty(Util.ATTR_NID, tmp) ) {
						// Get the original data from the outside storage
						code.setOriginalData(partToFill.getDataStore().getOriginalDataForId(tmp));
						nid = tmp;
					}
				}
				break;

			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( tmp.equals(Util.ELEM_OPENINGCODE) 
					|| tmp.equals(Util.ELEM_CLOSINGCODE)
					|| tmp.equals(Util.ELEM_PLACEHOLDER) )
				{
					if ( checkInsideVersusOutside(nid, content, code) ) {
						code.setOriginalData(content.toString());
					}
					code = null;
					nid = null;
				}
				else if ( tmp.equals(Util.ELEM_PAIREDCODES) ) {
					frag.append(pairs.pop());
				}
				else if ( tmp.equals(Util.ELEM_SOURCE) ) {
					partToFill.setSource(frag);
					return;
				}
				else if ( tmp.equals(Util.ELEM_TARGET) ) {
					partToFill.setTarget(frag);
					return;
				}
				break;
			}
		}
	}

	private boolean checkInsideVersusOutside (String outsideId,
		StringBuilder insideContent,
		ICode code)
	{
		if (( insideContent.length() > 0 ) && ( outsideId != null )) {
			throw new XLIFFReaderException(String.format("Code id='%s' cannot have both content and outside reference ('%s') defined.",
				code.getId(), outsideId));
		}
		// Else: it's OK to use the inside content
		return (outsideId == null);
	}

	private ExtendedAttributes gatherExtendedAttributes () {
		ExtendedAttributes attrs = null;
		// Get the namespaces
		for ( int i=0; i<reader.getNamespaceCount(); i++ ) {
			String namespaceURI = reader.getNamespaceURI(i);
			if ( !namespaceURI.equals(Util.NS_XLIFF20) ) {
				if ( attrs == null ) {
					attrs = new ExtendedAttributes();
				}
				attrs.setNamespace(reader.getNamespacePrefix(i), namespaceURI);
			}
		}
		// Get the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			QName qname = reader.getAttributeName(i);
			// Store only the attributes that are not XLIFF
			if (( qname.getNamespaceURI() != null ) && !qname.getNamespaceURI().isEmpty() ) {
				if ( !qname.getNamespaceURI().equals(Util.NS_XLIFF20) ) {
					if ( attrs == null ) {
						attrs = new ExtendedAttributes();
					}
					attrs.setAttribute(new ExtendedAttribute(qname, reader.getAttributeValue(i)));
				}
			}
		}
		if (( attrs == null ) || ( attrs.size() == 0 )) return null;
		return attrs;
	}
}
