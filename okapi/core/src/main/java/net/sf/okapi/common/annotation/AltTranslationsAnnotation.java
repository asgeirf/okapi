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

package net.sf.okapi.common.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.resource.TextFragment;

/**
 * Annotation for storing one or more alternate translations for a target content.
 * <p>
 * When used, this annotation is designed to be attached to the segments or the container of the targets.
 */
public class AltTranslationsAnnotation implements IAnnotation, Iterable<AltTranslation> {

	private ArrayList<AltTranslation> list;

	/**
	 * Creates a new empty AltTranslationsAnnotation object.
	 */
	public AltTranslationsAnnotation () {
		list = new ArrayList<AltTranslation>(2);
	}

	/**
	 * Adds an existing {@link AltTranslation} object to this annotation.
	 * 
	 * @param alt
	 *            the {@link AltTranslation} object to add.
	 */
	public void add (AltTranslation alt) {
		list.add(alt);
	}

	/**
	 * Adds a new entry to the list of alternate translations.
	 * 
	 * @param sourceLocId
	 *            the locale of the source.
	 * @param targetLocId
	 *            the locale of the target.
	 * @param originalSource
	 *            the original source content.
	 * @param alternateSource
	 *            the source content corresponding to the alternate translation.
	 * @param alternateTarget
	 *            the content of alternate translation.
	 * @param type
	 *            the type of alternate translation.
	 * @param score
	 *            the score for this alternate translation (must be between 0 and 100).
	 * @param origin
	 *            an optional identifier for the origin of this alternate translation.
	 * @return the {@link AltTranslation} object created and added to this annotation.
	 */
	public AltTranslation add (LocaleId sourceLocId,
		LocaleId targetLocId,
		TextFragment originalSource,
		TextFragment alternateSource,
		TextFragment alternateTarget,
		MatchType type,
		int score,
		String origin)
	{
		list.add(new AltTranslation(sourceLocId, targetLocId, originalSource, alternateSource,
			alternateTarget, type, score, origin));
		return list.get(list.size()-1);
	}

	/**
	 * Creates a new iterator for the entries in this annotations.
	 * 
	 * @return a new iterator for the entries in this annotations.
	 */
	@Override
	public Iterator<AltTranslation> iterator () {
		return new Iterator<AltTranslation>() {
			int current = 0;

			@Override
			public void remove() {
				throw new UnsupportedOperationException("The method remove() not supported.");
			}

			@Override
			public AltTranslation next() {
				if (current >= list.size()) {
					throw new NoSuchElementException("No more content parts.");
				}
				return list.get(current++);
			}

			@Override
			public boolean hasNext() {
				return (current < list.size());
			}
		};
	}

	/**
	 * Gets the first entry in the list of alternate translations.
	 * 
	 * @return the first alternate translation entry or null if the list is empty.
	 */
	public AltTranslation getFirst () {
		if ( list.isEmpty() ) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * Gets the last entry in the list of alternate translations.
	 * 
	 * @return the last alternate translation entry or null if the list is empty.
	 */
	public AltTranslation getLast () {
		if ( list.isEmpty() ) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	/**
	 * Indicates if the list of alternate translation is empty.
	 * 
	 * @return true if the list is empty.
	 */
	public boolean isEmpty () {
		return list.isEmpty();
	}

	/**
	 * Gets the number of entries in this annotation.
	 * 
	 * @return the number of alternate translations available.
	 */
	public int size () {
		return list.size();
	}

	/**
	 * Sorts the list of {@link AltTranslation}s in the order defined by {@link AltTranslation#compareTo(AltTranslation)}.
	 */
	public void sort () {
		Collections.sort(list);
	}

	/**
	 * Indicates if a) there are several matches of 100% with identical rank
	 * and at least two of them have different translations.
	 * @param forceSort true to force the entries to be sorted. If set to false, the
	 * code assumes the entries have been sorted already.
	 * @return true if the conditions above are true.
	 */
	public boolean hasSeveralBestMatches (boolean forceSort) {
		if ( list.size() < 2 ) return false;
		
		// Make sure it's sorted if needed
		if ( forceSort ) sort();
		
		// Get the best match
		AltTranslation best = list.get(0);
		if ( best.score < 100 ) return false;
		
		// Compare it to the next ones
		for ( int i=1; i<list.size(); i++ ) {
			// Loop through all other results until either:
			// - the match is different from the first
			// - or the match is identical but has a different translation
			AltTranslation res = list.get(i);
			//TODO: Cannot use matchType as not all connectors set it: if ( best.type != res.type ) return false;
			if ( best.score != res.score ) return false;
			if ( !best.getSource().toString().equals(res.getSource().toString()) ) return false;
			// Different target? (if yes -> return true)
			if ( !best.getTarget().toString().equals(res.getTarget().toString()) ) return true;
		}
		return false;
	}
	
	/**
	 * Downgrades, and possibly removes, all the 100% best matches that are identical.
	 * <p>A set of matches may have entries that have the same source but different targets, they
	 * are not duplicated. Some callers may need to treat exact matches like that as fuzzy matches
	 * to avoid triggering automated processes.
	 * <p>This methods examine the set of alternate translations here and downgrade by 1% the score
	 * of any top entry fall into that category. 
	 * @param forceSort true to re-sort the annotations. If you set this option to false the entries
	 * are expected to be already properly sorted to have the best matches first.
	 * @param threshold threshold under which the matches should be removed.
	 */
	public void downgradeIdenticalBestMatches (boolean forceSort,
		int threshold)
	{
		if ( !hasSeveralBestMatches(forceSort) ) return;
		
		// Get the best match
		AltTranslation best = list.get(0);
		// Compare it to the next ones
		for ( int i=1; i<list.size(); i++ ) {
			// Loop through all other results until either:
			// - the match is different from the first
			// - or the match is identical but has a different translation
			AltTranslation res = list.get(i);
			//TODO: Settle the matchType issue: Can't use it here as not all connector set it if ( best.type != res.type ) break;
			if ( best.score != res.score ) break;
			if ( !best.getSource().toString().equals(res.getSource().toString()) ) break;
			res.score--;
		}
		best.score--;
		
		// Remove any annotation below the threshold
		// Assumes the list is sorted
		Iterator<AltTranslation> iter = list.iterator();
		while ( iter.hasNext() ) {
			AltTranslation at = iter.next();
			if ( at.score >= threshold ) break; // Done
			iter.remove(); // Or remove
		}
	}

}
