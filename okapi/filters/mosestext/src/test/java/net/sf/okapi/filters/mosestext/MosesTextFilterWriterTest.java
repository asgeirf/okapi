/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mosestext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.LocaleId;

import org.junit.Test;
import static org.junit.Assert.*;

public class MosesTextFilterWriterTest {
	
	private String root;
	private MosesTextFilter filter;
	private LocaleId locEN = LocaleId.ENGLISH;
	private LocaleId locFR = LocaleId.FRENCH;

	public MosesTextFilterWriterTest () {
		filter = new MosesTextFilter();
		URL url = MosesTextFilterWriterTest.class.getResource("/Test01.txt");
		root = Util.getDirectoryName(url.getPath()) + File.separator;
	}

	@Test
	public void testSimpleOutputFromMosesText () {
		String snippet = "Line 1\rLine 2\r";
		String res = generateOutput(getEvents(snippet));
		assertEquals(snippet, res);
	}
	
	@Test
	public void testOutputFromXLIFF01 () {
		// Read from XLIFF and generate the Moses file (in a string)
		IFilter xlfFilter = new net.sf.okapi.filters.xliff.XLIFFFilter();
		String res = generateOutput(getEventsFromFile(xlfFilter, root+"Test-XLIFF01.xlf"));
		
		// Read the Moses string and compare with the expected result
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(res), 3);
		assertNotNull(tu);
		assertEquals("3", tu.getId());
		assertEquals("Help Authoring Guidelines", tu.getSource().toString());
	}

	@Test
	public void testFileOutputFromXLIFF01 () {
		File outFile = new File(root+"Test-XLIFF01.out.txt");
		outFile.delete();
		
		IFilter xlfFilter = new net.sf.okapi.filters.xliff.XLIFFFilter();
		generateFileOutput(
			getEventsFromFile(xlfFilter, root+"Test-XLIFF01.xlf"),
			outFile);
		
		// Read the Moses file and compare with the expected result
		String res = generateOutput(getEventsFromFile(filter, root+"Test-XLIFF01.out.txt"));
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(res), 3);
		assertNotNull(tu);
		assertEquals("3", tu.getId());
		assertEquals("Help Authoring Guidelines", tu.getSource().toString());
		
	}
	
	@Test
	public void testOutputFromXLIFF02 () {
		// Read from XLIFF and generate the Moses file (in a string)
		IFilter xlfFilter = new net.sf.okapi.filters.xliff.XLIFFFilter();
		String res = generateOutput(getEventsFromFile(xlfFilter, root+"Test-XLIFF02.xlf"));
		
		// Check the Moses output
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(res), 4);
		assertNotNull(tu);
		// Here two TUs are 5 entries because of the line breaks
		assertEquals("4", tu.getId());
		assertEquals("and line 3.", tu.getSource().toString());
	}

	private String generateOutput (List<Event> list) {
		try {
			IFilterWriter writer = new MosesTextFilterWriter();
			writer.setOptions(locEN, "UTF-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writer.setOutput(baos);
			for ( Event event : list ) {
				writer.handleEvent(event);
			}
			writer.close();
			return baos.toString("UTF-8");
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException("Error when creating the output");
		}
	}
	
	private void generateFileOutput (List<Event> list,
		File outFile)
	{
		IFilterWriter writer = new MosesTextFilterWriter();
		writer.setOptions(locEN, "UTF-8");
		writer.setOutput(outFile.getAbsolutePath());
		for ( Event event : list ) {
			writer.handleEvent(event);
		}
		writer.close();
	}
	
	private ArrayList<Event> getEventsFromFile (IFilter filter,
		String path)
	{
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(new File(path).toURI(), "UTF-8", locEN, locFR));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

	private ArrayList<Event> getEvents (String snippet) {
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, locEN));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

}
