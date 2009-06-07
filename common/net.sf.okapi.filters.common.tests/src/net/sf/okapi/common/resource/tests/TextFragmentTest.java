/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource.tests;

import java.util.List;

import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.AnnotatedSpan;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.InlineAnnotation;
import net.sf.okapi.common.resource.InvalidPositionException;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextFragmentTest {

	private GenericContent fmt;
	
	@Before
	public void setUp () throws Exception {
		fmt = new GenericContent();
	}
	
	@Test
	public void testConstructors () {
		TextFragment tf1 = new TextFragment();
		assertTrue(tf1.isEmpty());
		assertNotNull(tf1.toString());
		assertNotNull(tf1.getCodedText());
		tf1 = new TextFragment("text");
		assertFalse(tf1.isEmpty());
		TextFragment tf2 = new TextFragment(tf1);
		assertEquals(tf1.toString(), tf2.toString());
		assertNotSame(tf1, tf2);
	}
	
	@Test
	public void testAppend () {
		TextFragment tf1 = new TextFragment();
		tf1.append('c');
		assertEquals(tf1.toString(), "c");
		tf1 = new TextFragment();
		tf1.append("string");
		assertEquals(tf1.toString(), "string");
		tf1.append('c');
		assertEquals(tf1.toString(), "stringc");
		TextFragment tf2 = new TextFragment();
		tf2.append(tf1);
		assertEquals(tf2.toString(), "stringc");
		assertNotSame(tf1, tf2);
		assertFalse(tf1.hasCode());
		
		tf1 = new TextFragment("string");
		tf1.append(TagType.PLACEHOLDER, "br", "<br/>");
		String s1 = tf1.getCodedText();
		s1 = s1.toUpperCase();
		assertEquals(tf1.toString(), "string<br/>");
		tf1.setCodedText(s1);
		assertEquals(tf1.toString(), "STRING<br/>");
		
		// Test with in-line codes
		tf1 = new TextFragment();
		tf1.append(TagType.PLACEHOLDER, "br", "<br/>");
		assertTrue(tf1.hasCode());
		Code code = tf1.getCode(0);
		assertEquals(code.getData(), "<br/>");
		assertEquals(tf1.toString(), "<br/>"); 
	}
	
	@Test
	public void testInsert () {
		TextFragment tf1 = new TextFragment();
		tf1.insert(0, new TextFragment("[ins1]"));
		assertEquals(tf1.toString(), "[ins1]");
		tf1.insert(4, new TextFragment("ertion"));
		assertEquals(tf1.toString(), "[insertion1]");
		tf1.insert(0, new TextFragment("<"));
		assertEquals(tf1.toString(), "<[insertion1]");
		tf1.insert(13, new TextFragment(">"));
		assertEquals(tf1.toString(), "<[insertion1]>");
		tf1.insert(-1, new TextFragment("$"));
		assertEquals(tf1.toString(), "<[insertion1]>$");
		// Test with in-line codes
		tf1 = new TextFragment();
		tf1.insert(0, new TextFragment("abc"));
		TextFragment tf2 = new TextFragment();
		tf2.append(TagType.PLACEHOLDER, "br", "<br/>");
		tf1.insert(1, tf2);
		Code code = tf1.getCode(0);
		assertEquals(code.getData(), "<br/>");
		assertEquals(fmt.setContent(tf1).toString(true), "a<br/>bc");
		tf2 = new TextFragment();
		tf2.append(TagType.OPENING, "b", "<b>");
		tf1.insert(4, tf2);
		tf2 = new TextFragment();
		tf2.append(TagType.CLOSING, "b", "</b>");
		tf1.insert(7, tf2);
		tf2 = new TextFragment();
		tf2.append(TagType.PLACEHOLDER, "x", "<x/>");
		tf1.insert(-1, tf2);
		assertEquals(tf1.toString(), "a<br/>b<b>c</b><x/>");
	}

	@Test
	public void testRemove () {
		TextFragment tf1 = makeFragment1();
		assertEquals(fmt.setContent(tf1).toString(true), "[b]A[br/]B[/b]C");
		tf1.remove(2, 3); // xxAxxBxxC -> xxxxBxxC
		tf1.remove(4, 5); // xxxxBxxC -> xxxxxxC
		tf1.remove(6, 7); // xxxxxxC -> xxxxxx
		assertFalse(tf1.hasText(true));
		assertEquals(tf1.getCodedText().length(), 3*2);
		assertEquals(tf1.toString(), "[b][br/][/b]");

		tf1 = makeFragment1();
		tf1.remove(0, 2); // xxAxxBxxC -> AxxBxxC
		tf1.remove(1, 3); // AxxBxxC -> ABxxC
		tf1.remove(2, 4); // ABxxC -> ABC
		assertFalse(tf1.hasCode());
		assertEquals(tf1.getCodedText().length(), 3);
		assertEquals(tf1.toString(), "ABC");
	}
	
	@Test
	public void testInlines () {
		TextFragment tf1 = makeFragment1();
		assertTrue(tf1.hasCode());
		assertEquals(tf1.toString(), "[b]A[br/]B[/b]C");
		assertEquals(tf1.getCode(0).getData(), "[b]");
		assertEquals(tf1.getCode(1).getData(), "[br/]");
		assertEquals(tf1.getCode(2).getData(), "[/b]");
		assertEquals(fmt.setContent(tf1).toString(false), "<1>A<2/>B</1>C");
		tf1.remove(0, 2);
		//TODO: assertEquals(display.setContent(tf1).toString(false), "A<2/>B<1/>C");
		assertEquals(tf1.toString(), "A[br/]B[/b]C");
		TextFragment tf2 = new TextFragment();
		tf2.append(TagType.OPENING, "b", "[b]");
		tf1.insert(0, tf2);
		//TODO: assertEquals(display.setContent(tf1).toString(false), "<1/>A<2/>B</1>C");
		
		Code code1 = new Code(TagType.PLACEHOLDER, "type", "data");
		code1.setReferenceFlag(true);
		code1.setId(100);
		code1.setOuterData("outer");
		assertEquals(code1.getType(), "type");
		assertEquals(code1.getData(), "data");
		assertEquals(code1.getOuterData(), "outer");
		assertEquals(code1.getId(), 100);
		assertEquals(code1.getTagType(), TagType.PLACEHOLDER);

		tf1 = new TextFragment();
		Code code2 = tf1.append(code1);
		Code code3 = tf1.getCode(0);
		assertSame(code1, code2);
		assertSame(code2, code3);
		code1 = null;
		assertEquals(code2.getType(), "type");
		assertEquals(code2.getData(), "data");
		assertEquals(code2.getOuterData(), "outer");
		assertEquals(code2.getId(), 100);
		assertEquals(code2.getTagType(), TagType.PLACEHOLDER);
		
		Code code4 = code2.clone();
		assertNotSame(code4, code2);
		assertEquals(code4.getType(), "type");
		assertEquals(code4.getData(), "data");
		assertEquals(code4.getOuterData(), "outer");
		assertEquals(code4.getId(), 100);
		assertEquals(code4.getTagType(), TagType.PLACEHOLDER);
		
		code1 = new Code(TagType.PLACEHOLDER, "t", "d");
		assertFalse(code1.hasReference());
		assertFalse(code1.isCloneable());
		assertFalse(code1.isDeleteable());
		code2 = code1.clone();
		assertFalse(code2.hasReference());
		assertFalse(code2.isCloneable());
		assertFalse(code2.isDeleteable());
		
		code1.setReferenceFlag(true);
		code1.setCloneable(true);
		code1.setDeleteable(true);
		assertTrue(code1.hasReference());
		assertTrue(code1.isCloneable());
		assertTrue(code1.isDeleteable());
		
		code1.setReferenceFlag(false);
		assertFalse(code1.hasReference());
		assertTrue(code1.isCloneable());
		assertTrue(code1.isDeleteable());
		
		code1.setCloneable(false);
		assertFalse(code1.hasReference());
		assertFalse(code1.isCloneable());
		assertTrue(code1.isDeleteable());

		code1.setDeleteable(false);
		assertFalse(code1.hasReference());
		assertFalse(code1.isCloneable());
		assertFalse(code1.isDeleteable());

		code1.setReferenceFlag(true);
		code1.setDeleteable(true);
		assertTrue(code1.hasReference());
		assertFalse(code1.isCloneable());
		assertTrue(code1.isDeleteable());
		
		tf1 = new TextFragment();
		tf1.append(code1);
		String codesStorage1 = Code.codesToString(tf1.getCodes());
		String textStorage1 = tf1.getCodedText();
		assertNotNull(codesStorage1);
		assertNotNull(textStorage1);
		tf2 = new TextFragment();
		tf2.setCodedText(textStorage1, Code.stringToCodes(codesStorage1));
		assertEquals(tf1.toString(), tf2.toString());
		String codesStorage2 = Code.codesToString(tf2.getCodes());
		String textStorage2 = tf2.getCodedText();
		assertEquals(codesStorage1, codesStorage2);
		assertEquals(textStorage1, textStorage2);
	}
	
	@Test
	public void testCodedText () {
		TextFragment tf1 = makeFragment1();
		assertEquals(tf1.getCodedText().length(), (2*3)+3); // 2 per code + 3 chars
		assertEquals(tf1.getCodedText(3, 5).length(), 2); // code length for <br/>
		
		String codedText = tf1.getCodedText();
		List<Code> codes = tf1.getCodes();
		TextFragment tf2 = new TextFragment();
		tf2.setCodedText(codedText, codes);
		assertEquals(tf1.toString(), tf2.toString());
		assertEquals(fmt.setContent(tf1).toString(false), fmt.setContent(tf2).toString(false));
		assertNotSame(tf1, tf2);

		codes = null;
		codes = tf1.getCodes(0, 5); // xxAxxBxxC
		assertNotNull(codes);
		assertEquals(codes.size(), 2);
		assertEquals(codes.get(0).getData(), "[b]");
		assertEquals(codes.get(1).getData(), "[br/]");
	}

	@Test
	public void testHasText () {
		TextFragment tf1 = new TextFragment();
		assertFalse(tf1.hasText(true));
		assertFalse(tf1.hasText(false));
		tf1.append(TagType.PLACEHOLDER, "br", "<br/>");
		assertFalse(tf1.hasText(true));
		assertFalse(tf1.hasText(false));
		tf1.append('\t');
		assertTrue(tf1.hasText(true));
		assertFalse(tf1.hasText(false));
		tf1 = new TextFragment();
		tf1.append(TagType.PLACEHOLDER, "br", "<br/>");
		tf1.append('c');
		assertTrue(tf1.hasText(true));
		assertTrue(tf1.hasText(false));
	}
	
	@Test
	public void testHasCode () {
		TextFragment tf1 = new TextFragment();
		assertFalse(tf1.hasCode());
		tf1.append('\t');
		assertFalse(tf1.hasCode());
		tf1.append('c');
		assertFalse(tf1.hasCode());
		tf1.append(TagType.PLACEHOLDER, "br", "<br/>");
		assertTrue(tf1.hasCode());
	}

	@Test
	public void testTextCodesChanges () {
		TextFragment tf1 = new TextFragment("<b>New file:</b> %s");

		// Change the codes
		int diff = tf1.changeToCode(0, 3, TagType.OPENING, "b");
		diff += tf1.changeToCode(12+diff, 16+diff, TagType.CLOSING, "b");
		List<Code> list1 = tf1.getCodes();
		assertEquals(list1.get(0).getData(), "<b>");
		assertEquals(list1.get(1).getData(), "</b>");
		assertEquals(tf1.toString(), "<b>New file:</b> %s");
		assertEquals(fmt.setContent(tf1).toString(false), "<1>New file:</1> %s");

		// Add an annotation: "%s" (use diff because %s is after both added codes) 
		tf1.annotate(17+diff, 19+diff, "protected", null);
		assertEquals(tf1.toString(), "<b>New file:</b> %s");
		list1 = tf1.getCodes();
		assertTrue(list1.get(2).hasAnnotation());
		assertTrue(list1.get(2).hasAnnotation("protected"));
		assertEquals(fmt.setContent(tf1).toString(true), "<b>New file:</b> %s");
		assertEquals(fmt.setContent(tf1).toString(false), "<1>New file:</1> <2>%s</2>");
		
		// Test if we can rebuild the annotation from the storage string
		String codesStorage1 = Code.codesToString(tf1.getCodes());
		String textStorage1 = tf1.getCodedText();
		assertNotNull(codesStorage1);
		assertNotNull(textStorage1);
		TextFragment tf2 = new TextFragment();
		tf2.setCodedText(textStorage1, Code.stringToCodes(codesStorage1));
		assertEquals(tf1.toString(), tf2.toString());
		List<Code> list2 = tf2.getCodes();
		assertTrue(list1.get(2).hasAnnotation());
		assertTrue(list1.get(2).hasAnnotation("protected"));
		assertEquals(fmt.setContent(tf2).toString(true), "<b>New file:</b> %s");
		assertEquals(fmt.setContent(tf2).toString(false), "<1>New file:</1> <2>%s</2>");

		// Add an annotation for "New" (don't use diff, correct manually: xxNew file:</b>)
		tf1.annotate(2, 5, "term", new InlineAnnotation("Nouveau"));
		assertEquals(fmt.setContent(tf1).toString(true), "<b>New file:</b> %s");
		assertEquals(fmt.setContent(tf1).toString(false), "<1><3>New</3> file:</1> <2>%s</2>");

		// Test start/end annotation and cloning
		InlineAnnotation annot1 = list1.get(4).getAnnotation("term"); // Opening
		InlineAnnotation annot2 = list1.get(5).getAnnotation("term"); // Closing
		assertSame(annot1, annot2);
		annot1.setData("new data"); // Check that changing in one, affects both
		assertEquals(annot2.toString(), "new data");
		annot2.setData("Nouveau"); // Check changing back
		assertEquals(annot1.toString(), "Nouveau");
		assertEquals(list1.get(4).getAnnotation("term").toString(), annot2.toString());
		// Check cloning
		Code c1 = list1.get(4);
		Code c2 = c1.clone();
		annot1 = c1.getAnnotation("term");
		annot2 = c2.getAnnotation("term");
		assertNotSame(annot1, annot2);
		
		// Test if we can rebuild the annotation from the storage string
		tf2 = new TextFragment();
		tf2.setCodedText(tf1.getCodedText(),
			Code.stringToCodes(Code.codesToString(tf1.getCodes())));
		assertEquals(tf1.toString(), tf2.toString());
		list2 = tf2.getCodes();
		assertTrue(list2.get(2).hasAnnotation());
		assertTrue(list2.get(2).hasAnnotation("protected"));
		assertTrue(list2.get(4).hasAnnotation("term"));
		InlineAnnotation annotation = list2.get(4).getAnnotation("term");
		assertEquals(annotation.getData(), "Nouveau");
		// Test annotation change
		annotation.setData("Neue");
		// Get the codes of tf1
		list1 = tf1.getCodes();
		// Check if the same annotation is now changed like in tf2:
		// It should not as tf2 is a clone.
		assertEquals(list1.get(4).getAnnotation("term").getData(), "Nouveau");
		assertEquals(list2.get(4).getAnnotation("term").getData(), "Neue");
		// Checks same annotation object after reading from string storage
		//TODO: Fix the storage issue!!! 
		//annot1 = list2.get(4).getAnnotation("term"); // Opening
		//annot2 = list2.get(5).getAnnotation("term"); // Closing
		//assertSame(annot1, annot2);
		
		// Test re-use of codes for adding annotations
		// Add annotations for "yyNewyy file:" xxyyNewyy file:xx
		tf1.annotate(2, 15, "mt", new InlineAnnotation("MT1"));
		tf1.annotate(2, 15, "term", new InlineAnnotation("TERM2"));
		// The added annotations should have used <1></1>
		assertEquals(fmt.setContent(tf1).toString(false), "<1><3>New</3> file:</1> <2>%s</2>");
		list1 = tf1.getCodes();
		assertEquals(list1.get(0).getAnnotation("mt").getData(), "MT1");
		assertEquals(list1.get(0).getAnnotation("term").getData(), "TERM2");

		// Test spans
		List<AnnotatedSpan> spans = tf1.getAnnotatedSpans("term");
		assertEquals(spans.size(), 2);
		assertEquals(fmt.setContent(spans.get(0).span).toString(true), "New file:");		
		assertEquals(fmt.setContent(spans.get(0).span).toString(false), "<3>New</3> file:");		
		assertEquals(fmt.setContent(spans.get(1).span).toString(true), "New");		
		assertEquals(fmt.setContent(spans.get(1).span).toString(false), "New");
		assertEquals(spans.get(0).range.start, 2);
		assertEquals(spans.get(0).range.end, 15);
		assertEquals(spans.get(1).range.start, 4);
		assertEquals(spans.get(1).range.end, 7);
		
		// Test clearing the annotations
		assertTrue(tf1.hasAnnotation());
		// Clear annotations on <b>
		list1.get(0).removeAnnotations();
		assertTrue(tf1.hasAnnotation()); // Has still other annotations
		assertEquals(fmt.setContent(tf1).toString(false), "<1><3>New</3> file:</1> <2>%s</2>");
		assertFalse(list1.get(0).hasAnnotation());
		// Clear annotation on <3>
		int n = list1.size();
		list1.get(4).removeAnnotations();
		// Should be same number of codes: clearing the code does not remove it
		assertEquals(list1.size(), n);
		assertEquals(fmt.setContent(tf1).toString(false), "<1><3>New</3> file:</1> <2>%s</2>");
		// Clear on the whole text
		tf1.removeAnnotations();
		assertFalse(tf1.hasAnnotation());
		assertEquals(fmt.setContent(tf1).toString(true), "<b>New file:</b> %s");
		// Codes with annotations only should be removed by removeAnnotations()
		assertEquals(fmt.setContent(tf1).toString(false), "<1>New file:</1> %s");
		
		// Check annotate behavior
		tf1 = new TextContainer("w1 ");
		tf1.append(TagType.OPENING, "b", "<b>");
		tf1.append("w2 w3");
		tf1.append(TagType.CLOSING, "b", "</b>");
		tf1.append(" w4 ");
		tf1.append(TagType.OPENING, "i", "<i>");
		tf1.append("w5 w6");
		tf1.append(TagType.CLOSING, "i", "</i>");
		tf1.append(" w7");
		assertEquals(fmt.setContent(tf1).toString(false), "w1 <1>w2 w3</1> w4 <2>w5 w6</2> w7");
		// Annotate "<1>[w2 w3]</1>"
		tf1.annotate(5, 10, "a1", null);
		// Should be the same as annotation uses <1> and </1>
		assertTrue(tf1.hasAnnotation("a1"));
		assertEquals(fmt.setContent(tf1).toString(false), "w1 <1>w2 w3</1> w4 <2>w5 w6</2> w7");
		// Annotate "[<1>w2 w3</1>]"
		tf1.annotate(3, 12, "a2", null);
		// Should be the same as annotation uses <1> and </1>
		assertTrue(tf1.hasAnnotation("a2"));
		//TODO: Re-use existing annotation markers, don't add new ones
		//assertEquals(fmt.setContent(tf1).toString(false), "w1 <1>w2 w3</1> w4 <2>w5 w6</2> w7");
		// Annotate "<1>[w2] w3</1>]"
		tf1.annotate(5, 7, "a3", null);
		assertTrue(tf1.hasAnnotation("a3"));
		//TODO: Re-use existing annotation markers, don't add new ones
		//assertEquals(fmt.setContent(tf1).toString(false), "w1 <1><3>w2</3> w3</1> w4 <2>w5 w6</2> w7");
		// Annotate "<1>w2 [w3]</1>]" (w1 xxyyw2yy w3xx)
		tf1.annotate(12, 14, "a4", null);
		assertTrue(tf1.hasAnnotation("a4"));
		//TODO: Re-use existing annotation markers, don't add new ones
		//assertEquals(fmt.setContent(tf1).toString(false), "w1 <1><3>w2</3> <4>w3</4></1> w4 <2>w5 w6</2> w7");

		// Clear all annotations
		tf1.removeAnnotations();
		assertFalse(tf1.hasAnnotation());
		//TODO: Re-use existing annotation markers, don't add new ones
		//assertEquals(fmt.setContent(tf1).toString(false), "w1 <1>w2 w3</1> w4 <2>w5 w6</2> w7");
		
		// Annotate "[w1 <1>w2] w3</1>"
		tf1.annotate(0, 7, "a5", null);
		spans = tf1.getAnnotatedSpans("a5");
		//TODO: Re-use existing annotation markers, don't add new ones
		//assertEquals(spans.size(), 2);
		//assertEquals(fmt.setContent(tf1).toString(false), "<3>w1 </3><1><4>w2</4> w3</1> w4 <2>w5 w6</2> w7");

	}
	
	@Test(expected = InvalidPositionException.class)
    public void testGetCodedTextWithBadRange () {
		TextFragment tf = makeFragment1();
		tf.getCodedText(1, 3); // 1 is middle of first code
    }
	
	@Test
    public void testCompareToSameString () {
		TextFragment tf = new TextFragment("text of the fragment");
		assertEquals(0, tf.compareTo("text of the fragment"));
    }
	
	@Test
    public void testCompareToDifferentString () {
		TextFragment tf = new TextFragment("text of the fragment");
		assertFalse(0==tf.compareTo("Text Of The Fragment"));
    }
	
	@Test
    public void testCompareToSameFragment () {
		TextFragment tf1 = new TextFragment("text of the fragment");
		TextFragment tf2 = new TextFragment("text of the fragment");
		assertEquals(0, tf1.compareTo(tf2));
    }
	
	@Test
    public void testCompareToDifferentFragment () {
		TextFragment tf1 = new TextFragment("text of the fragment");
		TextFragment tf2 = new TextFragment("Text Of The Fragment");
		assertFalse(0==tf1.compareTo(tf2));
    }
	
	@Test
    public void testCompareToSameFragmentWithSameCodes () {
		TextFragment tf1 = makeFragment1();
		TextFragment tf2 = makeFragment1();
		assertEquals(0, tf1.compareTo(tf2, true));
    }
	
	@Test
    public void testCompareToSameFragmentWithDifferentCodes () {
		TextFragment tf1 = makeFragment1();
		tf1.getCodes().get(0).setData("[zzz]");
		TextFragment tf2 = makeFragment1();
		assertTrue(0==tf1.compareTo(tf2, false));
		assertFalse(0==tf1.compareTo(tf2, true));
    }
	
	@Test
    public void testSynchronizeCodeIdentifiers () {
		TextFragment tf1 = makeFragment1();
		TextFragment tf2 = makeFragment2();
		tf2.synchronizeCodes(tf1);
		List<Code> srcCodes = tf1.getCodes();
		List<Code> trgCodes = tf2.getCodes();
		for ( Code srcCode : srcCodes ) {
			for ( Code trgCode : trgCodes ) {
				// Same ID must have the same content, except for open/close
				if ( srcCode.getId() == trgCode.getId() ) {
					switch ( srcCode.getTagType() ) {
					case OPENING:
						if ( trgCode.getTagType() == TagType.CLOSING ) break;
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					case CLOSING:
						if ( trgCode.getTagType() == TagType.OPENING ) break;
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					default:
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					}
				}
			}
		}
		assertEquals(fmt.setContent(tf2).toString(false),
			"<2/>A<1>B</1>C");
    }
	
	@Test
    public void testSynchronizeCodeIdentifiersComplex () {
		TextFragment tf1 = makeFragment1();
		TextFragment tf2 = makeFragment3();
		tf2.synchronizeCodes(tf1);
		List<Code> srcCodes = tf1.getCodes();
		List<Code> trgCodes = tf2.getCodes();
		for ( Code srcCode : srcCodes ) {
			for ( Code trgCode : trgCodes ) {
				// Same ID must have the same content, except for open/close
				if ( srcCode.getId() == trgCode.getId() ) {
					switch ( srcCode.getTagType() ) {
					case OPENING:
						if ( trgCode.getTagType() == TagType.CLOSING ) break;
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					case CLOSING:
						if ( trgCode.getTagType() == TagType.OPENING ) break;
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					default:
						assertEquals(srcCode.getData(), trgCode.getData());
						break;
					}
				}
			}
		}
		assertEquals(fmt.setContent(tf2).toString(false),
			"<3><2/>A</3>B<1>C</1>D<4/>");
    }
	
	/**
	 * Makes a fragment <code>[b]A[br/]B[/b]C<code>
	 * @return the new fragment.
	 */
	private TextFragment makeFragment1 () {
		TextFragment tf = new TextFragment();
		tf.append(TagType.OPENING, "b", "[b]");
		tf.append("A");
		tf.append(TagType.PLACEHOLDER, "br", "[br/]");
		tf.append("B");
		tf.append(TagType.CLOSING, "b", "[/b]");
		tf.append("C");
		return tf;
	}

	/**
	 * Makes a fragment <code>[br/]A[b]B[/b]C<code>
	 * @return the new fragment.
	 */
	private TextFragment makeFragment2 () {
		TextFragment tf = new TextFragment();
		tf.append(TagType.PLACEHOLDER, "br", "[br/]");
		tf.append("A");
		tf.append(TagType.OPENING, "b", "[b]");
		tf.append("B");
		tf.append(TagType.CLOSING, "b", "[/b]");
		tf.append("C");
		return tf;
	}

	/**
	 * Makes a fragment <code>[u][br/]A[/u]B[b]C[/b]D[br/]<code>
	 * @return the new fragment.
	 */
	private TextFragment makeFragment3 () {
		TextFragment tf = new TextFragment();
		tf.append(TagType.OPENING, "u", "[u]");
		tf.append(TagType.PLACEHOLDER, "br", "[br/]");
		tf.append("A");
		tf.append(TagType.CLOSING, "u", "[/u]");
		tf.append("B");
		tf.append(TagType.OPENING, "b", "[b]");
		tf.append("C");
		tf.append(TagType.CLOSING, "b", "[/b]");
		tf.append("D");
		tf.append(TagType.PLACEHOLDER, "br", "[br/]");
		return tf;
	}

}
