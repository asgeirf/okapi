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

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.tmdb.IProgressCallback;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.DbUtil.PageMode;
import net.sf.okapi.lib.tmdb.lucene.OField;
import net.sf.okapi.lib.tmdb.lucene.OFields;
import net.sf.okapi.lib.tmdb.lucene.OTranslationUnitInput;
import net.sf.okapi.lib.tmdb.lucene.OTranslationUnitVariant;
import net.sf.okapi.lib.tmdb.lucene.OWriter;

public class Indexer implements Runnable {

	private final IProgressCallback callback;
	private final IRepository repo;
	private final String tmName;
	
	public Indexer (IProgressCallback progressCallback,
		IRepository repo,
		String tmName)
	{
		this.callback = progressCallback;
		this.repo = repo;
		this.tmName = tmName;
	}
	
	@Override
	public void run () {
		long totalCount = 0;
		ITm tm = null;
		boolean canceled = false;
		IIndexAccess ia = null;
		
		try {
			callback.startProcess("Indexing TM...");
			
			//=== Split entries
			
			// Get the original TM and set it for iteration
			tm = repo.openTm(tmName);
			
			List<String> locales = tm.getLocales();
			String srcLoc = locales.get(0);
			
			ArrayList<String> fields = new ArrayList<String>();
			String srcFn = DbUtil.TEXT_PREFIX+srcLoc;
			fields.add(srcFn);
			tm.setRecordFields(fields);
			tm.setPageMode(PageMode.ITERATOR);

			ia = repo.getIndexAccess();
			OWriter writer = ia.getWriter();
		    OFields searchFields = new OFields();
		    searchFields.put("tm", new OField("tm", tm.getUUID(), Index.NOT_ANALYZED, Store.NO));
			
			IRecordSet rs = tm.getFirstPage();
			while  (( rs != null ) && !canceled ) {
				while ( rs.next() && !canceled ) {
					totalCount++;

					OTranslationUnitInput inputTu = new OTranslationUnitInput(String.valueOf(rs.getSegKey()), searchFields);
			
					String srcText = rs.getString(srcFn);
				    OTranslationUnitVariant tuvSrc = new OTranslationUnitVariant("EN", new TextFragment(srcText));
				    inputTu.add(tuvSrc);
				    
				    writer.index(inputTu);
					
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
		}
		catch ( Throwable e ) {
			callback.logMessage(IProgressCallback.MSGTYPE_ERROR, e.getMessage());
		}
		finally {
			callback.endProcess(totalCount, false);
		}
	}

}
