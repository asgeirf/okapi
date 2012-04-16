/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.segmentation;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.sf.okapi.common.StringUtil;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;

class Placeholder {
	
	// Plane 0 PUA: U+E000 to U+F8FF (6,400 code locations)
	// Plane 15 PUA: U+F0000 to U+FFFFD (65,534 code locations)
	// Plane 16 PUA: U+100000 to U+10FFFD (65,534 code locations)
	
	static final char GRAPHEME_CLUSTER = 0xE010; // Grapheme cluster placeholder
	static final char WORD_BOUNDARY = 0xE011; // Word boundary placeholder
	static final char WORD_NON_BOUNDARY = 0xE012; // Word non-boundary placeholder
	static final char START_BEFORE = 0xE013;
	static final char END_BEFORE = 0xE014;
	static final char START_AFTER = 0xE015;
	static final char END_AFTER = 0xE016;
	static final char BASE = 0xE020; // Base for other placeholders
	
	static final String AREA_MARKERS = String.format("[%s%s%s%s]", 
					START_BEFORE, END_BEFORE, START_AFTER, END_AFTER);
	
	// GC - http://source.icu-project.org/repos/icu/icu/tags/release-4-8-1-1/source/data/brkitr/char.txt
	private static final String GC_RBBI_RULES =
		"$CR          = [\\p{Grapheme_Cluster_Break = CR}];" +
		"$LF          = [\\p{Grapheme_Cluster_Break = LF}];" +
		"$Control     = [\\p{Grapheme_Cluster_Break = Control}];" +
		"$Extend      = [\\p{Grapheme_Cluster_Break = Extend}];" +
		"$SpacingMark = [\\p{Grapheme_Cluster_Break = SpacingMark}];" +
		"$L       = [\\p{Grapheme_Cluster_Break = L}];" +
		"$V       = [\\p{Grapheme_Cluster_Break = V}];" +
		"$T       = [\\p{Grapheme_Cluster_Break = T}];" +
		"$LV      = [\\p{Grapheme_Cluster_Break = LV}];" +
		"$LVT     = [\\p{Grapheme_Cluster_Break = LVT}];" +
		"!!chain;" +
		"!!forward;" +
		"$CR $LF;" +
		"$L ($L | $V | $LV | $LVT) {700};" +
		"($LV | $V) ($V | $T) {700};" +
		"($LVT | $T) $T {700};" +
		"[^$Control $CR $LF] $Extend {700};" +
		"[^$Control $CR $LF] $SpacingMark {700};" +
		"!!reverse;" +
		"!!safe_reverse;" +
		"!!safe_forward;";
	
	private static final String RBBI_RULES =
		"!!chain;" +
		"!!forward;" +
		"%s{%d};" + // Slot for pattern
		"!!reverse;" +
		"!!safe_reverse;" +
		"!!safe_forward;";
	
	private static final RuleBasedBreakIterator GC_RBBI = 
			new RuleBasedBreakIterator(GC_RBBI_RULES);
	
	private int index;
	private int lexemId;
	private char value;
	private Set<Character> charSet = new TreeSet<Character>();
	private Pattern phPattern;
	private RuleBasedBreakIterator iterator;
				
	public static Placeholder createGraphemeCluster(int lexemId) {
		return new Placeholder(GRAPHEME_CLUSTER, GC_RBBI, lexemId);
	}
	
	public static RuleBasedBreakIterator createPhIterator(String pattern, int lexemId) {
		return new RuleBasedBreakIterator(String.format(RBBI_RULES, pattern, lexemId));
	}
	
	public Placeholder(char value, RuleBasedBreakIterator iterator, int lexemId) {
		this.index = -1;
		this.lexemId = lexemId;
		setValue(value);
		setIterator(iterator);
	}
	
	public Placeholder(int index, RuleBasedBreakIterator iterator, int lexemId) {
		this.index = index;
		this.lexemId = lexemId;
		setValue((char) (BASE + index));
		setIterator(iterator);
	}
	
	private void setIterator(RuleBasedBreakIterator iterator) {
		if (iterator == null) {
			throw new RuntimeException("Iterator not set for placeholder"); 
		}
		this.iterator = iterator;		
	}

	private void setValue(char value) {
		this.value = value;
		phPattern = Pattern.compile(toString());
	}
	
	public int getIndex() {
		return index;
	}
	
	private void addChars(String chars) {
		for (char c : chars.toCharArray()) {
			charSet.add(c);
		}
	}
	
	public String getChars() {
		return escapeSpecChars(StringUtil.charsToString(charSet));
	}
	
	private String escapeSpecChars(String chars) {
		// TODO ? wrap with \Q..\E
		chars = chars.replace("\\", "\\\\)");
		chars = chars.replace("[", "\\[");
		chars = chars.replace("]", "\\]");
		chars = chars.replace("(", "\\(");
		chars = chars.replace(")", "\\)");				
		return chars;
//		return Util.isEmpty(chars)? "" : String.format("\\Q%s\\E", chars);
	}

	public Pattern getPhPattern() {
		return phPattern;
	}
	
	@Override
	public String toString() {
		return Character.toString(value);		
	}
	
	public RuleBasedBreakIterator getIterator() {
		return iterator;
	}

	public void processText(String codedText) {
		int start = 0;
		int end = 0;
		
		iterator.setText(codedText);
		start = iterator.first();
		end = start;
//		System.out.println("------------------ RBBI rules: " + iterator.toString());
		//StringBuilder sb = new StringBuilder();
		
		for(;;) {
			end = iterator.next();
			if (end == BreakIterator.DONE) break;
			if (start >= end) break;
			
			int status = iterator.getRuleStatus();
//			System.out.println(String.format("%d: %s", status, 
//					codedText.substring(start, end)));
			
			if (status == lexemId) {
				//sb.append(StringUtil.getString(end - start, GRAPHEME_CLUSTER));
				String match = codedText.substring(start, end);
				addChars(match);
			}
			
			start = end;
		}
	}
}
