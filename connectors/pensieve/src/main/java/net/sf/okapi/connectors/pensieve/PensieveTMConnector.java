/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.pensieve;

import java.io.IOException;
import java.util.List;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.lib.translation.QueryResult;
import net.sf.okapi.tm.pensieve.common.TmHit;
import net.sf.okapi.tm.pensieve.seeker.PensieveSeeker;
import net.sf.okapi.tm.pensieve.seeker.TmSeekerFactory;

public class PensieveTMConnector implements ITMQuery {
	
	private int maxHits = 5;
	private int threshold = 60;
	private List<QueryResult> results;
	private int current = -1;
	private String srcLang;
	private String trgLang;
	private Parameters params;
	private PensieveSeeker seeker;

	public PensieveTMConnector () {
		params = new Parameters();
	}
	
	public String getName () {
		return "Pensieve TM";
	}

	public String getSettingsDisplay () {
		return "Database: " + params.getDbDirectory();
	}
	
	public void setMaximumHits (int max) {
		if ( max < 1 ) maxHits = 1;
		else maxHits = max;
	}

	public void setThreshold (int threshold) {
		this.threshold = threshold;
	}

	public void close() {
		seeker = null;
	}

	public boolean hasNext () {
		if ( results == null ) return false;
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}
	
	public QueryResult next () {
		if ( results == null ) return null;
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	public void open () {
		seeker = (PensieveSeeker)TmSeekerFactory.createFileBasedTmSeeker(params.getDbDirectory());
	}

	public int query (String plainText) {
		TextFragment tf = new TextFragment(plainText);
		return query(tf);
	}
	
	public int query (TextFragment text) {
		results.clear();
		current = -1;
		try {
			// Do the search
			List<TmHit> list = seeker.searchExact(text.toString(), maxHits);
			int more = maxHits;
			if ( !Util.isEmpty(list) ) {
				more = more-list.size();
			}
			if ( more > 0 ) {
				list.addAll(seeker.searchFuzzyWuzzy(text.toString(), more));
			}
			// Convert to normalized results
			for ( TmHit hit : list ) {
				QueryResult qr = new QueryResult();
				qr.score = hit.getScore().intValue();
				qr.source = hit.getTu().getSource().getContent();
				qr.target = hit.getTu().getTarget().getContent();
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error when querying the TM.", e);
		}
		if ( results == null ) return 0;
		current = 0;
		return results.size();
	}
	
	public void setAttribute (String name,
		String value)
	{
		//TODO
	}
	
	public void removeAttribute (String name) {
		//TODO
	}

	public void setLanguages (String sourceLang,
		String targetLang)
	{
		srcLang = sourceLang;
		trgLang = targetLang;
	}

	public String getSourceLanguage () {
		return srcLang;
	}
	
	public String getTargetLanguage () {
		return trgLang;
	}

	public void export (String outputPath) {
		//TODO
	}

	public int getMaximumHits () {
		return maxHits;
	}

	public int getThreshold () {
		return threshold;
	}

	public IParameters getParameters() {
		return params;
	}

	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

}
