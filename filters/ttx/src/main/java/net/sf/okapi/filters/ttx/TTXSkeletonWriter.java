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

package net.sf.okapi.filters.ttx;

import java.util.List;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.IReferenceable;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;

public class TTXSkeletonWriter extends GenericSkeletonWriter {

	private String srcLangCode;
	private String trgLangCode;
	
	public void setSourceLanguageCode (String langCode) {
		srcLangCode = langCode;
	}
	
	public void setTargetLanguageCode (String langCode) {
		trgLangCode = langCode;
	}
	
	@Override
	public String processTextUnit (TextUnit tu) {
		if ( tu == null ) return "";
		StringBuilder tmp = new StringBuilder();
		
		TextContainer srcCont = tu.getSource();
		TextFragment srcFrag;
		if ( !srcCont.isSegmented() ) {
			srcCont = srcCont.clone();
			srcCont.createSegment(0, -1);
		}
		List<Segment> srcSegments = srcCont.getSegments();
		String text = srcCont.getCodedText();

		TextFragment trgFrag;
		List<Segment> trgSegments = null;
		if ( tu.hasTarget(outputLoc) ) {
			TextContainer trgCont = tu.getTarget(outputLoc);
			if ( !trgCont.isSegmented() ) {
				trgCont = trgCont.clone();
				trgCont.createSegment(0, -1);
			}
			trgSegments = trgCont.getSegments();
		}
		else {
			trgSegments = srcSegments;
		}

		Code code;
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_ISOLATED:
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
				tmp.append(expandCode(srcCont.getCode(text.charAt(++i))));
				break;
				
			case TextFragment.MARKER_SEGMENT:
				code = srcCont.getCode(text.charAt(++i));
				int n = Integer.valueOf(code.getData());
				// Get segments source/target
				srcFrag = srcSegments.get(n).text;
				trgFrag = trgSegments.get(n).text;
				tmp.append(processSegment(srcFrag, trgFrag));
				break;

			default:
				tmp.append(text.charAt(i));
				break;
			}
		}

		return tmp.toString();
	}
	
	private String processSegment (TextFragment srcFrag,
		TextFragment trgFrag)
	{
		if ( trgFrag == null ) { // No target available: use the source
			trgFrag = srcFrag;
		}

		StringBuilder tmp = new StringBuilder();
//TODO: Tu attributes		
		tmp.append("<Tu PercentageMatch=\"0\">");
		
		tmp.append(String.format("<Tuv Lang=\"%s\">", srcLangCode));
		tmp.append(processFragment(srcFrag));
		tmp.append("</Tuv>");
		
		tmp.append(String.format("<Tuv Lang=\"%s\">", trgLangCode));
		tmp.append(processFragment(trgFrag));
		tmp.append("</Tuv>");

		tmp.append("</Tu>");
		return tmp.toString();
	}

	private String processFragment (TextFragment frag) {
		StringBuilder tmp = new StringBuilder();
		String text = frag.getCodedText();

		// No MARKER_SEGMENT at this stage
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_ISOLATED:
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
				tmp.append(expandCode(frag.getCode(text.charAt(++i))));
				break;
				
			default:
				tmp.append(text.charAt(i));
				break;
			}
		}
		
		return tmp.toString(); 
	}

	private String expandCode (Code code) {
		if ( layer != null ) {
			return layer.startInline() 
				+ layer.encode(code.getOuterData(), 2)
				+ layer.endInline();
		}
		return code.getOuterData();
	}

}
