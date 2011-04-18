/**===========================================================================
 Additional changes Copyright (C) 2009-2011 by the Okapi Framework contributors
 ===========================================================================*/
/*  Copyright 2009 Welocalize, Inc. 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package net.sf.okapi.steps.gcaligner;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.AlignedPair;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnitUtil;

/**
 * SentenceAligner aligns source and target (paragraph) {@link TextUnit}s and returns a list of aligned sentence-based
 * {@link TextUnit} objects.
 */

public class SentenceAligner {
	private static final Logger LOGGER = Logger.getLogger(SentenceAligner.class.getName());
	
	private static final long MAX_CELL_SIZE = 80000L;

	/*
	 * TODO: set value for what we consider low scoring matches 
	 */
	//private static final int LOW_SCORE_THRESHOLD = 0;

	public ITextUnit align(ITextUnit sourceParagraph, ITextUnit targetParagraph, LocaleId srcLocale,
			LocaleId trgLocale) {
		return alignWithoutSkeletonAlignment(sourceParagraph, targetParagraph, srcLocale, trgLocale);
	}

	public ITextUnit align(ITextUnit bilingualParagraph, LocaleId srcLocale, LocaleId trgLocale) {
		return alignWithoutSkeletonAlignment(bilingualParagraph, srcLocale, trgLocale);
	}

	private ITextUnit alignWithoutSkeletonAlignment(ITextUnit sourceParagraph,
			ITextUnit targetParagraph, LocaleId srcLocale, LocaleId trgLocale) {
		SegmentAlignmentFunction alignmentFunction = new SegmentAlignmentFunction(srcLocale,
				trgLocale);
		return alignSegments(sourceParagraph, targetParagraph, srcLocale, trgLocale,
				alignmentFunction);
	}

	private ITextUnit alignWithoutSkeletonAlignment(ITextUnit bilingualParagraph, LocaleId srcLocale,
			LocaleId trgLocale) {
		SegmentAlignmentFunction alignmentFunction = new SegmentAlignmentFunction(srcLocale,
				trgLocale);
		return alignSegments(bilingualParagraph, srcLocale, trgLocale, alignmentFunction);
	}

	private ITextUnit alignSegments(ITextUnit sourceParagraph, ITextUnit targetParagraph,
			LocaleId srcLocale, LocaleId trgLocale, SegmentAlignmentFunction alignmentFunction) {

		// To prevent OutOfMemory exception, simply don't perform the
		// alignment for a block with a lot of segments. TEMPORARY FIX
		if (sourceParagraph.getSource().getSegments().count()
				* targetParagraph.getSource().getSegments().count() > MAX_CELL_SIZE) {
			throw new IllegalArgumentException("Too many segments. Can only align "
					+ Long.toString(MAX_CELL_SIZE)
					+ ". Where the number equals the source segments times the target segments.");
		}

		DpMatrix matrix = new DpMatrix(sourceParagraph.getSource().getSegments().asList(),
				targetParagraph.getSource().getSegments().asList(), alignmentFunction);

		List<DpMatrixCell> result = matrix.align();

		// record the result in a list of AlignedPairs
		List<AlignedPair> alignedPairs = new LinkedList<AlignedPair>();

		String srcTuid = sourceParagraph.getName();				
		Iterator<DpMatrixCell> it = result.iterator();
		while (it.hasNext()) {
			DpMatrixCell cell = it.next();
			if (cell.getState() == DpMatrixCell.DELETED) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				alignedPairs.add(new AlignedPair(sourceSegment, null, trgLocale));
				LOGGER.warning(sourceSegment.toString() + 
						"\nTarget segment deleted (TU ID: " + srcTuid + "): Non 1-1 match. Please confirm alignment.");
			} else if (cell.getState() == DpMatrixCell.INSERTED) {
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedPairs.add(new AlignedPair(null, targetSegment, trgLocale));
				LOGGER.warning(targetSegment.toString() + 
						"\nSource segment deleted (TU ID: " + srcTuid + "): Non 1-1 match. Please confirm alignment.");
			} else if (cell.getState() == DpMatrixCell.MATCH) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedPairs.add(new AlignedPair(sourceSegment, targetSegment, trgLocale));
			} else if (cell.getState() == DpMatrixCell.MULTI_MATCH) {
				List<Segment> sourceSegments = matrix.getAlignmentElementsX(
						cell.getMultiMatchXIndexBegin(), cell.getMultiMatchXIndexEnd());
				List<Segment> targetSegments = matrix.getAlignmentElementsY(
						cell.getMultiMatchYIndexBegin(), cell.getMultiMatchYIndexEnd());
				alignedPairs.add(new AlignedPair(new LinkedList<TextPart>(sourceSegments),
						new LinkedList<TextPart>(targetSegments), trgLocale));
				Segment s = null;
				try {
					s = sourceSegments.get(0);
				} catch (IndexOutOfBoundsException e) {
					s = targetSegments.get(0);
				}
				LOGGER.warning(s.toString() 
						+ "\nMulti-Segment Match (TU ID: " + srcTuid + "): Non 1-1 match. Please confirm alignment.");
			}
		}
		
		sourceParagraph.getAlignedSegments().align(alignedPairs, trgLocale);
		return sourceParagraph;
	}

	private ITextUnit alignSegments(ITextUnit bilingualParagraph, LocaleId srcLocale,
			LocaleId trgLocale, SegmentAlignmentFunction alignmentFunction) {

		// To prevent OutOfMemory exception, simply don't perform the
		// alignment for a block with a lot of segments. TEMPORARY FIX
		if (bilingualParagraph.getSource().getSegments().count()
				* bilingualParagraph.getTarget(trgLocale, false).getSegments().count() > MAX_CELL_SIZE) {
			throw new IllegalArgumentException("Too many segments. Can only align "
					+ Long.toString(MAX_CELL_SIZE)
					+ ". Where the number equals the source segments times the target segments.");
		}

		DpMatrix matrix = new DpMatrix(bilingualParagraph.getSource().getSegments().asList(),
				bilingualParagraph.getTarget(trgLocale, false).getSegments().asList(), alignmentFunction);

		List<DpMatrixCell> result = matrix.align();

		// record the result in a list of AlignedPairs
		List<AlignedPair> alignedPairs = new LinkedList<AlignedPair>();

		Iterator<DpMatrixCell> it = result.iterator();
		while (it.hasNext()) {
			DpMatrixCell cell = it.next();
			if (cell.getState() == DpMatrixCell.DELETED) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				alignedPairs.add(new AlignedPair(sourceSegment, null, trgLocale));
			} else if (cell.getState() == DpMatrixCell.INSERTED) {
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedPairs.add(new AlignedPair(null, targetSegment, trgLocale));
			} else if (cell.getState() == DpMatrixCell.MATCH) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedPairs.add(new AlignedPair(sourceSegment, targetSegment, trgLocale));
			} else if (cell.getState() == DpMatrixCell.MULTI_MATCH) {
				List<Segment> sourceSegments = matrix.getAlignmentElementsX(
						cell.getMultiMatchXIndexBegin(), cell.getMultiMatchXIndexEnd());
				List<Segment> targetSegments = matrix.getAlignmentElementsY(
						cell.getMultiMatchYIndexBegin(), cell.getMultiMatchYIndexEnd());
				alignedPairs.add(new AlignedPair(new LinkedList<TextPart>(sourceSegments),
						new LinkedList<TextPart>(targetSegments), trgLocale));
			}
		}

		bilingualParagraph.getAlignedSegments().align(alignedPairs, trgLocale);
		return bilingualParagraph;
	}
}
