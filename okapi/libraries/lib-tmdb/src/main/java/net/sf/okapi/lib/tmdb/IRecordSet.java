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

package net.sf.okapi.lib.tmdb;

/**
 * provides an implementation-agnostic access to a set of entries for the Olifant TM.
 * <p>The fields in each row are numbered starting at 1.
 * The first field is always the segKey field
 * The second field is always the flag field
 * The requested fields come after those two first fields.
 */
public interface IRecordSet {

	/**
	 * Moves the cursor forward one row from its current position.
	 * The cursor is initially positioned before the first entry; the first call to the
	 * method next makes the first row the current row; the second call makes the
	 * second row the current row, and so on.
	 * @return true if the new current row is valid; false if there are no more rows.
	 */
	boolean next ();
	
	/**
	 * Moves the cursor at the last entry of this set.
	 * @return true if the new current entry is valid; false if there are no more rows.
	 */
	boolean last ();

	/**
	 * Retrieves the value of the designated column in the current entry of this set as a boolean.
	 * @param index the index of the field to retrieve: the first column is 1, the second is 2
	 * @return the column value; if the value is null, the value returned is null.
	 */
	boolean getBoolean (int index);
	
	/**
	 * Retrieves the value of the designated column in the current entry of this set as a String.
	 * @param index the index of the field to retrieve: the first column is 1, the second is 2
	 * @return the column value; if the value is null, the value returned is null.
	 */
	String getString (int index);
	
	/**
	 * Retrieves the value of the given field name in the current entry of this set as a String.
	 * @param index the index of the field to retrieve: the first column is 1, the second is 2
	 * @return the column value; if the value is null, the value returned is null.
	 */
	String getString (String name);
	
	/**
	 * Retrieves the value of the given field name in the current entry of this set as an Object.
	 * @param index the index of the field to retrieve: the first column is 1, the second is 2
	 * @return the column value; if the value is null, the value returned is null.
	 */
	Object getObject (String name);
	
	/**
	 * Retrieves the value of the designated column in the current entry of this set as a long.
	 * @param index the index of the field to retrieve: the first column is 1, the second is 2
	 * @return the column value; if the value is null, the value returned is null.
	 */
	long getLong (int index);

	/**
	 * Gets the value of the flag field for the current entry.
	 * @return true or false.
	 */
	boolean getFlag ();
	
	/**
	 * Gets the segment key value for the current entry.
	 * @return the segment key value for the current entry.
	 */
	long getSegKey ();
	
	/**
	 * Gets the text unit reference for the current segment.
	 * @return the text unit reference for the current segment.
	 */
	long getTuRef ();

	/**
	 * Gets the number of fields in this entry.
	 * @return the number of fields in this entry.
	 */
	int getFieldCount ();
	
	/**
	 * Retrieves the name of the field for a given index.
	 * @param index the index of the field to look-up.
	 * @return the name of the field for the given index
	 */
	String getFieldName (int index);

}
