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

package net.sf.okapi.lib.segmentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.lib.segmentation.LanguageMap;
import net.sf.okapi.lib.segmentation.Rule;
import net.sf.okapi.lib.segmentation.SRXDocument;
import net.sf.okapi.lib.segmentation.SRXSegmenter;

public class SRXDocumentTest {

	@Before
	public void setUp() {
	}

	@Test
	public void testGetSet () {
		SRXDocument doc = new SRXDocument();
		
		// Check defaults
		assertFalse(doc.cascade());
		assertFalse(doc.includeStartCodes());
		assertTrue(doc.includeEndCodes());
		assertFalse(doc.includeIsolatedCodes());
		assertTrue(doc.segmentSubFlows());
		assertFalse(doc.oneSegmentIncludesAll());
		assertFalse(doc.trimLeadingWhitespaces());
		assertFalse(doc.trimTrailingWhitespaces());
		
		// Check changing options
		doc.setCascade(true);
		assertTrue(doc.cascade());
		doc.setIncludeStartCodes(true);
		assertTrue(doc.includeStartCodes());
		doc.setIncludeEndCodes(false);
		assertFalse(doc.includeEndCodes());
		doc.setIncludeIsolatedCodes(true);
		assertTrue(doc.includeIsolatedCodes());
		doc.setSegmentSubFlows(false);
		assertFalse(doc.segmentSubFlows());
		doc.setOneSegmentIncludesAll(true);
		assertTrue(doc.oneSegmentIncludesAll());
		doc.setTrimLeadingWhitespaces(true);
		assertTrue(doc.trimLeadingWhitespaces());
		doc.setTrimTrailingWhitespaces(true);
		assertTrue(doc.trimTrailingWhitespaces());
		
	}

	@Test
	public void testObjects () {
		// Check rule
		Rule rule = new Rule();
		assertEquals(rule.getBefore(), "");
		assertEquals(rule.getAfter(), "");
		assertTrue(rule.isBreak());
		assertTrue(rule.isActive());
		String tmp = "regex";
		rule.setAfter(tmp);
		assertEquals(rule.getAfter(), tmp);
		rule.setBefore(tmp);
		assertEquals(rule.getBefore(), tmp);
		rule.setBreak(false);
		assertFalse(rule.isBreak());
		rule.setActive(false);
		assertFalse(rule.isActive());
		
		// Check LanguageMap
		String pattern = "pattern";
		String ruleName = "ruleName";
		LanguageMap lm = new LanguageMap(pattern, ruleName);
		assertEquals(lm.getPattern(), pattern);
		assertEquals(lm.getRuleName(), ruleName);
	}
	
	@Test
	public void testRules () {
		SRXDocument doc = new SRXDocument();
		doc.setCascade(true);

		ArrayList<Rule> list = new ArrayList<Rule>();
		list.add(new Rule("Mr\\.", "\\s", false));
		doc.addLanguageRule("english", list);
		ArrayList<Rule> rules = doc.getLanguageRules("english");
		assertEquals(rules.size(), 1);
		
		list = new ArrayList<Rule>();
		list.add(new Rule("\\.+", "\\s", true));
		doc.addLanguageRule("default", list);

		doc.addLanguageMap(new LanguageMap("en.*", "english"));
		doc.addLanguageMap(new LanguageMap(".*", "default"));

		SRXSegmenter seg = (SRXSegmenter)doc.compileLanguageRules(
			LocaleId.fromString("en"), null);
		assertNotNull(seg);
		assertEquals(seg.getLanguage(), "en");
		assertNull(seg.getRanges()); // Null set yet
		seg.computeSegments("Mr. Holmes. The detective.");
		assertNotNull(seg.getRanges());
		assertEquals(seg.getRanges().size(), 2);
		seg.computeSegments("MR. Holmes. The detective.");
		assertEquals(seg.getRanges().size(), 3);
		
		TextFragment tf = new TextFragment("One.");
		tf.append(TagType.OPENING, "b", "<b>");
		tf.append(" Two.");
		tf.append(TagType.CLOSING, "b", "</b>");
		TextContainer tc = new TextContainer(tf);
		seg.setOptions(true, true, true, false, false, false, false, true);
		seg.computeSegments(tc);
		// "One.XX Two.YY" --> "[One.XX][ Two.YY]"
		List<Range> ranges = seg.getRanges();
		assertNotNull(ranges);
		assertEquals(ranges.size(), 2);
		assertEquals(ranges.get(0).end, 6);
		assertEquals(ranges.get(1).start, 6);
		seg.setOptions(true, false, true, false, false, false, false, true);
		seg.computeSegments(tc);
		// "One.XX Two.YY" --> "[One.][XX Two.YY]"
		ranges = seg.getRanges();
		assertNotNull(ranges);
		assertEquals(ranges.size(), 2);
		assertEquals(ranges.get(0).end, 4);
		assertEquals(ranges.get(1).start, 4);
	}

	@Test
	public void testComments () {
		SRXDocument doc = createDocument();
		assertNotNull(doc.getComments());
		assertEquals("Main comment", doc.getComments());
		assertNotNull(doc.getHeaderComments());
		assertEquals("Header comment", doc.getHeaderComments());
		Map<String, ArrayList<Rule>> list = doc.getAllLanguageRules();
		assertNotNull(list);
		ArrayList<Rule> rules = list.get("default");
		assertNotNull(rules);
		assertEquals(1, rules.size());
		assertNotNull(rules.get(0).getComment());
		assertEquals("Rule comment", rules.get(0).getComment());
	}
	
	@Test
	public void testSimpleRule () {
		SRXDocument doc = createDocument();
		Map<String, ArrayList<Rule>> list = doc.getAllLanguageRules();
		assertNotNull(list);
		ArrayList<Rule> rules = list.get("default");
		assertNotNull(rules);
		assertEquals(1, rules.size());
		assertEquals("([A-Z]\\.){2,}", rules.get(0).getBefore());
		assertEquals("\\s", rules.get(0).getAfter());
		assertFalse(rules.get(0).isBreak());
	}
	
	@Test
	public void testLoadRulesFromStream () {
		SRXDocument doc = createDocument();
		// Use the SRX in the package tree
		doc.loadRules(SRXDocumentTest.class.getResourceAsStream("Test02.srx"));
		Map<String, ArrayList<Rule>> list = doc.getAllLanguageRules();
		assertNotNull(list);
		ArrayList<Rule> rules = list.get("default");
		assertNotNull(rules);
		assertEquals(2, rules.size());
	}
	
	@Test
	public void testLoadRulesFromPath () {
		SRXDocument doc = createDocument();
		// Use the Test01.srx at the root level (not in the package tree)
		String root = TestUtil.getParentDir(getClass(), "/Test01.srx");
		doc.loadRules(root+"Test01.srx");
		Map<String, ArrayList<Rule>> list = doc.getAllLanguageRules();
		assertNotNull(list);
		ArrayList<Rule> rules = list.get("default");
		assertNotNull(rules);
		assertEquals(2, rules.size());
	}
	
	private SRXDocument createDocument () {
		SRXDocument doc = new SRXDocument();
		String srx = "<!--Main comment-->"
			+ "<srx xmlns='http://www.lisa.org/srx20' version='2.0'>"
			+ "<!--Header comment-->"
			+ "<header segmentsubflows='yes' cascade='no'>"
			+ "<formathandle type='start' include='no'/>"
			+ "<formathandle type='end' include='yes'/>"
			+ "<formathandle type='isolated' include='no'/>"
			+ "</header>"
			+ "<body>"
			+ "<languagerules>"
			+ "<languagerule languagerulename='default'>"
			+ "<!--Rule comment-->"
			+ "<rule break='no'>"
			+ "<beforebreak>([A-Z]\\.){2,}</beforebreak>"
			+ "<afterbreak>\\s</afterbreak>"
			+ "</rule>"
			+ "</languagerule>"
			+ "</languagerules>"
			+ "<maprules>"
			+ "<languagemap languagepattern='.*' languagerulename='default'/>"
			+ "</maprules>"
			+ "</body></srx>";
		doc.loadRules((CharSequence)srx);
		return doc;
	}
}
