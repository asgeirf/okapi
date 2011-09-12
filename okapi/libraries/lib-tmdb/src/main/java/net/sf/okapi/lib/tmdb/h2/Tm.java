/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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
import net.sf.okapi.lib.tmdb.ITm;

public class Tm implements ITm {

	private Repository store;
	private String name;
	private String uuid;
	private PreparedStatement pstmGet;
	private long offset = 0;
	private long limit = 30;
	private long currentRowCount = 0;

	private PreparedStatement pstmAddSeg;
	private PreparedStatement pstmAddTu;
	private ArrayList<String> existingTuFields;
	private ArrayList<String> existingSegFields;
	private LinkedHashMap<String, Object> fieldsToImport;
	
	
	public Tm (Repository store,
		String uuid,
		String name)
	{
		this.store = store;
		this.name = name;
		this.uuid = uuid;
	}
	
	@Override
	protected void finalize() throws Throwable {
        close();
        super.finalize();
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

	public void close () {
		try {
			if ( pstmGet != null ) {
				pstmGet.close();
				pstmGet = null;
			}
			closeAddStatements();
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
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
		name = newName;
	}
	
	@Override
	public void setRecordFields (List<String> names) {
		try {
			// Create a prepared statement to use to retrieve the selection
			if ( pstmGet != null ) {
				pstmGet.close();
			}
			// Check if we have at least one field that is TU-level
			boolean hasTUField = false;
			for ( String name : names ) {
				if ( !isSegmentField(name) ) {
					hasTUField = true;
					break;
				}
			}
			
			StringBuilder tmp;
			if ( hasTUField ) {
				String tuTable = "\""+name+"_TU\"";
				String segTable = "\""+name+"_SEG\"";
				tmp = new StringBuilder("SELECT "+segTable+".SEGKEY, "+segTable+".FLAG");
				for ( String name : names ) {
					if ( isSegmentField(name) ) tmp.append(", "+segTable+".\""+name+"\"");
					else tmp.append(", "+tuTable+".\""+name+"\"");
				}
				tmp.append(" FROM "+segTable+" LEFT JOIN "+tuTable+" ON "+segTable+".TUREF="+tuTable+".TUKEY");
				
			}
			else { // Simple select in the segment table
				tmp = new StringBuilder("SELECT SEGKEY, FLAG");
				for ( String name : names ) {
					tmp.append(", \""+name+"\"");
				}
				tmp.append(" FROM \""+name+"_SEG\"");
			}
			tmp.append(" LIMIT ? OFFSET ?");
			pstmGet = store.getConnection().prepareStatement( tmp.toString(),
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}

	private ResultSet getPage (long start) {
		if ( start < 0 ) return null;
		ResultSet result = null;
		try {
			pstmGet.setLong(1, limit);
			pstmGet.setLong(2, start);
			result = pstmGet.executeQuery();
			// Get the size of the page
			if ( result.last() ) {
				currentRowCount = result.getRow();
			}
			else {
				currentRowCount = 0;
			}
			result.beforeFirst(); // Reset the current row
			if ( currentRowCount < limit ) offset = -1; // Last or first page
			else offset = start;
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public void moveBeforeFirstPage () {
		offset = 0;
	}
	
	@Override
	public ResultSet getFirstPage () {
		offset = 0;
		return getPage(offset);
	}

	@Override
	public ResultSet getLastPage () {
		// TODO Auto-generated method stub
//		offset = last row of query
//		offset -= pageSize;
		if ( offset < 0 ) offset = 0;
		return null;
	}

	@Override
	public ResultSet getNextPage () {
		// First row of next page is last of the current one
		return getPage(offset += (currentRowCount-1));
	}

	@Override
	public ResultSet getPreviousPage () {
		// Last row of previous page is first of the current one
		long start = offset - (limit-1);
		if ( start < 0 ) start = 0;
		return getPage(start);
	}

	@Override
	public void setPageSize (long size) {
		if ( size < 2 ) this.limit = 2;
		else this.limit = size;
	}
	
	private boolean isSegmentField (String name) {
		return (( name.indexOf(DbUtil.LANG_SEP) != -1 )
			|| name.equals("TUREF")
			|| name.equals("FLAG")
		); 
	}

	@Override
	public void startImport () {
		try {
			closeAddStatements();
			// Get the list of the original existing fields
			// This list will be use until the end of the import
			// It will be update with any added field from the API, not from the database
			existingTuFields = store.getFields(name, false);
			existingSegFields = store.getFields(name, true);
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
				if ( !isSegmentField(name) ) continue;
			}
			else {
				if ( isSegmentField(name) ) continue;
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
			
			if ( !Util.isEmpty(fields) ) {

				String type = null;
				// Go through the list of fields
				// and add the ones that do not exist to the list of fields to create
				for ( String name : fields.keySet() ) {
					// This is a TU-level field
					boolean add = false;
					if ( !existingFields.contains(name) ) {
						add = true;
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
						}
						else if (( value instanceof Long ) || ( value instanceof Integer )) {
							type = "INTEGER";
							if ( useValuesAsDefault ) fieldsToImport.put(name, value);
							else fieldsToImport.put(name, 0);
						}
						else if ( value instanceof Boolean ) {
							type = "BOOLEAN";
							if ( useValuesAsDefault ) fieldsToImport.put(name, value);
							else fieldsToImport.put(name, false);
						}
						else throw new RuntimeException("Invalid field type to add.");
					}
					
					// If the field is to create type should be set because
					// it was also a filed that wasn't in the fieldsToImport list
					if ( add ) {
						// Nothing to add, move on to the next field
						fieldsToCreate.put(name, type);
					}
				}
				
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
						if ( !isSegmentField(name) ) continue;  // Skip over TU level fields
						if ( first ) {
							first = false;
							tmp.append("\""+name+"\"");
						}
						else {
							tmp.append(", \""+name+"\"");
						}
						count++;
					}
					tmp.append(", TUREF) VALUES (?"); // Always include TUREF at the end
					for ( int i=0; i<count; i++ ) {
						tmp.append(", ?");
					}
					tmp.append(");");
					pstmAddSeg = store.getConnection().prepareStatement(tmp.toString());
				}
			}
			else {
				// Create or re-create the statement to insert the entry
				if (( pstmAddTu == null ) || !Util.isEmpty(fieldsToCreate) ) {
					if ( pstmAddTu != null ) {
						pstmAddTu.close();
					}
					StringBuilder tmp;
					if ( !Util.isEmpty(fieldsToCreate) ) {
						boolean first = true;
						int count = 0;
						tmp = new StringBuilder("INSERT INTO \""+name+"_TU\" (");
						for ( String name : fieldsToImport.keySet() ) {
							if ( isSegmentField(name) ) continue; // Skip over segment-level fields
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
}
