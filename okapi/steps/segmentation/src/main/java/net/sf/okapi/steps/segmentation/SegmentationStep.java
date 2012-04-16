/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.segmentation;

import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.lib.segmentation.SRXDocument;
import net.sf.okapi.steps.segmentation.Parameters.SegmStrategy;

@UsingParameters(Parameters.class)
public class SegmentationStep extends BasePipelineStep {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private Parameters params;
	private ISegmenter srcSeg;
	private ISegmenter trgSeg;
	private LocaleId sourceLocale;
	private LocaleId targetLocale;
	private boolean initDone;
	private String rootDir;
	private String inputRootDir;

	public SegmentationStep () {
		params = new Parameters();
		srcSeg = null;
	}
	
	@StepParameterMapping(parameterType = StepParameterType.SOURCE_LOCALE)
	public void setSourceLocale (LocaleId sourceLocale) {
		this.sourceLocale = sourceLocale;
	}
	
	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale (LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}
	
	@StepParameterMapping(parameterType = StepParameterType.ROOT_DIRECTORY)
	public void setRootDirectory (String rootDir) {
		this.rootDir = rootDir;
	}
	
	@StepParameterMapping(parameterType = StepParameterType.INPUT_ROOT_DIRECTORY)
	public void setInputRootDirectory (String inputRootDir) {
		this.inputRootDir = inputRootDir;
	}

	public String getName () {
		return "Segmentation";
	}

	public String getDescription () {
		return "Apply SRX segmentation to the text units content of a document. "
			+ "Expects: filter events. Sends back: filter events.";
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	protected Event handleStartBatch (Event event) {
		initDone = false;
		return event;
	}
	
	@Override
	protected Event handleStartBatchItem (Event event) {
		if ( initDone ) return event; // Initialize once per batch
		SRXDocument srxDoc = new SRXDocument();
		String src = null;
		if ( params.segmentSource ) {
			src = Util.fillRootDirectoryVariable(params.getSourceSrxPath(), rootDir);
			src = Util.fillInputRootDirectoryVariable(src, inputRootDir);			
			srxDoc.loadRules(src);
			if ( srxDoc.hasWarning() ) {
				logger.warning(srxDoc.getWarning());
			}
			// Change trimming options if requested
			if ( params.trimSrcLeadingWS != Parameters.TRIM_DEFAULT ) {
				srxDoc.setTrimLeadingWhitespaces(params.trimSrcLeadingWS==Parameters.TRIM_YES);
			}
			if ( params.trimSrcTrailingWS != Parameters.TRIM_DEFAULT ) {
				srxDoc.setTrimTrailingWhitespaces(params.trimSrcTrailingWS==Parameters.TRIM_YES);
			}
			// Instantiate the segmenter
			srcSeg = srxDoc.compileLanguageRules(sourceLocale, null);
		}
		if ( params.segmentTarget ) {
			String trg = Util.fillRootDirectoryVariable(params.getTargetSrxPath(), rootDir);
			trg = Util.fillInputRootDirectoryVariable(trg, inputRootDir);
			// Load target SRX only if different from sources
			if ( Util.isEmpty(src) || !src.equals(trg) ) {
				srxDoc.loadRules(trg);
				if ( srxDoc.hasWarning() ) {
					logger.warning(srxDoc.getWarning());
				}
				if ( srxDoc.hasWarning() ) logger.warning(srxDoc.getWarning());
			}
			// Change trimming options if requested
			if ( params.trimTrgLeadingWS != Parameters.TRIM_DEFAULT ) {
				srxDoc.setTrimLeadingWhitespaces(params.trimTrgLeadingWS==Parameters.TRIM_YES);
			}
			if ( params.trimTrgTrailingWS != Parameters.TRIM_DEFAULT ) {
				srxDoc.setTrimTrailingWhitespaces(params.trimTrgTrailingWS==Parameters.TRIM_YES);
			}
			// Instantiate the segmenter
			trgSeg = srxDoc.compileLanguageRules(targetLocale, null);
		}		
		
		return event;
	}
	
	@Override
	protected Event handleStartDocument (Event event) {
		if ( params.segmentSource || params.segmentTarget ) {
			// Possibly force the output segmentation, but only if we do any segmentation
			if ( params.getForcesegmentedOutput() ) {
				// Force to show the segments when possible
				IParameters prm = event.getStartDocument().getFilterParameters();
				if ( prm != null ) {
					prm.setInteger("outputSegmentationType", 3);
				}
			}
		}
		return event;
	}
	
	@Override
	protected Event handleTextUnit (Event event) {
		ITextUnit tu = event.getTextUnit();
		// Skip non-translatable
		if ( !tu.isTranslatable() ) return event;
		// Nothing to do
		if ( !params.segmentSource && !params.segmentTarget ) return event;  

		// Segment source if requested
		if ( params.segmentSource ) {
			if ( params.getSegmentationStrategy() == SegmStrategy.OVERWRITE_EXISTING || 
					!tu.getSource().hasBeenSegmented() ) {
				tu.createSourceSegmentation(srcSeg);
			}
			else if (params.getSegmentationStrategy() == SegmStrategy.DEEPEN_EXISTING) {
				// Has been segmented or not (if unsegmented, it's still 1 segment)
				deepenSegmentation(tu.getSource(), srcSeg);
			}
		}
		
		if (targetLocale != null) {
			TextContainer trgCont = tu.getTarget(targetLocale);
	
			// Segment target if requested
			if ( params.segmentTarget && ( trgCont != null )) {
				if ( params.getSegmentationStrategy() == SegmStrategy.OVERWRITE_EXISTING ||
						!trgCont.hasBeenSegmented() ) {
					trgSeg.computeSegments(trgCont);
					trgCont.getSegments().create(trgSeg.getRanges());
				}
				else if (params.getSegmentationStrategy() == SegmStrategy.DEEPEN_EXISTING) {
					// Has been segmented or not (if unsegmented, it's still 1 segment)
					deepenSegmentation(trgCont, trgSeg);
				}
			}
			
			// Make sure we have target content if needed, segmentation is incurred by the variant source 
			if ( params.copySource ) {
				trgCont = tu.createTarget(targetLocale, false, IResource.COPY_ALL);
			}
	
			// If requested, verify that we have one-to-one match
			// This is needed only if we do have a target
			if ( params.checkSegments && ( trgCont != null)) {
				if ( trgCont.getSegments().count() != tu.getSource().getSegments().count() ) {
					// Not the same number of segments
					logger.warning(String.format("Text unit id='%s': Source and target do not have the same number of segments.",
						tu.getId()));
				}
				// Otherwise make sure we have matches
				else {
					ISegments trgSegs = trgCont.getSegments();
					for ( Segment seg : tu.getSource().getSegments() ) {
						if ( trgSegs.get(seg.id) == null ) {
							// No target segment matching source segment seg.id
							logger.warning(String.format("Text unit id='%s': No target match found for source segment id='%s'",
								tu.getId(), seg.id));
						}
					}
				}
			}
		}
		
		return event;
	}

	/**
	 * Iterates a given TextContainer's segments to apply segmentation rules to them.
	 * @param tc the given TextContainer
	 * @param segmenter the segmenter to perform additional segmentation for existing segments
	 */
	private void deepenSegmentation(TextContainer tc, ISegmenter segmenter) {
		if (tc == null || segmenter == null) {
			logger.severe("Parameter cannot be null");
			return;
		}
		
		// Reverse order so we can insert parts in the loop
		for (int i = tc.count() - 1; i >= 0; i--) {
			TextPart part = tc.get(i);
			if (!part.isSegment()) continue;
			
			// Part is always a segment here
			TextContainer segTc = new TextContainer(part);
			segmenter.computeSegments(segTc);
			
			// Apply segmentation, replace segment with the new list of parts
			segTc.getSegments().create(segmenter.getRanges());
			replacePart(tc, i, segTc);
		}
	}
	
	private void replacePart(TextContainer oldPartContainer, int index, 
			TextContainer newPartsContainer) {
		for (int i = newPartsContainer.count() - 1; i >= 0; i--) {
			oldPartContainer.insert(index, newPartsContainer.get(i));
		}
		// Remove the old (unsegmented) segment.
		// Do it after inserting the new segments, because if the segment is the only one in 
		// its container, it won't be removed (TC always contains at least one segment) 
		oldPartContainer.remove(index+newPartsContainer.count());
	}

}
