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

package net.sf.okapi.lib.extra.steps;

import java.util.logging.Logger;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;

public class TextUnitLogger extends BasePipelineStep {

	private final Logger logger = Logger.getLogger(getClass().getName());
	private StringBuilder sb;
	private LocaleId srcLoc;
	
	@Override
	public String getName() {
		return "Text Unit Logger";
	}

	@Override
	public String getDescription() {
		return "Logs Text Unit resources going through the pipeline.";
	}
	
	@Override
	protected Event handleStartBatch(Event event) {
		sb = new StringBuilder("\n\n");
		return super.handleStartBatch(event);
	}

	@Override
	protected Event handleStartDocument(Event event) {
		StartDocument sd = (StartDocument) event.getResource();
		srcLoc = sd.getLocale();
		return super.handleStartDocument(event);
	}
	
	@Override
	protected Event handleTextUnit(Event event) {
		ITextUnit tu = event.getTextUnit();
		sb.append("---------------------------------\n");
		fillSB(sb, tu, srcLoc);
		return super.handleTextUnit(event);
	}
	
	@Override
	protected Event handleEndBatch(Event event) {
		logger.info(sb.toString());
		return super.handleEndBatch(event);
	}
	
	private static void fillSB(StringBuilder sb, ITextUnit tu, LocaleId srcLoc) {
		sb.append("tu [" + tu.getId() + "]");
		sb.append(":");
		if (tu.isReferent()) sb.append(" referent");
		sb.append("\n");
		
		if (tu.getAnnotations() != null) {
//			sb.append("             ");
//			sb.append("Source annotations:");
//			sb.append("\n");
			for (IAnnotation annot : tu.getAnnotations()) {
				sb.append("                    ");
				sb.append(annot.getClass().getName());
				sb.append(" ");
				sb.append(annot.toString());
				sb.append("\n");
			}		
		}
		
		if (tu.getSkeleton() != null) {
			sb.append(String.format("      Skeleton: %s", tu.getSkeleton().toString()));
			sb.append("\n");
		}		
		
		sb.append(String.format("      Source (%s): %s", srcLoc, tu.getSource()));
		sb.append("\n");
		
		TextContainer source = tu.getSource();
		if (source.getAnnotations() != null) {
//			sb.append("             ");
//			sb.append("Source annotations:");
//			sb.append("\n");
			for (IAnnotation annot : source.getAnnotations()) {
				sb.append("                    ");
				sb.append(annot.getClass().getName());
				sb.append(" ");
				sb.append(annot.toString());
				sb.append("\n");
			}		
		}
		
		ISegments segs = source.getSegments(); 
		for (Segment seg : segs) {
			sb.append(String.format("         %s: %s\n            %s", seg.getId(), 
					seg.getContent().toText(), seg.getContent().toString()));
			sb.append("\n");
			if (seg.getContent().getCodes() != null) {
				sb.append(String.format("         %s codes (%d): %s", seg.getId(), 
						seg.getContent().getCodes().size(), seg.getContent().getCodes().toString()));
				sb.append("\n");
			}
			
			if (seg.getAnnotations() != null) {
//				sb.append("Source annotations:");
//				sb.append("\n");
				for (IAnnotation annot : seg.getAnnotations()) {
					sb.append("                    ");
					sb.append(annot.getClass().getName());
					sb.append(" ");
					sb.append(annot.toString());
					sb.append("\n");
				}		
			}
		}
		
		for (LocaleId locId : tu.getTargetLocales()) {
			sb.append(String.format("      Target (%s): %s", locId.toString(), tu.getTarget(locId)));
			sb.append("\n");
			
			TextContainer target = tu.getTarget(locId);
			if (source.getAnnotations() != null) {
//				sb.append("             ");
//				sb.append("Target annotations:");
//				sb.append("\n");
				for (IAnnotation annot : target.getAnnotations()) {
					sb.append("                    ");
					sb.append(annot.getClass().getName());
					sb.append(" ");
					sb.append(annot.toString());
					sb.append("\n");
				}		
			}
			
			segs = target.getSegments(); 
			for (Segment seg : segs) {
				sb.append(String.format("         %s: %s\n            %s", seg.getId(), 
						seg.getContent().toText(), seg.getContent().toString()));
				sb.append("\n");
				if (seg.getContent().getCodes() != null) {
					sb.append(String.format("         %s codes (%d): %s", seg.getId(), 
							seg.getContent().getCodes().size(), seg.getContent().getCodes().toString()));
					sb.append("\n");
				}
				
				if (seg.getAnnotations() != null) {
//					sb.append("Target annotations:");
//					sb.append("\n");
					for (IAnnotation annot : seg.getAnnotations()) {
						sb.append("                    ");
						sb.append(annot.getClass().getName());
						sb.append(" ");
						sb.append(annot.toString());
						sb.append("\n");
					}		
				}
			}
		}
	}
	
	public static String getTuInfo(ITextUnit tu, LocaleId srcLoc) {
		StringBuilder sb = new StringBuilder();
		fillSB(sb, tu, srcLoc);
		return sb.toString();
	}
}
