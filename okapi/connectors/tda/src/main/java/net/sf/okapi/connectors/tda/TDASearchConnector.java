/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.tda;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.lib.translation.QueryResult;
import net.sf.okapi.lib.translation.TextMatcher;

public class TDASearchConnector implements ITMQuery {

	// Language code to TDA code, except for reg=lang cases (fr-fr)
	private static final String[][] LANGSMAP = {
		{"ar", "ar-ar"}, // Yes, ar-ar (Argentina) is Arabic default in TDA, TODO: or use ar-sa?
		{"cs", "cs-cz"},
		{"cy", "cy-gb"},
		{"da", "da-dk"},
		{"el", "el-gr"},
		{"en", "en-us"},
		{"et", "et-ee"},
		{"fa", "fa-ir"},
		{"he", "he-il"},
		{"ko", "ko-kr"},
		{"nb", "nb-no"},
		{"nn", "nn-no"},
		{"sl", "sl-si"},
		{"sv", "sv-se"},
		{"uk", "uk-ua"},
		{"vi", "vi-vn"},
		{"zh", "zh-cn"}
	};
	
	private JSONParser parser;
	private Parameters params;
	private String baseURL;
	private String authKey;
	private String srcCode;
	private String trgCode;
	private int current = -1;
	private int maxHits = 20;
	private List<QueryResult> results;
	private TextMatcher matcher;
	private ScoreComparer scorComp = new ScoreComparer();
	private int threshold = 60;

	class ScoreComparer implements Comparator<QueryResult> {
		public int compare(QueryResult arg0, QueryResult arg1) {
			return (arg0.score>arg1.score ? -1 : (arg0.score==arg1.score ? 0 : 1));
		}
	}
	
	public TDASearchConnector () {
		parser = new JSONParser();
		params = new Parameters();
	}
	
	@Override
	public void close () {
		authKey = null;
	}

	@Override
	public String getName () {
		return "TDA-Search";
	}

	@Override
	public String getSettingsDisplay () {
		String tmp = "Server: " + (Util.isEmpty(params.getServer())
			? "<To be specified>"
			: params.getServer());
		return tmp + "\nUser: " + (Util.isEmpty(params.getUsername())
			? "<To be specified>"
			: params.getUsername());
	}

	@Override
	public void open () {
		baseURL = params.getServer();
		if ( !baseURL.endsWith("/") ) baseURL += "/";
		authKey = null;
	}

	@Override
	public int query (String plainText) {
		return query(new TextFragment(plainText));
	}
	
	@Override
	public int query (TextFragment frag) {
		results = new ArrayList<QueryResult>();
		current = -1;
		try {
			loginIfNeeded();
			// Check if there is actually text to translate
			if ( !frag.hasText(false) ) return 0;
			String qtext = prepareQuery(frag);

			// Create the connection and query
			URL url = new URL(baseURL + String.format("segment.json?limit=%d&source_lang=%s&target_lang=%s",
				maxHits, srcCode, trgCode) + "&auth_auth_key="+authKey
				+ (params.getIndustry()>0 ? "&industry="+String.valueOf(params.getIndustry()) : "")
				+ (params.getContentType()>0 ? "&content_type="+String.valueOf(params.getContentType()) : "")
				+ "&q=" + URLEncoder.encode(qtext, "UTF-8"));
			URLConnection conn = url.openConnection();

			// Get the response
			JSONObject object = (JSONObject)parser.parse(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>)object;
	    	JSONArray array = (JSONArray)map.get("segment");
	    	
			// We keep our own hit count as TDA 'limit' may return more than the value asked
			int count = ((array.size() > maxHits) ? maxHits : array.size());
	    	for ( int i=0; i<count; i++ ) {
	    		@SuppressWarnings("unchecked")
	    		Map<String, Object> entry = (Map<String, Object>)array.get(i);
	    		QueryResult result = new QueryResult();
	    		result.source = new TextFragment((String)entry.get("source"));
	    		result.target = new TextFragment((String)entry.get("target"));
	    		result.origin = "TDA";
	    		@SuppressWarnings("unchecked")
	    		String tmp = (String)((Map<String, Object>)entry.get("provider")).get("name");
	    		if ( !Util.isEmpty(tmp) ) result.origin += ("/" + tmp);
	    		result.score = 90; //TODO: re-score the data to get meaningfull hits
	    		results.add(result);
	    	}

			// Adjust scores
			//TODO: re-order and re-filter results
	    	//TODO: fixup based on original text, not pre-processed one
			fixupResults(frag.toString());
			
	    	current = (( results.size() > 0 ) ? 0 : -1);
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error querying the server." + e.getMessage(), e);
		}
		return ((current==0) ? 1 : 0);
	}

	private String prepareQuery (TextFragment frag) {
		String tmp1 = frag.toString(); // Plain text for now
		// Remove punctuation
		return tmp1.replaceAll("\\p{Po}", "");
	}
	
	@Override
	public IParameters getParameters () {
		return this.params;
	}
	
	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}
	
	private String toInternalCode (LocaleId locale) {
		String code = locale.toBCP47(); 
		String lang = locale.getLanguage();
		if ( locale.getRegion() == null ) {
			// TDA langs have all a region code: Try to add it here.
			boolean found = false;
			for ( int i=0; i<LANGSMAP.length; i++ ) {
				if ( lang.equals(LANGSMAP[i][0]) ) {
					code = LANGSMAP[i][1];
					found = true;
					break;
				}
			}
			if ( !found ) { // Default: region code is same as lang code: fr-fr
				code = lang+"-"+lang;
			}
		}
		return code;
	}
	
	private LocaleId fromInternalCode (String code) {
		return LocaleId.fromString(code);
	}
	
	private void loginIfNeeded () {
		if ( authKey != null ) return;
		try {
			// Create the connection and query
			URL url = new URL(baseURL + "auth_key.json?action=login");
			URLConnection conn = url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			// Write out the content string to the stream.
			String content = String.format("auth_username=%s&auth_password=%s&app_key=%s",
				URLEncoder.encode(params.getUsername(), "UTF-8"),
				URLEncoder.encode(params.getPassword(), "UTF-8"),
				params.getAppKey());
			out.writeBytes(content);
			out.flush ();
			out.close ();
			
			// Read the response
			JSONObject object = (JSONObject)parser.parse(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>)object;
	    	@SuppressWarnings("unchecked")
	    	Map<String, Object> data = (Map<String, Object>)map.get("auth_key");
	    	authKey = (String)data.get("id");
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error while login to the server." + e.getMessage(), e);
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
	public void setMaximumHits (int max) {
		maxHits = max;
	}

	@Override
	public void setThreshold (int threshold) {
		this.threshold = threshold;
	}

	@Override
	public void clearAttributes () {
		// TODO Auto-generated method stub
	}

	@Override
	public LocaleId getSourceLanguage () {
		return fromInternalCode(srcCode);
	}

	@Override
	public LocaleId getTargetLanguage () {
		return fromInternalCode(trgCode);
	}

	@Override
	public boolean hasNext () {
		if ( results == null ) return false;
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	@Override
	public QueryResult next () {
		if ( results == null ) return null;
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	@Override
	public void removeAttribute (String name) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setAttribute (String name,
		String value)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void setLanguages (LocaleId sourceLocale,
		LocaleId targetLocale)
	{
		matcher = new TextMatcher(sourceLocale, sourceLocale);
		srcCode = toInternalCode(sourceLocale);
		trgCode = toInternalCode(targetLocale);
	}

	@Override
	public void setRootDirectory (String rootDir) {
		// Not used
	}

	/**
	 * Re-calculates the scores, re-orders and filters the results based on
	 * more meaning full comparisons.
	 * @param plainText the original text query.
	 */
	private void fixupResults (String plainText) {
		if ( results.size() == 0 ) return;
		List<String> tokens = matcher.prepareBaseTokens(plainText);
		// Loop through the results
		for ( Iterator<QueryResult> iter = results.iterator(); iter.hasNext(); ) {
			QueryResult qr = iter.next();
			// Compute the adjusted score
			qr.score = matcher.compareToBaseTokens(plainText, tokens, qr.source);
			// Remove the item if lower than the threshold 
			if ( qr.score < threshold ) iter.remove();
		}
		// Re-order the list from the 
		Collections.sort(results, scorComp);
	}
}
