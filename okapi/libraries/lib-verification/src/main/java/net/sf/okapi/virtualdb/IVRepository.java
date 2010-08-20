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

package net.sf.okapi.virtualdb;

import net.sf.okapi.common.resource.RawDocument;

public interface IVRepository {

	/**
	 * Gets the document associated with a given id.
	 * @param docId the id of the document to retrieve.
	 * @return the document associated with the given id.
	 */
	public IVDocument getDocument (String docId);
	
	/**
	 * Creates an iterable object for all the documents contained into this repository.
	 * @return an new iterable object for all the items contained into this repository.
	 */
	public Iterable<IVDocument> documents ();

	/**
	 * Creates an iterable object for all the items contained into this repository.
	 * @return an new iterable object for all the items contained into this repository.
	 */
	public Iterable<IVItem> items ();

	/**
	 * Creates an iterable object for all the virtual text units contained into this document.
	 * @return an new iterable object for all the virtual text units contained into this document.
	 */
	public Iterable<IVTextUnit> textUnits ();

	/**
	 * Imports a document into this repository.
	 * @param rawDoc the document to import (must be URI based).
	 * @return the document id of the imported document.
	 */
	public String importDocument (RawDocument rawDoc);

	/**
	 * Removes a given document from this repository.
	 * @param doc the virtual document to remove.
	 */
	public void removeDocument (IVDocument doc);
	
	/**
	 * Gets the first virtual document in this repository. 
	 * @return the first virtual document in this repository or null.
	 */
	public IVDocument getFirstDocument ();
	
	public void close ();

}
