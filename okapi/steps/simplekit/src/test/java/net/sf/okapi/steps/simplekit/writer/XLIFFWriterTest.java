/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.simplekit.writer;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;

import org.junit.Before;
import org.junit.Test;

public class XLIFFWriterTest {
	
	private XLIFFWriter writer;
	private String root;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		writer = new XLIFFWriter();
		root = TestUtil.getParentDir(this.getClass(), "/test01.properties");
	}

	@Test
	public void testMinimal ()
		throws IOException
	{
		writer.create(root+"out.xlf", null, locEN, null, null, "original.ext", null);
		writer.writeStartFile(null, null, null);
		writer.writeEndFile();
		writer.close();
		
		String result = readFile(root+"out.xlf");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
			+ "<file original=\"unknown\" source-language=\"en\" datatype=\"x-undefined\">\n"
			+ "<body>\n</body>\n</file>\n</xliff>\n", result);
	}

	@Test
	public void testBasicSourceOnly ()
		throws IOException
	{
		writer.create(root+"out.xlf", null, locEN, null, null, "original.ext", null);
		TextUnit tu = new TextUnit("tu1", "src1");
		writer.writeTextUnit(tu);
		writer.close();

		String result = readFile(root+"out.xlf");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
			+ "<file original=\"original.ext\" source-language=\"en\" datatype=\"x-undefined\">\n"
			+ "<body>\n"
			+ "<trans-unit id=\"tu1\">\n"
			+ "<source xml:lang=\"en\">src1</source>\n"
			+ "</trans-unit>\n"
			+ "</body>\n</file>\n</xliff>\n", result);
	}

	@Test
	public void testBasicSourceAndTarget ()
		throws IOException
	{
		writer.create(root+"out.xlf", null, locEN, locFR, null, "original.ext", null);
		TextUnit tu = new TextUnit("tu1", "src1<&\"\'>");
		tu.setTarget(locFR, new TextContainer("trg1"));
		writer.writeTextUnit(tu);
		writer.close();

		String result = readFile(root+"out.xlf");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
			+ "<file original=\"original.ext\" source-language=\"en\" target-language=\"fr\" datatype=\"x-undefined\">\n"
			+ "<body>\n"
			+ "<trans-unit id=\"tu1\">\n"
			+ "<source xml:lang=\"en\">src1&lt;&amp;\"\'></source>\n"
			+ "<target xml:lang=\"fr\">trg1</target>\n"
			+ "</trans-unit>\n"
			+ "</body>\n</file>\n</xliff>\n", result);
	}

	@Test
	public void testCompleteSourceAndTarget ()
		throws IOException
	{
		writer.create(root+"out.xlf", "skel.skl", locEN, locFR, "dtValue", "original.ext", "messageValue");
		TextUnit tu = new TextUnit("tu1", "src1<&\"\'>");
		tu.setTarget(locFR, new TextContainer("trg1"));
		writer.writeTextUnit(tu);
		writer.close();

		String result = readFile(root+"out.xlf");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
			+ "<!--messageValue-->\n"
			+ "<file original=\"original.ext\" source-language=\"en\" target-language=\"fr\" datatype=\"x-dtValue\">\n"
			+ "<header><skl><external-file href=\"skel.skl\"></external-file></skl></header>\n"
			+ "<body>\n"
			+ "<trans-unit id=\"tu1\">\n"
			+ "<source xml:lang=\"en\">src1&lt;&amp;\"\'></source>\n"
			+ "<target xml:lang=\"fr\">trg1</target>\n"
			+ "</trans-unit>\n"
			+ "</body>\n</file>\n</xliff>\n", result);
	}

	private String readFile (String path)
		throws IOException
	{
		byte[] buffer = new byte[1024];
		int count = 0;
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
		StringBuilder sb = new StringBuilder();
		while ((count = bis.read(buffer)) != -1) {
			sb.append(new String(buffer, 0, count));
		}
		String tmp = sb.toString().replace("\r\n", "\n");
		tmp = tmp.replace("\r", "\n");
		return tmp;
	}

}
