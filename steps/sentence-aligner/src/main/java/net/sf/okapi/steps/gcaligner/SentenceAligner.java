/**
 *  Copyright 2009 Welocalize, Inc. 
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

/*===========================================================================
 Additional changes Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.gcaligner;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextUnit;

/**
 * SentenceAligner aligns source and target (paragraph) {@link TextUnit}s and returns a list of aligned sentence-based
 * {@link TextUnit} objects.
 */

public class SentenceAligner {
	private static final Logger LOGGER = Logger.getLogger(SentenceAligner.class.getName());
	private static final long MAX_CELL_SIZE = 80000L;

	public List<TextUnit> align(TextUnit sourceParagraph, TextUnit targetParagraph,
			LocaleId srcLocale, LocaleId trgLocale) {
		List<TextUnit> alignedTextUnits = null;
		alignedTextUnits = alignWithoutSkeletonAlignment(sourceParagraph, targetParagraph,
				srcLocale, trgLocale);
		return alignedTextUnits;
	}

	private List<TextUnit> alignWithoutSkeletonAlignment(TextUnit sourceParagraph,
			TextUnit targetParagraph, LocaleId srcLocale, LocaleId trgLocale) {
		SegmentAlignmentFunction alignmentFunction = new SegmentAlignmentFunction(srcLocale,
				trgLocale);
		return alignSegments(sourceParagraph, targetParagraph, srcLocale, trgLocale,
				alignmentFunction);
	}

	private List<TextUnit> alignSegments(TextUnit sourceParagraph, TextUnit targetParagraph,
			LocaleId srcLocale, LocaleId trgLocale, SegmentAlignmentFunction alignmentFunction) {

		// make sure the paragraphs have been segmented
		if (!(sourceParagraph.getSource().isSegmented() || targetParagraph.getSource()
				.isSegmented())) {
			throw new OkapiBadStepInputException("Source and target TextUnits must be segmented.");
		}

		// To prevent OutOfMemory exception, simply don't perform the
		// alignment for a block with a lot of segments. TEMPORARY FIX
		if (sourceParagraph.getSource().getSegmentCount()
				* targetParagraph.getSource().getSegmentCount() > MAX_CELL_SIZE) {
			throw new IllegalArgumentException("Too many segments. Can only align "
					+ Long.toString(MAX_CELL_SIZE)
					+ ". Where the number equals the source segments times the target segments.");
		}

		DpMatrix matrix = new DpMatrix(sourceParagraph.getSource().getSegments(), targetParagraph
				.getSource().getSegments(), alignmentFunction);

		List<DpMatrixCell> result = matrix.align();

		// record the result in a list of TextUnit objects
		List<TextUnit> alignedTextUnits = new LinkedList<TextUnit>();
		
		Iterator<DpMatrixCell> it = result.iterator();
		while (it.hasNext()) {
			DpMatrixCell cell = it.next();
			if (cell.getState() == DpMatrixCell.DELETED) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				alignedTextUnits.add(Utils.makeBilingualTextUnit(sourceParagraph, sourceSegment));
			} else if (cell.getState() == DpMatrixCell.INSERTED) {
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedTextUnits.add(Utils.makeBilingualTextUnit(sourceParagraph, targetSegment,
						trgLocale));
			} else if (cell.getState() == DpMatrixCell.MATCH) {
				Segment sourceSegment = matrix.getAlignmentElementX(cell.getXindex());
				Segment targetSegment = matrix.getAlignmentElementY(cell.getYindex());
				alignedTextUnits.add(Utils.makeBilingualTextUnit(sourceParagraph, sourceSegment,
						targetSegment, trgLocale));
			} else if (cell.getState() == DpMatrixCell.MULTI_MATCH) {
				List<Segment> sourceSegments = matrix.getAlignmentElementsX(cell
						.getMultiMatchXIndexBegin(), cell.getMultiMatchXIndexEnd());
				List<Segment> targetSegments = matrix.getAlignmentElementsY(cell
						.getMultiMatchYIndexBegin(), cell.getMultiMatchYIndexEnd());
				alignedTextUnits.add(Utils.makeBilingualTextUnit(sourceParagraph, sourceSegments,
						targetSegments, trgLocale));
			}
		}

		return alignedTextUnits;
	}
}
