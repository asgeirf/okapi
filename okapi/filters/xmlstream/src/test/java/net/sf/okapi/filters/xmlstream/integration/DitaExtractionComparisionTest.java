package net.sf.okapi.filters.xmlstream.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.Util;
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.LocaleId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DitaExtractionComparisionTest {

	private XmlStreamFilter xmlStreamFilter;
	private String[] ditaFileList;
	private String ditaRoot;
	private LocaleId locEN = LocaleId.fromString("en");

	@Before
	public void setUp() throws Exception {
		xmlStreamFilter = new XmlStreamFilter();	
		xmlStreamFilter.setParametersFromURL(XmlStreamFilter.class.getResource("dita.yml"));
		URL ditaUrl = DitaExtractionComparisionTest.class.getResource("/bookmap-readme.dita");				
		ditaRoot = Util.getDirectoryName(ditaUrl.getPath()) + File.separator;
		ditaFileList = TestUtils.getTestFiles("/bookmap-readme.dita", ".dita");		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testStartDocument() {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(xmlStreamFilter,
				new InputDocument(ditaRoot + "bookmap-readme.dita", null),
				"UTF-8", locEN, locEN));
	}
	

	@Test
	public void testOpenTwice () throws URISyntaxException {
		File file = new File(ditaRoot + "bookmap-readme.dita");
		RawDocument rawDoc = new RawDocument(file.toURI(), "UTF-8", locEN);
		xmlStreamFilter.open(rawDoc);
		xmlStreamFilter.close();
		xmlStreamFilter.open(rawDoc);
		xmlStreamFilter.close();
	}
	
	@Test
	public void testDoubleExtractionSingle() throws URISyntaxException, MalformedURLException {
		RoundTripComparison rtc = new RoundTripComparison();
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(ditaRoot + "bookmap-readme.dita", null));
		assertTrue(rtc.executeCompare(xmlStreamFilter, list, "UTF-8", locEN, locEN));
	}
	
	@Test
	public void testDoubleExtraction() throws URISyntaxException {
		RoundTripComparison rtc = new RoundTripComparison();
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		for (String f : ditaFileList) {			
			list.add(new InputDocument(ditaRoot + f, null));			
		}
		assertTrue(rtc.executeCompare(xmlStreamFilter, list, "UTF-8", locEN, locEN));
	}
}
