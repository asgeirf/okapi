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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import net.sf.okapi.lib.tmdb.DbUtil.PageMode;
import net.sf.okapi.lib.tmdb.IIndexAccess;
import net.sf.okapi.lib.tmdb.IRecordSet;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.lucene.OSeeker;
import net.sf.okapi.lib.tmdb.lucene.OWriter;

class IndexAccess implements IIndexAccess {

	private final Repository store;
	private OWriter writer;
	private OSeeker seeker;
	private boolean inMemory;
	
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
			writer = new OWriter(idxDir, false);
			seeker = new OSeeker(idxDir);
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
	public int search (String codedText) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Long> getHits () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close () {
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
		if ( seeker != null ) {
			seeker.close();
			seeker = null;
		}
	}

}
