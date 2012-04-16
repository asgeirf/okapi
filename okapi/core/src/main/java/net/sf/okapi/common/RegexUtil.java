/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of helper function for working with regular expressions.
 */
public class RegexUtil {
	
	private static final Pattern QUOTED_AREA = Pattern.compile("\\\\Q(.+?)\\\\E");
	private static final Pattern BACKREF_PATTERN = Pattern.compile("\\\\([1-9][0-9]*)");
	
	private static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();

	public static Pattern getPattern(String regex) {		
		Pattern pattern = patternCache.get(regex);
		if (pattern == null) {
			
			pattern = Pattern.compile(regex);
			patternCache.put(regex, pattern);
		}
		
		return pattern;
	}
	
	public static String replaceAll(String string, Pattern pattern, int group, String replacement) {	
	    Matcher matcher = pattern.matcher(string);
	    
	    // Replace all occurrences of pattern in input
	    StringBuilder buf = new StringBuilder();
	    
	    int start = 0;
	    int end = 0;
	    
	    while (matcher.find()) {	    
	        start = matcher.start(group);
	        if (start != -1) { // The group might not present in the match
	        	buf.append(string.substring(end, start));
		        buf.append(replacement);
		        end = matcher.end(group);
	        }	        
	    }
	    
	    buf.append(string.substring(end));
	    return buf.toString();
	}
	
	public static String replaceAll(String string, String regex, int group, String replacement) {		
		return replaceAll(string, getPattern(regex), group, replacement);
	}
	
	public static int countMatches(String string, String regex) {				
	    return countMatches(string, regex, 0);
	}
	
	public static int countMatches(String string, String regex, int matchLen) {	
		Pattern pattern = getPattern(regex);
	    Matcher matcher = pattern.matcher(string);
	    
	    int count = 0;
	    
	    while (matcher.find())
	    	if (matchLen == 0)
	    		count++;
	    	else
	    		count += string.substring(matcher.start(0), matcher.end(0)).length() / matchLen;
	    
	    return count;
	}
	
	public static int countLeadingQualifiers(String string, String qualifier) {		
		return countMatches(string, qualifier + "+\\b", qualifier.length());
	}
	
	public static int countTrailingQualifiers(String string, String qualifier) {		
		return countMatches(string, "\\b" + qualifier + "+", qualifier.length());
	}

	/**
	 * Escapes a given string for regex.
	 * @param str the given string
	 * @return escaped string
	 */
	public static String escape(String str) {
		str = str.replace("[", "\\[");
		str = str.replace("]", "\\]");
		str = str.replace("\"", "\\\"");
		str = str.replace("^", "\\^");
		str = str.replace("$", "\\$");
		str = str.replace(".", "\\.");
		str = str.replace("|", "\\|");
		str = str.replace("?", "\\?");
		str = str.replace("*", "\\*");
		str = str.replace("+", "\\+");
		str = str.replace("(", "\\(");
		str = str.replace(")", "\\)");
		str = str.replace("{", "\\{");
		str = str.replace("}", "\\}");
        
        return str;
	}

	public static boolean matches(String st, Pattern pattern) {
		Matcher matcher = pattern.matcher(st);
		return matcher.matches();
	}
	
	public static List<Range> getQuotedAreas(String regex) {
		List<Range> quotedAreas = new ArrayList<Range>();
		// Determine areas between \Q and \E
		Matcher m = QUOTED_AREA.matcher(regex);
		while(m.find()) {
			quotedAreas.add(new Range(m.start(1), m.end(1) - 1));
		}
		return quotedAreas;
	}
	
	private static boolean isQuotedArea(int pos, List<Range> quotedAreas) {
		for (Range area : quotedAreas) {
			if (area.contains(pos))	return true;
		}
		return false;
	}
	
	public static int getGroupAtPos(String regex, int position) {
		int group = 0;
		int maxGroup = 0;
		boolean ignoreNext = false;
		List<Range> quotedAreas = getQuotedAreas(regex);
		
		String searchSt = regex.substring(0, position);
		for (int i = 0; i < searchSt.length(); i++) {
			if (ignoreNext) {
				ignoreNext = false;
				continue;
			}
			char ch = regex.charAt(i);
			if (ch == '\\') {
				ignoreNext = !isQuotedArea(i, quotedAreas);				
			}				
			else {				
				if (ch == '(' && !ignoreNext) {
					group = ++maxGroup;
				}
				// Group numbers are assigned based on left parenthesis
				else if (ch == ')' && !ignoreNext) {
					group--;
				} 
			}				
		}
		return group;
	}
	
	/**
	 * Adjust values in back references to capturing groups (like \1) of a given regex.
	 * This method needs to be called when a new group is added to a regex,
	 * and the regex contains back references to existing groups.
	 * Values in the references having number equal or greater than groupNum, should
	 * be increased by 1.
	 * @param regex the given regex containing back references to capturing groups. 
	 * @param groupNum the number of the new group.
	 * @return the given regex with updated references.
	 */
	public static String updateGroupReferences(String regex, int groupNum) {
		Matcher m = BACKREF_PATTERN.matcher(regex);
		List<String> values = new ArrayList<String>();
		
		// Collect group values in a length-sorted list
		while(m.find()) {
			String n = m.group(1);
			values.add(n);				
		}
		
		// Sorted by string length, not by int value
		Collections.sort(values, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.length() < o2.length()) return 1;
				if (o1.length() > o2.length()) return -1;
				return 0;
			}				
		});
		
		// Replace group numbers starting from the longest to shortest to not replace 
		// parts of longer values
		for (String value : values) {
			int oldValue = Integer.valueOf(value);
			if (oldValue < groupNum - 1) continue;
			
			int newValue = oldValue + 1;
			regex = regex.replace("\\" + value, String.format("\\%d", newValue));
		}
		
		return regex;
	}
}
