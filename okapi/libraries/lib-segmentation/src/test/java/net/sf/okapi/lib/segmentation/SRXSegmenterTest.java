/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.segmentation;

import java.util.ArrayList;

import org.junit.Before;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.lib.segmentation.LanguageMap;
import net.sf.okapi.lib.segmentation.Rule;
import net.sf.okapi.lib.segmentation.SRXDocument;
import net.sf.okapi.lib.segmentation.SRXSegmenter;

import org.junit.Test;
import static org.junit.Assert.*;

public class SRXSegmenterTest {

	@Before
	public void setUp() {
	}

	@Test
	public void testDefaultOptions () {
		SRXSegmenter seg = new SRXSegmenter();
		// Check default options
		assertFalse(seg.cascade());
		assertTrue(seg.segmentSubFlows());
		assertFalse(seg.includeStartCodes());
		assertTrue(seg.includeEndCodes());
		assertFalse(seg.includeIsolatedCodes());
		assertFalse(seg.oneSegmentIncludesAll());
		assertFalse(seg.trimLeadingWhitespaces());
		assertFalse(seg.trimTrailingWhitespaces());
	}		
	
	@Test
	public void testChangedOptions () {
		SRXSegmenter seg = new SRXSegmenter();
		// Check changing options
		seg.setOptions(false, true, false, true, true, true, true, true);
		assertFalse(seg.segmentSubFlows());
		assertTrue(seg.includeStartCodes());
		assertFalse(seg.includeEndCodes());
		assertTrue(seg.includeIsolatedCodes());
		assertTrue(seg.oneSegmentIncludesAll());
		assertTrue(seg.trimLeadingWhitespaces());
		assertTrue(seg.trimTrailingWhitespaces());
		assertTrue(seg.useJavaRegex());
	}
	
	@Test
	public void testSimpleSegmentationDefault () {
		ISegmenter seg = createSegmenterWithRules(LocaleId.fromString("en"));
		TextContainer tc = new TextContainer("Part 1.  Part 2. ");
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(3, n);
		segments.create(seg.getRanges());
		assertEquals(3, segments.count());
		assertEquals("Part 1.", segments.get(0).toString());
		assertEquals("  Part 2.", segments.get(1).toString());
		assertEquals(" ", segments.get(2).toString());
	}
	
	@Test
	public void testSimpleSegmentationTrimLeading () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), seg.includeStartCodes(), seg.includeEndCodes(), seg.includeIsolatedCodes(),
			seg.oneSegmentIncludesAll(), true, false, seg.useJavaRegex());
		TextContainer tc = new TextContainer(" Part 1.  Part 2. ");
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(2, n);
		segments.create(seg.getRanges());
		assertEquals(2, segments.count());
		assertEquals("Part 1.", segments.get(0).toString());
		assertEquals("Part 2.", segments.get(1).toString());
	}
	
	@Test
	public void testSimpleSegmentationTrimTrailing () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), seg.includeStartCodes(), seg.includeEndCodes(), seg.includeIsolatedCodes(),
			seg.oneSegmentIncludesAll(), false, true, seg.useJavaRegex());
		TextContainer tc = new TextContainer(" Part 1.  Part 2. ");
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(2, n);
		segments.create(seg.getRanges());
		assertEquals(2, segments.count());
		assertEquals(" Part 1.", segments.get(0).toString());
		assertEquals("  Part 2.", segments.get(1).toString());
	}
	
	@Test
	public void testSimpleSegmentationOneIsAll () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), seg.includeStartCodes(), seg.includeEndCodes(), seg.includeIsolatedCodes(),
			true, true, true, seg.useJavaRegex());
		TextContainer tc = new TextContainer(" Part 1  ");
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(1, n);
		segments.create(seg.getRanges());
		assertEquals(1, segments.count());
		assertEquals(" Part 1  ", segments.get(0).toString());
	}
	
	@Test
	public void testTUSegmentation () {
		ITextUnit tu = createMultiTargetSegmentedTextUnit();
		// Check default source segmentation
		ISegments segs = tu.getSource().getSegments();
		assertEquals(3, segs.count());
		assertEquals("Part 1.", segs.get(0).toString());
		assertEquals(" Part 2.", segs.get(1).toString());
		assertEquals(" Part 3.", segs.get(2).toString());
		assertTrue(tu.hasVariantSources());
		// Switch to French
		segs = tu.getVariantSources().get(LocaleId.FRENCH).getSegments();
		assertEquals(2, segs.count());
		assertEquals("Part 1. Part 2.", segs.get(0).toString());
		assertEquals("Part 3.", segs.get(1).toString());
		// back to German
		segs = tu.getVariantSources().get(LocaleId.GERMAN).getSegments();
		assertEquals(" Part 2.", segs.get(1).toString());
		// Default should be like for German
		segs = tu.getSourceSegments();
		assertEquals(" Part 2.", segs.get(1).toString());
	}

	@Test
	public void testCodedSegmentationDefault1 () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), false, true, false,
			false, false, false, seg.useJavaRegex());
		TextContainer tc = createCodedText();
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(5, n);
		segments.create(seg.getRanges());
		assertEquals(5, segments.count());
		assertEquals("PH after.", segments.get(0).toString());
		assertEquals("<br/><b> End after.</b>", segments.get(1).toString());
		assertEquals(" Start after.", segments.get(2).toString());
		assertEquals("<i> Text.</i>", segments.get(3).toString());
		assertEquals("  ", segments.get(4).toString());
	}
	
	@Test
	public void testCodedSegmentationNotDefault1 () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), true, false, true,
			false, false, false, seg.useJavaRegex());
		TextContainer tc = createCodedText();
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(5, n);
		segments.create(seg.getRanges());
		assertEquals(5, segments.count());
		assertEquals("PH after.<br/><b>", segments.get(0).toString());
		assertEquals(" End after.", segments.get(1).toString());
		assertEquals("</b> Start after.<i>", segments.get(2).toString());
		assertEquals(" Text.", segments.get(3).toString());
		assertEquals("</i>  ", segments.get(4).toString());
	}
	
	@Test
	public void testCodedSegmentationDefault2 () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), false, true, false,
			false, false, false, seg.useJavaRegex());
		TextContainer tc = createCodedText2();
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(5, n);
		segments.create(seg.getRanges());
		assertEquals(5, segments.count());
		assertEquals("PH after.", segments.get(0).toString());
		assertEquals("<br/><br/><b><i> End after.</i></b>", segments.get(1).toString());
		assertEquals(" Start after.", segments.get(2).toString());
		assertEquals("<u><i> Text.</i></u>", segments.get(3).toString());
		assertEquals("  ", segments.get(4).toString());
	}

	@Test
	public void testCodedSegmentationNotDefault2 () {
		SRXSegmenter seg = (SRXSegmenter)createSegmenterWithRules(LocaleId.fromString("en"));
		seg.setOptions(seg.segmentSubFlows(), true, false, true,
			false, false, false, seg.useJavaRegex());
		TextContainer tc = createCodedText2();
		ISegments segments = tc.getSegments();
		int n = seg.computeSegments(tc);
		assertEquals(5, n);
		segments.create(seg.getRanges());
		assertEquals(5, segments.count());
		assertEquals("PH after.<br/><br/><b><i>", segments.get(0).toString());
		assertEquals(" End after.", segments.get(1).toString());
		assertEquals("</i></b> Start after.<u><i>", segments.get(2).toString());
		assertEquals(" Text.", segments.get(3).toString());
		assertEquals("</i></u>  ", segments.get(4).toString());
	}
	
	@Test
	public void testTUSegmentationRemoval () {
		ITextUnit tu = createMultiTargetSegmentedTextUnit();
		// Removing the target does not change the source associated with it
		tu.removeTarget(LocaleId.FRENCH);
		ISegments segs = tu.getVariantSources().get(LocaleId.FRENCH).getSegments();
		assertEquals(2, segs.count());
		assertEquals("Part 3.", segs.get(1).toString());
		// You have to explicitly remove the source variant
		tu.getVariantSources().remove(LocaleId.FRENCH);
		segs = tu.getVariantSources().get(LocaleId.FRENCH).getSegments();
		assertEquals(3, segs.count());
		assertEquals(" Part 2.", segs.get(1).toString());
	}
	
	@Test
	public void testTUSegmentationRemovalAll () {
		ITextUnit tu = createMultiTargetSegmentedTextUnit();
		assertTrue(tu.hasVariantSources());
		tu.removeAllSegmentations();
		assertFalse(tu.hasVariantSources());
		// Nothing is segmented now
		ISegments segs = tu.getSource().getSegments();
		assertEquals(1, segs.count());
		assertEquals("Part 1. Part 2. Part 3.", tu.getVariantSources().get(LocaleId.GERMAN).getLastContent().toText());
		assertEquals("Part 1. Part 2. Part 3.", tu.getVariantSources().get(LocaleId.FRENCH).getLastContent().toText());
	}
	
	@Test
	public void testICUSpecificPatterns () {
		SRXDocument doc = new SRXDocument();
		LanguageMap langMap = new LanguageMap(".*", "default");
		doc.addLanguageMap(langMap);
		// Add the rules
		ArrayList<Rule> langRules = new ArrayList<Rule>();
		langRules.add(new Rule("\\w", "\\s", true));
		langRules.add(new Rule("\\d", "\\s", true));
		langRules.add(new Rule("\\N{WAVE DASH}", "\\s", true));
		langRules.add(new Rule("z", "\\x{0608}", true));
		doc.addLanguageRule("default", langRules);
		// Create the segmenter
		ISegmenter segmenter = doc.compileLanguageRules(LocaleId.ENGLISH, null);
		
		assertEquals(2, segmenter.computeSegments("e ")); // e + space
		assertEquals(2, segmenter.computeSegments("e\u00a0")); // e + nbsp
		assertEquals(2, segmenter.computeSegments("e\u1680")); // e + Ogham space
		assertEquals(2, segmenter.computeSegments("\u0104 ")); // A-ogonek + space
		assertEquals(2, segmenter.computeSegments("\u0104\u00a0")); // A-ogonek + nbsp
		assertEquals(2, segmenter.computeSegments("\u0104\u1680")); // A-ogonek + Ogham space

		assertEquals(2, segmenter.computeSegments("1 ")); // 1 + space
		assertEquals(2, segmenter.computeSegments("\u0b66 ")); // Oryia zero + space
		assertEquals(2, segmenter.computeSegments("\uff19 ")); // Full-width 9 + space
		assertEquals(2, segmenter.computeSegments("1\u1680")); // 1 + Ogham space
		assertEquals(2, segmenter.computeSegments("\u0b66\u1680")); // Oryia zero + Ogham space
		assertEquals(2, segmenter.computeSegments("\uff19\u1680")); // Full-width 9 + Ogham space
		
		assertEquals(2, segmenter.computeSegments("\u301c\u1680")); // wave-dash + Ogham space

		assertEquals(2, segmenter.computeSegments("z\u0608")); // z + Arabic ray

		assertEquals(1, segmenter.computeSegments("\u20ac\u1680")); // Euro + Ogham space -> no break
	}
	
	private ISegmenter createSegmenterWithRules (LocaleId locId) {
		SRXDocument doc = new SRXDocument();
		LanguageMap langMap = new LanguageMap(".*", "default");
		doc.addLanguageMap(langMap);
		// Add the rules
		ArrayList<Rule> langRules = new ArrayList<Rule>();
		langRules.add(new Rule("\\.", "\\s", true));
		// Add the ruls to the document
		doc.addLanguageRule("default", langRules);
		// Create the segmenter
		return doc.compileLanguageRules(locId, null);
	}

	private ITextUnit createMultiTargetSegmentedTextUnit () {
		ISegmenter segmenter = createSegmenterWithRules(LocaleId.fromString("en"));
		// Create the source and segment it
		ITextUnit tu = new TextUnit("id1", "Part 1. Part 2. Part 3.");
		tu.createSourceSegmentation(segmenter);
		// Create the German target
		TextContainer tc1 = tu.setTarget(LocaleId.GERMAN, new TextContainer("DE_Part 1. DE_Part 2. DE_Part 3."));
		// Create same segmentation
		segmenter.computeSegments(tc1);
		tc1.getSegments().create(segmenter.getRanges());
		// Create the French target
		TextContainer tc2 = tu.setTarget(LocaleId.FRENCH, new TextContainer("FR_Part 1 and part 2. FR_Part 3."));
		// Create same segmentation
		segmenter.computeSegments(tc2);
		tc2.getSegments().create(segmenter.getRanges());
		// Create the source segmentation corresponding to the French segmentation
		ArrayList<Range> ranges = new ArrayList<Range>();
		ranges.add(new Range(0, 15, "0"));
		ranges.add(new Range(16, 23, "1"));
		tu.getVariantSources().create(LocaleId.FRENCH, true, IResource.COPY_ALL);
		tu.getVariantSources().get(LocaleId.FRENCH).getSegments().create(ranges);
		return tu;
	}

	private TextContainer createCodedText () {
		TextFragment tf = new TextFragment();
		tf.append("PH after.");
		tf.append(TagType.PLACEHOLDER, "break", "<br/>");
		tf.append(TagType.OPENING, "bold", "<b>");
		tf.append(" End after.");
		tf.append(TagType.CLOSING, "bold", "</b>");
		tf.append(" Start after.");
		tf.append(TagType.OPENING, "italics", "<i>");
		tf.append(" Text.");
		tf.append(TagType.CLOSING, "italics", "</i>");
		tf.append("  ");
		return new TextContainer(tf);
	}

	private TextContainer createCodedText2 () {
		TextFragment tf = new TextFragment();
		tf.append("PH after.");
		tf.append(TagType.PLACEHOLDER, "break", "<br/>");
		tf.append(TagType.PLACEHOLDER, "break", "<br/>");
		tf.append(TagType.OPENING, "bold", "<b>");
		tf.append(TagType.OPENING, "italics", "<i>");
		tf.append(" End after.");
		tf.append(TagType.CLOSING, "italics", "</i>");
		tf.append(TagType.CLOSING, "bold", "</b>");
		tf.append(" Start after.");
		tf.append(TagType.OPENING, "under", "<u>");
		tf.append(TagType.OPENING, "italics", "<i>");
		tf.append(" Text.");
		tf.append(TagType.CLOSING, "italics", "</i>");
		tf.append(TagType.CLOSING, "under", "</u>");
		tf.append("  ");
		return new TextContainer(tf);
	}
}
