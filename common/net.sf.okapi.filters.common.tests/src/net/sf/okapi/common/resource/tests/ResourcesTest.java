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

package net.sf.okapi.common.resource.tests;

import java.util.ArrayList;

import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import junit.framework.*;

public class ResourcesTest extends TestCase {

	public void testMETATag1 () {
		String test = "<meta http-equiv=\"keywords\" content=\"one,two,three\"/>";
		System.out.println(" in: "+test);
		ArrayList<FilterEvent> list = new ArrayList<FilterEvent>();
		
		// Build the input
		GenericSkeleton skel = new GenericSkeleton();
		TextUnit tu = new TextUnit("t1", "one,two,three");
		skel.add("content=\"");
		skel.addContentPlaceholder(tu);
		skel.add("\"");		
		tu.setIsReferent(true);
		tu.setName("content");
		tu.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
		
		skel = new GenericSkeleton();
		DocumentPart dp = new DocumentPart("dp1", false);		
		skel.add("<meta http-equiv=\"keywords\" ");
		skel.addReference(tu);
		skel.add("/>");
		dp.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));

		// Output and compare
		assertEquals(generateOutput(list, test), test);
	}
	
	public void testPWithAttribute () {
		String test = "<p title='my title'>Text of p</p>";
		System.out.println(" in: "+test);
		ArrayList<FilterEvent> list = new ArrayList<FilterEvent>();
		
		// Build the input
		GenericSkeleton skel = new GenericSkeleton();
		TextUnit tu1 = new TextUnit("t1", "my title");
		skel.add("title='");
		skel.addContentPlaceholder(tu1);
		skel.add("'");		
		tu1.setIsReferent(true);
		tu1.setName("title");
		tu1.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu1));
		
		skel = new GenericSkeleton();
		TextUnit tu2 = new TextUnit("tu2", "Text of p");		
		skel.add("<p ");
		skel.addReference(tu1);
		skel.add(">");
		skel.addContentPlaceholder(tu2);
		skel.append("</p>");
		tu2.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu2));

		// Output and compare
		assertEquals(generateOutput(list, test), test);
	}
	
	public void testComplexEmptyElement () {
		String test = "<elem wr-prop1='wr-value1' ro-prop1='ro-value1' wr-prop2='wr-value2' text='text'/>";
		System.out.println(" in: "+test);
		ArrayList<FilterEvent> list = new ArrayList<FilterEvent>();
		
		// Build the input
		GenericSkeleton skel = new GenericSkeleton();
		TextUnit tu = new TextUnit("t1", "text");
		skel.add("text='");
		skel.addContentPlaceholder(tu);
		skel.add("'");		
		tu.setIsReferent(true);
		tu.setName("text");
		tu.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));
		
		skel = new GenericSkeleton();
		DocumentPart dp = new DocumentPart("dp1", false);		
		skel.add("<elem wr-prop1='");
		dp.setSourceProperty(new Property("wr-prop1", "wr-value1", false));
		skel.addValuePlaceholder(dp, "wr-prop1", null);
		skel.add("' ro-prop1='ro-value1' wr-prop2='");
		dp.setSourceProperty(new Property("ro-prop1", "ro-value1"));
		dp.setSourceProperty(new Property("wr-prop2", "wr-value2", false));
		skel.addValuePlaceholder(dp, "wr-prop2", null);
		skel.append("' ");
		skel.addReference(tu);
		skel.append("/>");
		
		dp.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));

		// Output and compare
		assertEquals(generateOutput(list, test), test);
	}

	public void testPWithInline () {
		String test = "<p>test an inline <img href=\"there\"/> tag</p>";
		System.out.println(" in: "+test);
		ArrayList<FilterEvent> list = new ArrayList<FilterEvent>();
		
		GenericSkeleton skel = new GenericSkeleton();
		DocumentPart dp1 = new DocumentPart("dp1", true);
		skel.add("<img href=\"");
		skel.addValuePlaceholder(dp1, "href", null);
		dp1.setSourceProperty(new Property("href", "there"));
		skel.add("\"/>");
		dp1.setName("href");
		dp1.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp1));
		
		skel = new GenericSkeleton();
		TextUnit tu1 = new TextUnit("tu1", "test an inline ");
		TextFragment tf = tu1.getSourceContent();
		Code code = new Code(TagType.PLACEHOLDER, "img");
		code.appendReference("dp1");
		tf.append(code);
		tf.append(" tag");
		skel.add("<p>");
		skel.addContentPlaceholder(tu1);
		skel.append("</p>");
		tu1.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu1));

		// Output and compare
		assertEquals(generateOutput(list, test), test);
	}

	
	public void testMETATag2 () {
		String test = "<meta http-equiv=\"Content-Language\" content=\"en\"/>";
		System.out.println(" in: "+test);
		ArrayList<FilterEvent> list = new ArrayList<FilterEvent>();
		
		GenericSkeleton skel = new GenericSkeleton();
		DocumentPart dp = new DocumentPart("dp1", false);		
		skel.add("<meta http-equiv=\"Content-Language\" content=\"");
		skel.addValuePlaceholder(dp, "language", null);
		skel.add("\"/>");
		dp.setSourceProperty(new Property("language", "en", false));
		dp.setSkeleton(skel);
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));

		// Output and compare
		assertEquals(generateOutput(list, test), test);
	}
	
	private String generateOutput (ArrayList<FilterEvent> list,
		String original)
	{
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		StringBuilder tmp = new StringBuilder();
		writer.processStart("en", "utf-8", null, new EncoderManager());
		for ( FilterEvent event : list ) {
			switch ( event.getEventType() ) {
			case TEXT_UNIT:
				tmp.append(writer.processTextUnit((TextUnit)event.getResource()));
				break;
			case DOCUMENT_PART:
				tmp.append(writer.processDocumentPart((DocumentPart)event.getResource()));
				break;
			}
		}
		System.out.println("out: "+tmp.toString());
		return tmp.toString();
	}

}
