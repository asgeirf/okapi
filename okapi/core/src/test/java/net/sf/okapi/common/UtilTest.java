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

package net.sf.okapi.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

public class UtilTest {
	
	private CharsetEncoder chsEnc;
	private DocumentBuilderFactory docBuilderFact;
	
	@Before
	public void setUp() throws Exception {
		docBuilderFact = DocumentBuilderFactory.newInstance();
		docBuilderFact.setValidating(false);
		chsEnc = Charset.forName("iso-8859-1").newEncoder();
	}

	@After
	public void tearDown() {
	}

    @Test
    public void isEmptyNull(){
        assertTrue("null should be empty", Util.isEmpty((String)null));
    }

    @Test
    public void isEmptyEmpty(){
        assertTrue("Empty should be empty", Util.isEmpty(""));
    }

    @Test
    public void isEmptyNotEmpty(){
        assertFalse("Not empty should be not empty", Util.isEmpty("not"));
    }

    @Test
    public void isEmptyNotEmptyWhitespace(){
        assertFalse("Space should not be empty", Util.isEmpty(" "));
    }

    @Test
    public void isEmptyIgnoreWSNotEmptyWhitespace(){
        assertTrue("Space should be empty", Util.isEmpty(" ", true));
    }

    @Test
    public void isEmptyIgnoreWSEmpty(){
        assertTrue("Empty should be empty", Util.isEmpty("", true));
    }

    @Test
    public void isEmptyIgnoreWSNull(){
        assertTrue("Null should be empty", Util.isEmpty(null, true));
    }

    @Test
    public void isEmptyIgnoreWSNotEmpty(){
        assertFalse("Not empty should be not empty", Util.isEmpty("s", true));
    }

    @Test
    public void isEmptyIgnoreWSNotEmptyImmutable(){
        String tmp = "s ";
        Util.isEmpty(tmp, true);
        assertEquals("tmp after method call", "s ", tmp);
    }

	@Test
	public void testTrimStart () {
		assertEquals("textz \t ", Util.trimStart(" \t ztextz \t ", " \tz"));
		assertEquals("", Util.trimStart(" \t ", " \tz"));
		assertEquals(null, Util.trimStart(null, " \tz"));
		assertEquals("", Util.trimStart("", " \tz"));
	}

	@Test
	public void testTrimEnd () {
		assertEquals(" \t ztext", Util.trimEnd(" \t ztextz \t ", " \tz"));
		assertEquals("", Util.trimEnd(" \t ", " \tz"));
		assertEquals(null, Util.trimEnd(null, " \tz"));
		assertEquals("", Util.trimEnd("", " \tz"));
	}

	@Test
	public void testGetDirectoryName_BSlash () {
		String in = "C:\\test\\file";
		assertEquals("C:\\test", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_MixedCases () {
		String in = "/home/test\\file";
		assertEquals("/home/test", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_Slash () {
		String in = "/home/test/file";
		assertEquals("/home/test", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_DirBSlash () {
		String in = "C:\\test\\";
		assertEquals("C:\\test", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_DirSlash () {
		String in = "/home/test/";
		assertEquals("/home/test", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_Filename () {
		String in = "myFile.ext";
		assertEquals("", Util.getDirectoryName(in));
	}

	@Test
	public void testGetDirectoryName_URL () {
		String in = "/C:/test/file.ext";
		assertEquals("/C:/test", Util.getDirectoryName(in));
	}

	@Test
	public void testEscapeToXML_Quote0 () {
		String in = "&<>'\"";
		assertEquals("&amp;&lt;>'\"", Util.escapeToXML(in, 0, false, null));
	}

	@Test
	public void testEscapeToXML_Quote1 () {
		String in = "&<>'\"";
		assertEquals("&amp;&lt;>&apos;&quot;", Util.escapeToXML(in, 1, false, null));
	}

	@Test
	public void testEscapeToXML_Quote2 () {
		String in = "&<>'\"";
		assertEquals("&amp;&lt;>&#39;&quot;", Util.escapeToXML(in, 2, false, null));
	}

	@Test
	public void testEscapeToXML_Quote3 () {
		String in = "&<>'\"";
		assertEquals("&amp;&lt;>'&quot;", Util.escapeToXML(in, 3, false, null));
	}

	@Test
	public void testEscapeToXML_GT () {
		String in = "&<>'\"";
		assertEquals("&amp;&lt;&gt;'&quot;", Util.escapeToXML(in, 3, true, null));
	}

	@Test
	public void testEscapeToXML_ExtCharsWithNull () {
		String in = "\u00d0\u0440Z\uD840\uDC00";
		assertEquals("\u00d0\u0440Z\uD840\uDC00", Util.escapeToXML(in, 0, false, null));
	}

	@Test
	public void testEscapeToXML_ExtCharsWithLatin1 () {
		String in = "\u00d0\u0440Z\uD840\uDC00";
		assertEquals("\u00d0&#x0440;Z&#x20000;", Util.escapeToXML(in, 0, false, chsEnc));
	}

	@Test
	public void testGetExtension () {
		String in = "myFile.abc.ext";
		assertEquals(".ext", Util.getExtension(in));
	}

	@Test
	public void testGetExtension_Alone () {
		String in = ".ext";
		assertEquals(".ext", Util.getExtension(in));
	}

	@Test
	public void testGetExtension_None () {
		String in = "myFile";
		assertEquals("", Util.getExtension(in));
	}

	@Test
	public void testGetExtension_Dot () {
		String in = "myFile.";
		assertEquals(".", Util.getExtension(in));
	}
	
	@Test
	public void testGetPercentage () {
		assertEquals(45, Util.getPercentage(450, 1000));
	}

	@Test
	public void testGetPercentage_WithZero () {
		assertEquals(1, Util.getPercentage(10, 0));
	}

	@Test
	public void testIsSameLanguage_DoNotIgnoreRegion () {
		assertTrue(Util.isSameLanguage("en", "en", false));
		assertTrue(Util.isSameLanguage("en", "EN", false));
		assertTrue(Util.isSameLanguage("En", "eN", false));
		assertFalse(Util.isSameLanguage("en", "fr", false));
		assertFalse(Util.isSameLanguage("en", "en-us", false));
		assertFalse(Util.isSameLanguage("en-us", "en", false));
		assertFalse(Util.isSameLanguage("abc-xyz", "abc-QWE", false));
		assertFalse(Util.isSameLanguage("abc-xyz", "iop-QWE", false));
	}

	@Test
	public void testIsSameLanguage_IgnoreRegion () {
		assertTrue(Util.isSameLanguage("en", "en", true));
		assertTrue(Util.isSameLanguage("en", "EN", true));
		assertTrue(Util.isSameLanguage("En", "eN", true));
		assertFalse(Util.isSameLanguage("en", "fr", true));
		assertTrue(Util.isSameLanguage("en", "en-us", true));
		assertTrue(Util.isSameLanguage("en-us", "en", true));
		assertTrue(Util.isSameLanguage("abc-xyz", "abc-QWE", true));
		assertFalse(Util.isSameLanguage("abc-xyz", "iop-QWE", true));
	}

	@Test
	public void testGetTextContent_Simple () {
		Document doc = createXMLdocument("<d>\n\nText</d>");
		Element elem = doc.getDocumentElement();
		assertEquals("\n\nText", Util.getTextContent(elem));
	}
	
	@Test
	public void testGetTextContent_WithComment () {
		Document doc = createXMLdocument("<d><!--comment-->Text</d>");
		Element elem = doc.getDocumentElement();
		assertEquals("Text", Util.getTextContent(elem));
	}
	
	@Test
	public void testGetTextContent_Empty () {
		Document doc = createXMLdocument("<d/>");
		Element elem = doc.getDocumentElement();
		assertEquals("", Util.getTextContent(elem));
	}
	
	@Test
	public void testMin () {
		assertEquals(-10, Util.min(10, 20, 30, -10, 0, 5));		
		assertEquals(-100, Util.min(-99, -98, -100, 1000));
		assertEquals(10, Util.min(10, 20, 30, 40, 15));
		assertEquals(0, Util.min());
	}
	
	@Test
	public void testToURI () {
		assertEquals("/C:/test", Util.toURI("C:\\test").getPath());		
		assertEquals("/C:/test", Util.toURI("file:///C:/test").getPath());		
		assertEquals("/C:/test", Util.toURI("/C:/test").getPath());
		assertEquals("/C:/test", Util.toURI("file:/C:/test").getPath()); 
	}
	
	@Test
	public void testEmptyURI() throws URISyntaxException {
		// IllegalArgumentException expected
		try{
			assertEquals("", Util.toURI(null).toString());
		}
		catch (IllegalArgumentException e) {
		}
		try{
			assertEquals("", Util.toURI(null).getPath());
		}
		catch (IllegalArgumentException e) {
		}
		try{
			assertEquals("", Util.makeURIFromPath(null));
		}
		catch (IllegalArgumentException e) {
		}
		try{
			assertEquals("", Util.toURI("").toString());
		}
		catch (IllegalArgumentException e) {
		}
		try{
			assertEquals("", Util.toURI("").getPath());
		}
		catch (IllegalArgumentException e) {
		}
		try{
			assertEquals("", Util.makeURIFromPath(""));
		}
		catch (IllegalArgumentException e) {
			return;
		}
		fail();		
	}	

	@Test
	public void testEnsureSeparator() {
		assertEquals(null, Util.ensureSeparator(null, false));
		assertEquals("", Util.ensureSeparator("", false));
		assertEquals("/C:/test/", Util.ensureSeparator("/C:/test/", false));
		assertEquals("/C:/test" + File.separator, Util.ensureSeparator("/C:/test" + File.separator, false));
		assertEquals("/C:/test/", Util.ensureSeparator("/C:/test" + File.separator, true));
		assertEquals("/C:/test" + File.separator, Util.ensureSeparator("/C:/test", false));
		assertEquals("/C:/test/", Util.ensureSeparator("/C:/test", true));
	}
	
	@Test
	public void testFixFilename() {
		assertEquals("", Util.fixFilename(null));
		assertEquals("", Util.fixFilename(null, null));
		assertEquals("abs:def", Util.fixFilename("abs:def", null));
		assertEquals("a_bc_de_fgh_ijk{l};mn_op_qr (s) t_uvw!x_y[z]", Util.fixFilename("a*bc:de<fgh>ijk{l};mn?op\\qr (s) t|uvw!x/y[z]"));
	}
	
	@Test
	public void testFixPath() {
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1////dir2/dir3/dir4/filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1/dir2/////dir3/dir4/filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1/dir2\\/dir3/dir4/filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1/dir2//\\\\\\\\\\dir3/dir4/filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1/dir2/dir3/dir4/\\filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1//dir2/\\//\\\\dir3/dir4/filename.ext"));
		assertEquals("/C:/dir1/dir2/dir3/dir4/filename.ext", Util.fixPath("/C:/dir1//dir2\\/\\\\/\\//\\\\dir3/dir4/filename.ext"));
	}
	
// Unused
//	@Test
//	public void generateRandomId() {
//		String one = Util.generateRandomId(5);
//		String two = Util.generateRandomId(10);
//		assertTrue(one.length() == 5);
//		assertTrue(two.length() == 10);
//		
//		one = Util.generateRandomId(5);
//		two = Util.generateRandomId(5);
//		assertFalse(one.equals(two));
//	}
	
	@Test
	public void testMapGetValue() {
		Map<String, Long> map = new HashMap<String, Long>();
		map.put("one", 1L);
		map.put("two", 2L);
		assertEquals((Long) 1L, Util.getValue(map, "one", 0L));
		assertEquals((Long) 2L, Util.getValue(map, "two", 0L));
		assertEquals((Long) 0L, Util.getValue(map, "three", 0L));
		assertEquals((Long) 5L, Util.getValue(map, "three", 5L));
	}
	
	@Test
	public void testNormalizRange() {
		int nv = Util.normalizeRange(0, 100, 50);
		assertEquals(50, nv);
		
		nv = Util.normalizeRange(-5, 5, 0);
		assertEquals(50, nv);
		
		nv = Util.normalizeRange(-5, 5, 5);
		assertEquals(100, nv);
		
		nv = Util.normalizeRange(-5, 5, -5);
		assertEquals(0, nv);
	}
	
	private Document createXMLdocument (String data) {
		InputSource input = new InputSource(new StringReader(data));
		Document doc = null;
		try {
			doc = docBuilderFact.newDocumentBuilder().parse(input);
		}
		catch ( SAXException e ) {
			e.printStackTrace();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( ParserConfigurationException e ) {
			e.printStackTrace();
		}
		assertNotNull(doc);
		return doc;
	}

}
