/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.lib.translation.QueryResult;
import net.sf.okapi.tm.pensieve.common.Metadata;
import net.sf.okapi.tm.pensieve.common.MetadataType;
import net.sf.okapi.tm.pensieve.common.TmHit;
import net.sf.okapi.tm.pensieve.seeker.ITmSeeker;
import net.sf.okapi.tm.pensieve.seeker.TmSeekerFactory;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class PensieveTMConnector extends BaseConnector implements ITMQuery {

	private int maxHits = 25;
	private int threshold = 95;
	private List<QueryResult> results;
	private int current = -1;
	private Parameters params;
	private ITmSeeker seeker;
	private Metadata attrs;
	private String rootDir;
	private JSONParser parser;
	private String basePart;
	private String origin;
	
	public PensieveTMConnector () {
		params = new Parameters();
		attrs = new Metadata();
	}

	@Override
	public String getName() {
		return "Pensieve TM";
	}

	@Override
	public String getSettingsDisplay () {
		if ( params.getUseServer() ) {
			return "Server: " + (Util.isEmpty(params.getHost())
				? "<To be specified>"
				: params.getHost());
		}
		else {
			return "Database: " + (Util.isEmpty(params.getDbDirectory())
				? "<To be specified>"
				: params.getDbDirectory());
		}
	}

	@Override
	public void setMaximumHits (int max) {
		if ( max < 1 ) {
			maxHits = 1;
		} 
		else {
			maxHits = max;
		}
	}

	@Override
	public void setThreshold (int threshold) {
		this.threshold = threshold;
	}

	@Override
	public void close () {
		if ( seeker != null ) {
			seeker.close();
		}
	}

	@Override
	public boolean hasNext () {
		if ( results == null ) {
			return false;
		}
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	@Override
	public QueryResult next () {
		if ( results == null ) {
			return null;
		}
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	@Override
	public void open () {
		if ( params.getUseServer() ) {
			parser = new JSONParser();
			// tmName is set when setting languages for the server
			origin = null;
		}
		else {
			// Create a seeker (the TM must exist: we are just querying)
			if ( seeker != null ) seeker.close();
			origin = Util.fillRootDirectoryVariable(params.getDbDirectory(), rootDir); 
			seeker = TmSeekerFactory.createFileBasedTmSeeker(origin);
			// For the real origin value, keep just the filename
			origin = Util.getFilename(origin, true);
		}
	}

	@Override
	public int query (String plainText) {
		results = new ArrayList<QueryResult>();
		current = -1;
		if ( params.getUseServer() ) {
			return queryServer(new TextFragment(plainText));
		}
		else {
			return queryDirectory(new TextFragment(plainText));
		}
	}

	@Override
	public int query (TextFragment text) {
		results = new ArrayList<QueryResult>();
		current = -1;
		if ( params.getUseServer() ) {
			return queryServer(text);
		}
		else {
			return queryDirectory(text);
		}
	}
	
	@Override
	public List<List<QueryResult>> batchQuery (List<TextFragment> fragments) {
		throw new OkapiNotImplementedException();
	}
	
	// Direct query, using the seeker
	private int queryDirectory (TextFragment text) {
		List<TmHit> list;
		if ( threshold >= 100 ) { 
			list = seeker.searchExact(text, attrs);
		}
		else {
			list = seeker.searchFuzzy(text, threshold, maxHits, attrs);
		}

		// Convert to normalized results
		for ( TmHit hit : list ) {
			Float f = hit.getScore();
			QueryResult qr = new QueryResult();
			qr.weight = getWeight();
			qr.score = f.intValue();
			qr.source = hit.getTu().getSource().getContent();
			qr.target = hit.getTu().getTarget().getContent();
			qr.matchType = hit.getMatchType();
			qr.origin = origin;
			results.add(qr);
		}
		if ( results.size() > 0 ) {
			current = 0;
		}
		return results.size();
	}
	
	// Indirect query, using the pensieve-server API
	private int queryServer (TextFragment fragment) {
		try {
			// Check if there is actually text to translate
			if ( !fragment.hasText(false) ) return 0;

			//TODO: deal with inline codes
			String qtext = fragment.toText();
			
			// Create the connection and query
			URL url = new URL(basePart + String.format("?q=%s", URLEncoder.encode(qtext, "UTF-8")));
			URLConnection conn = url.openConnection();
			
			// Get the response
			JSONArray array = (JSONArray)parser.parse(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>)array;
			for ( Map<String, Object> map : list ) {
				QueryResult result = new QueryResult();
				result.weight = getWeight();
				result.source = new TextFragment((String)map.get("source"));
				result.target = new TextFragment((String)map.get("target"));
				result.score = ((Double)map.get("score")).intValue();
				result.origin = origin;
				results.add(result);
			}
			if ( !Util.isEmpty(list) ) current = 0;
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error querying the server." + e.getMessage(), e);
		}
		return ((current==0) ? results.size() : 0);
	}

	@Override
	public void setAttribute (String name,
		String value)
	{
		if ( "resname".equals(name) ) {
			attrs.put(MetadataType.ID, value);
		}
		else if ( "restype".equals(name) ) {
			attrs.put(MetadataType.TYPE, value);
		}
		else if ( "GroupName".equals(name) ) {
			attrs.put(MetadataType.GROUP_NAME, value);
		}
		else if ( "FileName".equals(name) ) {
			attrs.put(MetadataType.FILE_NAME, value);
		}
	}

	@Override
	public void clearAttributes () {
		attrs.clear();
	}

	@Override
	public void removeAttribute (String name) {
		if ( "resname".equals(name) ) {
			attrs.remove(MetadataType.ID);
		}
		else if ( "restype".equals(name) ) {
			attrs.remove(MetadataType.TYPE);
		}
		else if ( "GroupName".equals(name) ) {
			attrs.remove(MetadataType.GROUP_NAME);
		}
		else if ( "FileName".equals(name) ) {
			attrs.remove(MetadataType.FILE_NAME);
		}
	}

	@Override
	public void setLanguages (LocaleId sourceLocale,
		LocaleId targetLocale)
	{
		super.setLanguages(sourceLocale, targetLocale);
	
		if ( params.getUseServer() ) {
			String host = params.getHost();
			if ( host.endsWith("/") || host.endsWith("\\") ) {
				host = host.substring(0, host.length()-1);
			}
			basePart = String.format("%s/search/%s/%s/", host, srcLoc.toBCP47(), trgLoc.toBCP47());
			origin = host;
		}
	}

	@Override
	public int getMaximumHits () {
		return maxHits;
	}

	@Override
	public int getThreshold () {
		return threshold;
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public void setRootDirectory (String rootDir) {
		this.rootDir = rootDir;
	}	

}
