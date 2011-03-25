/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.wordcount.categorized.gmx;

import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.steps.wordcount.common.AltAnnotationBasedCountStep;
import net.sf.okapi.steps.wordcount.common.GMX;

/**
 * M11: No step provides the metrics yet. 
 *
 */
public class GMXRepetitionMatchedWordCountStep extends AltAnnotationBasedCountStep {

	public static final String METRIC = GMX.RepetitionMatchedWordCount; 
		
	@Override
	protected String getMetric() {
		return METRIC;
	}

	@Override
	public String getDescription() {
		return "An accumulation of the word count for repeating text units that have not been matched in any " +
				"other form. Repetition matching is deemed to take precedence over fuzzy matching."		
		+ " Expects: filter events. Sends back: filter events.";
	}

	@Override
	public String getName() {
		return "GMX Repetition Matched Word Count";
	}

	@Override
	protected boolean accept(MatchType type) {
		return false; // TODO Implement accept(), probably change the superclass
	}

//	@Override
//	protected long count(Segment segment, LocaleId locale) {
//		return super.count(segment, locale) - (BaseCounter.getCount(segment, GMX.ExactMatchedWordCount) + 
//				BaseCounter.getCount(segment, GMX.LeveragedMatchedWordCount));
//	}
//
//	@Override
//	protected long count(TextContainer textContainer, LocaleId locale) {
//		long res = super.count(textContainer, locale) - (BaseCounter.getCount(textContainer, GMX.ExactMatchedWordCount) + 
//				BaseCounter.getCount(textContainer, GMX.LeveragedMatchedWordCount)); 
//		return res > 0 ? res : 0;
//	}
}
