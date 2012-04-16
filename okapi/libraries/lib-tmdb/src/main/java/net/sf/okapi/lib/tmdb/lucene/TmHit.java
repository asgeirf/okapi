/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.tmdb.lucene;

import net.sf.okapi.common.HashCodeUtil;
import net.sf.okapi.common.query.MatchType;

public class TmHit implements Comparable<TmHit> {

	private String entryId;
	private float score;
	private String segKey;
	private Variant variant;
	private boolean codeMismatch;
	private MatchType matchType;

	public TmHit () {
        setMatchType(MatchType.UKNOWN);
        setCodeMismatch(false);
	}
	
	public String getId () {
		return entryId;
	}

	public void setId (String entryId) {
		this.entryId = entryId;
	}

	public String getSegKey () {
		return segKey;
	}

	public void setSegKey (String segKey) {
		this.segKey = segKey;
	}
	
	public float getScore () {
		return score;
	}
	
	public void setScore (float score) {
		this.score = score;
	}
	
	public boolean getCodeMismatch () {
		return codeMismatch;
	}
	
	public void setCodeMismatch (boolean codeMismatch) {
		this.codeMismatch = codeMismatch;
	}
	
	public MatchType getMatchType () {
		return matchType;
	}
	
	public void setMatchType (MatchType matchType) {
		this.matchType = matchType;
	}
	
	public Variant getVariant () {
		return variant;
	}

	public void setVariant (Variant variant) {
		this.variant = variant;
	}

	public int compareTo (TmHit other) {
        final int EQUAL = 0;
        if ( this == other ) return EQUAL;

        String thisSource = this.variant.getGenericTextField().stringValue();
        String otherSource = other.variant.getGenericTextField().stringValue();
        
        // Only sort by match type if this or other is some kind of exact match
        int comparison;
        if ( isExact(this.matchType) || isExact(other.matchType) ) {            
        	// Compare MatchType
        	comparison = this.matchType.compareTo(other.getMatchType());
        	if ( comparison != EQUAL ) return comparison;
        }

        // Compare score
        comparison = Float.compare(this.score, other.getScore());
        if ( comparison != EQUAL ) return comparison * -1;  // we want to reverse the normal score sort

        // Compare source strings with codes
        comparison = thisSource.compareTo(otherSource);
        if ( comparison != EQUAL ) return comparison;

        // Default
        return EQUAL;
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) return true;
		if ( !(other instanceof TmHit) ) return false;
	
		TmHit otherHit = (TmHit)other;
		return (this.matchType == otherHit.getMatchType())
			&& (this.variant.getGenericTextField().stringValue().equals(
				otherHit.variant.getGenericTextField().stringValue()));
	}

	@Override
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash(result, matchType);
		result = HashCodeUtil.hash(result, variant.getGenericTextField().stringValue());
		return result;
	}

	private boolean isExact (MatchType type) {
		if ( type.ordinal() <= MatchType.EXACT_REPAIRED.ordinal() ) {
            return true;
		}
		return false;
	}

}
