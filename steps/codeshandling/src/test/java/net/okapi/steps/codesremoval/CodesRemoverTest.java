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

package net.okapi.steps.codesremoval;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.steps.codesremoval.CodesRemover;
import net.sf.okapi.steps.codesremoval.Parameters;

import org.junit.Test;
import static org.junit.Assert.*;

public class CodesRemoverTest {

	private CodesRemover remover;
	private Parameters params;
	
	@Test
	public void testSimple () {
		params = new Parameters();
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextUnit tu = new TextUnit("id");
		tu.setSourceContent(createSimpleFragment());
		tu.setTargetContent(LocaleId.SPANISH, createSimpleFragment());
		
		remover.processTextUnit(tu);
		assertEquals("t1t2t3", tu.toString());
		assertEquals("t1t2t3", tu.getTarget(LocaleId.SPANISH).toString());
	}
	
	@Test
	public void testSkipNonTranslatable () {
		params = new Parameters();
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextUnit tu = new TextUnit("id");
		tu.setSourceContent(createSimpleFragment());
		tu.setTargetContent(LocaleId.SPANISH, createSimpleFragment());
		params.setIncludeNonTranslatable(false);
		tu.setIsTranslatable(false);
		
		remover.processTextUnit(tu);
		assertEquals("t1<br/>t2<b>t3</b>", tu.toString());
		assertEquals(3, tu.getSource().getCodes().size());
		assertEquals("t1<br/>t2<b>t3</b>", tu.getTarget(LocaleId.SPANISH).toString());
		assertEquals(3, tu.getTarget(LocaleId.SPANISH).getCodes().size());
	}
	
	@Test
	public void testDontStripSource () {
		params = new Parameters();
		params.setStripSource(false);
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextUnit tu = new TextUnit("id");
		tu.setSourceContent(createSimpleFragment());
		tu.setTargetContent(LocaleId.SPANISH, createSimpleFragment());
		
		remover.processTextUnit(tu);
		assertEquals("t1<br/>t2<b>t3</b>", tu.toString());
		assertEquals(3, tu.getSource().getCodes().size());
		assertEquals("t1t2t3", tu.getTarget(LocaleId.SPANISH).toString());
	}
	
	@Test
	public void testDontStripTarget () {
		params = new Parameters();
		params.setStripTarget(false);
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextUnit tu = new TextUnit("id");
		tu.setSourceContent(createSimpleFragment());
		tu.setTargetContent(LocaleId.SPANISH, createSimpleFragment());
		
		remover.processTextUnit(tu);
		assertEquals("t1t2t3", tu.toString());
		assertEquals("t1<br/>t2<b>t3</b>", tu.getTarget(LocaleId.SPANISH).toString());
		assertEquals(3, tu.getTarget(LocaleId.SPANISH).getCodes().size());
	}

	@Test
	public void testDontStripSegments () {
		params = new Parameters();
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextContainer tc = new TextContainer();
		tc.append("C1");
		tc.appendSegment(createSimpleFragment());
		tc.append("C2");
		tc.appendSegment(createSimpleFragment());
		
		remover.processContainer(tc);
		assertEquals("C10C21", tc.toString());
		assertEquals("t1t2t3", tc.getSegments().get(0).text.toString());
		assertEquals("t1t2t3", tc.getSegments().get(1).text.toString());
		tc.mergeAllSegments();
		assertEquals("C1t1t2t3C2t1t2t3", tc.toString());
	}

	@Test
	public void testKeepCodeRemoveContent () {
		params = new Parameters();
		params.setMode(Parameters.KEEPCODE_REMOVECONTENT);
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextFragment tf = createSimpleFragment();
		
		remover.processFragment(tf);
		assertEquals("t1t2t3", tf.toString());
		assertEquals(3, tf.getCodes().size());
	}
	
	@Test
	public void testRemoveCodeKeepContent () {
		params = new Parameters();
		params.setMode(Parameters.REMOVECODE_KEEPCONTENT);
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextFragment tf = createSimpleFragment();
		
		remover.processFragment(tf);
		assertEquals("t1<br/>t2<b>t3</b>", tf.toString());
		assertEquals(0, tf.getCodes().size());
	}
	
	@Test
	public void testRemoveCodeRemoveContent () {
		params = new Parameters();
		params.setMode(Parameters.REMOVECODE_REMOVECONTENT);
		remover = new CodesRemover(params, LocaleId.SPANISH);
		TextFragment tf = createSimpleFragment();
		
		remover.processFragment(tf);
		assertEquals("t1t2t3", tf.toString());
		assertEquals(0, tf.getCodes().size());
	}
	
	TextFragment createSimpleFragment () {
		TextFragment tf = new TextFragment("t1");
		tf.append(TagType.PLACEHOLDER, "br", "<br/>");
		tf.append("t2");
		tf.append(TagType.OPENING, "b", "<b>");
		tf.append("t3");
		tf.append(TagType.CLOSING, "b", "</b>");
		return tf;
	}

}
