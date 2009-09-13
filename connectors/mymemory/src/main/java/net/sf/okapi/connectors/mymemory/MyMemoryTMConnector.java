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

package net.sf.okapi.connectors.mymemory;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis.AxisFault;
import org.tempuri.GetResponse;
import org.tempuri.Match;
import org.tempuri.OtmsSoapStub;
import org.tempuri.Query;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.lib.translation.QueryResult;
import net.sf.okapi.lib.translation.QueryUtil;

public class MyMemoryTMConnector implements ITMQuery {

	private static final String SERVER_URL = "http://mymemory.translated.net/otms/";
	
	private String srcLang;
	private String trgLang;
	private List<QueryResult> results;
	private int current = -1;
	private int maxHits = 25;
	private int threshold = 75;
	private Parameters params;
	private OtmsSoapStub otms;
	private QueryUtil qutil;

	public MyMemoryTMConnector () {
		params = new Parameters();
		qutil = new QueryUtil();
	}

	public String getName () {
		return "MyMemory-TM";
	}

	public String getSettingsDisplay () {
		return String.format("Server: %s\nAllow MT: %s", SERVER_URL,
			((params.getUseMT()==1) ? "Yes" : "No"));
	}
	
	public void close () {
	}

	public void export (String outputPath) {
		throw new OkapiNotImplementedException("The export() method is not supported.");
	}

	public String getSourceLanguage () {
		return toISOCode(srcLang);
	}

	public String getTargetLanguage () {
		return toISOCode(trgLang);
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
		try {
			results = new ArrayList<QueryResult>();
			URL url = new URL(SERVER_URL);
			otms = new OtmsSoapStub(url, null);
		}
		catch ( AxisFault e ) {
			throw new RuntimeException("Error creating the GlobalSight Web services.", e);
		}
		catch ( MalformedURLException e ) {
			throw new RuntimeException("Invalid server URL.", e);
		}
	}

	// Assumes results are reset and emptiness checked
	private int queryText (String text,
		boolean hadCodes)
	{
		try {
			Query query = new Query(null, text, srcLang, trgLang, null, params.getUseMT());
			GetResponse gresp = otms.otmsGet(params.getKey(), query);
			if ( gresp.isSuccess() ) {
				QueryResult res;
				Match[] matches = gresp.getMatches();
				int i = 0;
				for ( Match match : matches ) {
					if ( ++i > maxHits ) break; // Maximum reached
					res = new QueryResult();
					res.source = new TextFragment(match.getSegment());
					res.target = new TextFragment(match.getTranslation());
					res.score = match.getScore();
					// To workaround bug in score calculation
					// Score > 100 should be treated as 100 per Alberto's info.
					//if (res.score > 100 ) res.score = 100;
					//TODO: if ( res.score < getThreshold() ) break;
					if ( hadCodes ) {
						if ( res.score >= 100 ) res.score = 99;
						else res.score--;
					}
					results.add(res);
				}
			}
		}
		catch ( RemoteException e ) {
			throw new RuntimeException("Error querying TM.", e);
		}
		if ( results.size() > 0 ) current = 0;
		return results.size();
	}


	public int query (String text) {
		results.clear();
		current = -1;
		if ( Util.isEmpty(text) ) return 0;
		return queryText(text, false);
	}

	public int query (TextFragment frag) {
		results.clear();
		current = -1;
		if ( !frag.hasText(false) ) return 0;
		return queryText(qutil.separateCodesFromText(frag), frag.hasCode());
	}
	
	public void removeAttribute (String name) {
		//TODO: use domain
	}

	public void setAttribute (String name,
		String value)
	{
		//TODO: use domain
	}

	public void setLanguages (String sourceLang,
		String targetLang)
	{
		srcLang = toInternalCode(sourceLang);
		trgLang = toInternalCode(targetLang);
	}

	private String toInternalCode (String standardCode) {
		//TODO: The expected MM code in lang-Region with region mandatory
		// We will need to build a lookup table for the non-easy re-naming case like en -> en-us
		return standardCode;
	}

	private String toISOCode (String internalCode) {
		//TODO: Check
		return internalCode;
	}

	/**
	 * Sets the maximum number of hits to return.
	 */
	public void setMaximumHits (int max) {
		if ( max < 1 ) maxHits = 1;
		else maxHits = max;
	}

	public void setThreshold (int threshold) {
		this.threshold = threshold;
	}

	public int getMaximumHits () {
		return maxHits;
	}

	public int getThreshold () {
		return threshold;
	}

	public IParameters getParameters () {
		return params;
	}

	public void setParameters (IParameters params) {
		params = (Parameters)params;
	}

}
