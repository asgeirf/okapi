/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.xliffkit.writer;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.lib.beans.v0.TestEvent;
import net.sf.okapi.lib.beans.v0.TestEventBean;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.lib.beans.sessions.OkapiXmlSession;
import net.sf.okapi.lib.extra.pipelinebuilder.XBatch;
import net.sf.okapi.lib.extra.pipelinebuilder.XBatchItem;
import net.sf.okapi.lib.extra.pipelinebuilder.XParameter;
import net.sf.okapi.lib.extra.pipelinebuilder.XPipeline;
import net.sf.okapi.lib.extra.pipelinebuilder.XPipelineStep;
import net.sf.okapi.lib.persistence.PersistenceSession;


import org.junit.Test;

@SuppressWarnings("unused")
public class XMLWriterTest {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private static final LocaleId FRFR = new LocaleId("fr", "fr");
	private static final LocaleId DEDE = new LocaleId("de", "de");	
	private static final LocaleId ITIT = new LocaleId("it", "it");
	
	// DEBUG @Test
	public void testPackageFormat4xml() throws URISyntaxException, MalformedURLException {

		int loops = 1;
		long start = System.currentTimeMillis();
		
		String pathBase = Util.getDirectoryName(this.getClass().getResource("test2.txt").getPath()) + "/";
		String src1Path = pathBase + "src1/";		
		String src2Path = pathBase + "src2/";
		//System.out.println(pathBase);
		
		XLIFFKitWriterStep writerStep = new XLIFFKitWriterStep();
		PersistenceSession session = new OkapiXmlSession();
		writerStep.setSession(session);
		writerStep.setResourcesFileExt(".xml");
		
		new XPipeline(
				"Test pipeline for XLIFFKitWriterStep",
				new XBatch(
//						new BatchItem(
//								new URL("file", null, src1Path + "test5.txt"),
//								"UTF-8",
//								ENUS,
//								FRFR)
//						,								
//						new BatchItem(
//								new URL("file", null, src1Path + "test6.txt"),
//								"UTF-8",
//								ITIT,
//								LocaleId.CHINA_CHINESE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test7.txt"),
//								"UTF-8",
//								ENUS,
//								FRFR),
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test8.txt"),
//								"UTF-8",
//								ITIT,
//								LocaleId.CHINA_CHINESE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test5.txt"),
//								"UTF-8",
//								ENUS,
//								DEDE),
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test8.txt"),
//								"UTF-8",
//								ITIT,
//								DEDE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test9.odt"),
//								"UTF-8",
//								ENUS,
//								DEDE)
//						,
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test10.html"),
//								"UTF-8",
//								ENUS,
//								DEDE),
// TODO DOCX is not mapped to any default filter configuration								
//						new BatchItem(
//								new URL("file", null, src1Path + "test11.docx"),
//								"UTF-8",
//								ENUS,
//								DEDE)
//						
						new XBatchItem(
								(new URL("file", null, src1Path + "BoldWorld.docx")).toURI(),
								"UTF-8",
								"okf_openxml",
								null,
								"UTF-8",
								ENUS,
								DEDE)
						),
								
				new RawDocumentToFilterEventsStep()
				,				
//				new PipelineStep(new LeveragingStep(), 
//						new Parameter("resourceClassName", net.sf.okapi.connectors.google.GoogleMTConnector.class.getName()),
//						new Parameter("threshold", 80),
//						new Parameter("fillTarget", true)
//				),
				
				new XPipelineStep(
						writerStep,								
						new XParameter("gMode", true),
						new XParameter("includeOriginal", true),
						new XParameter("message", "This document is a part of the test t-kit, generated from net.sf.okapi.steps.xliffkit.writer.testPackageFormat()"),
						//new XParameter("outputURI", this.getClass().getResource("draft4.xliff.kit").toURI().toString()))
						new XParameter("outputURI", new URL("file", null, pathBase + "testPackageFormat4.xml.kit").toURI().toString()))
		).execute();
		log(" Total: " + (System.currentTimeMillis() - start) + " milliseconds.");
	}
	
	private void log(String str) {
		Logger logger = Logger.getLogger(getClass().getName()); // loggers are cached
		logger.setLevel(Level.FINE);
		logger.fine(str);
	}
	
	// DEBUG @Test
	public void testPackageFormat5xml() throws URISyntaxException, MalformedURLException {

		int loops = 10;
		long start = System.currentTimeMillis();
		
		String pathBase = Util.getDirectoryName(this.getClass().getResource("test2.txt").getPath()) + "/";
		String src1Path = pathBase + "src1/";		
		String src2Path = pathBase + "src2/";
		//System.out.println(pathBase);
		
		XLIFFKitWriterStep writerStep = new XLIFFKitWriterStep();
		PersistenceSession session = new OkapiXmlSession();
		writerStep.setSession(session);
		writerStep.setResourcesFileExt(".xml");
		for(int i = 0; i < loops; i++) {
		new XPipeline(
				"Test pipeline for XLIFFKitWriterStep",
				new XBatch(
						new XBatchItem(
								new URL("file", null, src1Path + "test5.txt"),
								"UTF-8",
								ENUS,
								FRFR)
//						,								
//						new BatchItem(
//								new URL("file", null, src1Path + "test6.txt"),
//								"UTF-8",
//								ITIT,
//								LocaleId.CHINA_CHINESE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test7.txt"),
//								"UTF-8",
//								ENUS,
//								FRFR),
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test8.txt"),
//								"UTF-8",
//								ITIT,
//								LocaleId.CHINA_CHINESE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test5.txt"),
//								"UTF-8",
//								ENUS,
//								DEDE),
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test8.txt"),
//								"UTF-8",
//								ITIT,
//								DEDE)
//						,
//						new BatchItem(
//								new URL("file", null, src2Path + "test9.odt"),
//								"UTF-8",
//								ENUS,
//								DEDE)
//						,
//								
//						new BatchItem(
//								new URL("file", null, src1Path + "test10.html"),
//								"UTF-8",
//								ENUS,
//								DEDE),
// TODO DOCX is not mapped to any default filter configuration								
//						new BatchItem(
//								new URL("file", null, src1Path + "test11.docx"),
//								"UTF-8",
//								ENUS,
//								DEDE)
//						
//						new BatchItem(
//								(new URL("file", null, src1Path + "BoldWorld.docx")).toURI(),
//								"UTF-8",
//								"okf_openxml",
//								null,
//								"UTF-8",
//								ENUS,
//								DEDE)
						),
								
				new RawDocumentToFilterEventsStep()
				,				
//				new PipelineStep(new LeveragingStep(), 
//						new Parameter("resourceClassName", net.sf.okapi.connectors.google.GoogleMTConnector.class.getName()),
//						new Parameter("threshold", 80),
//						new Parameter("fillTarget", true)
//				),
				
				new XPipelineStep(
						writerStep,								
						new XParameter("gMode", true),
						new XParameter("includeOriginal", true),
						new XParameter("message", "This document is a part of the test t-kit, generated from net.sf.okapi.steps.xliffkit.writer.testPackageFormat()"),
						//new XParameter("outputURI", this.getClass().getResource("draft4.xliff.kit").toURI().toString()))
						new XParameter("outputURI", new URL("file", null, pathBase + "testPackageFormat5.xml.kit").toURI().toString()))
		).execute();
		}
		log(" Total: " + (System.currentTimeMillis() - start) + " milliseconds.");
	}

	
	// DEBUG 		
	@Test
	public void testReferences3xml() throws MalformedURLException, URISyntaxException {
		XLIFFKitWriterStep writerStep = new XLIFFKitWriterStep();
		PersistenceSession session = new OkapiXmlSession();
		writerStep.setSession(session);
		writerStep.setResourcesFileExt(".xml");
		
		String pathBase = Util.getDirectoryName(this.getClass().getResource("test2.txt").getPath()) + "/";
		writerStep.setOutputURI(new URL("file", null, pathBase + "testReferences3.xml.kit").toURI());
		writerStep.setTargetLocale(DEDE);
		net.sf.okapi.steps.xliffkit.writer.Parameters params = 
			(net.sf.okapi.steps.xliffkit.writer.Parameters) writerStep.getParameters();
		
		params.setIncludeSource(false);
		params.setIncludeOriginal(false);
		
		
		session.registerBean(TestEvent.class, TestEventBean.class);
		
		TestEvent e1 = new TestEvent("e1");
		TestEvent e2 = new TestEvent("e2");
		e2.setParent(e1);
		e1.setParent(e2);

		writerStep.handleEvent(new Event(EventType.START_BATCH));
		StartDocument sd = new StartDocument("sd1");
		sd.setName("test_refs3.txt");
		sd.setLocale(ENUS);
		sd.setFilterWriter(new GenericFilterWriter(null, null));
		
		writerStep.handleEvent(new Event(EventType.START_DOCUMENT, sd));
		writerStep.handleEvent(e1);
		writerStep.handleEvent(e2);
		writerStep.handleEvent(new Event(EventType.END_DOCUMENT));
		writerStep.handleEvent(new Event(EventType.END_BATCH));
	}
}
