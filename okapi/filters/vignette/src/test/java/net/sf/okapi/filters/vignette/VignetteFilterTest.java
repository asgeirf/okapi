/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.vignette;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class VignetteFilterTest {
	
	private VignetteFilter filter;
	private String root;
	private LocaleId locENUS = LocaleId.fromString("en-us");
	private LocaleId locESES = LocaleId.fromString("es-es");

	@Before
	public void setUp() {
		filter = new VignetteFilter();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		filter.setFilterConfigurationMapper(fcMapper);
		root = TestUtil.getParentDir(this.getClass(), "/Test01.xml");
	}

	@Test
	public void testDefaultInfo () {
		assertNotNull(filter.getParameters());
		assertNotNull(filter.getName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}
	
	@Test
	public void testStartDocument () {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			new InputDocument(root+"Test01.xml", null),
			"UTF-8", locENUS, locESES));
	}
	
	@Test
	public void testSimpleEntry () {
		String snippet = createSimpleDoc();
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 1);
		assertNotNull(tu);
		assertEquals("ENtext", tu.getSource().toString());
	}

	@Test
	public void testSimpleEntryOutput () {
		String snippet = createSimpleDoc();
		String expected = "<importProject>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB><![CDATA[<p>ENtext</p>]]></valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<stuff/>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ENtext&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
		String result = generateOutput(getEvents(snippet, locENUS, locESES));
		assertEquals(expected, result);		
	}

	@Test
	public void testComplexEntry () {
		String snippet = createComplexDoc();
		// Order is driven by the targets
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 1);
		assertNotNull(tu);
		assertEquals("EN-id1", tu.getSource().toString());
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 2);
		assertNotNull(tu);
		assertEquals("EN-id2", tu.getSource().toString());
	}

	@Test
	public void testComplexEntryOutput () {
		String snippet = createComplexDoc();
		String expected = "<importProject>"
			// ES id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB><![CDATA[EN-id1]]></valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>"
			// ES id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB><![CDATA[EN-id2]]></valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
		String result = generateOutput(getEvents(snippet, locENUS, locESES));
		assertEquals(expected, result);		
	}

	@Test
	public void testDoubleExtraction () throws URISyntaxException {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(root+"Test01.xml", null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locENUS, locESES, ""));
	}
	
	private String createSimpleDoc () {
		return "<importProject>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ES&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<stuff/>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ENtext&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
	}

	private String createComplexDoc () {
		return "<importProject>"
			// ES id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>ES-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>"
			// ES id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>ES-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
	}

	private String generateOutput (List<Event> list) {
		IFilterWriter writer = filter.createFilterWriter();
		writer.setOptions(locESES, "UTF-8");
		ByteArrayOutputStream writerBuffer = new ByteArrayOutputStream();
		writer.setOutput(writerBuffer);
		for (Event event : list) {
			writer.handleEvent(event);
		}
		writer.close();
		return writerBuffer.toString();
	}
	
	private ArrayList<Event> getEvents (String snippet,
		LocaleId srcLang,
		LocaleId trgLang)
	{
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, srcLang, trgLang));
		while ( filter.hasNext() ) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

}
