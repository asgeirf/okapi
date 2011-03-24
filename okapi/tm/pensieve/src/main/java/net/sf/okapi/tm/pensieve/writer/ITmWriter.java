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

package net.sf.okapi.tm.pensieve.writer;

import net.sf.okapi.tm.pensieve.common.TranslationUnit;

/**
 *
 * @author HaslamJD
 * @author HARGRAVEJE
 */
public interface ITmWriter {

    /**
     * Closes the index and forces a commit against the index.
     * @throws OkapiIOException if the commit can not happen
     */
    void close();

    /**
     * Indexes a given translation unit.
     * @param tu the translation unit to index.
     * @throws OkapiIOException if the indexing cannot happen.
     */
    void indexTranslationUnit(TranslationUnit tu);

    /**
     * Indexes a given translation unit, possibly overwriting an existing one with the same source.
     * @param tu the translation unit to index.
     * @param overwrite true to overwrite any existing unit that has the same source content (codes excluded)
     */
    public void indexTranslationUnit (TranslationUnit tu,
    	boolean overwrite);
    
    /**
     * Deletes a TranslationUnit based on the id.
     * @param id The Unique ID of the TU to delete
     * @throws OkapiIOException if the delete can not happen
     */
    void delete(String id);

    /**
     * Updates a TranslationUnit.
     * @param tu The TranslationUnit to update
     * @throws OkapiIOException if the update can not happen
     */
    public void update(TranslationUnit tu);
    
    /**
     * Forces a commit against the index.
     * @throws OkapiIOException if the commit can not happen
     */
    void commit();
}
