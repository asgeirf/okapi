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

package net.sf.okapi.filters.openxml.tests;

import java.io.File;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
//import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;

import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.openxml.OpenXMLFilter;
import net.sf.okapi.filters.openxml.OpenXMLZipFilterWriter; // DWH 4-8-09
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;

import org.junit.Assert;
import org.junit.Test;

public class OpenXMLRoundTripTest {
	private ZipCompare zc=null;

	private static Logger LOGGER = Logger.getLogger(OpenXMLRoundTripTest.class.getName());

	@Test
	public void runTest () {
		LOGGER.setLevel(Level.FINE);
//		LOGGER.setLevel(Level.FINER);
//		LOGGER.setLevel(Level.FINEST);
		LOGGER.addHandler(new LogHandlerSystemOut());
		ArrayList<String> themfiles = new ArrayList<String>();
		zc = new ZipCompare();
		themfiles.add("BoldWorld.docx");
		themfiles.add("sample.docx");
		themfiles.add("sample.pptx");
		themfiles.add("sample.xlsx");
		themfiles.add("TranslationServicesOff.docx");
		themfiles.add("gtsftopic.docx");
		themfiles.add("OpenXML_text_reference_document.docx");
		themfiles.add("OpenXML_text_reference_v1_1.docx");
		themfiles.add("OpenXML_text_reference_v1_2.docx");
		themfiles.add("Mauris.docx");

		for(String s : themfiles)
		{
			runOneTest(s,false,false); // English
			runOneTest(s,true,false);  // PigLatin
			runOneTest(s,false,true);  // Codes
		}
	}

	public void runOneTest (String filename, boolean bTranslating, boolean bPeeking) {
		String sInputPath=null,sOutputPath=null,sGoldPath=null;
		Event event;
		URI uri;
		String sUserDir;
		OpenXMLFilter filter = null;
		boolean rtrued2;
		try {
			if (bPeeking)
				filter = new OpenXMLFilter(new CodePeekTranslator(),"en-US");
			else if (bTranslating)
				filter = new OpenXMLFilter(new PigLatinTranslator(),"pl");
			else
				filter = new OpenXMLFilter();
			filter.setOptions("en-US", "UTF-8", true);
//			filter.setLogLevel(Level.FINEST);
//			filter.setLogLevel(Level.FINE);
			sUserDir = System.getProperty("user.dir").replace('\\','/').toLowerCase();
			sInputPath = sUserDir + "/data/";
			sOutputPath = sUserDir + "/output/";
			sGoldPath = sUserDir + "/gold/";
			uri = new URI(sInputPath+filename);
			try
			{
				filter.open(new RawDocument(uri,"UTF-8","en-US"),true,false,Level.FINEST); // DWH 3-27-09
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);				
			}
			filter.setLogger(LOGGER);
			
			OpenXMLZipFilterWriter writer = new OpenXMLZipFilterWriter(); // DWH 4-8-09 was just ZipFilterWriter

			if (bPeeking)
				writer.setOptions("en-US", "UTF-8");
			else if (bTranslating)
				writer.setOptions("pl", "UTF-8");
			else
				writer.setOptions("en-US", "UTF-8");

			writer.setOutput(sOutputPath+ (bPeeking ? "Peek" : (bTranslating ? "Tran" : "Out"))+filename);
			
			while ( filter.hasNext() ) {
				event = filter.next();
				if (event!=null)
				{
					if (event.getEventType()==EventType.START_SUBDOCUMENT) // DWH 4-16-09 was START_DOCUMENT
						writer.setParameters(filter.getParameters());
					writer.handleEvent(event);
				}
				else
					event = null; // just for debugging
			}
			writer.close();
			rtrued2 = zc.zipsExactlyTheSame(sOutputPath+(bPeeking ? "Peek" : (bTranslating ? "Tran" : "Out"))+filename,
					   sGoldPath+(bPeeking ? "Peek" : (bTranslating ? "Tran" : "Out"))+filename);
			LOGGER.log(Level.INFO,(bPeeking ? "Peek" : (bTranslating ? "Tran" : "Out"))+filename+" SUCCEEDED");
			assert(rtrued2);
		}
		catch ( Throwable e ) {
			LOGGER.log(Level.SEVERE,e.getMessage());
			assert(0==1);
		}
		finally {
			if ( filter != null ) filter.close();
		}
	}
}
