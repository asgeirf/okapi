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

import java.io.File;
import java.io.FilenameFilter;
//import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level; // DWH 4-22-09
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.RawDocument;
//import net.sf.okapi.filters.markupfilter.Parameters;
//import net.sf.okapi.filters.openxml.OpenXMLContentFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is a test that filters all files in the data directory.
 */

public class OpenXMLZipFullFileTest {
	private static Logger LOGGER;
	private OpenXMLFilter openXMLFilter;
	private String[] testFileList;

	@Before
	public void setUp() throws Exception {
		LOGGER = Logger.getLogger(OpenXMLZipFullFileTest.class.getName());
		openXMLFilter = new OpenXMLFilter();	
		openXMLFilter.setLogger(LOGGER);
		LOGGER.setLevel(Level.FINE);
		if (LOGGER.getHandlers().length<1)
			LOGGER.addHandler(new LogHandlerSystemOut());		
		openXMLFilter.setOptions("en", "UTF-8", true);

		// read all files in the test html directory
		URL url = OpenXMLZipFullFileTest.class.getResource("/BoldWorld.docx");
		File dir = new File(Util.getDirectoryName(url.getPath()));

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return ((name.endsWith(".docx") || name.endsWith(".pptx") || name.endsWith(".xlsx")) && !name.startsWith("Output"));
			}
		};
		testFileList = dir.list(filter);
	}

	@After
	public void tearDown() {
		openXMLFilter.close();
	}

	@Test
	public void testAll() throws URISyntaxException {
		for (String f : testFileList) {
			try {
				URL url = OpenXMLZipFullFileTest.class.getResource("/"+f);
				//URI uriFf = new URI(fff);
				openXMLFilter.open(new RawDocument(url.toURI(), "UTF-8", "en-US"),true,true,Level.FINEST); // DWH 4-22-09
				while (openXMLFilter.hasNext()) {
					openXMLFilter.next();
				}
			}
			catch (Exception e) {
				//System.err.println("Error for file: " + f + ": " + e.toString());
				throw new RuntimeException("Error for file: " + f + ": " + e.toString());
			}
		}
	}

	@Test
	public void testNonwellformed() {
		String filename = "/nonwellformed.specialtest";
		try
		{
			URI uriFf = new URI(filename);
			openXMLFilter.open(uriFf,true,Level.FINEST); // DWH 4-22-09
			while (openXMLFilter.hasNext()) {
				Event event = openXMLFilter.next();
				assertNotNull(event);
			}
			throw new RuntimeException("Should have recognized" + filename + " is not an MSOffice 2007 file");
		}
		catch(Exception e)
		{
			//System.err.println("Error for file: " + f + ": " + e.toString());
			filename = "All is swell";
		}
	}
}
