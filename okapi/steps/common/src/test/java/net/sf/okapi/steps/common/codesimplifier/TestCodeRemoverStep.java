/*===========================================================================
  Copyright (C) 2010-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.common.codesimplifier;

import static org.junit.Assert.assertEquals;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextUnit;

import org.junit.Before;
import org.junit.Test;

public class TestCodeRemoverStep {

	private GenericContent fmt;
	private CodeRemoverStep crs;
	
	@Before
	public void setup() {
		crs = new CodeRemoverStep(); 
		fmt = new GenericContent();
	}	
	
	@Test
	public void testSource() {
		TextFragment tf = new TextFragment();		
		tf.append(TagType.PLACEHOLDER, "x1", "<x1/>");
		tf.append(TagType.PLACEHOLDER, "x2", "<x2/>");
		tf.append("T1");		
		tf.append(TagType.PLACEHOLDER, "x3", "<x3/>");
		tf.append(TagType.PLACEHOLDER, "x4", "<x4/>");
		tf.append(TagType.OPENING, "a", "<a>");
		tf.append("T2");
		tf.append(TagType.CLOSING, "a", "</a>");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append(TagType.PLACEHOLDER, "x5", "<x5/>");
		tf.append(TagType.PLACEHOLDER, "x6", "<x6/>");
				
		assertEquals("<1/><2/>T1<3/><4/><5>T2</5><e8/><6/><7/>", fmt.setContent(tf).toString());
		
		TextUnit tu1 = new TextUnit("tu1");
		tu1.setSourceContent(tf);
	
		Event tue1 = new Event(EventType.TEXT_UNIT, tu1);
		crs.handleEvent(tue1);
		tf = tu1.getSource().getUnSegmentedContentCopy();
		assertEquals("T1T2", fmt.setContent(tf).toString());
	}
	
	@Test
	public void testTarget() {
		TextFragment tf = new TextFragment();		
		tf.append(TagType.PLACEHOLDER, "x1", "<x1/>");
		tf.append(TagType.PLACEHOLDER, "x2", "<x2/>");
		tf.append("T1");		
		tf.append(TagType.PLACEHOLDER, "x3", "<x3/>");
		tf.append(TagType.PLACEHOLDER, "x4", "<x4/>");
		tf.append(TagType.OPENING, "a", "<a>");
		tf.append("T2");
		tf.append(TagType.CLOSING, "a", "</a>");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append(TagType.PLACEHOLDER, "x5", "<x5/>");
		tf.append(TagType.PLACEHOLDER, "x6", "<x6/>");
				
		assertEquals("<1/><2/>T1<3/><4/><5>T2</5><e8/><6/><7/>", fmt.setContent(tf).toString());
		
		TextUnit tu1 = new TextUnit("tu1");
		tu1.setSourceContent(tf);
		tu1.setTarget(LocaleId.SPANISH, new TextContainer(tf));
	
		Event tue1 = new Event(EventType.TEXT_UNIT, tu1);
		crs.handleEvent(tue1);
		
		tf = tu1.getSource().getUnSegmentedContentCopy();
		assertEquals("T1T2", fmt.setContent(tf).toString());
		
		tf = tu1.getTarget(LocaleId.SPANISH).getUnSegmentedContentCopy();
		assertEquals("T1T2", fmt.setContent(tf).toString());
	}
}
