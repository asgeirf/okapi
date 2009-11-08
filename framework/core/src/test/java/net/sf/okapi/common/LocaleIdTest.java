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

package net.sf.okapi.common;

import java.util.Locale;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocaleIdTest {

	@Test
	public void testConstructorFromIdentifier () {
		LocaleId locId = new LocaleId("en-CA", true);
		assertEquals("en", locId.getLanguage());
		assertEquals("ca", locId.getRegion());
		
		locId = new LocaleId("EN_CA", true);
		assertEquals("en", locId.getLanguage());
		assertEquals("ca", locId.getRegion());
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromNullIdentifier () {
		new LocaleId((String)null, true);
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromEmptyIdentifier () {
		new LocaleId("", true);
	}
	
    @Test //(expected = IllegalArgumentException.class)
    public void testConstructorFromBadIdentifier () {
		// Try without normalization
		LocaleId locId = new LocaleId("EN_CA", false);
		// The bad result is expected
		assertEquals("EN_CA", locId.getLanguage());
		assertNull(null, locId.getRegion());
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromNullLanguage () {
		new LocaleId((String)null);
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromEmptyLanguage () {
		new LocaleId("");
	}
	
	@Test
	public void testConstructorFromLanguage () {
		LocaleId locId = new LocaleId("en");
		assertEquals("en", locId.getLanguage());
		assertNull(locId.getRegion());

		locId = new LocaleId("EN");
		assertEquals("en", locId.getLanguage());
		assertNull(locId.getRegion());
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromNullLanguageAndRegion () {
		new LocaleId((String)null, "CA");
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromEmptyLanguageAndRegion () {
		new LocaleId("", "CA");
	}
	
	@Test
	public void testConstructorFromLanguageAndRegion () {
		LocaleId locId = new LocaleId("de", "CH");
		assertEquals("de", locId.getLanguage());
		assertEquals("ch", locId.getRegion());

		locId = new LocaleId("DE", null);
		assertEquals("de", locId.getLanguage());
		assertNull(locId.getRegion());

		locId = new LocaleId("DE", "");
		assertEquals("de", locId.getLanguage());
		assertNull(locId.getRegion());
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromNullJavaLocale () {
		new LocaleId((Locale)null);
	}
	
	@Test
	public void testConstructorFromJavaLocale () {
		LocaleId locId = new LocaleId(Locale.CANADA_FRENCH);
		assertEquals("fr", locId.getLanguage());
		assertEquals("ca", locId.getRegion());

		locId = new LocaleId(Locale.SIMPLIFIED_CHINESE);
		assertEquals("zh", locId.getLanguage());
		assertEquals("cn", locId.getRegion());

		locId = new LocaleId(Locale.CHINESE);
		assertEquals("zh", locId.getLanguage());
		assertNull(locId.getRegion());

		locId = new LocaleId(Locale.TRADITIONAL_CHINESE);
		assertEquals("zh", locId.getLanguage());
		assertEquals("tw", locId.getRegion());

		// Java pre-defined th_TH_TH 
		locId = new LocaleId(new Locale("th", "TH", "TH"));
		assertEquals("th", locId.getLanguage());
		assertEquals("th", locId.getRegion());
	}		

	@Test
	public void testFromBCP () {
		LocaleId locId = LocaleId.fromBCP47("en-us");
		assertEquals("en", locId.getLanguage());
		assertEquals("us", locId.getRegion());
		
		locId = LocaleId.fromBCP47("kok");
		assertEquals("kok", locId.getLanguage());
		
		locId = LocaleId.fromBCP47("ar-Latn-EG");
		assertEquals("ar", locId.getLanguage());
		assertEquals("eg", locId.getRegion());
		
		locId = LocaleId.fromBCP47("az-latn");
		assertEquals("az", locId.getLanguage());
		assertNull(locId.getRegion());
		
		locId = LocaleId.fromBCP47("zh-Hant-TW");
		assertEquals("zh", locId.getLanguage());
		assertEquals("tw", locId.getRegion());

		locId = LocaleId.fromBCP47("zh-Latn-TW-pinyin");
		assertEquals("zh", locId.getLanguage());
		assertEquals("tw", locId.getRegion());
		
		locId = LocaleId.fromBCP47("es-419");
		assertEquals("es", locId.getLanguage());
		assertEquals("419", locId.getRegion());
		
		locId = LocaleId.fromBCP47("de-CH-1996");
		assertEquals("de", locId.getLanguage());
		assertEquals("ch", locId.getRegion());

		locId = LocaleId.fromBCP47("ja-Latn-hepburn");
		assertEquals("ja", locId.getLanguage());
	}

	@Test
	public void testFromPOSIX () {
		LocaleId locId = LocaleId.fromPOSIXLocale("zu");
		assertEquals("zu", locId.getLanguage());

		locId = LocaleId.fromPOSIXLocale("kok");
		assertEquals("kok", locId.getLanguage());
		
		locId = LocaleId.fromPOSIXLocale("de_AT");
		assertEquals("de", locId.getLanguage());
		assertEquals("at", locId.getRegion());
		
		locId = LocaleId.fromPOSIXLocale("de_AT.UTF-8");
		assertEquals("de", locId.getLanguage());
		assertEquals("at", locId.getRegion());

		locId = LocaleId.fromPOSIXLocale("de_AT.UTF-8@ATS");
		assertEquals("de", locId.getLanguage());
		assertEquals("at", locId.getRegion());

		locId = LocaleId.fromPOSIXLocale("sr@latin");
		assertEquals("sr", locId.getLanguage());
	}
	
	@Test
	public void testToPOSIX () {
		LocaleId locId = LocaleId.fromPOSIXLocale("en_US.UTF-8");
		String res = locId.toPOSIXLocaleId();
		assertNotNull(res);
		assertEquals("en_US", res);

		locId = LocaleId.fromPOSIXLocale("DE");
		res = locId.toPOSIXLocaleId();
		assertNotNull(res);
		assertEquals("de", res);

//		locId = LocaleID.fromPOSIXLocale("ca@valencia");
//		res = LocaleID.toPOSIXLocale(locId);
//		assertNotNull(res);
//		assertEquals("ca@valencia", res);
	}
	
	@Test
	public void testToJavaLocale () {
		LocaleId locId = new LocaleId(Locale.CANADA_FRENCH);
		Locale loc = locId.toJavaLocale();
		assertNotNull(loc);
		assertEquals(Locale.CANADA_FRENCH.toString(), loc.toString());

		Locale jloc = new Locale("th", "TH", "TH");
		locId = new LocaleId(jloc);
		loc = locId.toJavaLocale();
		assertNotNull(loc);
		assertEquals(jloc.toString(), loc.toString());

		locId = new LocaleId(Locale.CHINESE);
		loc = locId.toJavaLocale();
		assertNotNull(loc);
		assertEquals(Locale.CHINESE.toString(), loc.toString());
	}

	@Test
	public void testEqualsWithLocaleId () {
		LocaleId locId1 = new LocaleId("fi-fi", false);
		LocaleId locId2 = new LocaleId("fi-se", false);
		assertFalse(locId1.equals(locId2));

		locId1 = new LocaleId("kok-abc", false);
		locId2 = new LocaleId("KOK_aBc", true);
		assertTrue(locId1.equals(locId2));

		locId1 = new LocaleId("br");
		locId2 = new LocaleId("br");
		assertTrue(locId1.equals(locId2));
	}

	@Test
	public void testEqualsWithString () {
		LocaleId locId1 = new LocaleId("fi-fi", false);
		assertFalse(locId1.equals("fi-se"));

		locId1 = new LocaleId("kok-abc", false);
		assertTrue(locId1.equals("KOK-aBc"));

		locId1 = new LocaleId("br");
		assertTrue(locId1.equals("BR"));
	}

	@Test
	public void testUsage () {
		assertEquals("Austria", new LocaleId("de-at", false).toJavaLocale().getDisplayCountry(Locale.ENGLISH));
		assertEquals("French", new LocaleId("fr-ca", false).toJavaLocale().getDisplayLanguage(Locale.ENGLISH));
	}

	@Test
	public void testSameLanguageWithLocaleId () {
		LocaleId locId1 = new LocaleId("fi-fi", false);
		LocaleId locId2 = new LocaleId("fi-se", false);
		assertTrue(locId1.sameLanguageAs(locId2));

		locId1 = new LocaleId("kok", false);
		locId2 = new LocaleId("KOK_id", true);
		assertTrue(locId1.sameLanguageAs(locId2));

		locId1 = new LocaleId("br");
		locId2 = new LocaleId("br");
		assertTrue(locId1.sameLanguageAs(locId2));
	}

	@Test
	public void testSameLanguageWithString () {
		LocaleId locId = new LocaleId("fi-fi", false);
		assertTrue(locId.sameLanguageAs("fi-se"));

		locId = new LocaleId("kok", false);
		assertTrue(locId.sameLanguageAs("KoK_id"));

		locId = new LocaleId("br");
		assertTrue(locId.sameLanguageAs("br"));
	}

	@Test
	public void testDifferentLanguages () {
		LocaleId locId1 = new LocaleId("fi-fi", false);
		LocaleId locId2 = new LocaleId("sv-fi", true);
		assertFalse(locId1.sameLanguageAs(locId2));

		locId1 = new LocaleId("nn", false);
		assertFalse(locId1.sameLanguageAs("no"));
	}
	
	@Test
	public void testSplitLanguageCode () {
		String in = "en";
		String[] res = LocaleId.splitLanguageCode(in);
		assertEquals(res[0], "en");
		assertEquals(res[1], "");
	}

	@Test
	public void testSplitLanguageCode_4Letters () {
		String in = "en-BZ";
		String[] res = LocaleId.splitLanguageCode(in);
		assertEquals(res[0], "en");
		assertEquals(res[1], "BZ");
	}

	@Test
	public void testSplitLanguageCode_Underline () {
		String in = "en_BZ";
		String[] res = LocaleId.splitLanguageCode(in);
		assertEquals(res[0], "en");
		assertEquals(res[1], "BZ");
	}

}