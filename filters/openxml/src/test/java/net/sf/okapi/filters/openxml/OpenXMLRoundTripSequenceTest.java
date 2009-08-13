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

package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.BaseContext;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.openxml.ConditionalParameters;
import net.sf.okapi.filters.openxml.OpenXMLFilter;
import net.sf.okapi.filters.openxml.OpenXMLZipFilterWriter;
// Comment the following to remove the parameter UI $$$
//import net.sf.okapi.ui.filters.openxml.Editor;

import org.junit.Test;

/**
 * This tests OpenXMLFilter (including OpenXMLContentFilter) and
 * OpenXMLZipFilterWriter (including OpenXMLContentSkeleton writer)
 * by filtering, automatically translating, and then writing the
 * zip file corresponding to a Word, Excel or Powerpoint 2009 file, 
 * then comparing it to a gold file to make sure nothing has changed.
 * It does this with a specific list of files.
 * 
 * <p>This is done with no translator first, to make sure the same
 * file is created that was filtered in the first place.  Then it
 * is translated into Pig Latin by PigLatinTranslator, translated so
 * codes are expanded by CodePeekTranslator, and then translated to
 * see a view like the translator will see by TagPeekTranslator.
 */

public class OpenXMLRoundTripSequenceTest {
	private ZipCompare zc=null;

	private static Logger LOGGER = Logger.getLogger(OpenXMLRoundTripTest.class.getName());
	private boolean allGood=true;
	private ConditionalParameters cparams; // DWH 6-18-09
	private boolean bSquishy=true; // DWH 7-16-09
	private OpenXMLFilter filter=null;

	@Test
	public void runTest () {
		LOGGER.setLevel(Level.FINE);
//		LOGGER.setLevel(Level.FINER);
//		LOGGER.setLevel(Level.FINEST);
		LOGGER.addHandler(new LogHandlerSystemOut());
		cparams = getParametersFromUserInterface();

		ArrayList<String> themfiles = new ArrayList<String>();
		zc = new ZipCompare();
		themfiles.add("BoldWorld.docx");
		themfiles.add("sample.docx");
		
		filter = new OpenXMLFilter(new PigLatinTranslator(),"pl");
		for(String s : themfiles)
		{
			runOneTest(s,filter); // English
		}
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	public void runOneTest (String filename, OpenXMLFilter filter) {
		String sInputPath=null,sOutputPath=null,sGoldPath=null;
		Event event;
		File filly;
		URI uri;
		String sUserDir;
		String sUserDirURI;  // DWH 6-9-09 URI
		BufferedInputStream bis;
		boolean rtrued2;
		final BaseContext context; // DWH 6-18-09
		try {	
			filter.setParameters(cparams);

			filter.setOptions("en-US", "UTF-8", true);
//			filter.setLogLevel(Level.FINEST);
//			filter.setLogLevel(Level.FINE);
			sUserDir = OpenXMLRoundTripTest.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();;
			sUserDirURI = sUserDir.substring(0,sUserDir.length()-5); // DWH 6-9-09
			sUserDir = sUserDir.substring(6,sUserDir.length()-5);
//			sUserDir = System.getProperty("user.dir").replace('\\','/').toLowerCase();
			sInputPath = sUserDirURI + "/data/"; // DWH 6-9-09
//			sInputPath = sUserDir + "/data/"; // DWH 6-9-09
			sOutputPath = sUserDir + "/ootput/";
			sGoldPath = sUserDir + "/gold/";
			uri = new URI(sInputPath+filename);
			try
			{
//				filly = new File(sInputPath+filename);
//				bis = new BufferedInputStream(new FileInputStream(filly));
//				filter.open(new RawDocument(bis,"UTF-8","en-US"),true,false,Level.FINEST); // DWH 6-09-09			

				filter.open(new RawDocument(uri,"UTF-8","en-US"),true,bSquishy,Level.FINEST); // DWH 7-16-09 squishiness
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);				
			}
			filter.setLogger(LOGGER);
			
			OpenXMLZipFilterWriter writer = new OpenXMLZipFilterWriter(); // DWH 4-8-09 was just ZipFilterWriter

			writer.setOptions("pl", "UTF-8");

			writer.setOutput(sOutputPath+"Tran"+filename);
			
			while ( filter.hasNext() ) {
				event = filter.next();
				if (event!=null)
				{
//					if (event.getEventType()==EventType.START_SUBDOCUMENT) // DWH 4-16-09 was START_DOCUMENT
// 6-27-09				writer.setParameters(filter.getParameters());
					writer.handleEvent(event);
				}
				else
					event = null; // just for debugging
			}
			writer.close();
			rtrued2 = zc.zipsExactlyTheSame(sOutputPath+"Tran"+filename,sGoldPath+"Tran"+filename);
			LOGGER.log(Level.INFO,"Tran"+filename+(rtrued2 ? " SUCCEEDED" : " FAILED"));
			if (!rtrued2)
				allGood = false;
		}
		catch ( Throwable e ) {
			LOGGER.log(Level.WARNING,e.getMessage());
			fail("An unexpected exception was thrown " + e);
		}
		finally {
			if ( filter != null ) filter.close();
		}
	}
	private ConditionalParameters getParametersFromUserInterface()
	{
		ConditionalParameters parms;
//    Choose the first to get the UI $$$
//		parms = (new Editor()).getParametersFromUI(new ConditionalParameters());
		parms = new ConditionalParameters();
		return parms;
	}
}
