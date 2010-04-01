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

package net.sf.okapi.common.resource;

import java.util.LinkedList;
import java.util.List;

import net.sf.okapi.common.ReversedIterator;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.SkeletonUtil;

/**
 * Helper methods to manipulate {@link TextFragment}, {@link TextContainer}, and {@link TextUnit} objects.
 */
public class TextUnitUtil {

	/**
	 * Removes leading whitespaces from a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment which leading whitespaces are to be removed.
	 */
	public static void trimLeading(TextFragment textFragment) {

		trimLeading(textFragment, null);
	}

	/**
	 * Removes leading whitespaces from a given text fragment, puts removed whitespaces to the given skeleton.
	 * 
	 * @param textFragment
	 *            the text fragment which leading whitespaces are to be removed.
	 * @param skel
	 *            the skeleton to put the removed whitespaces.
	 */
	public static void trimLeading(TextFragment textFragment, GenericSkeleton skel) {

		if (textFragment == null)
			return;
		String st = textFragment.getCodedText();
		TextFragment skelTF;

		int pos = TextFragment.indexOfFirstNonWhitespace(st, 0, -1, true, true, true, true);
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

		st = skelTF.toString();
		if (!Util.isEmpty(st))
			skel.append(st); // Codes get removed
	}

	/**
	 * Removes trailing whitespaces from a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment which trailing whitespaces are to be removed.
	 */
	public static void trimTrailing(TextFragment textFragment) {
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
	public static void trimTrailing(TextFragment textFragment, GenericSkeleton skel) {

		if (textFragment == null)
			return;

		String st = textFragment.getCodedText();
		TextFragment skelTF;

		int pos = TextFragment.indexOfLastNonWhitespace(st, -1, 0, true, true, true, true);
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

		st = skelTF.toString();
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
	public static boolean endsWith(TextFragment textFragment, String substr) {

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
	public static boolean isEmpty(TextUnit textUnit) {
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
	public static boolean hasSource(TextUnit textUnit) {
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
	public static boolean isEmpty(TextUnit textUnit, boolean ignoreWS) {
		return ((textUnit == null) || Util.isEmpty(getSourceText(textUnit), ignoreWS));
	}

	/**
	 * Gets the coded text of the source part of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the text unit resource which source text should be returned.
	 * @return the source part of the given text unit resource.
	 */
	public static String getSourceText(TextUnit textUnit) {
		// if ( textUnit == null ) return "";
		// return getCodedText(textUnit.getSourceContent());
		return textUnit.getSource().getFirstPartContent().getCodedText();
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
	public static String getSourceText(TextUnit textUnit, boolean removeCodes) {
		if (textUnit == null)
			return "";
		if (removeCodes) {
			return getText(textUnit.getSource().getFirstPartContent());
		} else {
			return textUnit.getSource().getFirstPartContent().getCodedText();
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
	public static String getTargetText(TextUnit textUnit, LocaleId locId) {
		if (textUnit == null)
			return "";
		if (Util.isNullOrEmpty(locId))
			return "";

		return getCodedText(textUnit.getTarget(locId).getFirstPartContent());
	}

	/**
	 * Gets text of a given text fragment object possibly containing inline codes.
	 * 
	 * @param textFragment
	 *            the given text fragment object.
	 * @return the text of the given text fragment object possibly containing inline codes.
	 */
	public static String getCodedText(TextFragment textFragment) {
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
	public static String getText(TextFragment textFragment, List<Integer> markerPositions) {
		if (textFragment == null)
			return "";

		String res = textFragment.getCodedText();
		StringBuilder sb = new StringBuilder();
		if (markerPositions != null) {
			markerPositions.clear();
		}

		// Collect marker positions & remove markers
		int startPos = 0;
		for (int i = 0; i < res.length(); i++) {
			switch (res.charAt(i)) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				if (markerPositions != null) {
					markerPositions.add(i);
				}
				if (i > startPos) {
					sb.append(res.substring(startPos, i));
				}
				startPos = i + 2;
				i = startPos;
			}
		}

		if (startPos < res.length()) {
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
	public static String getText(TextFragment textFragment) {
		return getText(textFragment, null);
	}

	/**
	 * Gets the last character of a given text fragment.
	 * 
	 * @param textFragment
	 *            the text fragment to examin.
	 * @return the last character of the given text fragment, or '\0'.
	 */
	public static char getLastChar(TextFragment textFragment) {

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
	public static void deleteLastChar(TextFragment textFragment) {

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
	public static int lastIndexOf(TextFragment textFragment, String findWhat) {

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
	public static boolean isEmpty(TextFragment textFragment) {
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
	public static TextUnit buildTU(TextContainer source) {
		return buildTU(null, "", source, null, LocaleId.EMPTY, "");
	}

	/**
	 * Creates a new text unit resource based a given string becoming the source text of the text unit.
	 * 
	 * @param source
	 *            the given string becoming the source text of the text unit.
	 * @return a new text unit resource with the given string being its source text.
	 */
	public static TextUnit buildTU(String source) {
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
	public static TextUnit buildTU(String srcPart, String skelPart) {

		TextUnit res = buildTU(srcPart);
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
	public static TextUnit buildTU(TextUnit textUnit, String name, TextContainer source,
			TextContainer target, LocaleId locId, String comment) {
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
	public static GenericSkeleton forceSkeleton(TextUnit tu) {

		if (tu == null)
			return null;

		GenericSkeleton skel = (GenericSkeleton) tu.getSkeleton();
		if (skel == null) {

			skel = new GenericSkeleton();
			if (skel == null)
				return null;

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
	public static GenericSkeleton convertToSkeleton(TextUnit textUnit) {

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
	public static <A extends IAnnotation> A getSourceAnnotation(TextUnit textUnit, Class<A> type) {

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
	public static void setSourceAnnotation(TextUnit textUnit, IAnnotation annotation) {

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
	public static <A extends IAnnotation> A getTargetAnnotation(TextUnit textUnit, LocaleId locId,
			Class<A> type) {

		if (textUnit == null)
			return null;
		if (Util.isNullOrEmpty(locId))
			return null;
		if (textUnit.getTarget(locId) == null)
			return null;

		return textUnit.getTarget(locId).getAnnotation(type);
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
	public static void setTargetAnnotation(TextUnit textUnit, LocaleId locId, IAnnotation annotation) {

		if (textUnit == null)
			return;
		if (Util.isNullOrEmpty(locId))
			return;
		if (textUnit.getTarget(locId) == null)
			return;

		textUnit.getTarget(locId).setAnnotation(annotation);
	}

	/**
	 * Sets the coded text of the un-segmented source of a given text unit resource.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param text
	 *            the text to be set.
	 */
	public static void setSourceText(TextUnit textUnit, String text) {
		// fail fast if ( textUnit == null ) return;
		TextFragment source = textUnit.getSource().getFirstPartContent();
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
	public static void setTargetText(TextUnit textUnit, LocaleId locId, String text) {
		// fail fast if ( textUnit == null ) return;
		// fail fast if ( Util.isNullOrEmpty(locId) ) return;
		TextFragment target = textUnit.getTarget(locId).getFirstPartContent();
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
	public static void trimTU(TextUnit textUnit, boolean trimLeading, boolean trimTrailing) {
		if (textUnit == null)
			return;
		if (!trimLeading && !trimTrailing)
			return;

		TextContainer source = textUnit.getSource();
		GenericSkeleton tuSkel = TextUnitUtil.forceSkeleton(textUnit);
		GenericSkeleton skel = new GenericSkeleton();

		if (trimLeading) {
			trimLeading(source.getFirstPartContent(), skel);
		}
		skel.addContentPlaceholder(textUnit);

		if (trimTrailing) {
			trimTrailing(source.getFirstPartContent(), skel);
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
	public static void removeQualifiers(TextUnit textUnit, String startQualifier,
			String endQualifier) {

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
	 * Removes from the source part of a given text unit resource qualifiers (quotation marks etc.) around text.
	 * 
	 * @param textUnit
	 *            the given text unit resource.
	 * @param qualifier
	 *            the qualifier to be removed before and after source text.
	 */
	public static void removeQualifiers(TextUnit textUnit, String qualifier) {
		removeQualifiers(textUnit, qualifier, qualifier);
	}

	/**
	 * Creates a new text unit based on an existing text unit, with its content set to the content of a source and a
	 * target segment. All other targets are removed.
	 * 
	 * @param srcTextUnit
	 *            text unit to use for origin.
	 * @param srcSegment
	 *            source segment.
	 * @param trgSegment
	 *            target segment.
	 * @param trgLocaleId
	 *            target locale.
	 * @return a new text unit with its content set to the given source and target segment.
	 */
	public static TextUnit createBilingualTextUnit(final TextUnit srcTextUnit, Segment srcSegment,
			Segment trgSegment, LocaleId trgLocaleId) {
		// Clone the original
		TextUnit tu = srcTextUnit.clone();
		// Empty the source content
		tu.getSource().clear();
		// Set the new source content: the given source segment
		tu.setSourceContent(srcSegment.getContent());
		// Removes all targets
		for (LocaleId locId : tu.getTargetLocales()) {
			tu.removeTarget(locId);
		}
		// Create a new target with all the source data
		tu.createTarget(trgLocaleId, true, TextUnit.COPY_ALL);
		// Set the new target content: the given target segment
		tu.setTargetContent(trgLocaleId, trgSegment.getContent());
		// Return the result
		return tu;
	}

	/**
	 * Creates a new text unit based on an existing text unit, with its content set to the content of a list of source
	 * and a list of target segments. All other targets are removed. The segments Ids are preserved, so they must be
	 * valid, and matching between source and target, when appropriate.
	 * 
	 * @param srcTextUnit
	 *            text unit to use for origin.
	 * @param srcSegments
	 *            list of source segments.
	 * @param trgSegments
	 *            list of target segments.
	 * @param trgLocaleId
	 *            target locale.
	 * @param interSegmentSpace
	 *            the string to add between each segments (use null or empty for nothing between segments).
	 * @return a new text unit with its content set to the given source and target segments.
	 */
	public static TextUnit createBilingualTextUnit(final TextUnit srcTextUnit,
			List<Segment> srcSegments, List<Segment> trgSegments, LocaleId trgLocaleId,
			String interSegmentSpace) {
		// Clone the original
		TextUnit tu = srcTextUnit.clone();
		if (interSegmentSpace == null)
			interSegmentSpace = "";

		// Add by building the markers so we can preserve the segment ids
		TextContainer tc = tu.getSource();
		tc.clear();
		int i = 0;
		for (Segment seg : srcSegments) {
			// No part before for the first segment
			tc.appendSegment(seg, ((i == 0) ? null : interSegmentSpace));
			i++;
		}

		// Removes all targets
		for (LocaleId locId : tu.getTargetLocales()) {
			tu.removeTarget(locId);
		}

		// Create a new target with all the source data
		tc = tu.createTarget(trgLocaleId, true, TextUnit.COPY_ALL);
		tc.clear();
		// Add by building the markers so we can preserve the segment ids
		i = 0;
		for (Segment seg : trgSegments) {
			tc.appendSegment(seg, ((i == 0) ? null : interSegmentSpace));
			i++;
		}

		// Return the result
		return tu;
	}

	/**
	 * Convenience method that calls {@link #createBilingualTextUnit(TextUnit, List, List, LocaleId, String)} with
	 * <code>" "</code> as the space between segments.
	 * 
	 * @param srcTextUnit
	 *            text unit to use for origin.
	 * @param srcSegments
	 *            list of source segments.
	 * @param trgSegments
	 *            list of target segments.
	 * @param trgLocaleId
	 *            target locale.
	 * @return a new text unit with its content set to the given source and target segments.
	 */
	public static TextUnit createBilingualTextUnit(final TextUnit srcTextUnit,
			List<Segment> srcSegments, List<Segment> trgSegments, LocaleId trgLocaleId) {
		return createBilingualTextUnit(srcTextUnit, srcSegments, trgSegments, trgLocaleId, " ");
	}

	public static TextUnit createMultilingualTextUnit(final TextUnit srcTextUnit,
			final List<AlignedPair> alignedSegmentPairs, final LocaleId trgLocaleId) {
		// Clone the original
		TextUnit tu = srcTextUnit.clone();
		// Empty the source content
		tu.getSource().clear();

		// Removes all targets
		for (LocaleId locId : tu.getTargetLocales()) {
			tu.removeTarget(locId);
		}

		// Create a new target with all the source data
		tu.createTarget(trgLocaleId, true, TextUnit.COPY_ALL);

		for (AlignedPair alignedPair : alignedSegmentPairs) {
			// make a shallow copy because we may modify the list elements
			List<TextPart> sourceParts = new LinkedList<TextPart>(alignedPair.getSourceParts());
			List<TextPart> targetParts = new LinkedList<TextPart>(alignedPair.getTargetParts());
			
			// +++ source +++
			
			// calculate indexes of the source before and after inter-segment TextParts
			int beforeIndex = 0;
			int afterIndex = sourceParts.size();
			for (TextPart part : sourceParts) {
				if (part.isSegment()) {
					break;
				}			
				beforeIndex++;			
			}						
			ReversedIterator<TextPart> ri = new ReversedIterator<TextPart>(sourceParts);
			for (TextPart part : ri) {
				if (part.isSegment()) {
					break;
				}
				afterIndex--;
			}

			// append the before inter-segment TextParts
			for (TextPart part : sourceParts.subList(0, beforeIndex)) {
				tu.getSource().appendPart(part);
			}

			// append segment parts
			TextFragment src = new TextFragment();
			for (TextPart part : sourceParts.subList(beforeIndex, afterIndex)) {
				src.append(part.getContent());
			}
			tu.getSource().appendSegment(new Segment(null, src));

			// append the after inter-segment TextParts			
			for (TextPart part : sourceParts.subList(afterIndex, sourceParts.size())) {
				tu.getSource().appendPart(part);
			}						
			
			//+++ target +++
			
			// calculate indexes of the target before and after inter-segment TextParts
			beforeIndex = 0;
			afterIndex = targetParts.size();
			for (TextPart part : targetParts) {
				if (part.isSegment()) {
					break;
				}			
				beforeIndex++;			
			}						
			ri = new ReversedIterator<TextPart>(targetParts);
			for (TextPart part : ri) {
				if (part.isSegment()) {
					break;
				}
				afterIndex--;
			}

			// append the before inter-segment TextParts
			for (TextPart part : targetParts.subList(0, beforeIndex)) {
				tu.getTarget(trgLocaleId).appendPart(part);
			}

			// append segment parts
			TextFragment trg = new TextFragment();
			for (TextPart part : targetParts.subList(beforeIndex, afterIndex)) {
				trg.append(part.getContent());
			}
			tu.getTarget(trgLocaleId).appendSegment(new Segment(tu.getSource().getLastSegment().getId(), trg));

			// append the after inter-segment TextParts			
			for (TextPart part : targetParts.subList(afterIndex, targetParts.size())) {
				tu.getTarget(trgLocaleId).appendPart(part);
			}			
		}

		// We now consider the source and target content to be segmented
		// if nothing else we need to prevent re-segmentation as that 
		// will break the alignments
		tu.getSource().setHasBeenSegmentedFlag(true);
		tu.getTarget(trgLocaleId).setHasBeenSegmentedFlag(true);

		return tu;
	}
}
