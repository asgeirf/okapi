/*===========================================================================
Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.tmdb.lucene;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.lib.tmdb.DbUtil;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SeekerTest  {

	static final String locEN = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
    static final String locFR = DbUtil.toOlifantLocaleCode(LocaleId.FRENCH);
    static final String locES = DbUtil.toOlifantLocaleCode(LocaleId.SPANISH);
	
    Directory DIR;
    Writer writer;
    Seeker seeker;
    List<TmHit> tmhits;

    @Before
    public void setUp () throws IOException {
    	DIR = new RAMDirectory();
    	writer = new Writer(DIR, true);
        seeker = new Seeker(writer.getIndexWriter());
    }

    @After
    public void tearDown () {
    	writer.close();
        seeker.close();
    }

    @Test
    public void testShortEntries () throws Exception {
    	String tmId1 = "tmId1_";
    	String tmId2 = "tmId2_";
    	TmEntry entry = new TmEntry("1", tmId1, locEN, "Text EN 1", null);
    	entry.addVariant(new Variant(locFR, "efgh", null));
    	entry.addVariant(new Variant(locES, "ijkl", null));
        writer.index(entry);
        
    	entry = new TmEntry("2", tmId1, locEN, "Engineering & Testing", null);
    	entry.addVariant(new Variant(locFR, "def", null));
    	entry.addVariant(new Variant(locES, "ghi", null));
        writer.index(entry);
        
    	entry = new TmEntry("3", tmId2, locEN, "Text in EN", null);
    	entry.addVariant(new Variant(locFR, "bm", null));
    	entry.addVariant(new Variant(locES, "cm", null));
    	String attr1name = "attr1";
    	String attr1Value = "attr1ValueABC";
    	entry.setAttribute(attr1name, attr1Value);
        writer.index(entry);
        
    	entry = new TmEntry("4", tmId2, locEN, "zq", null);
    	entry.addVariant(new Variant(locFR, "zr", null));
    	entry.addVariant(new Variant(locES, "zs", null));
        writer.index(entry);
        
    	entry = new TmEntry("5", tmId1, locEN, "zqq", null);
    	entry.addVariant(new Variant(locFR, "zrr", null));
    	entry.addVariant(new Variant(locES, "zss", null));
        writer.index(entry);

        writer.commit();
        
        List<TmHit> list;
        
        list = seeker.searchFuzzy("Text EN 1", null, tmId1, locEN, 1, 80, null);
        assertEquals(1, list.size());        
        TmHit hit = list.get(0);
        assertEquals("tmId1_1", hit.getId());
        assertEquals("Text EN 1", hit.getVariant().getGenericTextField().stringValue());
        assertTrue(100.0==hit.getScore());

        list = seeker.searchFuzzy("Text EN 1", null, tmId2, locEN, 1, 80, null);
        assertEquals(0, list.size()); // Not in tmId2

        list = seeker.searchFuzzy("Engineering & Testing", null, tmId1, locEN, 1, 70, null);
        assertEquals(1, list.size());
        hit = list.get(0);
        assertEquals("tmId1_2", hit.getId());
        assertEquals("Engineering & Testing", hit.getVariant().getGenericTextField().stringValue());
        assertTrue(100.0==hit.getScore());

        // Match with attribute
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(attr1name, attr1Value);
        list = seeker.searchFuzzy("Text in EN", null, tmId2, locEN, 1, 70, attributes);
        assertEquals(1, list.size());
        hit = list.get(0);
        assertEquals("tmId2_3", hit.getId());
        assertEquals("Text in EN", hit.getVariant().getGenericTextField().stringValue());
        assertTrue(100.0==hit.getScore());

        // No match because the attribute does not match
        attributes.clear();
        attributes.put(attr1name, "some value");
        list = seeker.searchFuzzy("Text in EN", null, tmId2, locEN, 1, 70, attributes);
        assertEquals(0, list.size());

        
//        //include missing category
//        searchfields = new OFields();
//        searchfields.put("category", new OField("category", "second"));
//        list = seeker.searchFuzzy(new TextFragment("abcd"), 100, 1, searchfields, locEN);
//        assertEquals("number of docs found", 0, list.size());
//        
//        list = seeker.searchFuzzy(new TextFragment("abcd"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("efgh"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("ijkl"), 100, 1, null, locES);
//        assertEquals("number of docs found", 1, list.size());
//        
//        list = seeker.searchFuzzy(new TextFragment("abc"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("def"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("ghi"), 100, 1, null, locES);
//        assertEquals("number of docs found", 1, list.size());
//        
//        list = seeker.searchFuzzy(new TextFragment("zqq"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("zrr"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("zss"), 100, 1, null, locES);
//        assertEquals("number of docs found", 1, list.size());
//
//        list = seeker.searchFuzzy(new TextFragment("am"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("bm"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("cm"), 100, 1, null, locES);
//        assertEquals("number of docs found", 1, list.size());
//        
//        list = seeker.searchFuzzy(new TextFragment("zq"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("zr"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 1, list.size());
//        list = seeker.searchFuzzy(new TextFragment("zs"), 100, 1, null, locES);
//        assertEquals("number of docs found", 1, list.size());
    }

//    @Test
//    public void penaltyDifferentSpaces () throws Exception {
//        OWriter writer = getWriter();
//        OTranslationUnitInput inputTu = new OTranslationUnitInput("1");
//        
//        inputTu.add(new OTranslationUnitVariant(locEN, new TextFragment("abcdef")));
//        inputTu.add(new OTranslationUnitVariant(locFR, new TextFragment("ghijkl")));
//        inputTu.add(new OTranslationUnitVariant(locES, new TextFragment("mnopqr")));
//        writer.index(inputTu);
//        writer.close();
//
//        List<OTmHit> list;
//        list = seeker.searchFuzzy(new TextFragment("abCdef"), 100, 1, null, locEN);
//        assertEquals("number of docs found", 0, list.size());
//        list = seeker.searchFuzzy(new TextFragment("ghIjkl"), 100, 1, null, locFR);
//        assertEquals("number of docs found", 0, list.size());
//        list = seeker.searchFuzzy(new TextFragment("mnOpqr"), 100, 1, null, locES);
//        assertEquals("number of docs found", 0, list.size());
//    }


}
