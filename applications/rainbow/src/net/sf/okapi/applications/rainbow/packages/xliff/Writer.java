/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel (at ENLASO Corporation)                  */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.applications.rainbow.packages.xliff;

import java.io.File;

import net.sf.okapi.applications.rainbow.lib.ILog;
import net.sf.okapi.applications.rainbow.lib.XMLWriter;
import net.sf.okapi.applications.rainbow.packages.BaseWriter;
import net.sf.okapi.common.resource.IExtractionItem;
import net.sf.okapi.filters.xliff.XLIFFContent;

/**
 * Implements IWriter for generic XLIFF translation packages.
 */
public class Writer extends BaseWriter {
	
	private XMLWriter        writer = null;
	private String           relativePath;
	private int              docKey;
	private XLIFFContent     xliffCont;


	public Writer(ILog log) {
		super(log);
		xliffCont = new XLIFFContent();
	}
	
	public String getPackageType () {
		return "xliff";
	}

	public void createDocument (int p_nDKey,
		String p_sRelativePath)
	{
		try {
			if ( writer == null ) writer = new XMLWriter();
			else writer.close(); // Else: make sure the previous output is closed
		
			docKey = p_nDKey;
			relativePath = p_sRelativePath + ".xlf";

			writer.create(manifest.getRoot() + File.separator
				+ ((manifest.getSourceLocation().length() == 0 ) ? "" : (manifest.getSourceLocation() + File.separator)) 
				+ relativePath);
		}
		catch ( Exception E ) {
			log.error(E.getLocalizedMessage());
		}
	}

	public void writeEndDocument () {
		writer.writeEndElement(); // body
		writer.writeEndElement(); // file
		writer.writeEndElement(); // xliff
		writer.writeEndDocument();
		writer.close();
		manifest.addDocument(docKey, relativePath);
	}

	public void writeItem (IExtractionItem sourceItem,
		IExtractionItem targetItem,
		int p_nStatus)
	{
		writer.writeStartElement("trans-unit");
		writer.writeAttributeString("id", String.valueOf(sourceItem.getID()));
		if ( sourceItem.getName().length() != 0 )
			writer.writeAttributeString("resname", sourceItem.getName());
		if ( sourceItem.getType().length() != 0 )
			writer.writeAttributeString("restype", sourceItem.getType());
		if ( !sourceItem.isTranslatable() )
			writer.writeAttributeString("translate", "no");
		if ( sourceItem.preserveFormatting() )
			writer.writeAttributeString("xml:space", "preserve");
//TODO		if (( p_Target != null ) && ( p_nStatus == IExtractionItem.TSTATUS_OK ))
//			m_XW.writeAttributeString("approved", "yes");
//		if ( p_Source.hasCoord() )
//			m_XW.writeAttributeString("coord", p_Source.getCoord());
//		if ( p_Source.hasFont() )
//			m_XW.writeAttributeString("font", p_Source.getFont());

		writer.writeStartElement("source");
		writer.writeAttributeString("xml:lang", manifest.getSourceLanguage());
		writer.writeRawXML(xliffCont.setContent(sourceItem.getContent()).toString());
		writer.writeEndElement(); // source

		// Target (if needed)
		if ( targetItem != null ) {
			writer.writeStartElement("target");
			writer.writeAttributeString("xml:lang", manifest.getTargetLanguage());
			
/*			switch ( p_nStatus ) {
				case IExtractionItem.TSTATUS_OK:
					m_XW.writeAttributeString("state", "final");
					break;
				case IExtractionItem.TSTATUS_TOEDIT:
					m_XW.writeAttributeString("state", "translated");
					break;
				case IExtractionItem.TSTATUS_TOREVIEW:
					m_XW.writeAttributeString("state", "needs-review-translation");
					break;
				case IExtractionItem.TSTATUS_TOTRANS:
					m_XW.writeAttributeString("state", "needs-translation");
					break;
			}
*/
			if ( sourceItem.hasTarget() ) {
				writer.writeRawXML(xliffCont.setContent(targetItem.getContent()).toString());
			}
			else {
				writer.writeRawXML(xliffCont.setContent(sourceItem.getContent()).toString());
			}

			writer.writeEndElement(); // target
		}

		// Note
		if ( sourceItem.hasNote() ) {
			writer.writeStartElement("note");
			writer.writeString(sourceItem.getNote());
			writer.writeEndElementLineBreak(); // note
		}

		writer.writeEndElement(); // trans-unit
	}

	public void writeStartDocument (String p_sOriginal) {
		writer.writeStartDocument();

		writer.writeStartElement("xliff");
		writer.writeAttributeString("version", "1.2");
		writer.writeAttributeString("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
		
		writer.writeStartElement("file");
		writer.writeAttributeString("source-language", manifest.getSourceLanguage());
		writer.writeAttributeString("target-language", manifest.getTargetLanguage());
		writer.writeAttributeString("original", p_sOriginal);
		writer.writeAttributeString("datatype", "TODO");
		
		writer.writeStartElement("header");
		writer.writeEndElement(); // header
		
		writer.writeStartElement("body");
	}

}
