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

import java.util.Stack;

import net.sf.okapi.common.filterwriter.XLIFFContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import junit.framework.*;

public class OptimizerTest extends TestCase {

	public void testOptimizer () {
		TextUnit tu = new TextUnit("tu1");
		TextFragment tf = tu.getSourceContent();
		XLIFFContent fmt = new XLIFFContent();

		tf.append("Hello ");
		tf.append(TagType.OPENING, "b1", "<b1>");
		tf.append(TagType.OPENING, "b2", "<b2>");
		tf.append("bold");
		tf.append(TagType.CLOSING, "b2", "</b2>");
		tf.append(TagType.CLOSING, "b1", "</b1>");
		tf.append("world");
		assertEquals(tf.getCodes().size(), 4); 
		OptimizerTest.optimizes(tf);
		assertEquals(tf.getCodes().size(), 2); 
		fmt.setContent(tf);
		System.out.println(fmt.toString());
	}
	
	static private void optimizes (TextFragment frag) {
		Stack<Code> stack = new Stack<Code>();
		String text = frag.getCodedText();;
		char ch;
		Code code;
		int i = 0;
	
		while ( i<text.length() ) {
			int start = -1;
			int end = -1;
			boolean stop = false;
			stack.clear();
			
			while ( i<text.length() ) {
				ch = text.charAt(i);
				if ( TextFragment.isMarker(ch) ) {
					code = frag.getCode(text.charAt(++i));
					switch ( code.getTagType() ) {
					case OPENING:
						stack.push(code);
						if ( start == -1 ) start = i-1;
						else end = i+1;
						break;
					case CLOSING:
						if ( stack.size() == 0 ) {
							if ( start == -1 ) start = i-1;
							else end = i+1;
						}
						else {
							if ( stack.peek().getTagType() == TagType.OPENING ) {
								if ( stack.peek().getType().equals(code.getType()) ) {
									stack.pop();
									if ( start == -1 ) start = i-1;
									else end = i+1;
								}
							}
							// Else: stop here
							stop = true;
						}
						break;
					case PLACEHOLDER:
						stack.push(code);
						if ( start == -1 ) start = i-1;
						// But not yet a end opportunity
						break;
					}
				}
				else {
					if ( start > -1 ) break;
				}
				if ( stop ) break;
				i++;
			}
			
			if (( start > -1 ) && ( end-start > 2 )) {
				i += frag.changeToCode(start, end, TagType.PLACEHOLDER, "group");
				text = frag.getCodedText();
			}
		}
		
		frag.renumberCodes();
	}
}
