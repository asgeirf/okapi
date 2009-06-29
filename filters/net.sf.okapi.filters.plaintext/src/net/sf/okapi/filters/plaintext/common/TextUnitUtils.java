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

package net.sf.okapi.filters.plaintext.common;

import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

/**
 * 
 * 
 * @version 0.1, 09.06.2009
 * @author Sergei Vasilyev
 */

public class TextUnitUtils {
	
	/**
	 * 
	 * @param textFragment
	 */
	public static void trimLeading(TextFragment textFragment) {
		trimLeading(textFragment, null);
	}
	
	/**
	 * 
	 * @param textFragment
	 * @param skel
	 */
	public static void trimLeading(TextFragment textFragment, GenericSkeleton skel) {
		
		if (textFragment == null) return;
		String st = textFragment.getCodedText();
		TextFragment skelTF;
		
		int pos = TextFragment.indexOfFirstNonWhitespace(st, 0, -1, true, true, true, true);		
		if (pos == -1) { // Whole string is whitespaces
			skelTF = new TextFragment(st);
			textFragment.setCodedText("");			
		}
		else {
			skelTF = textFragment.subSequence(0, pos);
			textFragment.setCodedText(st.substring(pos));			
		}
			
		if (skel == null) return;
		if (skelTF == null) return;
		
		skel.append(skelTF.toString());  // Codes get removed
	}
	
	/**
	 * 
	 * @param textFragment
	 */
	public static void trimTrailing(TextFragment textFragment) {
		trimTrailing(textFragment, null);
	}
	
	/**
	 * 
	 * @param textFragment
	 * @param skel
	 */
	public static void trimTrailing(TextFragment textFragment, GenericSkeleton skel) {
		
		if (textFragment == null) return;
		
		String st = textFragment.getCodedText();
		TextFragment skelTF;
		
		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1) { // Whole string is whitespaces
			skelTF = new TextFragment(st);
			textFragment.setCodedText("");			
		}
		else {
			skelTF = textFragment.subSequence(pos + 1, st.length());
			textFragment.setCodedText(st.substring(0, pos + 1));			
		}
						
		if (skel == null) return;
		if (skelTF == null) return;
		
		skel.append(skelTF.toString());  // Codes get removed);
	}

	/**
	 * !!! Trailing spaces are not counted. 
	 * @param textFragment
	 * @param substr
	 * @return
	 */
	public static boolean endsWith(TextFragment textFragment, String substr) {
		
		if (textFragment == null) return false;
		if (Util.isEmpty(substr)) return false;
		
		String st = textFragment.getCodedText();
		
		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1) return false;
		
		return st.lastIndexOf(substr) == pos - substr.length() + 1;
	}

	/**
	 * 
	 * @param textFragment
	 * @return
	 */
	public static char getLastChar(TextFragment textFragment) {
		
		if (textFragment == null) return '\0';
		
		String st = textFragment.getCodedText();
		
		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1) return '\0';
		
		return st.charAt(pos);
	}

	/**
	 * 
	 * @param textFragment
	 */
	public static void deleteLastChar(TextFragment textFragment) {
		
		if (textFragment == null) return;
		String st = textFragment.getCodedText();
		
		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1) return;
		
		textFragment.remove(pos, pos + 1);
	}
		
	/**
	 * 
	 * @param textFragment
	 * @param findWhat
	 * @return
	 */
	public static int lastIndexOf(TextFragment textFragment, String findWhat) {
		
		if (textFragment == null) return -1;
		if (Util.isEmpty(findWhat)) return -1;
		if (Util.isEmpty(textFragment.getCodedText())) return -1;
		
		return (textFragment.getCodedText()).lastIndexOf(findWhat);
	}
				
	public static boolean isEmpty(TextFragment textFragment) {
		
		return (textFragment == null || (textFragment != null && textFragment.isEmpty()));		
	}

	public static TextUnit buildTU(
			TextUnit textUnit, 
			String name, 
			TextContainer source, 
			TextContainer target, 
			String language, 
			String comment) {
		
		if (textUnit == null) {
			
			textUnit = new TextUnit("");			
		}
		
		if (textUnit.getSkeleton() == null) {
			
			GenericSkeleton skel = new GenericSkeleton();
			textUnit.setSkeleton(skel);
		}		
		
		if (!Util.isEmpty(name))
			textUnit.setName(name);
		
		if (source != null)
			textUnit.setSource(source);
		
		if (target != null && !Util.isEmpty(language))
			textUnit.setTarget(language, target);
		
		if (comment != null)
			textUnit.setProperty(new Property(Property.NOTE, comment));
		
		return textUnit;
	}

	public static GenericSkeleton forseSkeleton(TextUnit tu) {
		
		if (tu == null) return null;
		
		ISkeleton res = tu.getSkeleton();
		if (res == null) {
			
			res = new GenericSkeleton();
			tu.setSkeleton(res);
		}
		
		return (GenericSkeleton) res;
	}	
	
}
