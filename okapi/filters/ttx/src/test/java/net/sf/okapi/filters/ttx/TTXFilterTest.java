/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.filters.ttx;

import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.filters.ttx.TTXFilter;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TTXFilterTest {

	private TTXFilter filter1;
	private TTXFilter filter2;
	private GenericContent fmt;
	private String root;
	private LocaleId locENUS = LocaleId.fromString("en-us");
	private LocaleId locESEM = LocaleId.fromString("es-em");
	private LocaleId locFRFR = LocaleId.fromString("fr-fr");
//	private LocaleId locKOKR = LocaleId.fromString("ko-kr");
	
	private static final String STARTFILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<TRADOStag Version=\"2.0\"><FrontMatter>\n"
		+ "<ToolSettings CreationDate=\"20070508T094743Z\" CreationTool=\"TRADOS TagEditor\" CreationToolVersion=\"7.0.0.615\"></ToolSettings>\n"
		+ "<UserSettings DataType=\"STF\" O-Encoding=\"UTF-8\" SettingsName=\"\" SettingsPath=\"\" SourceLanguage=\"EN-US\" TargetLanguage=\"ES-EM\" SourceDocumentPath=\"abc.rtf\" SettingsRelativePath=\"\" PlugInInfo=\"\"></UserSettings>\n"
		+ "</FrontMatter><Body><Raw>\n";

	private static final String STARTFILENOLB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<TRADOStag Version=\"2.0\"><FrontMatter>\n"
		+ "<ToolSettings CreationDate=\"20070508T094743Z\" CreationTool=\"TRADOS TagEditor\" CreationToolVersion=\"7.0.0.615\"></ToolSettings>\n"
		+ "<UserSettings DataType=\"STF\" O-Encoding=\"UTF-8\" SettingsName=\"\" SettingsPath=\"\" SourceLanguage=\"EN-US\" TargetLanguage=\"ES-EM\" SourceDocumentPath=\"abc.rtf\" SettingsRelativePath=\"\" PlugInInfo=\"\"></UserSettings>\n"
		+ "</FrontMatter><Body><Raw>";

//	private static final String STARTFILEKO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
//		+ "<TRADOStag Version=\"2.0\"><FrontMatter>\n"
//		+ "<ToolSettings CreationDate=\"20070508T094743Z\" CreationTool=\"TRADOS TagEditor\" CreationToolVersion=\"7.0.0.615\"></ToolSettings>\n"
//		+ "<UserSettings DataType=\"STF\" O-Encoding=\"UTF-8\" SettingsName=\"\" SettingsPath=\"\" SourceLanguage=\"EN-US\" TargetLanguage=\"KO-KR\" TargetDefaultFont=\"\ubd7e\" SourceDocumentPath=\"abc.rtf\" SettingsRelativePath=\"\" PlugInInfo=\"\"></UserSettings>\n"
//		+ "</FrontMatter><Body><Raw>\n";

	@Before
	public void setUp() {
		filter1 = new TTXFilter();
		fmt = new GenericContent();
		root = TestUtil.getParentDir(this.getClass(), "/Test01.html.ttx");

		filter2 = new TTXFilter();
		Parameters params = (Parameters)filter2.getParameters();
		params.setForceSegments(false);
	}

	@Test
	public void testSegmentedSurroundedByInternalCodes () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\">bc</ut>"
			+ "<Tu MatchPercent=\"100\">"
			+ "<Tuv Lang=\"EN-US\">en1</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">es1</Tuv>"
			+ "</Tu>"
			+ "<ut Type=\"end\">ec</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		assertEquals("en1", segments.get(0).toString());
		cont = tu.getTarget(locESEM);
		segments = cont.getSegments();
		assertEquals(1, segments.count());
		assertEquals("<b1/>[es1]<e1/>", fmt.printSegmentedContent(cont, true));
	}

	@Test
	public void testNotSegmentedWithDFAndCodes () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"12\">"
			+ "<ut Type=\"start\" Style=\"external\">{P}</ut><ut Type=\"start\">{i}</ut>"
			+ "src text" 
			+ "</df><ut Type=\"end\">{/i}</ut>"
			+ "<ut Type=\"end\" Style=\"external\">{/P}</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		// </df> moved outside
		assertEquals("[<1>src text</1>]", fmt.printSegmentedContent(cont, true));
	}

	@Test
	public void testNotSegmentedWithDF () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"12\">"
			+ "<ut Type=\"start\" Style=\"external\">{P}</ut>"
			+ "src text" 
			+ "</df>"
			+ "<ut Type=\"end\" Style=\"external\">{/P}</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		// </df> moved outside
		assertEquals("[src text]", fmt.printSegmentedContent(cont, true));
	}

	@Test
	public void testOutputNotSegmentedWithDF_ForcingOutSeg () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"12\">"
			+ "<ut Type=\"start\" Style=\"external\">{P}</ut>"
			+ "src text" 
			+ "</df>"
			+ "<ut Type=\"end\" Style=\"external\">{/P}</ut>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<df Size=\"12\">"
			+ "<ut Type=\"start\" Style=\"external\">{P}</ut>"
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">src text</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">src text</Tuv>"
			+ "</Tu>"
			+ "</df>"
			+ "<ut Type=\"end\" Style=\"external\">{/P}</ut>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testSegmentedSurroundedByDF () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"16\">"
			+ "<ut Type=\"start\" Style=\"external\">bc</ut>"
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">en1</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">es1</Tuv>"
			+ "</Tu>"
			+ "</df>" // This is the potential issue
			+ "<ut Type=\"end\" Style=\"external\">ec</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		assertEquals("en1", segments.get(0).toString());
		segments = tu.getTarget(locESEM).getSegments();
		assertEquals(1, segments.count());
		assertEquals("es1", segments.get(0).toString());
		assertNull(segments.get(0).getAnnotation(AltTranslationsAnnotation.class));
	}

	@Test
	public void testOutputSegmentedSurroundedByDF () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"16\">"
			+ "<ut Type=\"start\" Style=\"external\">bc</ut>"
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">en1</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">es1</Tuv>"
			+ "</Tu>"
			+ "</df>" // This is the potential issue
			+ "<ut Type=\"end\" Style=\"external\">ec</ut>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<df Size=\"16\">"
			+ "<ut Type=\"start\" Style=\"external\">bc</ut>"
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">en1</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">es1</Tuv>"
			+ "</Tu>"
			+ "</df>"
			+ "<ut Type=\"end\" Style=\"external\">ec</ut>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testBasicWithEscapes () {
		String snippet = STARTFILENOLB
			+ "&lt;=lt, &amp;=amp, &gt;=gt, &quot;=quot."
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("<=lt, &=amp, >=gt, \"=quot.", cont.toString());
	}

	@Test
	public void testOutputBasicWithEscapes () {
		String snippet = STARTFILENOLB
			+ "&lt;=lt, &amp;=amp, &gt;=gt, &quot;=quot."
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "&lt;=lt, &amp;=amp, >=gt, &quot;=quot."
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testBasicNoExtractableData () {
		String snippet = STARTFILE
			+ " <ut Style=\"external\">some &amp; code</ut>\n\n  <!-- comments-->"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter2, snippet, locESEM), 1);
		assertNull(tu);
	}

	@Test
	public void testOutputNoExtractableData () {
		String snippet = STARTFILE
			+ " <ut Style=\"external\">some &amp; code</ut>\n\n  <!-- comments-->"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(snippet, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testBasicNoTU () {
		String snippet = STARTFILE
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter2, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("\ntext", cont.toString());
		assertFalse(tu.hasTarget(locESEM));
	}

	@Test
	public void testOutputBasicNoTU () {
		String snippet = STARTFILENOLB
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testOutputBasicNoTUWithSegmentationOption () {
		String snippet = STARTFILENOLB
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testOutputEscapesInSkeleton () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\" RightEdge=\"angle\" DisplayText=\"![if !IE]&gt;&lt;p&gt;Text &lt;b&gt;&lt;u&gt;non-validating&lt;/u&gt; downlevel-revealed conditional comment&lt;/b&gt; etc &lt;/p&gt;&lt;![\">code</ut>"
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<ut Type=\"start\" RightEdge=\"angle\" DisplayText=\"![if !IE]&gt;&lt;p&gt;Text &lt;b&gt;&lt;u&gt;non-validating&lt;/u&gt; downlevel-revealed conditional comment&lt;/b&gt; etc &lt;/p&gt;&lt;![\">code</ut>"
			+ "text"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testBasicNoTUWithDF () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"16\">text</df>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter2, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("text", cont.toString());
		assertFalse(tu.hasTarget(locESEM));
	}

	@Test
	public void testOutputBasicNoTUWithDFWithSegementation () {
		String snippet = STARTFILENOLB
			+ "<df Size=\"16\">text</df>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\"><df Size=\"16\">text</df></Tuv>"
			+ "<Tuv Lang=\"ES-EM\"><df Size=\"16\">text</df></Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testVariousTags () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "<ut Type=\"end\" Style=\"external\">&lt;/ul&gt;</ut>"
			+ "<ut Type=\"start\" Style=\"external\">&lt;P&gt;</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter2, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("paragraph <1/><2>text<3/></2>", fmt.printSegmentedContent(cont, false));
		assertEquals("[paragraph <1/><2>text<3/></2>]", fmt.printSegmentedContent(cont, true));
	}
	
	@Test
	public void testOutputVariousTags () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "<ut Type=\"end\" Style=\"external\">&lt;/ul&gt;</ut>"
			+ "<ut Type=\"start\" Style=\"external\">&lt;P&gt;</ut>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "<ut Type=\"end\" Style=\"external\">&lt;/ul&gt;</ut>"
			+ "<ut Type=\"start\" Style=\"external\">&lt;P&gt;</ut>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}
	
	@Test
	public void testOutputVariousTagsWithSegmentation () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "<ut Type=\"end\" Style=\"external\">&lt;/ul&gt;</ut>"
			+ "<ut Type=\"start\" Style=\"external\">&lt;P&gt;</ut>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>"
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">paragraph <df Italic=\"on\">"
			+ "<ut Type=\"start\">&lt;i&gt;</ut>text</df><ut Type=\"end\">&lt;/i&gt;</ut>"
			+ "</Tuv></Tu>"
			+ "<ut Type=\"end\" Style=\"external\">&lt;/ul&gt;</ut>"
			+ "<ut Type=\"start\" Style=\"external\">&lt;P&gt;</ut>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}
	
	@Test
	public void testBadlyNestedTags () {
		String snippet = STARTFILENOLB
			+ "<ut Type=\"start\" Style=\"external\">&lt;p&gt;</ut>"
			+ "Before <df Size=\"8\"><ut Type=\"start\">&lt;FONT face=Arial size=2&gt;</ut>After"
			+ "<ut Type=\"start\" Style=\"external\">&lt;p style=&quot;background-color: #e0e0e0&quot;&gt;</ut>"
			+ "Next</df><ut Type=\"end\">&lt;/FONT&gt;</ut>Last."
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("[Before <1/><b2/>After]", fmt.printSegmentedContent(cont, true));
		tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 2);
		assertNotNull(tu);
		cont = tu.getSource();
//		assertEquals("Next <1/><2/>Last.", fmt.setContent(cont).toString());
	}

	@Test
	public void testWithMixedSegmentation () {
		String snippet = STARTFILENOLB
			+ "<Tu MatchPercent=\"50\"><Tuv Lang=\"EN-US\">text</Tuv></Tu>"
			+ " more text"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("[text] more text", fmt.printSegmentedContent(cont, true));
		cont = tu.getTarget(locESEM);
		assertNotNull(cont);
		assertEquals("[text] more text", fmt.printSegmentedContent(cont, true));
		ISegments segs = cont.getSegments();
		AltTranslationsAnnotation ann = segs.get(0).getAnnotation(AltTranslationsAnnotation.class);
		assertNotNull(ann);
		assertEquals(1, ann.size());
		assertEquals("text", ann.getFirst().getTarget().toString());
	}

	@Test
	public void testOutputWithMixedSegmentation () {
		String snippet = STARTFILENOLB
			+ "<Tu MatchPercent=\"50\"><Tuv Lang=\"EN-US\">text</Tuv></Tu>"
			+ " more text"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<Tu MatchPercent=\"50\"><Tuv Lang=\"EN-US\">text</Tuv><Tuv Lang=\"ES-EM\">text</Tuv></Tu>"
			+ " more text"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testTUInfo () {
		String snippet = STARTFILENOLB
			+ "<Tu Origin=\"abc\" MatchPercent=\"50\"><Tuv Lang=\"EN-US\">en</Tuv><Tuv Lang=\"ES-EM\">es</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getTarget(locESEM);
		assertNotNull(cont);
		ISegments segs = cont.getSegments();
		AltTranslationsAnnotation ann = segs.get(0).getAnnotation(AltTranslationsAnnotation.class);
		assertNotNull(ann);
		assertEquals(1, ann.size());
		assertEquals("es", ann.getFirst().getTarget().toString());
		assertEquals("abc", ann.getFirst().getOrigin());
		assertEquals(50, ann.getFirst().getScore());
	}

	@Test
	public void testOutputTUInfo () {
		String snippet = STARTFILENOLB
			+ "<Tu Origin=\"abc\" MatchPercent=\"50\"><Tuv Lang=\"EN-US\">en</Tuv><Tuv Lang=\"ES-EM\">es</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<Tu Origin=\"abc\" MatchPercent=\"50\"><Tuv Lang=\"EN-US\">en</Tuv><Tuv Lang=\"ES-EM\">es</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testWithPINoTU () {
		String snippet = STARTFILE
			+ "<ut Class=\"procinstr\">pi</ut>text"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("text", cont.toString());
		assertFalse(tu.hasTarget(locESEM));
	}

	@Test
	public void testNoTUEndsWithUT () {
		String snippet = STARTFILE
			+ "text<ut Class=\"procinstr\">pi</ut>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("\ntext", cont.toString());
		assertFalse(tu.hasTarget(locESEM));
	}

	@Test
	public void testNoTUContentWithSplitStart () {
		String snippet = STARTFILENOLB
			+ "before <ut Type=\"start\" RightEdge=\"split\">[ulink={</ut>text1<ut Type=\"start\" LeftEdge=\"split\">}]</ut>text2<ut Type=\"end\">[/ulink]</ut> after"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("before [ulink={text1}]text2[/ulink] after", cont.toString());
		assertEquals("before <1>text1<2/>text2</1> after", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testNoTUContentWithUT () {
		String snippet = STARTFILENOLB
			+ "before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("before [in] after", cont.toString());
		assertFalse(tu.hasTarget(locESEM));
	}

	@Test
	public void testOutputNoTUContentWithUT () {
		String snippet = STARTFILENOLB
			+ "before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testOutputNoTUContentWithUTWithSegmentation () {
		String snippet = STARTFILENOLB
			+ "before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">before <ut Type=\"start\">[</ut>in<ut Type=\"end\">]</ut> after</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testForExternalDF () {
		String snippet = STARTFILENOLB
			+ "<df Font=\"Arial\"><ut Style=\"external\">code</ut>text<ut Style=\"external\">code</ut></df>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("text", cont.toString());
	}

	@Test
	public void testOutputForExternalDF () {
		String snippet = STARTFILENOLB
			+ "<df Font=\"Arial\"><ut Style=\"external\">code</ut>text<ut Style=\"external\">code</ut></df>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<df Font=\"Arial\"><ut Style=\"external\">code</ut>text<ut Style=\"external\">code</ut></df>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testOutputForExternalDFwithSegmentation () {
		String snippet = STARTFILENOLB
			+ "<df Font=\"Arial\"><ut Style=\"external\">code</ut>text<ut Style=\"external\">code</ut></df>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<df Font=\"Arial\"><ut Style=\"external\">code</ut>"
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text</Tuv></Tu>"
			+ "<ut Style=\"external\">code</ut></df>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter1, snippet, locESEM),
			locESEM, filter1.createSkeletonWriter(), filter1.getEncoderManager()));
	}

	@Test
	public void testForTwoTUs () {
		String snippet = STARTFILENOLB
			+ "text1<ut Style=\"external\">code</ut>text2"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		assertEquals("text1", cont.toString());
		tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 2);
		assertNotNull(tu);
		cont = tu.getSource();
		assertEquals("text2", cont.toString());
	}

	@Test
	public void testOutputForTwoTUs () {
		String snippet = STARTFILENOLB
			+ "text1<ut Style=\"external\">code</ut>text2"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "text1<ut Style=\"external\">code</ut>text2"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

//	@Test
//	public void testOutputForTwoTUsWithSegmentation () {
//		String snippet = STARTFILENOLB
//			+ "text1<ut Style=\"external\">code</ut>text2"
//			+ "</Raw></Body></TRADOStag>";
//		String expected = STARTFILENOLB
//			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text1</Tuv><Tuv Lang=\"ES-EM\">text1</Tuv></Tu>"
//			+ "<ut Style=\"external\">code</ut>"
//			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text2</Tuv><Tuv Lang=\"ES-EM\">text2</Tuv></Tu>"
//			+ "</Raw></Body></TRADOStag>";
//		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM), locESEM,
//			filter2.createSkeletonWriter(), filter2.getEncoderManager()));
//	}

	@Test
	public void testOutputWithPINoTU () {
		String snippet = STARTFILENOLB
			+ "<ut Class=\"procinstr\">pi</ut>text"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILENOLB
			+ "<ut Class=\"procinstr\">pi</ut>text"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

//	@Test
//	public void testOutputWithPINoTUWithSegmentation () {
//		String snippet = STARTFILENOLB
//			+ "<ut Class=\"procinstr\">pi</ut>text"
//			+ "</Raw></Body></TRADOStag>";
//		String expected = STARTFILENOLB
//			+ "<ut Class=\"procinstr\">pi</ut><Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text</Tuv>"
//			+ "<Tuv Lang=\"ES-EM\">text</Tuv></Tu>"
//			+ "</Raw></Body></TRADOStag>";
//		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet, locESEM), locESEM,
//			filter.createSkeletonWriter()));
//	}

	@Test
	public void testBasicNoUT () {
		String snippet = STARTFILE
			+ "<Tu>"
			+ "<Tuv Lang=\"EN-US\">text en</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text es</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		segments.joinAll();
		assertEquals("text en", cont.toString());
		cont = tu.getTarget(locESEM);
		segments = cont.getSegments();
		assertEquals(1, segments.count());
		segments.joinAll();
		assertEquals("text es", cont.toString());
	}

	@Test
	public void testBasicTwoSegInOneTextUnit () {
		String snippet = STARTFILENOLB
			+ "<Tu><Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"ES-EM\">text1 es</Tuv></Tu>"
			+ "  <Tu><Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"ES-EM\">text2 es</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(2, segments.count());
		assertEquals("text1 en", segments.get(0).text.toText());
		assertEquals("text2 en", segments.get(1).text.toText());
		assertEquals("[text1 en]  [text2 en]", fmt.printSegmentedContent(cont, true));
		cont = tu.getTarget(locESEM);
		segments = cont.getSegments();
		assertEquals(2, segments.count());
		assertEquals("text1 es", segments.get(0).text.toText());
		assertEquals("text2 es", segments.get(1).text.toText());
		assertEquals("[text1 es]  [text2 es]", fmt.printSegmentedContent(cont, true));

		tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 2);
		assertNull(tu);
	}

	@Test
	public void testOutputBasicTwoSegInOneTextUnit () {
		String snippet = STARTFILENOLB
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"ES-EM\">text1 es</Tuv></Tu>"
			+ "  <Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"ES-EM\">text2 es</Tuv></Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(snippet, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testBasicWithUT () {
		String snippet = STARTFILE
			+ "<Tu>"
			+ "<Tuv Lang=\"EN-US\">text <ut DisplayText=\"br\">&lt;br/&gt;</ut>en <ut Type=\"start\">&lt;b></ut>bold<ut Type=\"end\">&lt;/b></ut>.</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">TEXT <ut DisplayText=\"br\">&lt;br/&gt;</ut>ES <ut Type=\"start\">&lt;b></ut>BOLD<ut Type=\"end\">&lt;/b></ut>.</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(filter1, snippet, locESEM), 1);
		assertNotNull(tu);
		TextContainer cont = tu.getSource();
		ISegments segments = cont.getSegments();
		assertEquals(1, segments.count());
		assertEquals("text <br/>en <b>bold</b>.", segments.get(0).text.toText());
		assertEquals("text <1/>en <2>bold</2>.", fmt.setContent(segments.get(0).text).toString());
		cont = tu.getTarget(locESEM);
		segments = cont.getSegments();
		assertEquals(1, segments.count());
		assertEquals("TEXT <br/>ES <b>BOLD</b>.", segments.get(0).text.toText());
		assertEquals("TEXT <1/>ES <2>BOLD</2>.", fmt.setContent(segments.get(0).text).toString());
	}

	@Test
	public void testOutputSimple () {
		String snippet = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text en >=gt</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text es >=gt</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text en >=gt</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text es >=gt</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testOutputSimpleGTEscaped () {
		String snippet = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text en >=gt</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text es >=gt</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text en &gt;=gt</Tuv>"
			+ "<Tuv Lang=\"ES-EM\">text es &gt;=gt</Tuv>"
			+ "</Tu>"
			+ "</Raw></Body></TRADOStag>";
		((Parameters)(filter2.getParameters())).setEscapeGT(true);
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

	@Test
	public void testOutputTwoTU () {
		String snippet = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"ES-EM\">text1 es</Tuv></Tu>\n"
			+ "  <ut Style=\"external\">some code</ut>  "
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"ES-EM\">text2 es</Tuv></Tu>\n"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"ES-EM\">text1 es</Tuv></Tu>\n"
			+ "  <ut Style=\"external\">some code</ut>  "
			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"ES-EM\">text2 es</Tuv></Tu>\n"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}
	
	@Test
	public void testOutputWithOriginalWithoutTraget () {
		String snippet = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text1 en</Tuv></Tu>\n"
			+ "  <ut Style=\"external\">some code</ut>  "
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text2 en</Tuv></Tu>\n"
			+ "</Raw></Body></TRADOStag>";
		String expected = STARTFILE
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"ES-EM\">text1 en</Tuv></Tu>\n"
			+ "  <ut Style=\"external\">some code</ut>  "
			+ "<Tu MatchPercent=\"0\">"
			+ "<Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"ES-EM\">text2 en</Tuv></Tu>\n"
			+ "</Raw></Body></TRADOStag>";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locESEM),
			locESEM, filter2.createSkeletonWriter(), filter2.getEncoderManager()));
	}

//	@Test
//	public void testOutputWithOriginalWithoutTargetKO () {
//		String snippet = STARTFILEKO
//			+ "<Tu><Tuv Lang=\"EN-US\">text1 en</Tuv></Tu>\n"
//			+ "  <ut Style=\"external\">some code</ut>  "
//			+ "<Tu><Tuv Lang=\"EN-US\">text2 en</Tuv></Tu>\n"
//			+ "</Raw></Body></TRADOStag>";
//		String expected = STARTFILEKO
//			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text1 en</Tuv><Tuv Lang=\"KO-KR\"><df Font=\"\ubd7e\">text1 en</df></Tuv></Tu>\n"
//			+ "  <ut Style=\"external\">some code</ut>  "
//			+ "<Tu MatchPercent=\"0\"><Tuv Lang=\"EN-US\">text2 en</Tuv><Tuv Lang=\"KO-KR\"><df Font=\"\ubd7e\">text2 en</df></Tuv></Tu>\n"
//			+ "</Raw></Body></TRADOStag>";
//		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(filter2, snippet, locKOKR), locKOKR,
//			filter2.createSkeletonWriter()));
//	}

	@Test
	public void testDoubleExtraction () {
		// Read all files in the data directory
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(root+"Test01.html.ttx", null));
		list.add(new InputDocument(root+"Test02_noseg.html.ttx", null));
		list.add(new InputDocument(root+"Test02_allseg.html.ttx", null));
		RoundTripComparison rtc = new RoundTripComparison();
		// Use non-forced segmentation output
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENUS, locFRFR));
	}
	
	@Test
	public void textDoubleExtractionOriginalAllSegmented () {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(root+"Test02_allseg.html.ttx", null));
		RoundTripComparison rtc = new RoundTripComparison();
		// Use the output with forced segmentation, since all is segmented 
		// in the input file we should not add any.
		assertTrue(rtc.executeCompare(filter1, list, "UTF-8", locENUS, locFRFR));
	}

	// Disable this test for SVN commit
//	@Test
	public void __LOCALTEST_ONLY_testDoubleExtractionPrivateFiles () {
		// Read all files in the data directory
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		RoundTripComparison rtc = new RoundTripComparison();

		list.clear();
		list.add(new InputDocument(root+"private/set01/file04.mif.rtf.ttx", null));
		LocaleId locJA = LocaleId.fromString("JA");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENUS, locJA));
		list.clear();
		list.add(new InputDocument(root+"private/set01/file01.xml.ttx", null));
		list.add(new InputDocument(root+"private/set01/file02.xml.ttx", null));
		list.add(new InputDocument(root+"private/set01/file03.xml.ttx", null));
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENUS, locFRFR));

		list.clear();
		list.add(new InputDocument(root+"private/set02/file01.xml.ttx", null));
		list.add(new InputDocument(root+"private/set02/file02.xml.ttx", null));
		list.add(new InputDocument(root+"private/set02/file03.xml.ttx", null));
		list.add(new InputDocument(root+"private/set02/file04.xml.ttx", null));
		list.add(new InputDocument(root+"private/set02/file05.xml.ttx", null));
		LocaleId locFRCA = LocaleId.fromString("FR-CA");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENUS, locFRCA));

		list.clear();
		list.add(new InputDocument(root+"private/set03/file01.xml.ttx", null));
		list.add(new InputDocument(root+"private/set03/file02.xml.ttx", null));
		LocaleId locDEDE = LocaleId.fromString("DE-DE");
		LocaleId locENGB = LocaleId.fromString("EN-GB");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locDEDE, locENGB));

		list.clear();
		list.add(new InputDocument(root+"private/set03/file03.xml.ttx", null));
		list.add(new InputDocument(root+"private/set03/file04.xml.ttx", null));
		LocaleId locZHCN = LocaleId.fromString("ZH-CN");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locDEDE, locZHCN));

		list.clear();
		list.add(new InputDocument(root+"private/set03/file05.xml.ttx", null));
		list.add(new InputDocument(root+"private/set03/file06.xml.ttx", null));
		LocaleId locNLNL = LocaleId.fromString("NL-NL");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locDEDE, locNLNL));

		list.clear();
		list.add(new InputDocument(root+"private/set04/file01.mif.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set04/file02.mif.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set04/file03.mif.rtf.ttx", null));
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENGB, locFRFR));

		list.clear();
		list.add(new InputDocument(root+"private/set05/file01.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file02.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file03.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file04.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file05.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file06.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file07.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file08.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file09.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file10.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file11.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file12.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file13.rtf.ttx", null));
		list.add(new InputDocument(root+"private/set05/file14.rtf.ttx", null));
		LocaleId locPL = LocaleId.fromString("PL");
		assertTrue(rtc.executeCompare(filter2, list, "UTF-8", locENGB, locPL));
	}

	private ArrayList<Event> getEvents(IFilter filter, String snippet, LocaleId trgLocId) {
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, locENUS, trgLocId));
		while ( filter.hasNext() ) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

}
