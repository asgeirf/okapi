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

package net.sf.okapi.common.skeleton;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.RegexUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.MultiEvent;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

/**
 * Helper methods to manipulate skeleton objects. 
 */
public class SkeletonUtil {

	private static String tuRef = TextFragment.makeRefMarker("$self$");
	
	private static final Pattern PROPERTY_REGEX = Pattern.compile(String.format("%s%s%s.+%s", 
			RegexUtil.escape(TextFragment.REFMARKER_START), 
			RegexUtil.escape("$self$"),
			RegexUtil.escape(TextFragment.REFMARKER_SEP), 
			RegexUtil.escape(TextFragment.REFMARKER_END)));
	
	private static final Pattern REF_REGEX = Pattern.compile(String.format("%s.+%s", 
			RegexUtil.escape(TextFragment.REFMARKER_START), 
			RegexUtil.escape(TextFragment.REFMARKER_END)));
	
	private static GenericSkeletonWriter writer = new GenericSkeletonWriter();
	
	/**
	 * Finds source reference in the skeleton.
	 * @param skel the skeleton being sought for the reference
	 * @return index in the list of skeleton parts for the skeleton part containing the reference 
	 */
	public static int findTuRefInSkeleton(GenericSkeleton skel) {
		return findTuRefInSkeleton(skel, null);
	}
	
	/**
	 * Finds either source or target reference in the skeleton. If locId is specified, then its target reference is sought for.
	 * @param skel the skeleton being sought for the reference.
	 * @param locId the locale to search the reference for.
	 * @return index in the list of skeleton parts for the skeleton part containing the reference. 
	 */
	public static int findTuRefInSkeleton(GenericSkeleton skel,
		LocaleId locId)
	{
		if ( skel == null ) return -1; 		
		List<GenericSkeletonPart> list = skel.getParts();		
		
		for ( int i=0; i<list.size(); i++ ) {
			GenericSkeletonPart part = list.get(i);				
			String st = part.toString();
			if ( Util.isEmpty(st) ) continue;
			if ( st.equalsIgnoreCase(tuRef) ) {
				if ( Util.isNullOrEmpty(locId) ) {
					return i;
				}
				else if ( locId.equals(part.getLocale()) ) {
					return i;
				}
			}
		}
		return -1;
	}
		
	/**
	 * Determines if a given skeleton contains a source reference in it.
	 * @param skel the skeleton being sought for the reference. 
	 * @return true if the given skeleton contains such a reference.
	 */
	public static boolean hasTuRef (GenericSkeleton skel) {
		return findTuRefInSkeleton(skel) != -1;
	}
	
	/**
	 * Determines if a given skeleton contains a target reference in a given locale.
	 * @param skel the skeleton being sought for the reference. 
	 * @param locId the locale of the target part being sought.
	 * @return true if the given skeleton contains such a reference.
	 */
	public static boolean hasTuRef (GenericSkeleton skel, LocaleId locId) {
		return findTuRefInSkeleton(skel, locId) != -1;
	}

	/**
	 * Replaces a part of a given skeleton with another given skeleton part.
	 * @param skel the skeleton which part is being replaced.
	 * @param index the index of the skeleton part to be replaced.
	 * @param replacement the given new skeleton part to replace the existing one.
	 * @return true if replacement succeeded.
	 */
	public static boolean replaceSkeletonPart (GenericSkeleton skel,
		int index,
		GenericSkeleton replacement)
	{
		if ( skel == null ) return false;
		if ( replacement == null ) return false;
		
		List<GenericSkeletonPart> list = skel.getParts();
		if ( !Util.checkIndex(index, list) ) return false;

		List<GenericSkeletonPart> list2 = (List<GenericSkeletonPart>) ListUtil.moveItems(list); // clears the original list
		for (int i = 0; i < list2.size(); i++) {
			if ( i == index )						
				skel.add(replacement);
			else
				list.add(list2.get(i));
		}
		return true;
	}

	private static Event wrapEvent(Event event) {
		MultiEvent me = new MultiEvent();
		me.addEvent(event);
		return new Event(EventType.MULTI_EVENT, me);
	}
	
	public static int getNumParts(GenericSkeleton skel) {
		return skel.getParts().size();
	}
	
	public static GenericSkeletonPart getPart(GenericSkeleton skel, int index) {
		List<GenericSkeletonPart> parts = skel.getParts();
		if (!Util.checkIndex(index, parts)) return null;
		return parts.get(index);
	}
	
	private static boolean isTuRef(GenericSkeletonPart part) {
		return tuRef.equals(part.toString());		
	}
	
	private static boolean isRef(GenericSkeletonPart part) {
		String st = part.toString();
		return !isTuRef(part) && RegexUtil.matches(st, REF_REGEX);		
	}
	
	private static boolean isPropRef(GenericSkeletonPart part) {
		String st = part.toString();
		return !isTuRef(part) && RegexUtil.matches(st, PROPERTY_REGEX);
	}
	
	public static boolean isSourcePlaceholder(TextUnit resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isTuRef(part) && part.getParent() == resource && part.getLocale() == null;		
	}
	
	public static boolean isTargetPlaceholder(TextUnit resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isTuRef(part) && part.getParent() == resource && part.getLocale() != null;		
	}
	
	public static boolean isValuePlaceholder(INameable resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isPropRef(part) && part.getParent() == resource;		
	}
	
	public static boolean isExtSourcePlaceholder(TextUnit resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isTuRef(part) && part.getParent() != resource && part.getParent() != null && part.getLocale() == null;		
	}
	
	public static boolean isExtTargetPlaceholder(TextUnit resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isTuRef(part) && part.getParent() != resource && part.getParent() != null && part.getLocale() != null;		
	}
	
	public static boolean isExtValuePlaceholder(INameable resource, GenericSkeletonPart part) {
		if (resource == null || part == null) return false;
		return isPropRef(part) && part.getParent() != resource && part.getParent() != null;
	}
	
	public static boolean isReference(GenericSkeletonPart part) {
		if (part == null) return false;
		return isRef(part) && part.getParent() == null && part.getLocale() == null;
	}
	
	public static Event toMultiEvent(Event event) {
		if (event == null)
			throw new InvalidParameterException("Event cannot be null");
		
		IResource res = event.getResource();
		if (res == null)
			return wrapEvent(event);
		
		ISkeleton skel = res.getSkeleton();
		if (!(skel instanceof GenericSkeleton)) {
			// TODO log
			return wrapEvent(event);
		}
		
		List<GenericSkeletonPart> parts = ((GenericSkeleton) skel).getParts();
		
		switch (event.getEventType()) {
		case TEXT_UNIT:
		}
		
		return null;		
	}

	public static Event fromMultiEvent(Event event) {
		if (event == null)
			throw new InvalidParameterException("Event cannot be null");
		if (event.getEventType() != EventType.MULTI_EVENT)
			throw new InvalidParameterException("MULTI_EVENT type is expected");
		MultiEvent me = (MultiEvent) event.getResource();
		//if (me.iterator().)
		
		return null;		
	}
}
