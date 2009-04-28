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

package net.sf.okapi.filters.openxml; // DWH 4-8-09

import java.util.List;

import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filterwriter.ILayerProvider;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;

// For this to work, expandCodeContent has to be changed to protected in GenericSkeletonWriter

/**
 * Implements ISkeletonWriter for the GenericSkeleton skeleton. 
 */
public class OpenXMLContentSkeletonWriter extends GenericSkeletonWriter {

	public final static int MSWORD=1;
	public final static int MSEXCEL=2;
	public final static int MSPOWERPOINT=3;
	public final static int MSWORDCHART=4; // DWH 4-16-09
	private int configurationType; // DWH 4-10-09
	private ILayerProvider layer;
	private EncoderManager encoderManager;
	
	public OpenXMLContentSkeletonWriter(int configurationType) // DWH 4-8-09
	{
		super();
		this.configurationType = configurationType; // DWH 4-10-09
	}
	
@Override
	public String getContent (TextFragment tf,
		String langToUse,
		int context)
	{
		String sTuff; // DWH 4-8-09
		boolean bInBlankText=false; // DWH 4-8-09
		int nSurroundingCodes=0; // DWH 4-8-09
		// Output simple text
		if ( !tf.hasCode() ) {
			if (context==1)
			{
				if (configurationType==MSWORD)
					sTuff = "<w:r><w:t xml:space=\"preserve\">"+tf.toString()+"</w:t></w:r>"; // DWH 4-8-09
				else if (configurationType==MSPOWERPOINT)
					sTuff = "<a:r><a:t xml:space=\"preserve\">"+tf.toString()+"</a:t></a:r>"; // DWH 4-8-09
				else
					sTuff = tf.toString();
			}
			else
				sTuff = tf.toString();
			if ( encoderManager == null ) {
				if ( layer == null ) {
					return sTuff; // DWH 4-8-09 replaced tf.toString() with sTuff
				}
				else {
					return layer.encode(sTuff, context); // DWH 4-8-09 replaced tf.toString() with sTuff
				}
			}
			else {
				if ( layer == null ) {
					return encoderManager.encode(sTuff, context); // DWH 4-8-09 replaced tf.toString() with sTuff
				}
				else {
					return layer.encode(
						encoderManager.encode(sTuff, context), context); // DWH 4-8-09 replaced tf.toString() with sTuff
				}
			}
		}

		// Output text with in-line codes
		List<Code> codes = tf.getCodes();
		StringBuilder tmp = new StringBuilder();
		String text = tf.getCodedText();
		Code code;
		char ch;
		for ( int i=0; i<text.length(); i++ ) {
			ch = text.charAt(i);
			switch ( ch ) {
			case TextFragment.MARKER_OPENING:
				if (context==1 && bInBlankText && (nSurroundingCodes<=0)) { // DWH 4-13-09 whole if
					bInBlankText = false;
					if (configurationType==MSWORD)
						tmp.append(encody("</w:t></w:r>",context));
					else if (configurationType==MSPOWERPOINT)
						tmp.append(encody("</a:t></a:r>",context));
				}
				code = codes.get(TextFragment.toIndex(text.charAt(++i)));
				tmp.append(expandCodeContent(code, langToUse, context));
				nSurroundingCodes++;
				break;
			case TextFragment.MARKER_CLOSING:
				if (context==1 && bInBlankText && (nSurroundingCodes<=0)) { // DWH 4-13-09 whole if
					bInBlankText = false;
					if (configurationType==MSWORD)
						tmp.append(encody("</w:t></w:r>",context));
					else if (configurationType==MSPOWERPOINT)
						tmp.append(encody("</a:t></a:r>",context));
				}
				code = codes.get(TextFragment.toIndex(text.charAt(++i)));
				tmp.append(expandCodeContent(code, langToUse, context));
				nSurroundingCodes--;
				break;
			case TextFragment.MARKER_ISOLATED:
			case TextFragment.MARKER_SEGMENT:
				if (context==1 && bInBlankText && (nSurroundingCodes<=0)) { // DWH 4-13-09 whole if
					bInBlankText = false;
					if (configurationType==MSWORD)
						tmp.append(encody("</w:t></w:r>",context));
					else if (configurationType==MSPOWERPOINT)
						tmp.append(encody("</a:t></a:r>",context));
				}
				code = codes.get(TextFragment.toIndex(text.charAt(++i)));
				if (code.getTagType()==TextFragment.TagType.OPENING)
					nSurroundingCodes++;
				else if (code.getTagType()==TextFragment.TagType.CLOSING)
					nSurroundingCodes--;
				tmp.append(expandCodeContent(code, langToUse, context));
				break;
			default:
				if (context==1 && !bInBlankText && (nSurroundingCodes<=0)) { // DWH 4-13-09 whole if
					bInBlankText = true;
					if (configurationType==MSWORD)
//						tmp.append(encody("<w:r><w:t xml:space=\"preserve\">",context));
						tmp.append(encody("<w:r><w:t xml:space=\"preserve\">",context));
					else if (configurationType==MSPOWERPOINT)
//						tmp.append(encody("<a:r><a:t xml:space=\"preserve\">",context));
						tmp.append(encody("<a:r><a:t>",context));
				}
				if ( Character.isHighSurrogate(ch) ) {
					int cp = text.codePointAt(i);
					i++; // Skip low-surrogate
					if ( encoderManager == null ) {
						if ( layer == null ) {
							tmp.append(new String(Character.toChars(cp)));
						}
						else {
							tmp.append(layer.encode(cp, context));
						}
					}
					else {
						if ( layer == null ) {
							tmp.append(encoderManager.encode(cp, context));
						}
						else {
							tmp.append(layer.encode(
								encoderManager.encode(cp, context),
								context));
						}
					}
				}
				else { // Non-supplemental case
					if ( encoderManager == null ) {
						if ( layer == null ) {
							tmp.append(ch);
						}
						else {
							tmp.append(layer.encode(ch, context));
						}
					}
					else {
						if ( layer == null ) {
							tmp.append(encoderManager.encode(ch, context));
						}
						else {
							tmp.append(layer.encode(
								encoderManager.encode(ch, context),
								context));
						}
					}
				}
				break;
			}
		}
		if (context==1 && bInBlankText && (nSurroundingCodes<=0)) { // DWH 4-13-09 whole if
			bInBlankText = false;
			if (configurationType==MSWORD)
				tmp.append(encody("</w:t></w:r>",context));
			else if (configurationType==MSPOWERPOINT)
				tmp.append(encody("</a:t></a:r>",context));
		}
		return tmp.toString();
	}
	private String encody(String s, int context)
	{
		if ( encoderManager == null ) {
			if ( layer == null ) {
				return s; // DWH 4-8-09 replaced tf.toString() with sTuff
			}
			else {
				return layer.encode(s, context); // DWH 4-8-09 replaced tf.toString() with sTuff
			}
		}
		else {
			if ( layer == null ) {
				return encoderManager.encode(s, context); // DWH 4-8-09 replaced tf.toString() with sTuff
			}
			else {
				return layer.encode(
					encoderManager.encode(s, context), context); // DWH 4-8-09 replaced tf.toString() with sTuff
			}
		}
	}
}
