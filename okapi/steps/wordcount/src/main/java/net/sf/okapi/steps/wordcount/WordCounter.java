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

package net.sf.okapi.steps.wordcount;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.steps.tokenization.Tokenizer;
import net.sf.okapi.steps.tokenization.common.TokensAnnotation;
import net.sf.okapi.steps.tokenization.tokens.Tokens;
import net.sf.okapi.steps.wordcount.common.BaseCounter;
import net.sf.okapi.steps.wordcount.common.GMX;
import net.sf.okapi.steps.wordcount.common.Metrics;
import net.sf.okapi.steps.wordcount.common.MetricsAnnotation;
import net.sf.okapi.steps.wordcount.common.StructureParameters;

/**
 * Word Count engine. Contains static methods to calculate number of words in a given text fragment. 
 * 
 * @version 0.1 07.07.2009
 */

public class WordCounter extends BaseCounter {

	private static StructureParameters params;
	
	protected static void loadParameters() {
		
		if (params != null) return; // Already loaded
		
		params = new StructureParameters();
		if (params == null) return;
		
		params.loadFromResource("word_counter.tprm");
	}
	
	@Override
	protected long doCount(String text, LocaleId language) {
		
		Tokens tokens = Tokenizer.tokenize(text, language, getTokenName());		
		if (tokens == null) return 0;
		
		// DEBUG
//		System.out.println(String.format("Tokens: %d (%s)", tokens.size(), text));
//		System.out.println();
//		System.out.println(tokens.toString());
//		System.out.println();
		
		return tokens.size();
	}
	
	public static long count(TextUnit textUnit, LocaleId language) {
		return count(WordCounter.class, textUnit, language);		
	}
	
	public static long count(TextContainer textContainer, LocaleId language) {
		return count(WordCounter.class, textContainer, language);		
	}

	public static long count(TextFragment textFragment, LocaleId language) {
		return count(WordCounter.class, textFragment, language);		
	}
	
	public static long count(String string, LocaleId language) {
		return count(WordCounter.class, string, language);		
	}
	
	public static long getCount(TextUnit tu) {
		MetricsAnnotation ma = TextUnitUtil.getSourceAnnotation(tu, MetricsAnnotation.class);
		if (ma == null) return 0;
		
		Metrics m = ma.getMetrics();
		if (m == null) return 0;
		
		return m.getMetric(GMX.TotalWordCount);
	}
	
	public static long getCount(TextUnit tu, int segIndex) {
		ISegments segments = tu.getSource().getSegments();
		return getCount(segments.get(segIndex));		
	}
	
	public static long getCount(Segment segment) {
		MetricsAnnotation ma = segment.getAnnotation(MetricsAnnotation.class);
		if (ma == null) return 0;
		
		Metrics m = ma.getMetrics();
		if (m == null) return 0;
		
		return m.getMetric(GMX.TotalWordCount);		
	}
	
	public static String getTokenName() {		
		loadParameters();
		
		if (params == null) return "";
		return params.getTokenName();
	}
	
	public static void setCount(IResource res, long count) {
		MetricsAnnotation ma = res.getAnnotation(MetricsAnnotation.class);
		
		if (ma == null) {			
			ma = new MetricsAnnotation();
			res.setAnnotation(ma);
		}
		
		Metrics m = ma.getMetrics();		
		m.setMetric(GMX.TotalWordCount, count);
	}

}
