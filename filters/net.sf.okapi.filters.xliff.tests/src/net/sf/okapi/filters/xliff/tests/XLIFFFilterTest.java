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

package net.sf.okapi.filters.xliff.tests;

import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.tests.FilterTestDriver;
import net.sf.okapi.filters.tests.InputDocument;
import net.sf.okapi.filters.tests.RoundTripComparison;
import net.sf.okapi.filters.xliff.XLIFFFilter;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class XLIFFFilterTest {

	private XLIFFFilter filter;
	private GenericContent fmt;

	@Before
	public void setUp() {
		filter = new XLIFFFilter();
		fmt = new GenericContent();
	}

	@Test
	public void testStartDocument () {
		StartDocument sd = FilterTestDriver.getStartDocument(createSimpleXLIFF());
		assertNotNull(sd);
		assertNotNull(sd.getEncoding());
		assertNotNull(sd.getType());
		assertNotNull(sd.getMimeType());
		assertNotNull(sd.getLanguage());
		assertEquals("\r", sd.getLineBreak());
	}
	
	@Test
	public void testSimpleTransUnit () {
		TextUnit tu = FilterTestDriver.getTextUnit(createSimpleXLIFF(), 1);
		assertNotNull(tu);
		assertEquals("Hello World!", tu.getSource().toString());
		assertEquals("13", tu.getName());
	}

	@Test
	public void testBilingualTransUnit () {
		TextUnit tu = FilterTestDriver.getTextUnit(createBilingualXLIFF(), 1);
		assertNotNull(tu);
		assertEquals("S1, S2", tu.getSource().toString());
		fmt.setContent(tu.getSourceContent());
		assertEquals("<1>S1</1>, <2>S2</2>", fmt.toString());
		assertTrue(tu.hasTarget("fr"));
		assertEquals("T2, T1", tu.getTarget("fr").toString());
		fmt.setContent(tu.getTargetContent("fr"));
		assertEquals("<2>T2</2>, <1>T1</1>", fmt.toString());
	}

	@Test
	public void testBPTTypeTransUnit () {
		TextUnit tu = FilterTestDriver.getTextUnit(createBPTTypeXLIFF(), 1);
		assertNotNull(tu);
		fmt.setContent(tu.getSourceContent());
		assertEquals("<1>S1</1>, <2>S2</2>", fmt.toString());
		assertTrue(tu.hasTarget("fr"));
		fmt.setContent(tu.getTargetContent("fr"));
		assertEquals("<2>T2</2>, <1>T1</1>", fmt.toString());
	}

	@Test
	public void testBPTAndSUBTypeTransUnit () {
		TextUnit tu = FilterTestDriver.getTextUnit(createBPTAndSUBTypeXLIFF(), 1);
		assertNotNull(tu);
		fmt.setContent(tu.getSourceContent());
		assertEquals("<1>S1</1>, <2>S2</2>", fmt.toString());
	}

	@Test
	public void testBPTWithSUB () {
		TextUnit tu = FilterTestDriver.getTextUnit(createBPTAndSUBTypeXLIFF(), 1);
		assertNotNull(tu);
		Code code = tu.getSource().getCode(0);
		assertEquals(code.getData(), "a<sub>text</sub>");
		assertEquals(code.getOuterData(), "<bpt id=\"1\">a<sub>text</sub></bpt>");
	}

	@Test
	public void testPreserveSpaces () {
		TextUnit tu = FilterTestDriver.getTextUnit(createTUWithSpaces(), 1);
		assertNotNull(tu);
		fmt.setContent(tu.getSourceContent());
		assertTrue(tu.preserveWhitespaces());
		assertEquals("t1  t2 t3\t\t<1/>  t4", fmt.toString());
	}

	@Test
	public void testWrapSpaces () {
		TextUnit tu = FilterTestDriver.getTextUnit(createTUWithSpaces(), 2);
		assertNotNull(tu);
		fmt.setContent(tu.getSourceContent());
		assertFalse(tu.preserveWhitespaces());
		assertEquals("t1 t2 t3 <1/> t4", fmt.toString());
	}

	@Test
	public void testComplexSUB () {
		TextUnit tu = FilterTestDriver.getTextUnit(createComplexSUBTypeXLIFF(), 1);
		assertNotNull(tu);
		Code code = tu.getSource().getCode(0);
		assertEquals(code.getData(), "startCode<sub>[nested<ph id=\"2\">ph-in-sub</ph>still in sub]</sub>endCode");
		assertEquals(code.getOuterData(), "<ph id=\"1\">startCode<sub>[nested<ph id=\"2\">ph-in-sub</ph>still in sub]</sub>endCode</ph>");
	}

	@Test
	public void testComplexSUBInTarget () {
		TextUnit tu = FilterTestDriver.getTextUnit(createComplexSUBTypeXLIFF(), 1);
		assertNotNull(tu);
		tu.createTarget("fr", true, IResource.COPY_ALL);
		Code code = tu.getTarget("fr").getCode(0);
		assertEquals(code.getData(), "startCode<sub>[nested<ph id=\"2\">ph-in-sub</ph>still in sub]</sub>endCode");
		assertEquals(code.getOuterData(), "<ph id=\"1\">startCode<sub>[nested<ph id=\"2\">ph-in-sub</ph>still in sub]</sub>endCode</ph>");
	}

	@Test
	public void testDoubleExtraction () {
		// Read all files in the data directory
		URL url = XLIFFFilterTest.class.getResource("/JMP-11-Test01.xlf");
		String root = Util.getDirectoryName(url.getPath());
		root = Util.getDirectoryName(root) + "/data/";
		
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(root+"JMP-11-Test01.xlf", null));
		//list.add(new InputDocument(root+"Manual-12-AltTrans.xlf", null));
		list.add(new InputDocument(root+"PAS-10-Test01.xlf", null));
		list.add(new InputDocument(root+"RB-11-Test01.xlf", null));
		list.add(new InputDocument(root+"RB-12-Test02.xlf", null));
		list.add(new InputDocument(root+"SF-12-Test03.xlf", null));
		list.add(new InputDocument(root+"NStest01.xlf", null));
		list.add(new InputDocument(root+"BinUnitTest01.xlf", null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", "en", "fr"));

		list.clear();
		list.add(new InputDocument(root+"SF-12-Test01.xlf", null));
		list.add(new InputDocument(root+"SF-12-Test02.xlf", null));
		rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", "en", "es"));
	}

	private ArrayList<Event> createSimpleXLIFF () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"plaintext\" original=\"file.ext\">"
			+ "<body><trans-unit id=\"13\"><source>Hello World!</source></trans-unit></body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> createBilingualXLIFF () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"plaintext\" original=\"file.ext\">"
			+ "<body><trans-unit id=\"13\"><source><g id='1'>S1</g>, <g id='2'>S2</g></source>"
			+ "<target><g id='2'>T2</g>, <g id='1'>T1</g></target></trans-unit></body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> createBPTTypeXLIFF () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"x-test\" original=\"file.ext\">"
			+ "<body><trans-unit id=\"13\"><source><bpt id='1'>a</bpt>S1<ept id='1'>/a</ept>, <bpt id='2'>b</bpt>S2<ept id='2'>/b</ept></source>"
			+ "<target><bpt id='2'>b</bpt>T2<ept id='2'>/b</ept>, <bpt id='1'>a</bpt>T1<ept id='1'>/a</ept></target></trans-unit></body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> createBPTAndSUBTypeXLIFF () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"x-test\" original=\"file.ext\">"
			+ "<body><trans-unit id=\"13\"><source><bpt id=\"1\">a<sub>text</sub></bpt>S1<ept id=\"1\">/a</ept>, <bpt id=\"2\">b</bpt>S2<ept id=\"2\">/b</ept></source>"
			+ "</trans-unit></body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> createComplexSUBTypeXLIFF () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"x-test\" original=\"file.ext\">"
			+ "<body><trans-unit id=\"13\"><source>t1 <ph id=\"1\">startCode<sub>[nested<ph id=\"2\">ph-in-sub</ph>still in sub]</sub>endCode</ph> t2</source>"
			+ "</trans-unit></body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> createTUWithSpaces () {
		String snippet = "<?xml version=\"1.0\"?>\r"
			+ "<xliff version=\"1.2\">\r"
			+ "<file source-language=\"en\" datatype=\"x-test\" original=\"file.ext\">"
			+ "<body>"
			+ "<trans-unit id=\"1\" xml:space=\"preserve\"><source>t1  t2 t3\t\t<ph id='1'>X</ph>  t4</source></trans-unit>"
			+ "<trans-unit id=\"2\"><source>t1  t2 t3\t\t<ph id='1'>X</ph>  t4</source></trans-unit>"
			+ "</body>"
			+ "</file></xliff>";
		return getEvents(snippet);
	}
	
	private ArrayList<Event> getEvents(String snippet) {
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, "en", "fr"));
		while ( filter.hasNext() ) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}
	
}
