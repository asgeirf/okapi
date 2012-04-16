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

import java.util.List;

import net.sf.okapi.lib.tmdb.IProgressCallback;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.DbUtil.PageMode;
import net.sf.okapi.lib.tmdb.lucene.TmEntry;
import net.sf.okapi.lib.tmdb.lucene.Variant;
import net.sf.okapi.lib.tmdb.lucene.Writer;

public class Indexer implements Runnable {

	private final IProgressCallback callback;
	private final IRepository repo;
	private final String tmName;
	private final List<String> fields;
	
	/**
	 * Creates the indexer object.
	 * @param progressCallback the callback object for the progress.
	 * @param repo the repository of the TM to index.
	 * @param tmName the name of the TM to index.
	 * @param fields list of the text fields to index and attributes to store. 
	 * @param locale the Olifant locale name of the locale to index.
	 * @param metaFields the list of the TM columns to use as metadata (can be null).
	 */
	public Indexer (IProgressCallback progressCallback,
		IRepository repo,
		String tmName,
		List<String> fields)
	{
		this.callback = progressCallback;
		this.repo = repo;
		this.tmName = tmName;
		this.fields = fields;
	}
	
	@Override
	public void run () {
		long totalCount = 0;
		ITm tm = null;
		boolean canceled = false;
		IIndexAccess ia = null;
		
		try {
			callback.startProcess(String.format("Indexing %s...", tmName));
			
			// Get the original TM and set it for iteration
			tm = repo.openTm(tmName);
			
			// Set the list of the fields to fetch from the database
			tm.setRecordFields(fields);
			tm.setPageMode(PageMode.ITERATOR);

			ia = repo.getIndexAccess();
			Writer writer = ia.getWriter();
			
			IRecordSet rs = tm.getFirstPage();
			while  (( rs != null ) && !canceled ) {
				while ( rs.next() && !canceled ) {
					totalCount++;
					// Create the entry
					TmEntry entry = new TmEntry(String.valueOf(rs.getSegKey()), tm.getUUID());
					// Add the fields to index and attributes to store
					for ( String fn : fields ) {
						if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
							String loc = DbUtil.getFieldLocale(fn);
							entry.addVariant(new Variant(loc, rs.getString(fn), null)); //TODO: get the codesasString too!!
						}
						else {
							entry.setAttribute(fn, rs.getString(fn));
						}
					}
					// Index the entry
				    writer.index(entry);
					
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
			writer.commit();
			
			// Now: update the index information in the TM database
			tm.setIndexInfo(DbUtil.indexInfoToString(fields));
		}
		catch ( Throwable e ) {
			callback.logMessage(IProgressCallback.MSGTYPE_ERROR, e.getMessage());
		}
		finally {
			callback.endProcess(totalCount, false);
		}
	}

}
