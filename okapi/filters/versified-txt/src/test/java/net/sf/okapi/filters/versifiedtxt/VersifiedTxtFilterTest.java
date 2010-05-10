/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.versifiedtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.LocaleId;

import org.junit.Before;
import org.junit.Test;

public class VersifiedTxtFilterTest {
	private VersifiedTextFilter filter;
	private String root;
		
	@Before
	public void setUp() {
		filter = new VersifiedTextFilter();
		root = TestUtil.getParentDir(this.getClass(), "/part1.txt");
	}
	
	@Test
	public void testDefaultInfo () {		
		assertNotNull(filter.getName());
		assertNotNull(filter.getDisplayName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}
	
	@Test
	public void testSimpleVerse() {
		String snippet = "|v1\nThis is a test.";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("This is a test.\n", tu.getSource().toString());
		assertEquals("::1", tu.getName());
	}
	
	@Test
	public void testSimpleBookChapterVerse() {
		String snippet = "|bbook\n|cchapter\n|v1\nThis is a test.";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("This is a test.\n", tu.getSource().toString());
		assertEquals("book:chapter:1", tu.getName());
	}
	
	@Test
	public void testSimplePlaceholders() {
		String snippet = "|bbook\n|cchapter\n|v1\n{1}This is {2}a test{3}";
		TextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("{1}This is {2}a test{3}\n", tu.getSource().toString());
		assertEquals("book:chapter:1", tu.getName());
	}
	
	@Test
	public void testDoubleExtraction() throws URISyntaxException {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		list.add(new InputDocument(root+"part1.txt", null));		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "windows-1252", LocaleId.ENGLISH, LocaleId.ENGLISH));
	}
	
	@Test
	public void testOpenTwiceWithString() {
		RawDocument rawDoc = new RawDocument("|vtest", LocaleId.ENGLISH);
		filter.open(rawDoc);
		filter.open(rawDoc);
		filter.close();
	}
	
	private ArrayList<Event> getEvents(String snippet, IParameters params) {
		ArrayList<Event> list = new ArrayList<Event>();		
		filter.open(new RawDocument(snippet, LocaleId.ENGLISH));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}
}
