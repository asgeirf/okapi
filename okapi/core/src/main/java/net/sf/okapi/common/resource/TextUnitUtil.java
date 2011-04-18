/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.ReversedIterator;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.AltTranslation;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.SkeletonUtil;

/**
 * Helper methods to manipulate {@link TextFragment}, {@link TextContainer}, and {@link TextUnit} objects.
 */
public class TextUnitUtil {

	private static final Logger LOGGER = Logger.getLogger(TextUnitUtil.class.getName());
	
	// Segment markers
	private static final String SEG_START = "$seg_start$";
	private static final String SEG_END = "$seg_end$";
	
	// Text Part markers
	private static final String TP_START = "$tp_start$";
	private static final String TP_END = "$tp_end$";
	
	// Regex patterns for marker search
	private static final Pattern SEG_START_REGEX = Pattern.compile("\\[#\\$(.+)\\@\\%\\$seg_start\\$\\]");
	private static final Pattern SEG_END_REGEX = Pattern.compile("\\[#\\$(.+)\\@\\%\\$seg_end\\$\\]");
	
	private static final Pattern TP_START_REGEX = Pattern.compile("\\$tp_start\\$");
	private static final Pattern TP_END_REGEX = Pattern.compile("\\$tp_end\\$");
	
	private static final Pattern ANY_SEG_TP_REGEX = 
		Pattern.compile("\\[#\\$(.+)\\@\\%\\$seg_start\\$\\]|\\[#\\$(.+)\\@\\%\\$seg_end\\$\\]|\\$tp_start\\$|\\$tp_end\\$");
	
	/**
	 * Removes leading whitespaces from a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment which leading whitespaces are to be removed.
	 */
	public static void trimLeading (TextFragment textFragment) {
		trimLeading(textFragment, null);
	}

	/**
	 * Adjusts the inline codes of a new target text based on an original source.
	 * <p>This method compares an original source with a new target, and transfer the codes of the original source
	 * at their equivalent places in the new target. The text of the new target is left untouched.</p>
	 * <p>If the option alwaysCopyCodes is false, the codes are copied only if it the original source codes
	 * have references or if the new target codes are empty.
	 * @param oriSrc
	 *    the original source text fragment.
	 * @param newTrg
	 *    the new target text fragment (This is the fragment that will be adjusted).
	 * @param alwaysCopyCodes
	 *    indicates the adjustment of the codes is always done.
	 * @param addMissingCodes
	 *    indicates if codes that are in the original source but not in the new target should be
	 *    automatically added at the end of the new target copy (even if they are removable)
	 *    if there are references in the original source and/or empty codes in the new target.
	 * @param newSrc
	 *    the new source text fragment (Can be null). When available to speed up the inline code
	 *    processing in some cases.
	 * @param parent
	 *    the parent text unit (Can be null. Used for error information only).
	 * @return the newTrg parameter with its inline codes adjusted
	 */
	public static TextFragment adjustTargetCodes (TextFragment oriSrc,
		TextFragment newTrg,
		boolean alwaysCopyCodes,
		boolean addMissingCodes,
		TextFragment newSrc,
		ITextUnit parent)
	{
		List<Code> newCodes = newTrg.getCodes();
		List<Code> oriCodes = oriSrc.getCodes();
		
		// If not alwaysCopyCodes: no reason to adjust anything: use the target as-it
		// This allows targets with only code differences to be used as-it
		boolean needAdjustment = false;
		if ( !alwaysCopyCodes ) {
			// Check if we need to adjust regardless of copying the codes or not
			// For example: when we have empty codes in the destination target
			for ( Code code : newCodes ) {
				if ( !code.hasData() ) {
					needAdjustment = true;
					break;
				}
			}
			// Or when the original has references
			if ( !needAdjustment ) {
				for ( Code code : oriCodes ) {
					if ( code.hasReference() ) {
						needAdjustment = true;
						break;
					}
				}
			}
			if ( !needAdjustment ) {
				return newTrg;
			}
		}
		// If both new and original have no code, return the new fragment
		if ( !newTrg.hasCode() && !oriSrc.hasCode() ) {
			return newTrg;
		}
		
		// If the codes of the original sources and the matched one are the same: no need to adjust
		if ( newSrc != null ) {
			if ( !needAdjustment && oriCodes.toString().equals(newSrc.getCodes().toString()) ) {
				return newTrg;
			}
		}

		// Else: try to adjust
		int[] oriIndices = new int[oriCodes.size()];
		for ( int i=0; i<oriIndices.length; i++ ) oriIndices[i] = i;
		
		int done = 0;
		Code newCode, oriCode;

		for ( int i=0; i<newCodes.size(); i++ ) {
			newCode = newCodes.get(i);
			newCode.setOuterData(null); // Remove XLIFF outer codes if needed

			// Get the data from the original code (match on id)
			oriCode = null;
			for ( int j=0; j<oriIndices.length; j++ ) {
				// Do we have the same id?
				if ( oriCodes.get(j).getId() == newCode.getId() ) {
					// Do we have the same tag type?
					if ( oriCodes.get(j).getTagType() == newCode.getTagType() ) {
						if ( oriIndices[j] == -1 ) {
							// Was used already: this is a clone
							if ( !oriCodes.get(j).isCloneable() ) {
								String place = null;
								if ( parent != null ) {
									place = String.format(" (item id='%s', name='%s')",
										parent.getId(), (parent.getName()==null ? "" : parent.getName()));
								}
								LOGGER.warning(String.format("The extra code id='%d' cannot be cloned.",
									newCode.getId()) + ((place == null) ? "" : place));
							}
						}
					}
					else {
						// Same id but not the same tag-type
						// probably a ending matching on its starting
						continue; // Keep on searching
					}
					// Original code found, use it
					oriCode = oriCodes.get(j);
					oriIndices[j] = -1; // Mark it has used
					done++;
					break;
				}
			}
			
			if ( oriCode == null ) { // Not found in original (extra in target)
				if (( newCode.getData() == null ) || ( newCode.getData().length() == 0 )) {
					// Leave it like that
					String place = null;
					if ( parent != null ) {
						place = String.format(" (item id='%s', name='%s')",
							parent.getId(), (parent.getName()==null ? "" : parent.getName()));
					}
					LOGGER.warning(String.format("The extra target code id='%d' does not have corresponding data.",
						newCode.getId()) + ((place == null) ? "" : place));
				}
				// Else: This is a new code: keep it
			}
			else { // A code with same ID existed in the original
				// Get the data from the original
				newCode.setData(oriCode.getData());
				newCode.setOuterData(oriCode.getOuterData());
				newCode.setReferenceFlag(oriCode.hasReference());
			}
		}
		
		// If needed, check for missing codes in new fragment
		if ( oriCodes.size() > done ) {
			// Any index > -1 in source means it was was deleted in target
			for ( int i=0; i<oriIndices.length; i++ ) {
				if ( oriIndices[i] != -1 ) {
					Code code = oriCodes.get(oriIndices[i]);
					if ( addMissingCodes ) {
						newTrg.append(code.clone());
					}
//					else {
//						if ( !code.isDeleteable() ) {
//							String msg = String.format("The code id='%d' (%s) is missing in target.",
//								code.getId(), code.getData());
//							if ( parent != null ) {
//								msg += String.format(" (item id='%s', name='%s')", parent.getId(),
//									(parent.getName()==null ? "" : parent.getName()));
//							}
//							LOGGER.warning(msg);
//							LOGGER.info(String.format("Source='%s'\nTarget='%s'", oriSrc.toText(), newTrg.toText()));
//						}
//					}
				}
			}
		}
		return newTrg;
	}

	/**
	 * Removes leading whitespaces from a given text fragment, puts removed whitespaces to the given skeleton.
	 * 
	 * @param textFragment
	 *            the text fragment which leading whitespaces are to be removed.
	 * @param skel
	 *            the skeleton to put the removed whitespaces.
	 */
	public static void trimLeading (TextFragment textFragment,
		GenericSkeleton skel)
	{
		if (textFragment == null)
			return;
		String st = textFragment.getCodedText();
		TextFragment skelTF;

		int pos = TextFragment.indexOfFirstNonWhitespace(st, 0, -1, false, false, false, true);
		if (pos == -1) { // Whole string is whitespaces
			skelTF = new TextFragment(st);
			textFragment.setCodedText("");
		} else {
			skelTF = textFragment.subSequence(0, pos);
			textFragment.setCodedText(st.substring(pos));
		}

		if (skel == null)
			return;
		if (skelTF == null)
			return;

		st = skelTF.toText();
		if (!Util.isEmpty(st))
			skel.append(st); // Codes get removed
	}

	/**
	 * Removes trailing whitespaces from a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment which trailing whitespaces are to be removed.
	 */
	public static void trimTrailing (TextFragment textFragment) {
		trimTrailing(textFragment, null);
	}

	/**
	 * Removes trailing whitespaces from a given text fragment, puts removed whitespaces to the given skeleton.
	 * 
	 * @param textFragment
	 *            the text fragment which trailing whitespaces are to be removed.
	 * @param skel
	 *            the skeleton to put the removed whitespaces.
	 */
	public static void trimTrailing (TextFragment textFragment,
		GenericSkeleton skel)
	{
		if (textFragment == null)
			return;

		String st = textFragment.getCodedText();
		TextFragment skelTF;

		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, false, false, false, true);
		if (pos == -1) { // Whole string is whitespaces
			skelTF = new TextFragment(st);
			textFragment.setCodedText("");
		} else {
			skelTF = textFragment.subSequence(pos + 1, st.length());
			textFragment.setCodedText(st.substring(0, pos + 1));
		}

		if (skel == null)
			return;
		if (skelTF == null)
			return;

		st = skelTF.toText();
		if (!Util.isEmpty(st))
			skel.append(st); // Codes get removed
	}

	/**
	 * Indicates if a given text fragment ends with a given sub-string. <b>Trailing spaces are not counted</b>.
	 * 
	 * @param textFragment
	 *            the text fragment to examine.
	 * @param substr
	 *            the text to lookup.
	 * @return true if the given text fragment ends with the given sub-string.
	 */
	public static boolean endsWith (TextFragment textFragment,
		String substr)
	{
		if (textFragment == null)
			return false;
		if (Util.isEmpty(substr))
			return false;

		String st = textFragment.getCodedText();

		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1)
			return false;

		return st.lastIndexOf(substr) == pos - substr.length() + 1;
	}

	/**
	 * Indicates if a given text unit resource is null, or its source part is null or empty.
	 * 
	 * @param textUnit
	 *            the text unit to check.
	 * @return true if the given text unit resource is null, or its source part is null or empty.
	 */
	public static boolean isEmpty (ITextUnit textUnit) {
		return ((textUnit == null) || textUnit.getSource().isEmpty());
	}

	/**
	 * Indicates if a given text unit resource is null, or its source part is null or empty. Whitespaces are not taken
	 * into account, e.g. if the text unit contains only whitespaces, it's considered empty.
	 * 
	 * @param textUnit
	 *            the text unit to check.
	 * @return true if the given text unit resource is null, or its source part is null or empty.
	 */
	public static boolean hasSource (ITextUnit textUnit) {
		return !isEmpty(textUnit, true);
	}

	/**
	 * Indicates if a given text unit resource is null, or its source part is null or empty. Whitespaces are not taken
	 * into account, if ignoreWS = true, e.g. if the text unit contains only whitespaces, it's considered empty.
	 * 
	 * @param textUnit
	 *            the text unit to check.
	 * @param ignoreWS
	 *            if true and the text unit contains only whitespaces, then the text unit is considered empty.
	 * @return true if the given text unit resource is null, or its source part is null or empty.
	 */
	public static boolean isEmpty (ITextUnit textUnit,
		boolean ignoreWS)
	{
		return ((textUnit == null) || Util.isEmpty(getSourceText(textUnit), ignoreWS));
	}

	/**
	 * Gets the coded text of the source part of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the text unit resource which source text should be returned.
	 * @return the source part of the given text unit resource.
	 */
	public static String getSourceText (ITextUnit textUnit) {
		// if ( textUnit == null ) return "";
		// return getCodedText(textUnit.getSourceContent());
		return textUnit.getSource().getFirstContent().getCodedText();
	}

	/**
	 * Gets the coded text of the source part of a given text unit resource. If removeCodes = false, and the text
	 * contains inline codes, then the codes will be removed.
	 * 
	 * @param textUnit
	 *            the text unit resource which source text should be returned.
	 * @param removeCodes
	 *            true if possible inline codes should be removed.
	 * @return the source part of the given text unit resource.
	 */
	public static String getSourceText (ITextUnit textUnit,
		boolean removeCodes)
	{
		if (textUnit == null)
			return "";
		if (removeCodes) {
			return getText(textUnit.getSource().getFirstContent());
		} else {
			return textUnit.getSource().getFirstContent().getCodedText();
		}
	}

	/**
	 * Gets text of the target part of a given text unit resource in the given locale.
	 * 
	 * @param textUnit
	 *            the text unit resource which source text should be returned.
	 * @param locId
	 *            the locale the target part being sought.
	 * @return the target part of the given text unit resource in the given loacle, or an empty string if the text unit
	 *         doesn't contain one.
	 */
	public static String getTargetText (ITextUnit textUnit,
		LocaleId locId)
	{
		if (textUnit == null)
			return "";
		if (Util.isNullOrEmpty(locId))
			return "";

		return getCodedText(textUnit.getTarget(locId, false).getFirstContent());
	}

	/**
	 * Gets text of a given text fragment object possibly containing inline codes.
	 * 
	 * @param textFragment
	 *            the given text fragment object.
	 * @return the text of the given text fragment object possibly containing inline codes.
	 */
	public static String getCodedText (TextFragment textFragment) {
		if (textFragment == null)
			return "";
		return textFragment.getCodedText();
	}

	/**
	 * Extracts text from the given text fragment. Used to create a copy of the original string but without code
	 * markers. The original string is not stripped of code markers, and remains intact.
	 * 
	 * @param textFragment
	 *            TextFragment object with possible codes inside
	 * @param markerPositions
	 *            List to store initial positions of removed code markers. use null to not store the markers.
	 * @return The copy of the string, contained in TextFragment, but without code markers
	 */
	public static String getText (TextFragment textFragment,
		List<Integer> markerPositions)
	{
		if ( textFragment == null ) {
			return "";
		}

		String res = textFragment.getCodedText();
		if ( markerPositions != null ) {
			markerPositions.clear();
		}

		// No need to parse the text if there are no codes
		if ( !textFragment.hasCode() ) {
			return res;
		}
		
		// Collect marker positions & remove markers
		StringBuilder sb = new StringBuilder();
		int startPos = -1;
		for (int i = 0; i < res.length(); i++) {
			switch (res.charAt(i)) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				if (markerPositions != null) {
					markerPositions.add(i);
				}
				if (i > startPos && startPos >= 0) {
					sb.append(res.substring(startPos, i));
				}
				i += 1;
				startPos = -1;
//				startPos = i + 2;
//				i = startPos;
				break;
			default:
				if (startPos < 0)
					startPos = i;
			}
		}

		if (startPos < 0 && sb.length() == 0) // Whole string 
			startPos = 0; 		
		else			
			if (startPos > -1 && startPos < res.length()) {
				sb.append(res.substring(startPos));
			}

		return sb.toString();
	}

	/**
	 * Extracts text from the given text fragment. Used to create a copy of the original string but without code
	 * markers. The original string is not stripped of code markers, and remains intact.
	 * 
	 * @param textFragment
	 *            TextFragment object with possible codes inside
	 * @return The copy of the string, contained in TextFragment, but w/o code markers
	 */
	public static String getText (TextFragment textFragment) {
		return getText(textFragment, null);
	}

	/**
	 * Gets the last character of a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment to examin.
	 * @return the last character of the given text fragment, or '\0'.
	 */
	public static char getLastChar (TextFragment textFragment) {
		if (textFragment == null)
			return '\0';

		String st = textFragment.getCodedText();

		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1)
			return '\0';

		return st.charAt(pos);
	}

	/**
	 * Deletes the last non-whitespace and non-code character of a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment to examine.
	 */
	public static void deleteLastChar (TextFragment textFragment) {
		if (textFragment == null)
			return;
		String st = textFragment.getCodedText();

		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
		if (pos == -1)
			return;

		textFragment.remove(pos, pos + 1);
	}

	/**
	 * Returns the index (within a given text fragment object) of the rightmost occurrence of the specified substring.
	 * 
	 * @param textFragment
	 *            the text fragment to examine.
	 * @param findWhat
	 *            the substring to search for.
	 * @return if the string argument occurs one or more times as a substring within this object, then the index of the
	 *         first character of the last such substring is returned. If it does not occur as a substring,
	 *         <code>-1</code> is returned.
	 */
	public static int lastIndexOf (TextFragment textFragment,
		String findWhat)
	{
		if (textFragment == null)
			return -1;
		if (Util.isEmpty(findWhat))
			return -1;
		if (Util.isEmpty(textFragment.getCodedText()))
			return -1;

		return (textFragment.getCodedText()).lastIndexOf(findWhat);
	}

	/**
	 * Indicates if a given text fragment object is null, or the text it contains is null or empty.
	 * 
	 * @param textFragment
	 *            the text fragment to examine.
	 * @return true if the given text fragment object is null, or the text it contains is null or empty.
	 */
	public static boolean isEmpty (TextFragment textFragment) {
		return (textFragment == null || (textFragment != null && textFragment.isEmpty()));
	}

	/**
	 * Creates a new text unit resource based on a given text container object becoming the source part of the text
	 * unit.
	 * 
	 * @param source
	 *            the given text container becoming the source part of the text unit.
	 * @return a new text unit resource with the given text container object being its source part.
	 */
	public static ITextUnit buildTU (TextContainer source) {
		return buildTU(null, "", source, null, LocaleId.EMPTY, "");
	}

	/**
	 * Creates a new text unit resource based a given string becoming the source text of the text unit.
	 * 
	 * @param source
	 *            the given string becoming the source text of the text unit.
	 * @return a new text unit resource with the given string being its source text.
	 */
	public static ITextUnit buildTU (String source) {
		return buildTU(new TextContainer(source));
	}

	/**
	 * Creates a new text unit resource based on a given string becoming the source text of the text unit, and a
	 * skeleton string, which gets appended to the new text unit's skeleton.
	 * 
	 * @param srcPart
	 *            the given string becoming the source text of the created text unit.
	 * @param skelPart
	 *            the skeleton string appended to the new text unit's skeleton.
	 * @return a new text unit resource with the given string being its source text, and the skeleton string in the
	 *         skeleton.
	 */
	public static ITextUnit buildTU (String srcPart,
		String skelPart)
	{
		ITextUnit res = buildTU(srcPart);
		if (res == null)
			return null;

		GenericSkeleton skel = (GenericSkeleton) res.getSkeleton();
		if (skel == null)
			return null;

		skel.addContentPlaceholder(res);
		skel.append(skelPart);

		return res;
	}

	/**
	 * Creates a new text unit resource or updates the one passed as the parameter. You can use this method to create a
	 * new text unit or modify existing one (adding or modifying its fields' values).
	 * 
	 * @param textUnit
	 *            the text unit to be modified, or null to create a new text unit.
	 * @param name
	 *            name of the new text unit, or a new name for the existing one.
	 * @param source
	 *            the text container object becoming the source part of the text unit.
	 * @param target
	 *            the text container object becoming the target part of the text unit.
	 * @param locId
	 *            the locale of the target part (passed in the target parameter).
	 * @param comment
	 *            the optional comment becoming a NOTE property of the text unit.
	 * @return a reference to the original or newly created text unit.
	 */
	public static ITextUnit buildTU (ITextUnit textUnit,
		String name,
		TextContainer source,
		TextContainer target,
		LocaleId locId,
		String comment)
	{
		if (textUnit == null) {
			textUnit = new TextUnit("");
		}

		if (textUnit.getSkeleton() == null) {
			GenericSkeleton skel = new GenericSkeleton();
			textUnit.setSkeleton(skel);
		}

		if (!Util.isEmpty(name))
			textUnit.setName(name);

		if (source != null)
			textUnit.setSource(source);

		if (target != null && !Util.isNullOrEmpty(locId))
			textUnit.setTarget(locId, target);

		if (!Util.isEmpty(comment))
			textUnit.setProperty(new Property(Property.NOTE, comment));

		return textUnit;
	}

	/**
	 * Makes sure that a given text unit contains a skeleton. If there's no skeleton already attached to the text unit,
	 * a new skeleton object is created and attached to the text unit.
	 * 
	 * @param tu
	 *            the given text unit to have a skeleton.
	 * @return the skeleton of the text unit.
	 */
	public static GenericSkeleton forceSkeleton (ITextUnit tu) {
		if (tu == null)
			return null;

		GenericSkeleton skel = (GenericSkeleton) tu.getSkeleton();
		if (skel == null) {

			skel = new GenericSkeleton();			
			tu.setSkeleton(skel);
		}

		if (!SkeletonUtil.hasTuRef(skel))
			skel.addContentPlaceholder(tu);

		return skel;
	}

	/**
	 * Copies source and target text of a given text unit into a newly created skeleton. The original text unit remains
	 * intact, and plays a role of a pattern for a newly created skeleton's contents.
	 * 
	 * @param textUnit
	 *            the text unit to be copied into a skeleton.
	 * @return the newly created skeleton, which contents reflect the given text unit.
	 */
	public static GenericSkeleton convertToSkeleton (ITextUnit textUnit) {
		if (textUnit == null)
			return null;

		GenericSkeleton skel = (GenericSkeleton) textUnit.getSkeleton();

		if (skel == null)
			return new GenericSkeleton(textUnit.toString());

		List<GenericSkeletonPart> list = skel.getParts();
		if (list.size() == 0)
			return new GenericSkeleton(textUnit.toString());

		String tuRef = TextFragment.makeRefMarker("$self$");

		GenericSkeleton res = new GenericSkeleton();

		List<GenericSkeletonPart> list2 = res.getParts();

		for (GenericSkeletonPart part : list) {

			String st = part.toString();

			if (Util.isEmpty(st))
				continue;

			if (st.equalsIgnoreCase(tuRef)) {

				LocaleId locId = part.getLocale();
				if (Util.isNullOrEmpty(locId))
					res.add(TextUnitUtil.getSourceText(textUnit));
				else
					res.add(TextUnitUtil.getTargetText(textUnit, locId));

				continue;
			}

			list2.add(part);
		}

		return res;
	}

	/**
	 * Gets an annotation attached to the source part of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param type
	 *            reference to the requested annotation type.
	 * @return the annotation or null if not found.
	 */
	public static <A extends IAnnotation> A getSourceAnnotation (ITextUnit textUnit,
		Class<A> type)
	{
		if (textUnit == null)
			return null;
		if (textUnit.getSource() == null)
			return null;

		return textUnit.getSource().getAnnotation(type);
	}

	/**
	 * Attaches an annotation to the source part of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param annotation
	 *            the annotation to be attached to the source part of the text unit.
	 */
	public static void setSourceAnnotation (ITextUnit textUnit,
		IAnnotation annotation)
	{
		if (textUnit == null)
			return;
		if (textUnit.getSource() == null)
			return;

		textUnit.getSource().setAnnotation(annotation);
	}

	/**
	 * Gets an annotation attached to the target part of a given text unit resource in a given locale.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param locId
	 *            the locale of the target part being sought.
	 * @param type
	 *            reference to the requested annotation type.
	 * @return the annotation or null if not found.
	 */
	public static <A extends IAnnotation> A getTargetAnnotation (ITextUnit textUnit,
		LocaleId locId,
		Class<A> type)
	{
		if ( textUnit == null ) return null;
		if ( Util.isNullOrEmpty(locId) ) return null;
		if ( textUnit.getTarget(locId, false) == null ) return null;
		return textUnit.getTarget(locId, false).getAnnotation(type);
	}

	/**
	 * Attaches an annotation to the target part of a given text unit resource in a given language.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param locId
	 *            the locale of the target part being attached to.
	 * @param annotation
	 *            the annotation to be attached to the target part of the text unit.
	 */
	public static void setTargetAnnotation (ITextUnit textUnit,
		LocaleId locId,
		IAnnotation annotation)
	{
		if ( textUnit == null ) return;
		if ( Util.isNullOrEmpty(locId) ) return;
		if ( textUnit.getTarget(locId, false) == null ) return;
		textUnit.getTarget(locId, false).setAnnotation(annotation);
	}

	/**
	 * Sets the coded text of the un-segmented source of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param text
	 *            the text to be set.
	 */
	public static void setSourceText (ITextUnit textUnit,
		String text)
	{
		// fail fast if ( textUnit == null ) return;
		TextFragment source = textUnit.getSource().getFirstContent();
		// fail fast if ( source == null ) return;
		source.setCodedText(text);
	}

	/**
	 * Sets the coded text of the the target part of a given text unit resource in a given language.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param locId
	 *            the locale of the target part being set.
	 * @param text
	 *            the text to be set.
	 */
	public static void setTargetText (ITextUnit textUnit,
		LocaleId locId,
		String text)
	{
		// fail fast if ( textUnit == null ) return;
		// fail fast if ( Util.isNullOrEmpty(locId) ) return;
		TextFragment target = textUnit.getTarget(locId, false).getFirstContent();
		// fail fast if ( target == null ) return;
		target.setCodedText(text);
	}

	/**
	 * Removes leading and/or trailing whitespaces from the source part of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param trimLeading
	 *            true to remove leading whitespaces if there are any.
	 * @param trimTrailing
	 *            true to remove trailing whitespaces if there are any.
	 */
	public static void trimTU (ITextUnit textUnit,
		boolean trimLeading,
		boolean trimTrailing)
	{
		if (textUnit == null)
			return;
		if (!trimLeading && !trimTrailing)
			return;

		TextContainer source = textUnit.getSource();
		GenericSkeleton tuSkel = TextUnitUtil.forceSkeleton(textUnit);
		GenericSkeleton skel = new GenericSkeleton();

		if (trimLeading) {
			trimLeading(source.getFirstContent(), skel);
		}
		skel.addContentPlaceholder(textUnit);

		if (trimTrailing) {
			trimTrailing(source.getFirstContent(), skel);
		}

		int index = SkeletonUtil.findTuRefInSkeleton(tuSkel);
		if (index != -1) {
			SkeletonUtil.replaceSkeletonPart(tuSkel, index, skel);
		} else {
			tuSkel.add(skel);
		}
	}

	/**
	 * Removes from the source part of a given un-segmented text unit resource qualifiers (parenthesis, quotation marks
	 * etc.) around text. This method is useful when the starting and ending qualifiers are different.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param startQualifier
	 *            the qualifier to be removed before source text.
	 * @param endQualifier
	 *            the qualifier to be removed after source text.
	 */
	public static void removeQualifiers (ITextUnit textUnit,
		String startQualifier,
		String endQualifier)
	{
		if (textUnit == null)
			return;
		if (Util.isEmpty(startQualifier))
			return;
		if (Util.isEmpty(endQualifier))
			return;

		String st = getSourceText(textUnit);
		if (st == null)
			return;

		int startQualifierLen = startQualifier.length();
		int endQualifierLen = endQualifier.length();

		if (st.startsWith(startQualifier) && st.endsWith(endQualifier)) {

			GenericSkeleton tuSkel = TextUnitUtil.forceSkeleton(textUnit);
			GenericSkeleton skel = new GenericSkeleton();

			skel.add(startQualifier);
			skel.addContentPlaceholder(textUnit);
			skel.add(endQualifier);

			setSourceText(textUnit, st.substring(startQualifierLen, Util.getLength(st)
					- endQualifierLen));

			int index = SkeletonUtil.findTuRefInSkeleton(tuSkel);
			if (index != -1)
				SkeletonUtil.replaceSkeletonPart(tuSkel, index, skel);
			else
				tuSkel.add(skel);
		}
	}
	
	/**
	 * Simplifies all possible tags in the source part of a given text unit resource.
	 * @param textUnit the given text unit
	 * @param removeLeadingTrailingCodes true to remove leading and/or trailing codes
	 * of the source part and place their text in the skeleton.
	 */
	public static void simplifyCodes (ITextUnit textUnit, boolean removeLeadingTrailingCodes) {
		if (textUnit == null) {
			LOGGER.warning("Text unit is null.");
			return;
		}
		
		if (textUnit.getTargetLocales().size() > 0) {
			LOGGER.warning(String.format("Text unit %s has one or more targets, " +
					"desinchronization of codes in source and targets is possible.", textUnit.getId()));
		}
		
		CodeSimplifier simplifier = new CodeSimplifier();
		TextContainer tc = textUnit.getSource();
		String[] res;
		
		if (textUnit.getSource().hasBeenSegmented()) {
			res = simplifier.simplifyAll(tc, removeLeadingTrailingCodes);			
		}
		else {
			TextFragment tf = tc.getUnSegmentedContentCopy();  			
			res = simplifier.simplifyAll(tf, removeLeadingTrailingCodes);
			textUnit.setSourceContent(tf); // Because we modified a copy
		}
			
		// Move the codes found themselves outside the container/fragment, to the TU skeleton
		if (removeLeadingTrailingCodes && res != null) {
			GenericSkeleton tuSkel = TextUnitUtil.forceSkeleton(textUnit);
			GenericSkeleton skel = new GenericSkeleton();
			
			skel.add(res[0]);
			skel.addContentPlaceholder(textUnit);
			skel.add(res[1]);
		
			int index = SkeletonUtil.findTuRefInSkeleton(tuSkel);
			if (index != -1)
				SkeletonUtil.replaceSkeletonPart(tuSkel, index, skel);
			else
				tuSkel.add(skel);
		}
	}
	
	/**
	 * Simplifies all possible tags in a given text fragment.
	 * @param tf the given text fragment
	 * @param removeLeadingTrailingCodes true to remove leading and/or trailing codes
	 * of the source part and place their text in the skeleton.
	 * @return Null (no leading or trailing code removal was) or a string array with the
	 * original data of the codes removed. The first string if there was a leading code, the second string
	 * if there was a trailing code. Both or either can be null
	 */
	public static String[] simplifyCodes (TextFragment tf, boolean removeLeadingTrailingCodes) {
		CodeSimplifier simplifier = new CodeSimplifier();
		return simplifier.simplifyAll(tf, removeLeadingTrailingCodes);
	}
	
	/**
	 * Simplifies all possible tags in a given text container.
	 * @param tc the given text container
	 * @param removeLeadingTrailingCodes true to remove leading and/or trailing codes
	 * of the source part and place their text in the skeleton.
	 * @return Null (no leading or trailing code removal was) or a string array with the
	 * original data of the codes removed. The first string if there was a leading code, the second string
	 * if there was a trailing code. Both or either can be null
	 */
	public static String[] simplifyCodes (TextContainer tc, boolean removeLeadingTrailingCodes) {
		CodeSimplifier simplifier = new CodeSimplifier();
		return simplifier.simplifyAll(tc, removeLeadingTrailingCodes);
	}

	/**
	 * Removes from the source part of a given text unit resource qualifiers (quotation marks etc.) around text.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param qualifier
	 *            the qualifier to be removed before and after source text.
	 */
	public static void removeQualifiers (ITextUnit textUnit,
		String qualifier)
	{
		removeQualifiers(textUnit, qualifier, qualifier);
	}
	
	/**
	 * Adds an {@link AltTranslation} object to a given {@link TextContainer}. The {@link AltTranslationsAnnotation}
	 * annotation is created if it does not exist already.
	 * @param targetContainer the container where to add the object.
	 * @param alt alternate translation to add.
	 * @return the annotation where the object was added,
	 * it may be a new annotation or the one already associated with the container.  
	 */
	public static AltTranslationsAnnotation addAltTranslation (TextContainer targetContainer,
		AltTranslation alt)
	{
		AltTranslationsAnnotation altTrans = targetContainer.getAnnotation(AltTranslationsAnnotation.class);
		if ( altTrans == null ) {
			altTrans = new AltTranslationsAnnotation();
			targetContainer.setAnnotation(altTrans);
		}
		altTrans.add(alt);
		return altTrans;
	}
	
	/**
	 * Adds an {@link AltTranslation} object to a given {@link Segment}.
	 * The {@link AltTranslationsAnnotation} annotation is created if it does not exist already.
	 * @param seg the segment where to add the object.
	 * @param alt alternate translation to add.
	 * @return the annotation where the object was added,
	 * it may be a new annotation or the one already associated with the segment.  
	 */
	public static AltTranslationsAnnotation addAltTranslation (Segment seg,
		AltTranslation alt)
	{
		AltTranslationsAnnotation altTrans = seg.getAnnotation(AltTranslationsAnnotation.class);
		if ( altTrans == null ) {
			altTrans = new AltTranslationsAnnotation();
			seg.setAnnotation(altTrans);
		}
		altTrans.add(alt);
		return altTrans;
	}

	/**
	 * Creates a text fragment containing all segments and text parts of a given text container.
	 * Original segments and text parts are wrapped with special boundary place-holders, inserted to later restore the 
	 * original segmentation.
	 * @param tc the given text container
	 * @return a text fragment containing the given text container's text parts and segments and segment boundary place-holders. 
	 */
	public static TextFragment storeSegmentation (TextContainer tc) {
		// Join all segment and text parts into a new TextFragment
		
		// We need to have markers for both segments and text parts, because if we have segment markers only,
		// then adjacent text parts in an inter-segment space will get restored as one text part
		
		// Cannot store seg ids in Code.type, because if 2 codes are merged, the type of one of them is lost 
		
		TextFragment tf = new TextFragment();
		for ( TextPart part : tc ) {
			Segment seg = null;
			
			if (part.isSegment()) {
				seg = (Segment)part;
				tf.append(new Code(TagType.PLACEHOLDER, "seg", TextFragment.makeRefMarker(seg.getId(), SEG_START)));
			}
			else {
				tf.append(new Code(TagType.PLACEHOLDER, "tp", TP_START));
			}
			
			tf.append(part.getContent());
			
			if (part.isSegment()) {
				tf.append(new Code(TagType.PLACEHOLDER, "seg", TextFragment.makeRefMarker(seg.getId(), SEG_END)));
			}
			else {
				tf.append(new Code(TagType.PLACEHOLDER, "tp", TP_END));
			}
		}
		return tf;
	}		
	
	private static final class MarkerDescr {
		private boolean isSegment;
		private boolean isStart;
		private String id;
		private int position;
		
		private MarkerDescr(String id, int position, boolean isStart) {
			this.isSegment = true;
			this.isStart = isStart;
			this.id = id;
			this.position = position;
		}
		
		private MarkerDescr(int position, boolean isStart) {
			this.isSegment = false;
			this.isStart = isStart;
			this.id = null;
			this.position = position;
		}
	}
	
	/**
	 * Extracts segment and text part markers from a given string, creates codes (place-holder type) for those markers, 
	 * and appends them to a given text fragment.
	 * @param tf the given text fragment to append extracted codes
	 * @param original the given string
	 * @param removeFromOriginal remove found markers from the given string
	 * @return the given string if removeFromOriginal == false, or the modified original string with markers removed otherwise
	 */
	public static String extractSegMarkers(TextFragment tf, String original, boolean removeFromOriginal) {
		if (tf == null) {
			LOGGER.warning("Text fragment is null, no codes are added");
		}
		if (original == null) {
			LOGGER.warning("Original string is null, no processing was performed");
			return "";
		}
		
		Matcher matcher = ANY_SEG_TP_REGEX.matcher(original);
		while (tf != null && matcher.find()) {
			tf.append(new Code(TagType.PLACEHOLDER, null, matcher.group()));
		}
		
		return removeFromOriginal ? matcher.replaceAll("") : original;
	}
	
	/**
	 * Restores original segmentation of a given text container from a given text fragment created with storeSegmentation().
	 * @param tc the given text container
	 * @param segStorage the text fragment created with storeSegmentation() and containing the original segmentation info
	 * @return a test string containing a sequence of markers created by the internal algorithm. Used for tests only. 
	 */
	public static String restoreSegmentation(TextContainer tc, TextFragment segStorage) {
		
		// Empty tc
		tc.clear();
		
		// Scan the tf, create segments and text parts, and add them to tc
		TextFragment tf = segStorage;		
		String ctext = tf.getCodedText();
		String id;
		List<Code> codes = tf.getCodes();
		int pos;
		
		List<MarkerDescr> markers = new ArrayList<MarkerDescr> ();
		
		for (int i = 0; i < ctext.length(); i++){
			if ( TextFragment.isMarker(ctext.charAt(i)) ) {				
				int codeIndex = TextFragment.toIndex(ctext.charAt(i + 1));
				Code code = codes.get(codeIndex);
				String data = code.getData();
				
				Matcher matcher = SEG_START_REGEX.matcher(data);
				while (matcher.find()) {
					if (code.getTagType() == TagType.OPENING) {
						pos = i; // move before the code, AQ-B 52*
					}
					else {
						pos = i + 2; // move after the code
					}
					
					id = matcher.group(1);
					markers.add(new MarkerDescr(id, pos, true));
				}				
				
				matcher = SEG_END_REGEX.matcher(data);
				while (matcher.find()) {
					if (code.getTagType() == TagType.CLOSING) {
						pos = i + 2; // move after the code
					}
					else {
						pos = i; // move before the code
					}
					id = matcher.group(1);
					markers.add(new MarkerDescr(id, pos, false));
				}
				
				matcher = TP_START_REGEX.matcher(data);
				while (matcher.find()) {
					if (code.getTagType() == TagType.OPENING) {
						pos = i; // move before the code
					}
					else {
						pos = i + 2; // move after the code
					}
					markers.add(new MarkerDescr(pos, true));
				}				
				
				matcher = TP_END_REGEX.matcher(data);
				while (matcher.find()) {
					if (code.getTagType() == TagType.CLOSING) {
						pos = i + 2; // move after the code
					}
					else {
						pos = i; // move before the code
					}
					markers.add(new MarkerDescr(pos, false));
				}
				
				// Remove markers from the code data, code markers stay in place in the tf coded text
				SEG_START_REGEX.matcher(data).replaceAll("");
				SEG_END_REGEX.matcher(data).replaceAll("");
				TP_START_REGEX.matcher(data).replaceAll("");
				TP_END_REGEX.matcher(data).replaceAll("");
								
				i++; // Skip the pair
			}
			// For the 1-st iteration mind spaces only after a code not to trim-head the string
			else if (Character.isWhitespace(ctext.charAt(i))) {
				
			}
		}
		
		// Sort markers
		StringBuilder markersSb = new StringBuilder();
//		for (MarkerDescr d : markers) {
//			markersSb.append(String.format("(%d: %s %s) ", d.position, d.id != null ? d.id : "", d.isStart ? "start" : "end"));
//		}
		
		Collections.sort(markers, new Comparator<MarkerDescr>() {

//			@Override
//			public int compare(MarkerDescr d1, MarkerDescr d2) {
//				if (d1.isSegment && d2.isSegment || !d1.isSegment && !d2.isSegment) {
//					if (d1.position < d2.position) 
//						return -1;
//					else if (d1.position > d2.position) 
//						return 1;
//					else { // equal positions, markers from a merged code
//						if (!d1.isStart & d2.isStart)
//							return -1;
//						else if (d1.isStart & !d2.isStart)
//							return 1;
//						else
//							return 0;
//					}
//				}
//				else 
//					return 0;
//			}			
			
			@Override
			public int compare(MarkerDescr d1, MarkerDescr d2) {
					if (d1.position < d2.position) 
						return -1;
					else if (d1.position > d2.position) 
						return 1;
					else { // equal positions, markers from a merged code
						return 0;
					}
			}			
			
		});
		
		for (MarkerDescr d : markers) {
			markersSb.append(String.format("(%d: %s %s) ", d.position, d.id != null ? d.id : "", d.isStart ? "start" : "end"));
		}
		
		// Create segments and text parts in tc
		int start = -1;
		for (MarkerDescr d : markers) {			
			if (d.isSegment) { // segment
				if (d.isStart) {
					start = d.position;
				}
				else { // end
					if (start > -1) {
						if (start <= d.position)
							tc.append(new Segment(d.id, tf.subSequence(start, d.position)));
						else
							LOGGER.warning(String.format("Cannot create the segment %s - incorrect range: (%d - %d)", 
									d.id, start, d.position));
					}
					start = -1;
				}
			}
			else { // text part
				if (d.isStart) {
					start = d.position;
				}
				else { // end
					if (start > -1) {
						if (start <= d.position)
							tc.append(new TextPart(tf.subSequence(start, d.position)));
						else
							LOGGER.warning(String.format("Cannot create a text part - incorrect range: (%d - %d)", start, d.position));
					}
					start = -1;
				}
			}
		}
		
		return markersSb.toString().trim(); 		
	}

	/**
	 * Returns the content of a given text fragment, including the original codes whenever
	 * possible. Codes are decorated with '[' and ']' to tell them from regular text.
	 * @param tf the given text fragment 
	 * @return the content of the given fragment
	 */
	public static String toText (TextFragment tf) {
		List<Code> codes = tf.getCodes();
		String text = tf.getCodedText();
		
		if (( codes == null ) || ( codes.size() == 0 )) return text.toString();
		
		StringBuilder tmp = new StringBuilder();
		Code code;
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				code = codes.get(TextFragment.toIndex(text.charAt(++i)));
				tmp.append(String.format("[%s]", code.data));
				break;
			default:
				tmp.append(text.charAt(i));
				break;
			}
		}
		return tmp.toString();
	}
	
	/**
	 * Returns representation of a given coded text with code data enclosed in brackets.
	 * @param text the given coded text
	 * @param codes the given list of codes
	 * @return content of the given coded text
	 */
	public static String toText (String text, List<Code> codes) {
		
		if (( codes == null ) || ( codes.size() == 0 )) return text.toString();
		
		StringBuilder tmp = new StringBuilder();
		Code code;
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				int index = TextFragment.toIndex(text.charAt(++i));
				try {
					code = codes.get(index);
					tmp.append(String.format("[%s]", code.data));
				} catch (Exception e) {
					tmp.append(String.format("[-ERR:UNKNOWN-CODE- %d]", index));
				}				
				break;
			default:
				tmp.append(text.charAt(i));
				break;
			}
		}
		return tmp.toString();
	}

}
