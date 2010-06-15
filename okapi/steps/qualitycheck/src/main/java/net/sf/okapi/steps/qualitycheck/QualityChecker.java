/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.qualitycheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.XMLWriter;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;

public class QualityChecker {

	private Parameters params;
	private LocaleId trgLoc;
	private List<Issue> issues;
	private XMLWriter repWriter;
	private ArrayList<PatternItem> patterns;

	public QualityChecker () {
		params = new Parameters();
		patterns = new ArrayList<PatternItem>();
		patterns.add(new PatternItem("[\\?\\!]", PatternItem.SAME, true));
		patterns.add(new PatternItem("%s", PatternItem.SAME, true));
	}
	
	public void initialize (LocaleId targetLocale, String rootDir) {
		this.trgLoc = targetLocale;
		issues = new ArrayList<Issue>();

		String finalPath = Util.fillRootDirectoryVariable(params.getOutputPath(), rootDir);
		repWriter = new XMLWriter(finalPath);
		repWriter.writeStartDocument();
		repWriter.writeStartElement("html");
		repWriter.writeRawXML("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />"
			+ "<title>Quality Check Report</title><style type=\"text/css\">"
			+ "body { font-family: Verdana; font-size: smaller; }"
			+ "h1 { font-size: 110%; }"
			+ "h2 { font-size: 100%; }"
			+ "h3 { font-size: 100%; }"
			+ "p.item { font-family: Courier New, courier; font-size: 100%; white-space: pre;"
			+ "   border: solid 1px; padding: 0.5em; border-color: silver; background-color: whitesmoke; }"
      		+ "pre { font-family: Courier New, courier; font-size: 100%;"
      		+ "   border: solid 1px; padding: 0.5em; border-color: silver; background-color: whitesmoke; }"
			+ "span.hi { background-color: #FFFF00; }"
			+ "</style></head>");
		repWriter.writeStartElement("body");
		repWriter.writeLineBreak();
		repWriter.writeElementString("h1", "Quality Check Report");

		// Compile the patterns
		for ( PatternItem item : patterns ) {
			if ( item.enabled ) {
				item.compile();
			}
		}
	}

	public Parameters getParameters () {
		return params;
	}
	
	public void setParameters (Parameters params) {
		this.params = params;
	}

	public List<Issue> getIssues () {
		return issues;
	}
	
	public  void processStartDocument (StartDocument sd) {
		if ( repWriter != null ) {
			repWriter.writeRawXML("<hr />");
			repWriter.writeElementString("p", "Input: "+sd.getName());
		}
	}
	
	public void processTextUnit (TextUnit tu) {
		// Skip non-translatable entries
		if ( !tu.isTranslatable() ) return;
		
		// Get the containers
		TextContainer srcCont = tu.getSource();
		TextContainer trgCont = tu.getTarget(trgLoc);
		
		// Check if we have a target (even if option disabled)
		if ( trgCont == null ) {
			// No translation available
			reportIssue(IssueType.MISSING_TARGETTU, tu, null,
				"Missing translation.",
				0, 0, 0, 0, srcCont.toString(), "");
			return;
		}
		
		tu.synchronizeSourceSegmentation(trgLoc);
		ISegments srcSegs = srcCont.getSegments();
		ISegments trgSegs = trgCont.getSegments();
		for ( Segment srcSeg : srcSegs ) {
			Segment trgSeg = trgSegs.get(srcSeg.getId());
			if ( trgSeg == null ) {
				reportIssue(IssueType.MISSING_TARGETSEG, tu, srcSeg.getId(),
					"Missing translation.",
					0, 0, 0, 0, srcSeg.toString(), "");
				continue; // Cannot go further for that segment
			}
			
			// Check code differences, if requested
			if ( params.getCodeDifference() ) {
				checkInlineCodes(srcSeg, trgSeg, tu);
			}
			
			// Check for empty target, if requested
			if ( params.getEmptyTarget() ) {
				if ( trgSeg.text.isEmpty() && !srcSeg.text.isEmpty() ) {
					reportIssue(IssueType.EMPTY_TARGETSEG, tu, srcSeg.getId(),
						"Empty translation.",
						0, 0, 0, 0, srcSeg.toString(), "");
				}
			}
			
			// Check for target is the same as source, if requested
			if ( params.getTargetSameAsSource() ) {
				if ( srcSeg.text.hasText() ) {
					if ( srcSeg.text.compareTo(trgSeg.text, params.getTargetSameAsSourceWithCodes()) == 0 ) {
						reportIssue(IssueType.TARGET_SAME_AS_SOURCE, tu, srcSeg.getId(),
							"Translation is the same as the source.",
							0, 0, 0, 0, srcSeg.toString(), trgSeg.toString());
					}
				}
			}
			
			// Check for patterns, if requested
			if ( params.getPatterns() ) {
				checkPatterns(srcSeg, trgSeg, tu);
			}
		
		}

		String srcOri = null;
		if ( srcCont.contentIsOneSegment() ) {
			srcOri = srcCont.toString();
		}
		else {
			srcOri = srcCont.getUnSegmentedContentCopy().toString();
		}
		
		String trgOri = null;
		if ( trgCont.contentIsOneSegment() ) {
			trgOri = trgCont.toString();
		}
		else {
			trgOri = trgCont.getUnSegmentedContentCopy().toString();
		}

		checkWhiteSpaces(srcOri, trgOri, tu);
	}

	public void completeProcess () {
		if ( repWriter != null ) {
			repWriter.writeEndElementLineBreak(); // body
			repWriter.writeEndElementLineBreak(); // html
			repWriter.writeEndDocument();
			repWriter.close();
		}
	}

	private void checkInlineCodes (Segment srcSeg,
		Segment trgSeg,
		TextUnit tu)
	{
		String srcCodes = srcSeg.text.getCodes().toString();
		String trgCodes = trgSeg.text.getCodes().toString();
		if ( !srcCodes.equals(trgCodes) ) {
			reportIssue(IssueType.CODE_DIFFERENCE, tu, srcSeg.getId(),
				"The translation does not have the same codes as the source.",
				0, -1, 0, -1, srcSeg.toString(), trgSeg.toString());
		}
		
//		List<Code> srcCodes = new ArrayList<Code>();
//		for ( Code code: srcSeg.text.getCodes() ) {
//			srcCodes.add(code.clone());
//		}
//		List<Code> trgCodes = new ArrayList<Code>();
//		for ( Code code: trgSeg.text.getCodes() ) {
//			trgCodes.add(code.clone());
//		}
//		
//		for ( Code srcCode : srcCodes ) {
//			for ( int j=0; j<trgCodes.size(); j++ ) {
//				if ( srcCode.getId() == trgCodes.get(j).getId() ) {
//					if ( srcCode.getTagType() == trgCodes.get(j).getTagType() ) {
//						String srcData = srcCode.getData();
//						String trgData = trgCodes.get(j).getData();
//						if (( srcData != null ) && ( trgData != null )) {
//							if ( !srcData.equals(trgData) ) {
//								
//							}
//						}
//						// Else: either one or both are null
//						if (( srcData == null ) && ( trgData == null )) {
//							continue; // No difference
//						}
//						reportIssue(IssueType.EMPTY_TARGETSEG, tu, srcSeg.getId(),
//							"Code difference: source='%s' "
//					}
//				}
//			}
//		}
	}
	
	private void checkWhiteSpaces (String srcOri,
		String trgOri,
		TextUnit tu)
	{
		// Check for leading whitespaces
		if ( params.getLeadingWS() ) {
			
			// Missing ones
			for ( int i=0; i<srcOri.length(); i++ ) {
				if ( Character.isWhitespace(srcOri.charAt(i)) ) {
					if ( srcOri.length() > i ) {
						if ( trgOri.charAt(i) != srcOri.charAt(i) ) {
							reportIssue(IssueType.MISSINGORDIFF_LEADINGWS, tu, null,
								String.format("Missing or different leading white space at position %d.", i),
								i, i+1, -1, 0, srcOri, trgOri);
							break;
						}
					}
					else {
						reportIssue(IssueType.MISSING_LEADINGWS, tu, null,
							String.format("Missing leading white space at position %d.", i),
							i, i+1, -1, 0, srcOri, trgOri);
					}
				}
				else break;
			}

			// Extra ones
			for ( int i=0; i<trgOri.length(); i++ ) {
				if ( Character.isWhitespace(trgOri.charAt(i)) ) {
					if ( srcOri.length() > i ) {
						if ( srcOri.charAt(i) != trgOri.charAt(i) ) {
							reportIssue(IssueType.EXTRAORDIFF_LEADINGWS, tu, null,
								String.format("Extra or different leading white space at position %d.", i),
								-1, 0, i, i+1, srcOri, trgOri);
							break;
						}
					}
					else {
						reportIssue(IssueType.EXTRA_LEADINGWS, tu, null,
							String.format("Extra leading white space at position %d.", i),
							-1, 0, i, i+1, srcOri, trgOri);
					}
				}
				else break;
			}
		}
		
		// Check for trailing whitespaces
		if ( params.getTrailingWS() ) {

			// Missing ones
			int j = trgOri.length()-1;
			for ( int i=srcOri.length()-1; i>=0; i-- ) {
				if ( Character.isWhitespace(srcOri.charAt(i)) ) {
					if ( j >= 0 ) {
						if ( trgOri.charAt(j) != srcOri.charAt(i) ) {
							reportIssue(IssueType.MISSINGORDIFF_TRAILINGWS, tu, null,
								String.format("Missing or different trailing white space at position %d", i),
								i, i+1, -1, 0, srcOri, trgOri);
							break;
						}
					}
					else {
						reportIssue(IssueType.MISSING_TRAILINGWS, tu, null,
							String.format("Missing trailing white space at position %d.", i),
							i, i+1, -1, 0, srcOri, trgOri);
					}
				}
				else break;
				j--;
			}

			// Extra ones
			j = srcOri.length()-1;
			for ( int i=trgOri.length()-1; i>=0; i-- ) {
				if ( Character.isWhitespace(trgOri.charAt(i)) ) {
					if ( j >= 0 ) {
						if ( srcOri.charAt(j) != trgOri.charAt(i) ) {
							reportIssue(IssueType.EXTRAORDIFF_TRAILINGWS, tu, null,
								String.format("Extra or different trailing white space at position %d.", i),
								-1, 0, i, i+1, srcOri, trgOri);
							break;
						}
					}
					else {
						reportIssue(IssueType.EXTRA_TRAILINGWS, tu, null,
							String.format("Extra white trailing space at position %d.", i),
							-1, 0, i, i+1, srcOri, trgOri);
					}
				}
				else break;
				j--;
			}
		}
		
	}


	public void checkPatterns (Segment srcSeg,
		Segment trgSeg,
		TextUnit tu)
	{
		// Get the source text
		String srcCText = srcSeg.text.getCodedText();
		
		// Search for any enabled pattern in the source
		for ( PatternItem item : patterns ) {
			// Skip disabled items
			if ( !item.enabled ) continue;
			
			Matcher srcM = item.getSourcePattern().matcher(srcCText);
			
			// Use a copy for the target: it may get modified for the search
			StringBuilder trgCText = new StringBuilder(trgSeg.text.getCodedText());

			while ( srcM.find() ) {
				// Get the source text corresponding to the match
				String srcPart = srcCText.substring(srcM.start(), srcM.end());
				int start, end;
				boolean bFound = false;
				// Try to get the corresponding part in the target
				if ( item.target.equals(PatternItem.SAME) ) {
					// If the target pattern is defined as being the same as the source
					// Look for the same text in the source.
					bFound = ((start = trgCText.indexOf(srcPart)) != -1);
					end = start + srcPart.length();
				}
				else { // Target part has its own pattern
					Matcher trgM = item.getTargetPattern().matcher(trgCText);
					bFound = trgM.find();
					start = trgM.start();
					end = trgM.end();
				}
				// Process result
				if ( bFound ) { // Remove that match in case source has several occurrences to match
					trgCText.delete(start, end);
				}
				else { // Generate an issue
					reportIssue(IssueType.MISSING_PATTERN, tu, srcSeg.getId(),
						String.format("The source part '%s' has no correspondance in the target.", srcPart),
						srcM.start(), -1, start, -1,
						srcSeg.toString(), trgSeg.toString());
				}
			}
			
		}
	}

	private void reportIssue (IssueType issueType,
		TextUnit tu,
		String segId,
		String message,
		int srcStart,
		int srcEnd,
		int trgStart,
		int trgEnd,
		String srcOri,
		String trgOri)
	{
		Issue issue = new Issue(issueType, tu.getId(), segId, message, srcStart, srcEnd, trgStart, trgEnd);
		issues.add(issue);

		if ( repWriter != null ) {
			String position = String.format("ID=%s", tu.getId());
			if ( tu.getName() != null ) {
				position += (" ("+tu.getName()+")");
			}
			if ( segId != null ) {
				position += String.format(", segment=%s", segId);
			}
			repWriter.writeElementString("p", position+": "+issue.message);
			
			repWriter.writeRawXML("<p class=\"item\">");
			repWriter.writeString("Source: ["+srcOri+"]");
			repWriter.writeRawXML("<br />");
			repWriter.writeString("Target: ["+trgOri+"]");
			repWriter.writeRawXML("</p>");

			repWriter.writeLineBreak();
		}
	}

}
