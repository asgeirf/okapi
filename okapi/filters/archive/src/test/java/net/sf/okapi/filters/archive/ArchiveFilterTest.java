/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.tmx.TmxFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.lib.extra.pipelinebuilder.XBatch;
import net.sf.okapi.lib.extra.pipelinebuilder.XBatchItem;
import net.sf.okapi.lib.extra.pipelinebuilder.XPipeline;
import net.sf.okapi.lib.extra.steps.EventLogger;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.Before;
import org.junit.Test;

public class ArchiveFilterTest {

	private ArchiveFilter filter;
	private static final LocaleId EN = new LocaleId("en", "us");
	private static final LocaleId ESES = new LocaleId("es", "es");
	private String pathBase;
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	@Before
	public void setUp() {
		logger.setLevel(Level.FINE);
		filter = new ArchiveFilter();
		pathBase = Util.getDirectoryName(this.getClass().getResource("test1_es.archive").getPath()) + "/";
	}

	@Test
	public void testSubFilterOpen() throws ZipException, IOException {		
		URL url = new URL("file", null, pathBase + "test2_es.archive");
		URI uri = Util.toURI(url.getPath());
		ZipFile zipFile = new ZipFile(new File(uri));
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ZipEntry entry = entries.nextElement();
		IFilter subFilter = new XLIFFFilter();
		subFilter.open(new RawDocument(zipFile.getInputStream(entry), "UTF-8", EN, ESES));
	}
	
	@Test
	public void testFilterOpen() throws ZipException, IOException {		
		URL url = new URL("file", null, pathBase + "test2_es.archive");
		URI uri = Util.toURI(url.getPath());
		ZipFile zipFile = new ZipFile(new File(uri));
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ZipEntry entry = entries.nextElement();
		IFilter subFilter = new XLIFFFilter();
		subFilter.open(new RawDocument(zipFile.getInputStream(entry), "UTF-8", EN, ESES));
	}
	
	@Test
	public void testEvents() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		rd2fe.setFilter(filter);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test2_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testMimeType() throws MalformedURLException {		
		Parameters params = new Parameters();
		params.setFileExtensions("xliff2,tmx");
		params.setConfigIds("okf_xliff,okf_tmx");
		
		filter = new ArchiveFilter();
		assertEquals(ArchiveFilter.MIME_TYPE, filter.getMimeType());
		
		filter.setParameters(params);
		assertEquals(ArchiveFilter.MIME_TYPE, filter.getMimeType());
		
		params.setMimeType("okf_test");
		assertEquals("okf_test", filter.getMimeType());
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test2_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				new RawDocumentToFilterEventsStep(filter)
				
		).execute();
		assertEquals("okf_test", filter.getMimeType());
	}
	
	@Test
	public void testEvents2() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		FilterConfigurationMapper fcm = new FilterConfigurationMapper();
		// Create configuration for xliff2 extension
		fcm.addConfiguration(
				new FilterConfiguration("okf_xliff2",
						MimeTypeMapper.XLIFF_MIME_TYPE,
						XLIFFFilter.class.getName(),
						"XLIFF2",
						"Made-up format for Archive filter tests.",
						null,
						".xlf2;.xliff2;"));
		
		// Create configuration for tmx extension (if we need text units from tmx as well)
		fcm.addConfiguration(
				new FilterConfiguration("okf_tmx",
						MimeTypeMapper.TMX_MIME_TYPE,
						TmxFilter.class.getName(),
						//"net.sf.okapi.filters.tmx.TmxFilter",
						"TMX",
						"Configuration for Translation Memory eXchange (TMX) documents.",
						null,
						".tmx;"));
		
		
		filter = new ArchiveFilter(fcm);
		rd2fe.setFilter(filter);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test4_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents3() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		rd2fe.setFilter(new ArchiveFilter());
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test4_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents4() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		filter = new ArchiveFilter();
		rd2fe.setFilter(filter);
		
		Parameters params = new Parameters();
		params.setFileExtensions("xliff2,tmx");
		params.setConfigIds("okf_xliff,okf_tmx");
		filter.setParameters(params);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test4_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents5() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		filter = new ArchiveFilter();
		rd2fe.setFilter(filter);
		
		Parameters params = new Parameters();
		params.setFileExtensions("xliff2,xliff,xlf");
		params.setConfigIds("okf_xliff,okf_xliff,okf_xliff");
		filter.setParameters(params);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test4_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents6() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		filter = new ArchiveFilter();
		rd2fe.setFilter(filter);
		
		Parameters params = new Parameters();
		params.setFileExtensions("xlf,tmx");
		params.setConfigIds("okf_xliff,okf_tmx");
		filter.setParameters(params);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test5_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents7() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		rd2fe.setFilter(new ArchiveFilter());
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test5_es.archive"),
								"UTF-8",
								EN,
								ESES)
						),
						
				rd2fe,
				
				new EventLogger()
				
		).execute();
	}
	
	@Test
	public void testEvents8() throws MalformedURLException {		
		RawDocumentToFilterEventsStep rd2fe = new RawDocumentToFilterEventsStep();
		
		filter = new ArchiveFilter();
		rd2fe.setFilter(filter);
		
		Parameters params = new Parameters();
		params.setFileExtensions("xlf,tmx");
		params.setConfigIds("okf_xliff,okf_tmx");
		filter.setParameters(params);
		
		new XPipeline(
				"Test pipeline for ArchiveFilterContentTest",
				new XBatch(
						new XBatchItem(
								new URL("file", null, pathBase + "test5_es.archive"),
								"UTF-8",
								new URL("file", null, pathBase + "out/test5_es.archive"),
								"UTF-8",
								EN,
								ESES
								)
						),
						
				rd2fe,				
				new EventLogger(),
				new FilterEventsToRawDocumentStep()
				
		).execute();
	}
	
	@Test
	public void testDoubelextraction () {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(pathBase + "test1_es.archive", null));
		list.add(new InputDocument(pathBase + "test2_es.archive", null));
		list.add(new InputDocument(pathBase + "test3_es.archive", null));
		list.add(new InputDocument(pathBase + "test4_es.archive", null));
		list.add(new InputDocument(pathBase + "test5_es.archive", null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", EN, ESES, "out"));
	}
}
