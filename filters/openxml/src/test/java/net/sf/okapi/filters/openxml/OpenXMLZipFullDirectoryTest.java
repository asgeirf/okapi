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
//import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level; // DWH 4-22-09
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.RawDocument;
//import net.sf.okapi.filters.markupfilter.Parameters;
//import net.sf.okapi.filters.openxml.OpenXMLContentFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;
import net.sf.okapi.filters.openxml.OpenXMLZipFilterWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a test that filters all files in the data directory.
 */

public class OpenXMLZipFullDirectoryTest {
	private static Logger LOGGER;
	private OpenXMLFilter openXMLFilter;
	private String[] testFileList;
	private static final String deary="/data/";

	@Before
	public void setUp() throws Exception {
		LOGGER = Logger.getLogger(OpenXMLSnippetsTest.class.getName());
		openXMLFilter = new OpenXMLFilter(new TagPeekTranslator(),"en-US");	
		openXMLFilter.setLogger(LOGGER);
		LOGGER.setLevel(Level.FINE);
		if (LOGGER.getHandlers().length<1)
			LOGGER.addHandler(new LogHandlerSystemOut());		
		openXMLFilter.setOptions("en", "UTF-8", true);

	    JFileChooser chooser = new JFileChooser();
	    // Note: source for ExampleFileFilter can be found in FileChooserDemo,
	    // under the demo/jfc directory in the JDK.
	    ExampleFileFilter filenamefilter = new ExampleFileFilter();
	    filenamefilter.addExtension("docx");
	    filenamefilter.addExtension("pptx");
	    filenamefilter.addExtension("xlsx");
	    filenamefilter.setDescription("Office 2007 Files");
	    chooser.setFileFilter(filenamefilter);
	    chooser.setMultiSelectionEnabled(true);
	    chooser.setDialogTitle("Select files or click Cancel to skip this test");
	    int returnVal = chooser.showOpenDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    	File philly[] = chooser.getSelectedFiles();
	    	int plen = philly.length;
	    	testFileList = new String[plen];
	    	for(int i=0;i<plen;i++)
	    		testFileList[i] = philly[i].getAbsolutePath(); // .getName();	    	
	    }
	    else {
	    	testFileList = new String[]{}; // Empty to skip the manual tests
	    }
	}

	@After
	public void tearDown() {
		openXMLFilter.close();
	}

	@Test
	public void testAll() throws URISyntaxException {
		Event event;
		String sOutputPath;
		OpenXMLZipFilterWriter writer;
		int flen;
		//String base=System.getProperty("user.dir").replace('\\','/').toLowerCase();

		for (String f : testFileList) {
			String ff = f;
			String ff20 = ff.replace(" ","%20").toLowerCase();
			String ff20s = ff20.replace('\\','/');
			String fff = "file:/" + ff20s;// DWH 6-11-09 added file: and lowercase
			flen = f.length()-5;
			sOutputPath = f.substring(0,flen) + ".out" + f.substring(flen);
			writer = new OpenXMLZipFilterWriter(); // DWH 4-8-09 was just ZipFilterWriter
			writer.setOptions("en-US", "UTF-8");
			writer.setOutput(sOutputPath);
			try {
				URI uriFf = new URI(fff);
				openXMLFilter.open(new RawDocument(uriFf,"UTF-8","en-US"),true,true,Level.FINEST); // DWH 4-22-09
				while (openXMLFilter.hasNext()) {
					event = openXMLFilter.next();
					if (event!=null)
						writer.handleEvent(event);
					else
						event = null; // just for debugging
				}
				writer.close();
			} catch (Exception e) {
				//System.err.println("Error for file: " + f + ": " + e.toString());
				throw new RuntimeException("Error for file: " + f + ": " + e.toString());
			}
		}
	}
}
