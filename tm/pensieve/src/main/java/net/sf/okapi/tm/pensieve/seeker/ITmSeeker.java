/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.tm.pensieve.seeker;


import net.sf.okapi.tm.pensieve.common.TmHit;

import java.util.List;
import net.sf.okapi.tm.pensieve.common.Metadata;

/**
 * Used to query the TM.
 * @author HaslamJD
 */
public interface ITmSeeker {

    /**
     * Gets a list of matches for a given set of words. In this case OR is assumed.
     * @param query The words to query for
     * @param max The max number of results
     * @param metadata The metadata attributes to also match against, null for no metadata
     * @return A list of matches for a given set of words. In this case OR is assumed.
     * @throws net.sf.okapi.common.exceptions.OkapiIOException if the search cannot be completed do to I/O problems
     */
    List<TmHit> searchForWords(String query, int max, Metadata metadata);

    /**
     * Gets a list of exact matches for a given phrase.
     * @param query The exact text to search for
     * @param max The max number of results
     * @param metadata The metadata attributes to also match against, null for no metadata
     * @return A list of exact matches
     * @throws net.sf.okapi.common.exceptions.OkapiIOException if the search cannot be completed do to I/O problems
     */
    List<TmHit> searchExact(String query, int max, Metadata metadata);
/**
     * Gets a list of fuzzy matches for a given phrase.
     *
     *
     * @return A list of fuzzy matches
     * @throws OkapiIOException if the search cannot be completed do to I/O problems
     */


    /**
     * Gets a list of fuzzy matches for a given phrase.
     * @param query The query string to match WITHOUT ~ and threshold value
     * @param similarityThreshold The desired threshold - null for default threshold of 0.5
     * @param max The max number of results
     * @param metadata The metadata attributes to also match against, null for no metadata
     * @return A list of fuzzy matches
     * @throws net.sf.okapi.common.exceptions.OkapiIOException if the search cannot be completed do to I/O problems
     */
    List<TmHit> searchFuzzy(String query, Float similarityThreshold, int max, Metadata metadata);

    /**
     * Gets a list of TmHits which have segments that contain the provided subphrase
     * @param subPhrase The subphrase to match again
     * @param maxHits The maximum number of hits to return
     * @param metadata The metadata attributes to also match against, null for no metadata
     * @return A list of TmHits which have segments that contain the provided subphrase
     * @throws net.sf.okapi.common.exceptions.OkapiIOException if the search cannot be completed do to I/O problems
     */
    List<TmHit> searchSubphrase(String subPhrase, int maxHits, Metadata metadata);
   
}
