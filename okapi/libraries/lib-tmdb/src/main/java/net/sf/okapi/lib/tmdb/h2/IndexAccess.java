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

package net.sf.okapi.lib.tmdb.h2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import net.sf.okapi.lib.tmdb.IIndexAccess;
import net.sf.okapi.lib.tmdb.lucene.Seeker;
import net.sf.okapi.lib.tmdb.lucene.TmEntry;
import net.sf.okapi.lib.tmdb.lucene.TmHit;
import net.sf.okapi.lib.tmdb.lucene.Writer;

class IndexAccess implements IIndexAccess {

	private Writer writer;
	private Seeker seeker;
	private boolean inMemory;
	private List<TmHit> hits;
	private Repository store;
	
	public IndexAccess (Repository store) {
		try {
			this.store = store;
			Directory idxDir = null;
			// Get the location from the repository instance
			String dir = store.getDirectory();
			inMemory = (dir == null);
			if ( inMemory ) {
				idxDir = new RAMDirectory();
			}
			else { // Create the directory if needed
				File file = new File(dir);
				if ( !file.exists() ) {
					file.mkdirs();
				}
				idxDir = FSDirectory.open(file);
			}
			
			writer = new Writer(idxDir, false);
			seeker = new Seeker(writer.getIndexWriter());
		}
		catch (IOException e) {
			throw new RuntimeException("Error creating the index access object:\n"+e.getMessage(), e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
        close();
        super.finalize();
	}

	@Override
	public int search (String codedText,
		String codesAsString,
		String tmUUID,
		String locale,
		int maxHits,
		int threshold,
		HashMap<String, String> attributes)
	{
		hits = seeker.searchFuzzy(codedText, codesAsString, tmUUID, locale, maxHits, threshold, attributes);
		return hits.size();
	}

	@Override
	public List<TmHit> getHits () {
		return hits;
	}

	@Override
	public void close () {
		if ( seeker != null ) {
			seeker.close();
			seeker = null;
		}
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
	}

	@Override
	public Writer getWriter () {
		return writer;
	}

	@Override
	public void deleteTMIndex (String uuid) {
		writer.delete(TmEntry.TMID_FIELDNAME, uuid);
		// Now: update the index information in the TM database
		store.updateIndexInfo(uuid, null);
	}

}
