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

package net.sf.okapi.lib.translation;

import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryUtilTest {

	private QueryUtil qu;
	private GenericContent fmt;
	
	@Before
	public void setUp() {
		qu = new QueryUtil();
		fmt = new GenericContent();
	}

	@Test
	public void testEmptyFragment () {
		TextFragment tf = new TextFragment();
		assertEquals("", qu.separateCodesFromText(tf));
		assertEquals("", qu.createNewFragmentWithCodes("").toString());
	}
	
	@Test
	public void testFragmentWithoutCodes () {
		TextFragment tf = new TextFragment("text");
		assertEquals("text", qu.separateCodesFromText(tf));
		assertEquals("new", qu.createNewFragmentWithCodes("new").toString());
	}
	
	@Test
	public void testFragmentWithCodes () {
		TextFragment tf = makeFragment();
		assertEquals("a & < > \" \' <b>bold</b> t <br/> z", tf.toString());
		assertEquals("a & < > \" \' bold t  z", qu.separateCodesFromText(tf));
		assertEquals("new<b></b><br/>", qu.createNewFragmentWithCodes("new").toString());
	}
	
	@Test
	public void testToHTML () {
		TextFragment tf = makeFragment();
		String htmlText = qu.toCodedHTML(tf);
		assertEquals("a &amp; &lt; > \" \' <s id='1'>bold</s> t <br id='2'/> z", htmlText);
	}
	
	@Test
	public void testFromSameHTML () {
		TextFragment tf = makeFragment();
		String htmlText = qu.toCodedHTML(tf);
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertTrue(resFrag.compareTo(tf, false) == 0);
		assertTrue(resFrag.compareTo(tf, true) == 0);
	}
	
	@Test
	public void testFromModifiedHTML () {
		TextFragment tf = makeFragment();
		String htmlText = qu.toCodedHTML(tf);
		String codedText = qu.fromCodedHTML(htmlText, tf);
		codedText = codedText.toUpperCase();
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("A & < > \" \' <b>BOLD</b> T <br/> Z", resFrag.toString());
	}
	
	@Test
	public void testSimpleHTMLWithCorrection () {
		TextFragment tf = makeFragment();
		String htmlText = qu.toCodedHTML(tf);
		assertEquals("a &amp; &lt; > \" \' <s id='1'>bold</s> t <br id='2'/> z", htmlText);
		// Send something with missing codes (faking translation results)
		htmlText = "a <s id='1'>b</s> c";
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("a <1>b</1> c<2/>", fmt.setContent(resFrag).toString());
		assertEquals("a <b>b</b> c<br/>", resFrag.toString());
	}
	
	@Test
	public void testFromSameHTMLComplex () {
		TextFragment tf = makeComplexFragment();
		String htmlText = qu.toCodedHTML(tf);
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("t1<1><2>bs1</2></1>t2<3>b1</3>", fmt.setContent(resFrag).toString());
	}
	
	@Test
	public void testFromHTMLComplexWithMovedCodes () {
		TextFragment tf = makeComplexFragment();
		String htmlText = qu.toCodedHTML(tf);
		// What we send:
		assertEquals("t1<s id='1'><s id='2'>bs1</s></s>t2<s id='3'>b1</s>", htmlText);
		// What we get back from the translation:
		htmlText = "t1<s id='2'><s id='3'><s id='1'>t2</s></s>t3</s>";
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("t1<2><3><1>t2</1></3>t3</2>", fmt.setContent(resFrag).toString());
		assertEquals("t1<s><b><b>t2</b></b>t3</s>", resFrag.toString());
	}
	
	@Test
	public void testFromHTMLComplexWithMovedCodesWithCorrection () {
		TextFragment tf = makeComplexFragment();
		String htmlText = qu.toCodedHTML(tf);
		// What we send:
		assertEquals("t1<s id='1'><s id='2'>bs1</s></s>t2<s id='3'>b1</s>", htmlText);
		// What we get back from the translation:
		htmlText = "t1<s id='2'><s id='1'>t2</s>t3</s>";
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("t1<2><1>t2</1>t3</2><3></3>", fmt.setContent(resFrag).toString());
		assertEquals("t1<s><b>t2</b>t3</s><b></b>", resFrag.toString());
	}
	
	@Test
	public void testWithEscapes () {
		TextFragment tf = makeFragment();
		String htmlText = qu.toCodedHTML(tf);
		htmlText = htmlText + "&aacute;&amp;&#39;&#x0152;&apos;";
		String codedText = qu.fromCodedHTML(htmlText, tf);
		TextFragment resFrag = new TextFragment(codedText, tf.getCodes());
		assertEquals("a & < > \" \' <b>bold</b> t <br/> z\u00e1&'\u0152'", resFrag.toString());
	}
	
	private TextFragment makeFragment () {
		TextFragment tf = new TextFragment("a & < > \" \' ");
		tf.append(TagType.OPENING, "b", "<b>");
		tf.append("bold");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append(" t ");
		tf.append(TagType.PLACEHOLDER, null, "<br/>");
		tf.append(" z");
		return tf;
	}

	private TextFragment makeComplexFragment () {
		TextFragment tf = new TextFragment("t1");
		tf.append(TagType.OPENING, "b", "<b>");
		tf.append(TagType.OPENING, "s", "<s>");
		tf.append("bs1");
		tf.append(TagType.CLOSING, "s", "</s>");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append("t2");
		tf.append(TagType.OPENING, "b", "<b>");
		tf.append("b1");
		tf.append(TagType.CLOSING, "b", "</b>");
		return tf;
	}

}
