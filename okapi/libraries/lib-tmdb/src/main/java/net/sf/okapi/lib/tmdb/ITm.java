/*===========================================================================
  Copyright (C) 2011-2012 by the Okapi Framework contributors
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.okapi.lib.tmdb.DbUtil.PageMode;
import net.sf.okapi.lib.tmdb.filter.FilterNode;

/**
 * Provides implementation-agnostic access to an Olifant translation memory.
 */
public interface ITm {

	/**
	 * 1-Based index of the SegKey field in the result sets from the database.
	 */
	public static final int SEGKEY_FIELD = 1;
	
	/**
	 * 1-based index of the Flag field in the result sets from the database.
	 */
	public static final int FLAG_FIELD = 2;

	/**
	 * Gets the UUID of this TM.
	 * The UUID is a global universal unique identifier. It can be used for example
	 * to link display property with a specific TM.
	 * @return the UUID of this TM.
	 */
	public String getUUID ();
	
	/**
	 * Gets the name of this TM.
	 * The name of the TM is unique per repository.
	 * @return the name of this TM.
	 */
	public String getName ();
	
	/**
	 * Gets the description of this TM.
	 * @return the description of this TM.
	 */
	public String getDescription ();

	/**
	 * Gets whether or not this TM has an associated index for searching fuzzy matches.
	 * @return true if this TM is indexed, false otherwise.
	 */
	public boolean getHasIndex ();
	
	/**
	 * Gets a list of all available fields in this TM.
	 * <p>Note that the segKey and Flag fields are not included in this list.
	 * @return the list of all available fields in this TM.
	 */
	public List<String> getAvailableFields ();

	/**
	 * Rename this TM.
	 * If the new name is already used by another TM, nothing happens.
	 * @param newName the name name of the TM.
	 */
	public void rename (String newName);
	
	/**
	 * Sets the list of fields to be returned by {@link #getNextPage()} and other paging methods.
	 * <p>Note that the SegKey and Flag fields are always returned as the first and second fields
	 * of each record. If they are present in the parameter list, they should be ignored.
	 * @param names list of fields to be returned. 
	 */
	public void setRecordFields (List<String> names);
	
	/**
	 * Sets the sort order in which the records should be retrieved with {@link #getNextPage()}
	 * or other paging methods.
	 * @param fields list of the fields to sort on. The first on is the primary field,
	 * the second the secondary, etc.
	 * <p>The key of the map is the field full name, the boolean value is set to true for
	 * ascending order or false for descending order.
	 * <p>The fields must exist, but they may be not in the list of the fields returned by
	 * {@link #getNextPage()} or the other paging methods.
	 * Use a null or empty map to restore whatever default the natural order the back-end uses. 
	 */
	public void setSortOrder (LinkedHashMap<String, Boolean> fields);

	/**
	 * Sets the filter with which to query the records retrieved with {@link #getNextPage()}
	 * or other paging methods.
	 * @param root the root of the expression tree representing the filter.
	 * Use null to remove any filter expression.
	 */
	public void setFilter (FilterNode root);
	
	/**
	 * Prepares the system to import a set of new entries.
	 * This method must be called before calling {@link #addRecord(long, Map, Map)}.
	 * You must call {@link #finishImport()} to terminate the batch of import.
	 * @see #finishImport()
	 * @see #addRecord(long, Map, Map)
	 */
	public void startImport ();
	
	/**
	 * Finishes a batch of {@link #addRecord(long, Map, Map)}.
	 * @see #startImport()
	 * @see #addRecord(long, Map, Map)
	 */
	public void finishImport ();
	
	/**
	 * Adds a record to the repository.
	 * <p>You must call {@link #startImport()} once before calling this method, and call {@link #finishImport()}
	 * once all calls to this method are done for a given batch.
	 * @param tuKey the key for the text unit this record belongs.
	 * <b>You must use -1 when adding the first entry of this text unit</b> the call returns
	 * the text unit key that you can use for the subsequent call to add other records to
	 * that given text unit.
	 * @param tuFields the list of the text unit level fields. This list can be empty.
	 * @param segFields the list of the segment level fields. This list can be empty.
	 * @return the key of text unit of the added record. 
	 */
	public long addRecord (long tuKey,
		Map<String, Object> tuFields,
		Map<String, Object> segFields);
	
	/**
	 * Updates the given field for an existing record.
	 * <p>None of the fields can be a new field.
	 * <p>The SegKey value of a record can be obtained from the ResultSet returned by {@link #getNextPage()}
	 * or any other paging method.
	 * @param segKey the key of the segment to update. 
	 * @param tuFields the list of the text unit level fields to update.
	 * @param segFields the list of the segment level fields to update.
	 */
	public void updateRecord (long segKey,
		Map<String, Object> tuFields,
		Map<String, Object> segFields);
	
	/**
	 * Removes permanently a list of given segments from the database.
	 * If after this deletion, the TU-level entries that have no more attached segment 
	 * it are deleted automatically too.
	 * @param segKeys the keys of the segments to remove.
	 */
	public void deleteSegments (List<Long> segKeys);

	/**
	 * Sets the number of records a call to a paging method should return.
	 * @param size the number of records a call to a paging method should return.
	 * The minimum size of a page is 2. If a smaller value is given, the value is
	 * silently changed to 2.
	 */
	public void setPageSize (long size);
	
	/**
	 * Gets the current number of records per page.
	 * @return the current number of records per page.
	 */
	public long getPageSize ();
	
	/**
	 * Sets the type of pages the system returns.
	 * @param pageMode the type of pages the system should return.
	 * {@link PageMode#EDITOR} or {@link PageMode#ITERATOR} 
	 * <p>In {@link PageMode#EDITOR} mode the last record of the previous page
	 * is the first record of the next page, and the first record of the next page 
	 * is the last of the previous page. In other words: there is one record that is 
	 * common to each adjacent pages.
	 * <p>For example, if the database has 6 records numbered from 1 to 6 and 
	 * the page size is set to 3: There are 3 pages (not 2). The first record of 
	 * the first page is 1, the one of the second page is 3 (not 4), and the one
	 * of the last page is 5.
	 * <p>In {@link PageMode#ITERATOR} mode the last record of the previous page
	 * is the record before the first record of the next page, and the first record
	 * of the next page is the record after the last record of the previous page.
	 * In other words: page recored do not overlap. No record is common to several pages.
	 * <p>For example, if the database has 6 records numbered from 1 to 6 and the
	 * page size is set to 3: There are 2 pages. the first record of the first 
	 * page is 1 and the one of the second page is 4. 
	 */
	public void setPageMode (PageMode pageMode);
	
	/**
	 * Gets the type of page the system returns.
	 * @return the type of page the system returns.
	 * See {@link #setPageMode(int)} for details.
	 */
	public PageMode getPageMode ();
	
	/**
	 * Gets the records for the current page.
	 * @return the results for the current page, or null if there is no current page.
	 */
	public IRecordSet refreshCurrentPage ();
	
	/**
	 * Moves the page cursor before the first page.
	 * <p>After this call, calling {@link #getFirstPage()} and {@link #getNextPage()}
	 * has the same result.
	 * See {@link #setPageMode(int)} for details.
	 */
	public void moveBeforeFirstPage ();
	
	/**
	 * Gets the first page of records.
	 * This method is the same has calling {@link #moveBeforeFirstPage()} and then {@link #getNextPage()}.
	 * See {@link #setPageMode(int)} for details.
	 * @return the results for the first page, or null if there is no first page.
	 */
	public IRecordSet getFirstPage ();
	
	/**
	 * Gets the last page of records.
	 * @return the results for the last page, or null if there is no last page.
	 * Note that the number of records in the last page may be smaller than the 
	 * current page size.
	 * See {@link #setPageMode(int)} for details.
	 * @param the results for the last page, or null if there is no last page.
	 */
	public IRecordSet getLastPage ();
	
	/**
	 * Gets the next page of records.
	 * @return the results for the next page, or null if there is no next page.
	 */
	public IRecordSet getNextPage ();
	
	/**
	 * Gets the previous page of records.
	 * @return the results for the previous page, or null if there is no previous page.
	 */
	public IRecordSet getPreviousPage ();
	
	/**
	 * Finds the index of the page where a given segment is located.
	 * @param segKey the key of the segment to search for.
	 * @return the index of the page where the segment was found,
	 * or -1 if the key was not found. 
	 */
	public long findPageForSegment (long segKey);
	
	/**
	 * Gets the page for a given page index.
	 * The page is based on the current type of filtering/sorting.
	 * @param pageIndex the 0-based index of the page.
	 * @return the results for the given page, or null if an error occurs.
	 */
	public IRecordSet getPage (long pageIndex);

	/**
	 * Adds a given locale to the TM.
	 * <p>If the locale already exists for this TM, nothing happens.
	 * <p>Only the Text and Codes fields are created for the new locale.
	 * @param localeCode the locale code for the new locale.
	 * The code of the locale is case-insensitive.
	 */
	public void addLocale (String localeCode);
	
	/**
	 * Deletes all fields for a given locale.
	 * <p>If the locale does not exist in this TM, nothing happens.
	 * <p>A TM must have at least one locale, so if there is only a single locale left in
	 * this TM before the call, nothing happens. 
	 * @param localeCode the code of the locale to remove. 
	 * The code of the locale is case-insensitive.
	 */
	public void deleteLocale (String localeCode);

	/**
	 * Renames a locale.
	 * @param currentCode the current code of the locale to rename.
	 * If this code is not a current locale nothing happens. 
	 * @param newCode the new code to assign to the local.
	 * If this code is the code of another existing locale nothing happens.
	 */
	public void renameLocale (String currentCode,
		String newCode);
	
	/**
	 * Gets the list of the locales in this TM.
	 * @return the list of the locales in this TM.
	 */
	public List<String> getLocales ();
	
	/**
	 * Gets the zero-based index of the current page.
	 * @return the index of the current page, or -1 if no page is active.
	 * 0 is the first page.
	 */
	public long getCurrentPage ();
	
	/**
	 * Gets the number of pages available.
	 * @return the number of pages available.
	 */
	public long getPageCount ();

	/**
	 * Adds a new field to the TM.
	 * @param fullName the full name of the field to add.
	 * Locale-specific field must have their locale code suffix.
	 */
	public void addField (String fullName);
	
	/**
	 * Deletes a given field from this TM, all its data will be lost.
	 * <p>You cannot remove special fields. To remove all the fields of a locale use {@link #deleteLocale(String)}.
	 * @param fullName the full name of the field to remove.
	 * Locale-specific field must have their locale code suffix.
	 */
	public void deleteField (String fullName);
	
	/**
	 * Renames an existing field with a new name.
	 * <p>You cannot change a locale using this method.
	 * Special fields cannot be renamed.
	 * To change the locale suffix of all the fields of a locale use {@link #renameLocale(String, String)}.
	 * @param currentFullName the current full name.
	 * @param newFullName the new full name.
	 */
	public void renameField (String currentFullName,
		String newFullName);
	
	/**
	 * Gets the total number of segments in this TM.
	 * @return the total number of segments in this TM.
	 */
	public long getTotalSegmentCount ();

	/**
	 * Gets the repository where this TM is stored.
	 * @return the repository where this TM is stored.
	 */
	public IRepository getRepository ();
}
