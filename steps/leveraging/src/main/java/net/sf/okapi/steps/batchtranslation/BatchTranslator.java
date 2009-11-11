/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.batchtranslation;

import java.io.File;
import java.io.IOException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.XMLWriter;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.lib.translation.QueryUtil;

public class BatchTranslator {

	private IFilterConfigurationMapper fcMapper;
	private IFilter filter;
	private RawDocument rawDoc;
	private QueryUtil qutil;
	private File htmlSourceFile;
	private File htmlTargetFile;
	private Parameters params;
	
	public BatchTranslator (IFilterConfigurationMapper fcMapper,
		Parameters params)
	{
		this.fcMapper = fcMapper;
		this.params = params;
		
		if ( this.params == null ) {
			this.params = new Parameters();
		}
		qutil = new QueryUtil();
	}
	
	public void processDocument (RawDocument rd,
		String filterConfigId)
	{
		rawDoc = rd;
		filter = fcMapper.createFilter(filterConfigId, filter);
		if ( filter == null ) {
			throw new RuntimeException("Unsupported filter type.");
		}
		createBatchInput();
		runBatchTranslation();
		retrieveTranslation();
	}

	private void createBatchInput () {
		XMLWriter htmlWriter = null;
		try {
			// Open the document
			filter.open(rawDoc);

			htmlSourceFile = File.createTempFile("hft_", ".html");
			htmlWriter = new XMLWriter(htmlSourceFile.getPath());
			
			htmlWriter.writeStartElement("html");
			htmlWriter.writeStartElement("meta");
			htmlWriter.writeAttributeString("http-equiv", "Content-Type");
			htmlWriter.writeAttributeString("content", "text/html; charset=UTF-8");
			htmlWriter.writeEndElement();

			// Process
			Event event;
			int subDocId = 0;
			
			while ( filter.hasNext() ) {
				event = filter.next();
				switch ( event.getEventType() ) {
				case START_SUBDOCUMENT:
					subDocId++;
					htmlWriter.writeStartElement("div");
					break;
					
				case END_SUBDOCUMENT:
					htmlWriter.writeEndElement(); // div
					break;
					
				case TEXT_UNIT:
					TextUnit tu = (TextUnit)event.getResource();
					if ( !tu.isTranslatable() ) continue;
					TextContainer tc = tu.getSource();
					if ( tc.isSegmented() ) {
//TODO: segmented entries						
					}
					else {
						htmlWriter.writeStartElement("p");
						htmlWriter.writeAttributeString("id", String.format("%d:%s", subDocId, tu.getId()));
						htmlWriter.writeRawXML(qutil.toCodedHTML(tc.getContent()));
						htmlWriter.writeEndElement(); // p
					}
				}
			}
			
			if ( htmlWriter != null ) {
				htmlWriter.writeEndElement(); // html
				htmlWriter.writeEndDocument();
				htmlWriter.close();
			}
			if ( htmlSourceFile != null ) {
				htmlSourceFile.deleteOnExit();
			}
		}
		catch ( IOException e ) {
e.printStackTrace();
		}
		finally {
			if ( htmlWriter != null ) htmlWriter.close();
			if ( filter != null ) filter.close();
		}
	}

	private void runBatchTranslation () {
		try {
			String cmd = params.getCommand();
			cmd = cmd.replace("${input}", htmlSourceFile.getPath());
			cmd = cmd.replace("${output}", htmlTargetFile.getPath());
			//TODO: ${srcloc}, etc.
			Process p = Runtime.getRuntime().exec(cmd);
		}
		catch ( IOException e ) {
e.printStackTrace();
		}
	}
	
	private void retrieveTranslation () {
		
	}

}
