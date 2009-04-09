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

package net.sf.okapi.filters.po.tests;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.InputResource;
import net.sf.okapi.filters.po.POFilter;
import net.sf.okapi.filters.tests.FilterTestDriver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class POFilterTest {
	
	private POFilter filter;
	
	@Before
	public void setUp() {
		filter = new POFilter();
	}

	@Test
	public void optionLineTest () {
		String snippet = "#, c-format\n"
			+ "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		String result = FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr");
		assertEquals(result, snippet);
		
		snippet = "#, c-format, fuzz\n"
			+ "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		result = FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr");
		assertEquals(result, snippet);
		
		snippet = "#, fuzzy, c-format\n"
			+ "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		result = FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr");
		assertEquals(result, snippet);

		snippet = "#, x-stuff, fuzzy, c-format\n"
			+ "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		result = FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr");
		assertEquals(result, snippet);
	}
	
	@Test
	public void simpleTest () {
		String snippet = "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		String expect = "msgid \"Text 1\"\n"
			+ "msgstr \"Texte 1\"\n";
		assertEquals(expect, FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr"));
	}
	
	@Test
	public void noTransTest () {
		String snippet = "msgid \"Text 1\"\n"
			+ "msgstr \"\"\n";
		String expect = "msgid \"Text 1\"\n"
			+ "msgstr \"Text 1\"\n";
		assertEquals(expect, FilterTestDriver.generateOutput(getEvents(snippet, "en", "fr"), snippet, "fr"));
	}
	
	@Test
	public void externalFileTest () {
		POFilter filter = null;		
		try {
			FilterTestDriver testDriver = new FilterTestDriver();
			//testDriver.setShowSkeleton(true);
			//testDriver.setDisplayLevel(3);
			filter = new POFilter();
			InputStream input = POFilterTest.class.getResourceAsStream("/Test01.po");
			filter.open(new InputResource(input, "UTF-8", "en", "fr"));
			if ( !testDriver.process(filter) ) Assert.fail();
			filter.close();
		}
		catch ( Throwable e ) {
			e.printStackTrace();
			Assert.fail("Exception occured");
		}
		finally {
			if ( filter != null ) filter.close();
		}
	}
	
	private ArrayList<Event> getEvents(String snippet,
		String srcLang,
		String trgLang)
	{
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new InputResource(snippet, srcLang, trgLang));
		while ( filter.hasNext() ) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

	
}
