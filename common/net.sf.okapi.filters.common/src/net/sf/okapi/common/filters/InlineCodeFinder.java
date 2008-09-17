/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel                                          */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.filters;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.okapi.common.FieldsString;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment.TagType;

/**
 * Implements the methods needed to convert sections of a coded text
 * into in-line codes.
 */
public class InlineCodeFinder {

	private ArrayList<String>     rules;
	private String                sample;
	private Pattern               pattern;

	
	public InlineCodeFinder () {
		reset();
	}

	public void reset () {
		rules = new ArrayList<String>();
		sample = "";
	}
	
	@Override
	public InlineCodeFinder clone () {
		InlineCodeFinder tmp = new InlineCodeFinder();
		tmp.setSample(sample);
		tmp.getRules().addAll(getRules());
		return tmp;
	}

	/**
	 * Adds a new rule to the list.
	 * @param pattern The regular expression pattern for the rule.
	 */
	public void addRule (String pattern) {
		rules.add(pattern);
	}

	/**
	 * Gets a list of all rules.
	 * @return The list of all rules.
	 */
	public ArrayList<String> getRules () {
		return rules;
	}
	
	/**
	 * Gets the sample text to go with the finder.
	 * @return The sample text.
	 */
	public String getSample () {
		return sample;
	}
	
	/**
	 * Sets the sample text that can be used to check the rules
	 * in a regular expression editor.
	 * @param value The sample text.
	 */
	public void setSample (String value) {
		sample = value;
	}
	
	/**
	 * Compiles all the rules into a single compiled pattern.
	 * @throws PatternSyntaxException When there is a syntax error in one of the rules. 
	 */
	public void compile () {
		StringBuilder tmp = new StringBuilder();
		for ( String rule : rules ) {
			if ( tmp.length() > 0 ) tmp.append("|");
			tmp.append("("+rule+")");
		}
		pattern = Pattern.compile(tmp.toString(), Pattern.MULTILINE);
	}

	/**
	 * Applies the rules to a given content and converts all matching sections
	 * into in-line codes.
	 * @param fragment The fragment where to apply the rules.
	 */
	public void process (TextContainer fragment) {
		String tmp = fragment.getCodedText();
		Matcher m = pattern.matcher(tmp);
		int start = 0;
		int diff = 0;
		while ( m.find(start) ) {
			diff += fragment.changeToCode(m.start()+diff, m.end()+diff,
				TagType.PLACEHOLDER, null);
			start = m.end();
			// Check the case where the last match was at the end
			// which makes the next start invalid for find().
			if ( start >= tmp.length() ) break;
		}
	}

	@Override
	public String toString () {
		FieldsString tmp = new FieldsString();
		tmp.add("count", rules.size());
		int i = 0;
		for ( String rule : rules ) {
			tmp.add(String.format("rule%d", i), rule);
			i++;
		}
		tmp.add("sample", sample);
		return tmp.toString();
	}
	
	public void fromString (String data) {
		FieldsString tmp = new FieldsString(data);
		reset();
		int count = tmp.get("count", 0);
		for ( int i=0; i<count; i++ ) {
			String rule = tmp.get(String.format("rule%d", i), "");
			if ( rule.length() > 0 ) rules.add(rule);
		}
		sample = tmp.get("sample", sample);
	}
}
