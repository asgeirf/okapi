/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.tokenization.common;

import java.util.logging.Level;

import net.sf.okapi.lib.extra.Component;
import net.sf.okapi.steps.tokenization.tokens.Tokens;

public abstract class AbstractLexer extends Component implements ILexer {

	private boolean cancelled = false;
	//private int lexerId = 0;
	private LexerRules rules;
		
	protected abstract void lexer_init();
	protected abstract boolean lexer_hasNext();
	protected abstract Lexem lexer_next();
	protected abstract void lexer_open(String text, String language, Tokens tokens);
	
	public AbstractLexer() {
		
		super();
		
		Class<? extends LexerRules> rulesClass = lexer_getRulesClass();
		if (rulesClass == null) return;
		
		try {
			rules = rulesClass.newInstance();
			
		} catch (InstantiationException e) {

			logMessage(Level.FINE, "Lexer rules instantialion falied: " + e.getMessage());
			
		} catch (IllegalAccessException e) {
			
			logMessage(Level.FINE, "Lexer rules instantialion falied: " + e.getMessage());
		}
		
		
//		String rulesClassName = ClassUtil.qualifyName(this, "Rules");
//		
//		if (Util.isEmpty(rulesClassName)) return;
//		
//		try {
//			rules = (LexerRules) Class.forName(rulesClassName).newInstance();
//			
//		} catch (InstantiationException e) {
//			
//			logMessage(Level.FINE, "Lexer rules instantialion falied: " + e.getMessage());
//			
//		} catch (IllegalAccessException e) {
//			
//			logMessage(Level.FINE, "Lexer rules instantialion falied: " + e.getMessage());
//			
//		} catch (ClassNotFoundException e) {
//			
//			logMessage(Level.FINE, "Lexer rules instantialion falied: " + e.getMessage());
//		}
		
//		System.out.println(rulesClassName);
		
//		System.out.println(this.getClass().getPackage());
//		System.out.println(this.getClass().getSimpleName());
	}
	
	public void init() {
	
		lexer_init();
	}
	
	public void cancel() {
		
		cancelled = true;
	}

	public void close() {				
	}

//	public int getLexerId() {
//		
//		return lexerId;
//	}

	public LexerRules getRules() {

		return rules;
	}
	
	public boolean hasNext() {
		
		return !cancelled && lexer_hasNext();
	}
	
	public Lexem next() {
		
		if (cancelled) return null;
		
		return lexer_next();
	}
	
	public void open(String text, String language, Tokens tokens) {
		
		cancelled = false;
		
		lexer_open(text, language, tokens);
	}

//	public void setLexerId(int lexerId) {
//		
//		this.lexerId = lexerId;
//	}

	public void setRules(LexerRules rules) {
		
		this.rules = rules;
	}

	protected Class<? extends LexerRules> lexer_getRulesClass() {
		
		return LexerRules.class;
	}
}
