/*===========================================================================*/
/* Copyright (C) 2008 Jim Hargrave                                           */
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
/* Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA              */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.pipeline.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.UnsupportedEncodingException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipeline.IPipelineDriver;
import net.sf.okapi.common.pipeline.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FilterRoundtripTest {

	private IPipelineDriver driver;
	private IFilterConfigurationMapper fcMapper;
	
	@Before
	public void setUp() {
		fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		driver = new PipelineDriver();
		driver.getPipeline().getContext().setFilterConfigurationMapper(fcMapper);
		driver.addStep(new RawDocumentToFilterEventsStep());
		driver.addStep(new FilterEventsWriterStep());
	}

	@Test
	public void runPipelineFromString() {
		driver.clearItems();
		RawDocument rd = new RawDocument(
			"<p>Before <input type=\"radio\" name=\"FavouriteFare\" value=\"spam\" checked=\"checked\"/> after.</p>",
			"en", "es");
		driver.addBatchItem(rd, "okf_html",
			(new File("genericOutput.txt")).toURI(), "UTF-16LE");
		driver.processBatch();
		assertEquals("spam",
			getFirstTUSource(new RawDocument((new File("genericOutput.txt")).toURI(),
				"UTF-16LE", "es")));
	}

	@Test
	public void runPipelineFromStream() {
		driver.clearItems();
		RawDocument rd = new RawDocument("\nX\n\nY\n", "en", "fr");
		driver.addBatchItem(rd, "okf_html",
			(new File("genericOutput.txt")).toURI(), "UTF-8");
		driver.processBatch();
		assertEquals("X Y",
			getFirstTUSource(new RawDocument((new File("genericOutput.txt")).toURI(),
				"UTF-8", "fr")));
	}
	
	@Test
	public void runPipelineTwice() throws UnsupportedEncodingException {
		String snippet = "<b>TEST ME</b>";
		// First pass
		driver.clearItems();
		RawDocument rd = new RawDocument(snippet, "en", "es");
		driver.addBatchItem(rd, "okf_html",
			(new File("output1.html")).toURI(), "UTF-8");
		driver.processBatch();

		// Second pass
		driver.clearItems();
		rd = new RawDocument((new File("output1.html")).toURI(), "UTF-8", "es", "en");
		driver.addBatchItem(rd, "okf_html",
			(new File("output2.html")).toURI(), "UTF-8");
		driver.processBatch();
		
		// Check result
		assertEquals(snippet,
			getFirstTUSource(new RawDocument((new File("output2.html")).toURI(),
				"UTF-8", "es")));
	}

	private String getFirstTUSource (RawDocument rd) {
		IFilter filter = new HtmlFilter();
		try {
			filter.open(rd);
			Event event;
			while ( filter.hasNext() ) {
				event = filter.next();
				if ( event.getEventType() == EventType.TEXT_UNIT ) {
					TextUnit tu = (TextUnit)event.getResource();
					return tu.getSource().toString();
				}
			}
		}
		finally {
			if ( filter != null ) filter.close();
		}
		return null;
	}
	
	@After
	public void cleanUp() {
	}

}
