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

package net.sf.okapi.steps.tokenization;

import java.util.Arrays;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.steps.tokenization.common.TokensAnnotation;
import net.sf.okapi.steps.tokenization.locale.LocaleUtil;
import net.sf.okapi.steps.tokenization.tokens.Tokens;

/**
 * 
 * 
 * @version 0.1 08.07.2009
 */

public class Tokenizer {

	protected static TokenizationStep ts = new TokenizationStep();
	
	/**
	 * Extracts tokens from the given text.
	 * @param text Text to tokenize.
	 * @param language Language of the text.
	 * @param tokenNames Optional list of token names. If omitted, all tokens will be extracted.
	 * @return A list of Token objects.
	 */
	protected Tokens tokenizeString(String text, String language, String... tokenNames) {
			
		Tokens res = new Tokens();
		
		if (ts == null)	return res;
		
		language = LocaleUtil.normalizeLanguageCode_Okapi(language); 
			
		Parameters params = (Parameters) ts.getParameters();
		params.reset();
		
		params.tokenizeSource = true;
		params.tokenizeTargets = false;
		
		params.setLanguageMode(Parameters.LANGUAGES_ONLY_WHITE_LIST);
		params.setLanguageWhiteList(ListUtil.stringAsList(language));
		
		if (Util.isEmpty(tokenNames))			
			params.setTokenMode(Parameters.TOKENS_ALL);
			
		else {
		
			params.setTokenMode(Parameters.TOKENS_SELECTED);
			//params.setTokenNames(ListUtil.stringAsList(tokenNames.toString()));
			params.setTokenNames(Arrays.asList(tokenNames));
		}
					
		ts.handleEvent(new Event(EventType.START_BATCH)); // Calls component_init();
		
		StartDocument startDoc = new StartDocument("tokenization");
		startDoc.setLanguage(language);
		startDoc.setMultilingual(false);		
		Event event = new Event(EventType.START_DOCUMENT, startDoc);		
		ts.handleEvent(event);
				
		TextUnit tu = TextUnitUtil.buildTU(text);
		event = new Event(EventType.TEXT_UNIT, tu);		
		ts.handleEvent(event);
		
		// Move tokens from the event's annotation to result
		TokensAnnotation ta = TextUnitUtil.getSourceAnnotation(tu, TokensAnnotation.class);
		if (ta != null)
			res.addAll(ta.getTokens());
		
		ts.handleEvent(new Event(EventType.END_BATCH)); // Calls component_done();
		
		return res;
	}
	
	static private Tokens doTokenize(Object text, String language, String... tokenNames) {
		
		if (text == null) return null;
		if (Util.isEmpty(language)) return null;
		
		if (text instanceof TextUnit) {
		
			TextUnit tu = (TextUnit) text;
			
			if (tu.hasTarget(language))
				return doTokenize(tu.getTarget(language), language, tokenNames);
			else
				return doTokenize(tu.getSource(), language, tokenNames);
		}
		else if (text instanceof TextContainer) {
			
			TextContainer tc = (TextContainer) text;
			
			return doTokenize(tc.getContent(), language, tokenNames);
			
		}
		else if (text instanceof TextFragment) {
			
			TextFragment tf = (TextFragment) text;
			
			return doTokenize(TextUnitUtil.getText(tf), language, tokenNames);
		}
		else if (text instanceof String) {
			
			Tokenizer tokenizer = new Tokenizer();
			if (tokenizer == null) return null;
				
			return tokenizer.tokenizeString((String) text, language, tokenNames);
		}
		
		return null;		
	}
	
	static public Tokens tokenize(TextUnit textUnit, String language, String... tokenNames) {
		
		return doTokenize(textUnit, language, tokenNames);		
	}
	
	static public Tokens tokenize(TextContainer textContainer, String language, String... tokenNames) {
		
		return doTokenize(textContainer, language, tokenNames);		
	}
	
	static public Tokens tokenize(TextFragment textFragment, String language, String... tokenNames) {
		
		return doTokenize(textFragment, language, tokenNames);		
	}
	
	static public Tokens tokenize(String string, String language, String... tokenNames) {
		
		return doTokenize(string, language, tokenNames);		
	}
}
