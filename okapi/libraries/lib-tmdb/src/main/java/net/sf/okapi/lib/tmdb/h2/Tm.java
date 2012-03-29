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

package net.sf.okapi.lib.tmdb.h2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.Util;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.IRecordSet;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.DbUtil.PageMode;
import net.sf.okapi.lib.tmdb.filter.FilterNode;
import net.sf.okapi.lib.tmdb.filter.OperatorNode;
import net.sf.okapi.lib.tmdb.filter.ValueNode;
import net.sf.okapi.lib.tmdb.filter.ValueNode.TYPE;

public class Tm implements ITm {

	private final Repository store;
	private final String uuid;
	
	private String name;
	private String segTable;
	private String tuTable;
	private PreparedStatement pstmGet;
	private List<String> recordFields;
	
	private DbUtil.PageMode pageMode = PageMode.EDITOR;
	private long limit = 500;
	private boolean needPagingRefresh = true; // Must be set to true anytime we change the row count
	private long totalRows;
	private long pageCount;
	private long currentPage = -1; // 0-based
	private boolean usePagingType2 = true;
	private boolean testMode = true;

	private PreparedStatement pstmAddSeg;
	private PreparedStatement pstmAddTu;
	private ArrayList<String> existingTuFields;
	private ArrayList<String> existingSegFields;
	private LinkedHashMap<String, Object> fieldsToImport;
	
	private PreparedStatement pstmUpdSeg;
	private ArrayList<String> updSegFields;

	private PreparedStatement pstmAnchors;
	private ArrayList<Long> anchors;
	private String rownumSubQuery;
	
	private FilterNode filterRoot;
	private String whereClause;
	LinkedHashMap<String, Boolean> orderByFields;
	private String orderByClause;
	
	private String toSQL (FilterNode node) {
		String tmp = "";
		if ( node == null ) return tmp;
		if ( node.isOperator() ) {
			OperatorNode on = (OperatorNode)node;
			String sqlOp = "";
			switch ( on.getOperator().getType() ) {
			case EQUALS: sqlOp = "="; break;
			case NOT: sqlOp = "NOT"; break;
			case OR: sqlOp = "OR"; break;
			case AND: sqlOp = "AND"; break;
			}
			
			if ( on.isBinary() ) {
				tmp = toSQL(on.getRight());
				tmp = sqlOp + " " + tmp + ")";
				tmp = "(" + toSQL(on.getLeft()) + " " + tmp;
			}
			else { // Unary operator
				tmp = "(" + sqlOp + " ";
				tmp = tmp + toSQL(on.getRight()) + ")"; 
			}
		}
		else {
			ValueNode vn = (ValueNode)node;
			if ( vn.isField() ) {
				String fn = vn.getStringValue();
				if ( DbUtil.isSegmentField(fn) ) {
					return segTable+".\""+fn+"\"";
				}
				else {
					return tuTable+".\""+fn+"\"";
				}
			}
			else { // Constant
				if ( vn.getType() == TYPE.BOOLEAN ) {
					return vn.getBooleanValue() ? "TRUE" : "FALSE";
				}
				if ( vn.getType() == TYPE.STRING ) {
					return vn.getStringValue();
				}
			}
			return "ERROR";
		}
		return tmp;
	}

	public Tm (Repository store,
		String uuid,
		String name)
	{
		this.store = store;
		this.uuid = uuid;
		updateName(name);
	}
	
	@Override
	protected void finalize() throws Throwable {
        close();
        super.finalize();
	}

	private void updateName (String name) {
		this.name = name;
		this.segTable = "\""+name+"_SEG\"";
		this.tuTable = "\""+name+"_TU\"";
		
		// Reset the sort clause
		if ( Util.isEmpty(orderByFields) ) {
			orderByClause = "\""+DbUtil.SEGKEY_NAME+"\""; // Defaulr
		}
		else {
			StringBuilder tmp = new StringBuilder();
			for ( String fn : orderByFields.keySet() ) {
				if ( tmp.length() > 0 ) tmp.append(", ");
				tmp.append(
					(DbUtil.isSegmentField(fn) ? segTable : tuTable) +
					".\"" + fn + "\" " +
					(orderByFields.get(fn) ? "ASC" : "DESC"));
			}
			orderByClause = tmp.toString();
		}
		
		// Reset the filter clause
		if ( filterRoot == null ) whereClause = "";
		else {
			whereClause = toSQL(filterRoot);
		}
		
		updateMainQueries();
	}
	
	private void closeAddStatements ()
		throws SQLException
	{
		if ( pstmAddSeg != null ) {
			pstmAddSeg.close();
			pstmAddSeg = null;
		}
		if ( pstmAddTu != null ) {
			pstmAddTu.close();
			pstmAddTu = null;
		}
	}
	
	private void closeUpdateStatements ()
		throws SQLException
	{
		if ( pstmUpdSeg != null ) {
			pstmUpdSeg.close();
			pstmUpdSeg = null;
			updSegFields = null;
		}
	}

	public void close () {
		try {
			if ( pstmGet != null ) {
				pstmGet.close();
				pstmGet = null;
			}
			closeAddStatements();
			closeUpdateStatements();
			if ( pstmAnchors != null ) {
				pstmAnchors.close();
				pstmAnchors = null;
				anchors = null;
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean getHasIndex () {
		//TODO: store the info 
		return true;
	}

	@Override
	public List<String> getAvailableFields () {
		return store.getAvailableFields(name);
	}

	@Override
	public String getDescription () {
		String res[] = store.getTmData(uuid);
		return res[1];
	}

	@Override
	public String getName () {
		return name;
	}

	@Override
	public String getUUID () {
		return uuid;
	}

	public void rename (String newName) {
		store.renameTm(name, newName);
		updateName(newName);
		//TODO: predefined statements need to be reset too
	}
	
	@Override
	public void setRecordFields (List<String> names) {
		recordFields = names;
		updateMainQueries();
	}
	
	private void updateMainQueries () {
		try {
			// Create a prepared statement to use to retrieve the selection
			if ( pstmGet != null ) {
				pstmGet.close();
			}
			// Check if we have at least one field that is TU-level
			boolean hasTUField = false;
			if ( recordFields != null ) {
				for ( String name : recordFields ) {
					if ( !DbUtil.isSegmentField(name) ) {
						hasTUField = true;
						break;
					}
				}
			}
			
			StringBuilder tmp;
			if ( hasTUField ) {
				tmp = new StringBuilder(String.format("SELECT %s.\"%s\", %s.\"%s\"", segTable, DbUtil.SEGKEY_NAME, segTable, DbUtil.FLAG_NAME));
				if ( recordFields != null ) {
					for ( String name : recordFields ) {
						if ( DbUtil.isSegmentField(name) ) tmp.append(", "+segTable+".\""+name+"\"");
						else tmp.append(", "+tuTable+".\""+name+"\"");
					}
				}
				tmp.append(" FROM "+segTable+" LEFT JOIN "+tuTable+" ON "+segTable+".\""+DbUtil.TUREF_NAME+"\"="+tuTable+".TUKEY");
				
			}
			else { // Simple select in the segment table
				tmp = new StringBuilder(String.format("SELECT \"%s\", \"%s\"", DbUtil.SEGKEY_NAME, DbUtil.FLAG_NAME));
				if ( recordFields != null ) {
					for ( String name : recordFields ) {
						tmp.append(", \""+name+"\"");
					}
				}
				tmp.append(" FROM \""+name+"_SEG\"");
			}
			
			if ( testMode ) {
				tmp.append(String.format(" %s ORDER BY %s LIMIT ? OFFSET ?",
					( Util.isEmpty(whereClause) ? "" : "WHERE "+whereClause),
					orderByClause));
			}
			else {
				tmp.append(String.format(" WHERE \"%s\">=? ORDER BY \"%s\" LIMIT ?", DbUtil.SEGKEY_NAME, DbUtil.SEGKEY_NAME));
			}

			pstmGet = store.getConnection().prepareStatement(tmp.toString(),
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			rownumSubQuery = String.format("(SELECT \"%s\", ROWNUM() AS R FROM %s)", DbUtil.SEGKEY_NAME, segTable);
			
			if ( usePagingType2 ) {
				// Create statement for the anchors
				if ( pageMode == PageMode.ITERATOR ) {
					tmp = new StringBuilder(String.format(
						"SELECT \"%s\", R FROM (SELECT \"%s\", ROWNUM() AS R FROM %s) WHERE (R=1 OR MOD(R, ?)=1)", DbUtil.SEGKEY_NAME, DbUtil.SEGKEY_NAME, segTable));
				}
				else {
					tmp = new StringBuilder(String.format(
						"SELECT \"%s\", R FROM (SELECT \"%s\", ROWNUM() AS R FROM %s) WHERE (R=1 OR MOD(R, (?-1))=1)", DbUtil.SEGKEY_NAME, DbUtil.SEGKEY_NAME, segTable));
				}
				pstmAnchors = store.getConnection().prepareStatement(tmp.toString(),
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			}
			
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	public void moveBeforeFirstPage () {
		currentPage = -1;
	}
	
	@Override
	public void setPageSize (long size) {
		if ( size < 3 ) this.limit = 3;
		else this.limit = size;
		needPagingRefresh = true;
	}
	
	@Override
	public long getPageSize () {
		return limit;
	}
	
	@Override
	public void startImport () {
		try {
			closeAddStatements();
			closeUpdateStatements();
			// Get the list of the original existing fields
			// This list will be use until the end of the import
			// It will be update with any added field from the API, not from the database
			existingTuFields = store.getFields(name, false);
			existingSegFields = store.getFields(name, true);
			// Add the Flag field to the list, so we don't try to re-create it
			existingSegFields.add(DbUtil.FLAG_NAME);
			// Create the list of the fields to import (to use with the pre-defined statement
			fieldsToImport = new LinkedHashMap<String, Object>();
			
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void finishImport () {
		try {
			closeAddStatements();
			existingTuFields = null;
			existingSegFields = null;
			fieldsToImport = null;
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long addRecord (long tuKey,
		Map<String, Object> tuFields,
		Map<String, Object> segFields)
	{
		//TODO: need to wrap this into a transaction
		try {
			verifyFieldsToImport(false, tuFields, false);
			verifyFieldsToImport(true, segFields, false);
		
			fillStatement(false, tuFields, 0); // tuKey not used here
			pstmAddTu.executeUpdate();
			if ( tuKey == -1 ) {
				ResultSet keys = pstmAddTu.getGeneratedKeys();
				if ( keys.next() ) {
					tuKey = keys.getLong(1);
				}
			}

			// It's unlikely there are no segment-level fields but it could happens 
			if ( pstmAddSeg != null ) {
				fillStatement(true, segFields, tuKey);
				pstmAddSeg.executeUpdate();
			}
			
			// We changed the number of rows
			needPagingRefresh = true;
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		return tuKey;
	}

	private void fillStatement (boolean segmentLevel,
		Map<String, Object> fields,
		long tuKey)
		throws SQLException
	{
		PreparedStatement pstm;
		if ( segmentLevel ) {
			pstm = pstmAddSeg;
		}
		else {
			pstm = pstmAddTu;
		}
		if ( pstm == null ) {
			return; // Nothing to do
		}
		
		int n = 1;
		Object value;
		for ( String name : fieldsToImport.keySet() ) {
			// Skip fields for the other statement
			if ( segmentLevel ) {
				if ( !DbUtil.isSegmentField(name) ) continue;
			}
			else {
				if ( DbUtil.isSegmentField(name) ) continue;
			}
			// Get the value from the provided map or from the defaults
			// Empty list is treat like it's all defaults
			if ( fields.containsKey(name) ) {
				value = fields.get(name);
			}
			else {
				// Not in the current set of fields provided
				// Set the value to its default
				value = fieldsToImport.get(name);
			}

			if ( value instanceof String ) {
				pstm.setString(n, (String)value);
			}
			else if ( value instanceof Boolean ) {
				pstm.setBoolean(n, (Boolean)value);
			}
			else if ( value instanceof Integer ) {
				pstm.setLong(n, ((Integer)value).longValue());
			}
			else if ( value instanceof Long ) {
				pstm.setLong(n, (Long)value);
			}
			else if ( value == null ) {
				pstm.setString(n, null);
			}
			n++;
		}

		// If this is the segment table: set TUREF, which should be the last field
		if ( segmentLevel ) {
			pstm.setLong(n, tuKey);
		}
	}
	
	private void verifyFieldsToImport (boolean segmentLevel,
		Map<String, Object> fields,
		boolean useValuesAsDefault)
	{
		try {
			ArrayList<String> existingFields;
			if ( segmentLevel ) existingFields = existingSegFields;
			else existingFields = existingTuFields;
			
			LinkedHashMap<String, String> fieldsToCreate = null;
			boolean hasNewFieldToImport = false;
			
			if ( !Util.isEmpty(fields) ) {

				String type = null;
				// Go through the list of fields
				// and add the ones that do not exist to the list of fields to create
				for ( String name : fields.keySet() ) {
					// This is a TU-level field
					boolean hasFieldToCreate = false;
					if ( !existingFields.contains(name) ) {
						hasFieldToCreate = true;
						if ( fieldsToCreate == null ) {
							fieldsToCreate = new LinkedHashMap<String, String>();
						}
					}
	
					if ( !fieldsToImport.containsKey(name) ) {
						Object value = fields.get(name);
						if ( value instanceof String ) {
							type = "VARCHAR";
							if ( useValuesAsDefault ) fieldsToImport.put(name, value);
							else fieldsToImport.put(name, null);
							hasNewFieldToImport = true;
						}
						else if (( value instanceof Long ) || ( value instanceof Integer )) {
							type = "INTEGER";
							if ( useValuesAsDefault ) fieldsToImport.put(name, value);
							else fieldsToImport.put(name, 0);
							hasNewFieldToImport = true;
						}
						else if ( value instanceof Boolean ) {
							type = "BOOLEAN";
							if ( useValuesAsDefault ) fieldsToImport.put(name, value);
							else fieldsToImport.put(name, false);
							hasNewFieldToImport = true;
						}
						else {
							throw new RuntimeException("Invalid field type to add.");
						}
					}
					
					// If the field is to create type should be set because
					// it was also a field that wasn't in the fieldsToImport list
					if ( hasFieldToCreate ) {
						// Nothing to add, move on to the next field
						fieldsToCreate.put(name, type);
					}
				}
//TODO: detect new locale and make sure all fields for the locale are added (e.g. codes not just text)

				// Create the new fields as needed, and update the lists
				// The lists can be null or empty in this call
				store.createNewFields(name, segmentLevel, fieldsToCreate, existingFields);
			}
			
			// Create or re-create the statement to insert the entry
			if ( segmentLevel ) {
				if (( pstmAddSeg == null ) || !Util.isEmpty(fieldsToCreate) ) {
					if ( pstmAddSeg != null ) {
						pstmAddSeg.close();
					}
					boolean first = true;
					int count = 0;
					StringBuilder tmp = new StringBuilder("INSERT INTO \""+name+"_SEG\" (");
					for ( String name : fieldsToImport.keySet() ) {
						if ( !DbUtil.isSegmentField(name) ) continue;  // Skip over TU level fields
						if ( first ) {
							first = false;
							tmp.append("\""+name+"\"");
						}
						else {
							tmp.append(", \""+name+"\"");
						}
						count++;
					}
					tmp.append((first ? "" : ", ")+"\""+DbUtil.TUREF_NAME+"\") VALUES (?"); // Always include TUREF at the end
					for ( int i=0; i<count; i++ ) {
						tmp.append(", ?");
					}
					tmp.append(");");
					pstmAddSeg = store.getConnection().prepareStatement(tmp.toString());
				}
			}
			else {
				// Create or re-create the statement to insert the entry
				if (( pstmAddTu == null ) || !Util.isEmpty(fieldsToCreate) || hasNewFieldToImport ) {
					if ( pstmAddTu != null ) {
						pstmAddTu.close();
					}
					StringBuilder tmp;
					if ( !Util.isEmpty(fieldsToImport) ) {
						boolean first = true;
						int count = 0;
						tmp = new StringBuilder("INSERT INTO \""+name+"_TU\" (");
						for ( String name : fieldsToImport.keySet() ) {
							if ( DbUtil.isSegmentField(name) ) continue; // Skip over segment-level fields
							if ( first ) {
								first = false;
								tmp.append("\""+name+"\"");
							}
							else {
								tmp.append(", \""+name+"\"");
							}
							count++;
						}
						tmp.append(") VALUES (");
						for ( int i=0; i<count; i++ ) {
							tmp.append((i==0) ? "?" : ", ?");
						}
						tmp.append(");");
					}
					else { // We need to create the statement but have no fields
						// In that case we just pass the TUKey with NULL
						tmp = new StringBuilder("INSERT INTO \""+name+"_TU\" (TUKEY) VALUES (NULL)");
					}
					pstmAddTu = store.getConnection().prepareStatement(tmp.toString(), Statement.RETURN_GENERATED_KEYS);
				}
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getLocales () {
		return store.getTmLocales(name);
	}

	@Override	
	public IRecordSet refreshCurrentPage () {
		long oldPage = currentPage;
		needPagingRefresh = true;
		checkPagingVariables();
		if ( pageCount > oldPage ) currentPage = oldPage;
		else if ( pageCount > 0 ) currentPage = pageCount-1; 
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}
	
	@Override
	public IRecordSet getFirstPage () {
		checkPagingVariables();
		currentPage = 0;
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}

	@Override
	public IRecordSet getLastPage () {
		checkPagingVariables();
		currentPage = pageCount-1;
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}

	@Override
	public IRecordSet getNextPage () {
		checkPagingVariables();
		if ( currentPage >= pageCount-1 ) return null; // Last page reached
		currentPage++;
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}

	@Override
	public IRecordSet getPreviousPage () {
		checkPagingVariables();
		if ( currentPage <= 0 ) return null; // First page reached
		currentPage--;
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}

	private void checkPagingVariables () {
		// Do we need to re-compute the paging variables
		if ( !needPagingRefresh ) return;
		
		totalRows = store.getTotalSegmentCount(name);
		if ( totalRows < 1 ) {
			pageCount = 0;
		}
		else {
			if ( pageMode == PageMode.EDITOR ) {
				pageCount = (totalRows-1) / (limit-1); // -1 for overlap
				if ( totalRows == 1 ) pageCount++;
				else if ( (totalRows-1) % (limit-1) > 0 ) pageCount++; // Last page
			}
			else {
				pageCount = totalRows / limit; // -1 for overlap
				if ( totalRows % limit > 0 ) pageCount++; // Last page
			}
		}
		
		currentPage = -1;
		needPagingRefresh = false; // Stable until we add or delete rows or change the page-size
		//TODO: handle sort on other fields

		if ( usePagingType2 ) {
			try {
				pstmAnchors.setLong(1, limit);
				ResultSet result = pstmAnchors.executeQuery();
				anchors = new ArrayList<Long>();
				while ( result.next() ) {
					//long rn = result.getLong(2);
					anchors.add(result.getLong(1));
				}
			}		
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
	private IRecordSet moveToPage (long topSegKey) {
		if ( topSegKey < 1 ) return null;
		ResultSet result = null;
		try {
			if ( testMode ) {
				pstmGet.setLong(1, limit);
				pstmGet.setLong(2, (pageMode == PageMode.EDITOR ? limit-1 : limit) * currentPage);
			}
			else {
				pstmGet.setLong(1, topSegKey);
				pstmGet.setLong(2, limit);
			}
			result = pstmGet.executeQuery();
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		return new RecordSet(result);
	}

	private long getFirstKeySegValueForPage (long page) {
		long key = -1;
		if ( usePagingType2 ) {
			if ( anchors.size() > page ) {
				key = anchors.get((int)page);
			}
			else {
				key = -1; //temporary for test
			}
		}
		
		if ( page == 0 ) {
			long oldMethod = 1;
			return usePagingType2 ? key : oldMethod;
		}
			
		if ( pageMode == PageMode.EDITOR ) {
			long oldMethod = (page * (limit-1)) + 1;
			return usePagingType2 ? key : oldMethod;
		}
		else {
			long oldMethod = (page * limit) + 1;
			return usePagingType2 ? key : oldMethod;
		}
	}

	@Override
	public long getCurrentPage () {
		return currentPage;
	}

	@Override
	public long getPageCount () {
		return pageCount;
	}

	@Override
	public PageMode getPageMode () {
		return pageMode;
	}

	@Override
	public void setPageMode (PageMode pageMode) {
		this.pageMode = pageMode;
		needPagingRefresh = true;
	}

	@Override
	public void addLocale (String localeId) {
		localeId = localeId.toUpperCase();
		List<String> existing = getLocales();
		if ( existing.contains(localeId) ) {
			return; // This locale exists already
		}

		// Locale does not exists we can add it
		Statement stm = null;
		try {
			StringBuilder tmp = new StringBuilder();
			tmp.append(String.format("ALTER TABLE \"%s%s\" ADD \"%s\" VARCHAR; ",
				name, "_SEG", DbUtil.TEXT_PREFIX+localeId));
			tmp.append(String.format("ALTER TABLE \"%s%s\" ADD \"%s\" VARCHAR;",
				name, "_SEG", DbUtil.CODES_PREFIX+localeId));
			stm = store.getConnection().createStatement();
			stm.execute(tmp.toString());
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void deleteLocale (String localeId) {
		List<String> existing = getLocales();
		if ( existing.size() < 2 ) {
			return; // Must keep at least one locale
		}
		localeId = localeId.toUpperCase();
		if ( !existing.contains(localeId) ) {
			return; // This locale does not exist
		}

		// Locale does not exists and we need to remove all it fields
		Statement stm = null;
		try {
			StringBuilder tmp = new StringBuilder();
			stm = store.getConnection().createStatement();
			ResultSet result = stm.executeQuery("SHOW COLUMNS FROM \""+name+"_SEG\"");
			while ( result.next() ) {
				String fn = result.getString(1);
				int n = fn.lastIndexOf(DbUtil.LOC_SEP);
				if ( n > -1 ) {
					if ( fn.substring(n+1).equals(localeId) ) {
						// This field is to be removed
						tmp.append(String.format("ALTER TABLE \"%s%s\" DROP COLUMN \"%s\"; ",
							name, "_SEG", fn));
					}
				}
			}
			if ( tmp.length() > 0 ) {
				stm.execute(tmp.toString());
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void renameLocale (String currentCode,
		String newCode)
	{
		List<String> existing = getLocales();
		if ( !existing.contains(currentCode) ) {
			return; // There is not a locale with that name
		}
		if ( existing.contains(newCode) ) {
			return; // The name/code is already used
		}

		Statement stm = null;
		try {
			StringBuilder tmp = new StringBuilder();
			stm = store.getConnection().createStatement();
			ResultSet result = stm.executeQuery("SHOW COLUMNS FROM \""+name+"_SEG\"");
			while ( result.next() ) {
				String fn = result.getString(1);
				int n = fn.lastIndexOf(DbUtil.LOC_SEP);
				if ( n > -1 ) {
					if ( fn.substring(n+1).equals(currentCode) ) {
						// This field is to be renamed
						String fnRoot = fn.substring(0, n+1);
						tmp.append(String.format("ALTER TABLE \"%s%s\" ALTER COLUMN \"%s\" RENAME TO \"%s\"; ",
							name, "_SEG", fn, fnRoot+newCode));
					}
				}
			}
			if ( tmp.length() > 0 ) {
				stm.execute(tmp.toString());
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void updateRecord (long segKey,
		Map<String, Object> tuFields,
		Map<String, Object> segFields)
	{
		try {
			if ( segKey < 0 ) {
				throw new IllegalArgumentException("Illegal SegKey value.");
			}

			boolean changed = (pstmUpdSeg == null);
			if ( updSegFields == null ) {
				updSegFields = new ArrayList<String>(segFields.keySet());
			}
			else { // Update the existing list
				// Use brute force for now
				updSegFields = new ArrayList<String>(segFields.keySet());
				changed = true;
			}
			
			if ( changed ) {
				if ( pstmUpdSeg != null ) {
					pstmUpdSeg.close();
					pstmUpdSeg = null;
				}
				
				StringBuilder tmp = new StringBuilder("UPDATE \""+name+"_SEG\"");
				for ( int i=0; i<updSegFields.size(); i++ ) {
					tmp.append(String.format("%s%s \"%s\"=?", (i==0 ? "" : ","), (i==0 ? " SET" : ""), updSegFields.get(i)));
				}
				tmp.append(String.format(" WHERE \"%s\"=?", DbUtil.SEGKEY_NAME));
				
				pstmUpdSeg = store.getConnection().prepareStatement(tmp.toString());
			}
			// Fill the statement
			int i = 1;
			for ( String fn : segFields.keySet() ) {
				if ( fn.equals(DbUtil.FLAG_NAME) ) {
					pstmUpdSeg.setBoolean(i, (Boolean)segFields.get(fn));
				}
				else {
					pstmUpdSeg.setString(i, (String)segFields.get(fn));
				}
				i++;
			}
			// Fill the SegKey value
			pstmUpdSeg.setLong(i, segKey);
			pstmUpdSeg.execute();
			
			//TODO: Update the TM index if we have one
			//TODO: Check if this TM is indexed
//			IIndexAccess ia = store.getIndexAccess();
//			ia.getWriter().update(tu)
			
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addField (String fullName) {
		// Cannot add pre-defined fields
		if ( DbUtil.isPreDefinedField(fullName) ) {
			return;
		}
		
		String suffix = "_TU";
		
		// Check the locale
		String loc = DbUtil.getFieldLocale(fullName);
		if ( loc != null ) {
			suffix = "_SEG";
			if ( !getLocales().contains(loc) ) {
				// Not an existing locale
				return;
			}
		}

		Statement stm = null;
		try {
			stm = store.getConnection().createStatement();
			String tmp = String.format("ALTER TABLE \"%s%s\" ADD \"%s\" VARCHAR",
				name, suffix, fullName);
			stm.execute(tmp);
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void deleteField (String fullName) {
		// Block deletion of system fields
		if ( DbUtil.isPreDefinedField(fullName) ) {
			return;
		}
		String suffix = "_TU";
		if ( DbUtil.isSegmentField(fullName) ) suffix = "_SEG";
		
		Statement stm = null;
		try {
			stm = store.getConnection().createStatement();
			String tmp = String.format("ALTER TABLE \"%s%s\" DROP COLUMN \"%s\"",
				name, suffix, fullName);
			stm.execute(tmp);
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void renameField (String currentFullName,
		String newFullName)
	{
		// Block renaming of system fields
		if ( DbUtil.isPreDefinedField(currentFullName) ) {
			// Cannot rename those fields
			return;
		}
		List<String> existing = getAvailableFields();
		if ( !existing.contains(currentFullName) ) {
			// This field does not exists: do nothing
			return;
		}
		if ( existing.contains(newFullName) ) {
			// This field does exists already: do nothing
			return;
		}

		// Check if the locale part changes
		String loc1 = DbUtil.getFieldLocale(currentFullName);
		String loc2 = DbUtil.getFieldLocale(newFullName);
		if ( loc1 != null ) {
			if ( loc2 != null ) {
				if ( !loc1.equals(loc2) ) {
					// Check if it goes to an existing locale
					if ( !getLocales().contains(loc2) ) {
						return;
					}
				}
			}
			// It is allowed to make a segment level field into a unit level one
		}

		boolean sameTable = (( loc1 == null ) && ( loc2 == null)) || (( loc1 != null ) && ( loc2 != null ));
		if ( !sameTable ) {
			throw new RuntimeException("Changing level of field not implemented yet");
		}
		
		String suffix = "_SEG";
		if ( loc1 == null ) suffix = "_TU";
		
		Statement stm = null;
		try {
			stm = store.getConnection().createStatement();
			StringBuilder tmp = new StringBuilder();
			tmp.append(String.format("ALTER TABLE \"%s%s\" ALTER COLUMN \"%s\" RENAME TO \"%s\"; ",
				name, suffix, currentFullName, newFullName));
			stm.execute(tmp.toString());
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void setSortOrder (LinkedHashMap<String, Boolean> fields) {
		orderByFields = fields;
		updateName(name);
		needPagingRefresh = true;
	}

	@Override
	public long getTotalSegmentCount () {
		return store.getTotalSegmentCount(name);
	}

	@Override
	public void deleteSegments (List<Long> segKeys) {
		store.deleteSegments(name, segKeys);
	}

	@Override
	public IRepository getRepository () {
		return store;
	}

	@Override
	public long findPageForSegment (long segKey) {
		if ( pageCount < 1 ) return -1;
//TODO
		Statement stm = null;
		try {
			stm = store.getConnection().createStatement();
			String tmp = String.format(
				"SELECT \"%s\", R FROM %s WHERE (\"%s\" / %d)=1 AND MOD(\"%s\", %d)=0",
				DbUtil.SEGKEY_NAME, rownumSubQuery, DbUtil.SEGKEY_NAME, segKey, DbUtil.SEGKEY_NAME, segKey);
			
//			tmp = String.format(
//				"SELECT \"%s\", R FROM %s WHERE (-%s=-%d)",
//				DbUtil.SEGKEY_NAME, rowSubQuery, DbUtil.SEGKEY_NAME, segKey);

			ResultSet rs = stm.executeQuery(tmp.toString());
			if ( rs.next() ) {
				// Get the row number for this entry
				long rn = rs.getLong(2);
				// Compute which page this row number belong to (page is 0-based)
				if ( pageMode == PageMode.ITERATOR ) {
					long page = (rn / limit) - ((rn % limit) == 0 ? 1 : 0);
					return page;
				}
				else {
					long page = rn / (limit-1);
					return page;
				}
			}
			else {
				// Not entry with such a key
				return -1;
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public IRecordSet getPage (long pageIndex) {
		checkPagingVariables();
		if (( pageIndex < 0 ) || ( pageIndex >= pageCount )) {
			return null;
		}
		currentPage = pageIndex;
		return moveToPage(getFirstKeySegValueForPage(currentPage));
	}

	@Override
	public void setFilter (FilterNode root) {
		filterRoot = root;
		updateName(name);
		needPagingRefresh = true;
	}
	
}
