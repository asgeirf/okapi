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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

/**
 * Implements the {@link ISegmenter} interface for SRX rules.
 */
public class SRXSegmenter implements ISegmenter {
	
	private boolean segmentSubFlows;
	private boolean cascade;
	private boolean includeStartCodes;
	private boolean includeEndCodes;
	private boolean includeIsolatedCodes;
	private LocaleId currentLanguageCode;
	private boolean oneSegmentIncludesAll; // Extension
	private boolean trimLeadingWS; // Extension
	private boolean trimTrailingWS; // Extension
	private boolean useJavaRegex; // Extension
	private boolean trimCodes; // Extension
	private ArrayList<CompiledRule> rules;
	private Pattern maskRule; // Extension
	private TreeMap<Integer, Boolean> splits;
	private List<Integer> finalSplits;
	private ArrayList<Integer> starts;
	private ArrayList<Integer> ends;
	private ICURegex icuRegex;

	/**
	 * Creates a new SRXSegmenter object.
	 */
	public SRXSegmenter () {
		icuRegex = new ICURegex();
		reset();
	}

	/**
	 * Resets the options to their defaults, and the compiled rules
	 * to nothing.
	 */
	public void reset () {
		currentLanguageCode = null;
		rules = new ArrayList<CompiledRule>();
		maskRule = null;
		splits = null;
		segmentSubFlows = true; // SRX default
		cascade = false; // There is no SRX default for this
		includeStartCodes = false; // SRX default
		includeEndCodes = true; // SRX default
		includeIsolatedCodes = false; // SRX default
		oneSegmentIncludesAll = false; // Extension
		trimLeadingWS = false; // Extension IN TEST (was true for StringInfo)
		trimTrailingWS = false; // Extension IN TEST (was true for StringInfo)
		useJavaRegex = false; // Extension
		trimCodes = false; // Extension IN TEST (was false for StringInfo) NOT USED for now
		icuRegex.reset();
	}

	/**
	 * Sets the options for this segmenter.
	 * @param segmentSubFlows true to segment sub-flows, false to no segment them.
	 * @param includeStartCodes true to include start codes just before a break in the 'left' segment,
	 * false to put them in the next segment. 
	 * @param includeEndCodes  true to include end codes just before a break in the 'left' segment,
	 * false to put them in the next segment.
	 * @param includeIsolatedCodes true to include isolated codes just before a break in the 'left' segment,
	 * false to put them in the next segment.
	 * @param oneSegmentIncludesAll true to include everything in segments that are alone.
	 * @param trimLeadingWS true to trim leading white-spaces from the segments, false to keep them.
	 * @param trimTrailingWS true to trim trailing white-spaces from the segments, false to keep them.
	 * @param useJavaRegex true if the rules are for the Java regula expression engine, false if they are for ICU. 
	 */
	public void setOptions (boolean segmentSubFlows,
		boolean includeStartCodes,
		boolean includeEndCodes,
		boolean includeIsolatedCodes,
		boolean oneSegmentIncludesAll,
		boolean trimLeadingWS,
		boolean trimTrailingWS,
		boolean useJavaRegex)
	{
		this.segmentSubFlows = segmentSubFlows;
		this.includeStartCodes = includeStartCodes;
		this.includeEndCodes = includeEndCodes;
		this.includeIsolatedCodes = includeIsolatedCodes;
		this.oneSegmentIncludesAll = oneSegmentIncludesAll;
		this.trimLeadingWS = trimLeadingWS;
		this.trimTrailingWS = trimTrailingWS;
		this.useJavaRegex = useJavaRegex;
	}
	
	/**
	 * Indicates if, when there is a single segment in a text, it should include
	 * the whole text (no spaces or codes trim left/right)
	 * @return true if a text with a single segment should include the whole
	 * text.
	 */
	public boolean oneSegmentIncludesAll () {
		return oneSegmentIncludesAll;
	}

	/**
	 * Indicates if sub-flows must be segmented.
	 * @return true if sub-flows must be segmented, false otherwise.
	 */
	public boolean segmentSubFlows () {
		return segmentSubFlows;
	}
	
	/**
	 * Indicates if cascading must be applied when selecting the rules for 
	 * a given language pattern.
	 * @return true if cascading must be applied, false otherwise.
	 */
	public boolean cascade () {
		return cascade;
	}
	
	/**
	 * Indicates if leading white-spaces should be left outside the segments.
	 * @return true if the leading white-spaces should be trimmed.
	 */
	public boolean trimLeadingWhitespaces () {
		return trimLeadingWS;
	}
	
	/**
	 * Indicates if trailing white-spaces should be left outside the segments.
	 * @return true if the trailing white-spaces should be trimmed.
	 */
	public boolean trimTrailingWhitespaces () {
		return trimTrailingWS;
	}
	
	/**
	 * Indicates if this document has rules that are defined for the Java regular expression engine (vs ICU).
	 * @return true if the rules are for the Java regular expression engine, false if they are for ICU.
	 */
	public boolean useJavaRegex () {
		return useJavaRegex;
	}
	
	/**
	 * Sets the indicator that tells if this document has rules that are defined for the Java regular expression engine (vs ICU).
	 * @param useJavaRegex true if the rules should be treated as Java regular expression, false for ICU.
	 */
	public void setUseJavaRegex (boolean useJavaRegex) {
		this.useJavaRegex = useJavaRegex;
	}
	
	/**
	 * Indicates if start codes should be included (See SRX implementation notes).
	 * @return true if they should be included, false otherwise.
	 */
	public boolean includeStartCodes () {
		return includeStartCodes;
	}
	
	/**
	 * Indicates if end codes should be included (See SRX implementation notes).
	 * @return true if they should be included, false otherwise.
	 */
	public boolean includeEndCodes () {
		return includeEndCodes;
	}
	
	/**
	 * Indicates if isolated codes should be included (See SRX implementation notes).
	 * @return true if they should be included, false otherwise.
	 */
	public boolean includeIsolatedCodes () {
		return includeIsolatedCodes;
	}
	
	@Override
	public int computeSegments (String text) {
		TextContainer tmp = new TextContainer(text);
		return computeSegments(tmp);
	}
	
	@Override
	public int computeSegments (TextContainer container) {
		if ( currentLanguageCode == null ) {
			// Need to call selectLanguageRule()
			throw new SegmentationRuleException("No language defined for the segmenter.");
		}
		
		// Do we have codes?
		// Avoid to create an un-segmented copy if we can
		boolean hasCode;
		if ( container.contentIsOneSegment() ) hasCode = container.getSegments().getFirstContent().hasCode();
		else hasCode = container.getUnSegmentedContentCopy().hasCode();
		
		// Set the flag for trimming or not the in-line codes
		boolean isSCWS = (trimCodes ? !includeStartCodes : false); 
		boolean isECWS = (trimCodes ? !includeEndCodes : false); 
		boolean isICWS = (trimCodes ? !includeIsolatedCodes : false); 

		// Build the list of split positions
		// Get the coded text for the whole content
		String codedText = container.getCodedText();
		if (!useJavaRegex) icuRegex.processText(codedText, rules);

		splits = new TreeMap<Integer, Boolean>();
		Matcher m;
		for ( CompiledRule rule : rules ) {
			
			m = rule.pattern.matcher(codedText);
			while ( m.find() ) {
				int n = m.start()+m.group(1).length();
				if ( n >= codedText.length() ) continue; // Match the end m.group() m.end() String.format("%4X", (int) codedText.charAt(1))
				
				// Already a match: Per SRX algorithm, we use the first one only
				// see http://www.gala-global.org/oscarStandards/srx/srx20.html#Struct_classdefinitions
				if ( splits.containsKey(n) ) continue;
				if (!useJavaRegex && !icuRegex.verifyPos(n, rule, m)) continue;
				
				// Else add a split marker
				splits.put(n, rule.isBreak);
			}
		}
		
		if (!useJavaRegex) 
			codedText = container.getCodedText(); // restore codedText after word breaks
		
		// Set the additional split positions for mask-rules
		if ( maskRule != null ) {
			m = maskRule.matcher(codedText);
			while ( m.find() ) {
				// Remove any existing marker inside the range
				for ( int n=m.start(); n<m.end(); n++ ) {
					if ( splits.containsKey(n) ) {
						splits.remove(n);
					}
				}
				// Then set the start and end of the range as breaks
				// Don't include a split at 0 because it's an implicit one
				if ( m.start() > 0 ) splits.put(m.start(), true);
				splits.put(m.end(), true);
			}
		}
		
		// Adjust the split positions for in-line codes inclusion/exclusion options
		// And create the list of final splits at the same time
		finalSplits = new ArrayList<Integer>();
		if ( hasCode ) { // Do this only if we have in-line codes
			int finalPos;
			boolean done;
			for ( int pos : splits.keySet() ) {
				if ( !splits.get(pos) ) continue; // Skip non-break positions
				// Walk back through all sequential codes before the break
				finalPos = pos;
				done = false;
				while (( finalPos > 1 ) && !done ) {
					switch ( codedText.charAt(finalPos-2) ) {
					case (char)TextFragment.MARKER_OPENING:
					case (char)TextFragment.MARKER_CLOSING:
					case (char)TextFragment.MARKER_ISOLATED:
						finalPos-=2;
						break;
					default:
						done = true;
						break;
					}
				}
				// Now finalPos points to the first code in the sequence before the break.
				// Check, from that point to the break, if the break needs to change position
				done = false;
				while (( finalPos < pos ) && !done ) {
					switch ( codedText.charAt(finalPos) ) {
					case (char)TextFragment.MARKER_OPENING:
						if ( includeStartCodes ) finalPos+=2;
						else done = true;
						break;
					case (char)TextFragment.MARKER_CLOSING:
						if ( includeEndCodes ) finalPos+=2;
						else done = true;
						break;
					case (char)TextFragment.MARKER_ISOLATED:
						if ( includeIsolatedCodes ) finalPos+=2;
						else done = true;
						break;
					default:
						done = true;
						break;
					}
				}
				// Store the updated position
				finalSplits.add(finalPos);
			}
		}
		else { // Just copy the real splits
			for ( int pos : splits.keySet() ) {
				if ( splits.get(pos) ) finalSplits.add(pos);
			}
		}
		
		// Now build the lists of start and end of each segment
		// but trim them of any white-spaces.
		// Deal also with including or not the in-line codes.
		starts = new ArrayList<Integer>();
		ends = new ArrayList<Integer>();
		int textEnd;
		int textStart = 0;
		int trimmedTextStart;
		for ( int pos : finalSplits ) {
			// Trim white-spaces and codes as required at the front
			trimmedTextStart = TextFragment.indexOfFirstNonWhitespace(codedText,
				textStart, pos-1, isSCWS, isECWS, isICWS, trimLeadingWS);
			if ( trimmedTextStart == -1 ) { //pos-1 ) {
				// Only spaces in the segment: Continue with the next position
				continue;
			}
			if ( trimLeadingWS || trimCodes ) textStart = trimmedTextStart;
			// Trim white-spaces and codes as required at the back
			if ( trimTrailingWS || trimCodes ) {
				textEnd = TextFragment.indexOfLastNonWhitespace(codedText,
					pos-1, 0, isSCWS, isECWS, isICWS, trimTrailingWS);
			}
			else textEnd = pos-1;
			if ( textEnd >= textStart ) { // Only if there is something // was > only
				if ( textEnd < pos ) textEnd++; // Adjust for +1 position
				starts.add(textStart);
				ends.add(textEnd);
			}
			textStart = pos;
		}
		// Last one
		int lastPos = codedText.length();
		if ( textStart < lastPos ) {
			// Trim white-spaces and codes as required at the front
			trimmedTextStart = TextFragment.indexOfFirstNonWhitespace(codedText, textStart,
				lastPos-1, isSCWS, isECWS, isICWS, trimLeadingWS);
			if ( trimLeadingWS || trimCodes  ) {
				if ( trimmedTextStart != -1 ) textStart = trimmedTextStart;
			}
			if (( trimmedTextStart != -1 ) && ( trimmedTextStart < lastPos )) {
				// Trim white-spaces and code as required at the back
				if ( trimTrailingWS || trimCodes ) {
					textEnd = TextFragment.indexOfLastNonWhitespace(codedText, lastPos-1,
						textStart, isSCWS, isECWS, isICWS, trimTrailingWS);
				}
				else textEnd = lastPos-1;
				if ( textEnd >= textStart ) { // Only if there is something
					if ( textEnd < lastPos ) textEnd++; // Adjust for +1 position
					starts.add(textStart);
					ends.add(textEnd);
				}
			}
		}

		// Check for single-segment text case
		if (( starts.size() == 1 ) && ( oneSegmentIncludesAll )) {
			starts.set(0, 0);
			ends.clear(); // lastPos is added just after
		}

		// Add an extra value in ends to hold the total length of the coded text
		// to avoid having to re-create it when segmenting.
		ends.add(lastPos);
		
		// Return the number of segment found
		// (ends contains one extra value, so make sure to use starts for this)
		return starts.size();
	}
	
	@Override
	public Range getNextSegmentRange (TextContainer container) {
//TODO: Is this method really needed now that we assume no-incremental segmentation?		
return null;		
//		String text = container.getCodedText();
//		int start = 0;
//		if ( container.isSegmented() ) {
//			// Find the last segment marker in the main coded text
//			for ( int i=text.length()-1; i>=0; i-- ) {
//				if ( text.charAt(i) == TextFragment.MARKER_SEGMENT ) {
//					start = i+2; // Just after the marker
//					break;
//				}
//			}
//		}
//
//		// Do we have reach the end?
//		if ( start >= text.length() ) return null;
//
//		//TODO: implement same trimming at computeSegments()		
//		
//		// Else: search for next segment
//		Matcher m;
//		CompiledRule rule;
//		int end = -1;
//		int pos = start;
//		for ( int i=0; i<rules.size(); i++ ) {
//			rule = rules.get(i);
//			m = rule.pattern.matcher(text.substring(pos));
//			if ( m.find() ) {
//				if ( !rule.isBreak ) {
//					pos = pos+m.start()+m.group(1).length()+1;
//					if ( pos == text.length() ) break;
//					i = 0; // Look at all rules again
//				}
//				else {
//					end = pos+m.start()+m.group(1).length();
//					break;
//				}
//			}
//		}
//
//		// If not found: take all the remainder as the fragment
//		if ( end < 0 ) end = text.length()-1;
////TODO: implement same trimming at computeSegments()		
//		// Trim the white-spaces at the front of the segment
//		while ( true ) {
//			if ( start > end ) break;
//			if ( Character.isWhitespace(text.charAt(start)) ) start++;
//			else break;
//		}
//
//		// Trim the white-spaces and required codes at the end of the segment
//		end = TextFragment.indexOfLastNonWhitespace(text, end, start,
//			!includeStartCodes, !includeEndCodes, !includeIsolatedCodes, trimTrailingWS);
//		
//		// Adjust for +1 position (it's a range)
//		if ( end == -1 ) return null;
//		else end++;
//
//		// Return the range
//		if ( start == end ) return null;
//		return new Range(start, end);
	}

	@Override
	public List<Integer> getSplitPositions () {
		
		if ( finalSplits == null ) {
			finalSplits = new ArrayList<Integer>();
		}
		return Collections.unmodifiableList(finalSplits);
	}

	@Override
	public List<Range> getRanges () {
		ArrayList<Range> list = new ArrayList<Range>();
		if ( starts == null ) return null;
		for ( int i=0; i<starts.size(); i++ ) {
			list.add(new Range(starts.get(i), ends.get(i)));
		}
		return Collections.unmodifiableList(list);
	}
	
	@Override
	public LocaleId getLanguage () {
		return currentLanguageCode;
	}

	/**
	 * Sets the language used to apply the rules.
	 * @param languageCode Code of the language to use to apply the rules.
	 */
	protected void setLanguage (LocaleId languageCode) {
		currentLanguageCode = languageCode;
		icuRegex.setLanguage(languageCode);
	}
	
	/**
	 * Sets the flag indicating if cascading must be applied when selecting the 
	 * rules for a given language pattern.
	 * @param value true if cascading must be applied, false otherwise.
	 */
	protected void setCascade (boolean value) {
		cascade = value;
	}

	/**
	 * Adds a compiled rule to this segmenter.
	 * @param compiledRule the compiled rule to add.
	 */
	protected void addRule (CompiledRule compiledRule) {
		rules.add(compiledRule);
	}
	
	/**
	 * Sets the pattern for the mask rule.
	 * @param pattern the new pattern to use for the mask rule.
	 */
	protected void setMaskRule (String pattern) {
		if (( pattern != null ) && ( pattern.length() > 0 ))
			maskRule = Pattern.compile(pattern);
		else
			maskRule = null;
	}

	public ICURegex getICURegex() {
		return icuRegex;
	}

}
