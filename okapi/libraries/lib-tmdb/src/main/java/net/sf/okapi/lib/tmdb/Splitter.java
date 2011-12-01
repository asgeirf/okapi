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

package net.sf.okapi.lib.tmdb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.okapi.lib.tmdb.IProgressCallback;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.DbUtil.PageMode;

public class Splitter implements Runnable {

	private final IProgressCallback callback;
	private final IRepository repo;
	private final String tmName;
	private final SplitterOptions options;
	
	private List<String> tmNames;
	private List<String> locales;
	
	public Splitter (IProgressCallback progressCallback,
		IRepository repo,
		String tmName,
		SplitterOptions options)
	{
		this.callback = progressCallback;
		this.repo = repo;
		this.tmName = tmName;
		this.options = options;
	}
	
	@Override
	public void run () {
		long totalCount = 0;
		ITm tm = null;
		ITm outTm = null;
		boolean canceled = false;
		
		try {
			callback.startProcess("Splitting TM...");
			
			//=== Split entries
			
			// Get the original TM and set it for iteration
			tm = repo.openTm(tmName);
			tm.setRecordFields(tm.getAvailableFields());
			tm.setPageMode(PageMode.ITERATOR);
			//tm.setPageSize(5);
			
			// Get the list of all initial locales
			locales = tm.getLocales();
			
			long currentCount = -1;
			int tmCount = 0;
			long prevTuKey = -1;
			long outTuKey = -1;
			
			ResultSet rs = tm.getFirstPage();
			while  (( rs != null ) && !canceled ) {
				while ( rs.next() && !canceled ) {
					totalCount++;
					currentCount++;
					
					// Do we need to start a new TM for this entry?
					if (( currentCount == 0 ) || ( currentCount > options.getEntriesPerPart() )) {
						if ( outTm != null ) outTm.finishImport();
						outTm = createNewTm(++tmCount);
						outTm.startImport();
						currentCount = 1;
						prevTuKey = -1;
					}
					
					// Add the entry to the output TM
					long tuKey = rs.getLong(DbUtil.TUREF_NAME);
					if ( tuKey != prevTuKey ) outTuKey = -1;
					
					LinkedHashMap<String, Object> tuFields = new LinkedHashMap<String, Object>();
					LinkedHashMap<String, Object> segFields = new LinkedHashMap<String, Object>();
					ResultSetMetaData rsMetaData = rs.getMetaData();
				    int colCount = rsMetaData.getColumnCount();
				    for ( int i=1; i<=colCount; i++ ) { // 1-based index
				    	String fn = rsMetaData.getColumnName(i);
				    	if ( fn.equals(DbUtil.SEGKEY_NAME) || fn.equals(DbUtil.TUREF_NAME) ) {
				    		continue; // Auto-generated
				    	}
				    	Object value = rs.getObject(fn);
				    	if ( value == null ) {
				    		if ( fn.equals(DbUtil.FLAG_NAME) ) value = false;
				    		else { // Skip other null values 
				    			continue;
				    		}
				    	}
				    	if ( DbUtil.isSegmentField(fn) ) segFields.put(fn, value);
				    	else tuFields.put(fn, value);
				    }
					
					outTuKey = outTm.addRecord(outTuKey, tuFields, segFields);

					// Update UI from time to time
					if ( (totalCount % 652) == 0 ) {
						// And check for cancellation
						if ( !callback.updateProgress(totalCount) ) {
							if ( !canceled ) {
								callback.logMessage(1, "Process interrupted by user.");
								canceled = true;
							}
						}
					}
				}
				if ( !canceled ) {
					rs = tm.getNextPage();
				}
			}
			
		}
		catch ( Throwable e ) {
			callback.logMessage(IProgressCallback.MSGTYPE_ERROR, e.getMessage());
		}
		finally {
			if ( outTm != null ) outTm.finishImport();
			callback.endProcess(totalCount, true);
		}
	}

	private ITm createNewTm (int tmCount) {
		ITm tm = null;
		// Create the name
		String tmp = String.format("%s_%d", tmName, tmCount);
		
		// If it exists try another one
		if ( repo.isShared() || ( tmNames == null )) {
			tmNames = repo.getTmNames();
		}
		int copy = 0;
		while ( tmNames.contains(tmp) ) {
			tmp = String.format("%s_%d(%d)", tmName, tmCount, ++copy);
		}
		// Create the new TM
		tm = repo.createTm(tmp, String.format("Part of %s.", tmName), options.getSourceLocale());
		return tm;
	}
	
}