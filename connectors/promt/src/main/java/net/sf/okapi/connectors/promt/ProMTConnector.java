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

package net.sf.okapi.connectors.promt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.IQuery;
import net.sf.okapi.lib.translation.QueryResult;

public class ProMTConnector implements IQuery {

	private static final String SERVICE = "/pts8/services/ptservice.asmx/TranslateText";
    private static final Pattern RESULTPATTERN = Pattern.compile("<string(.*?)>(.*?)</string>");
	
	private String srcLang;
	private String trgLang;
	private QueryResult result;
	private int current = -1;
	private Parameters params;
	private URL url;
	private String pair;

	public ProMTConnector () {
		params = new Parameters();
	}

	public String getName () {
		return "ProMT";
	}

	public String getSettingsDisplay () {
		return String.format("Server: %s", params.getServerURL());
	}
	
	public void close () {
	}

	public void export (String outputPath) {
		throw new OkapiNotImplementedException("The export() method is not supported.");
	}

	public LocaleId getSourceLanguage () {
		return LocaleId.fromString(srcLang);
	}
	
	public LocaleId getTargetLanguage () {
		return LocaleId.fromString(trgLang);
	}

	public boolean hasNext () {
		return (current>-1);
	}
	
	public QueryResult next() {
		if ( current > -1 ) { // Only one result
			current = -1;
			return result;
		}
		return null;
	}

	public void open () {
		String tmp = params.getServerURL();
		// Make sure the URL does not end with separator
		if ( tmp.endsWith("/") ) tmp = tmp.substring(0, tmp.length()-1);
		else if ( tmp.endsWith("\\") ) tmp = tmp.substring(0, tmp.length()-1);
		// Set the full URL for the service
		try {
			url = new URL(tmp+SERVICE);
		}
		catch ( MalformedURLException e ) {
			throw new RuntimeException(String.format("Cannot open the connection to '%s'", tmp+SERVICE), e); 
		}
	}

	public int query (String text) {
		if ( Util.isEmpty(text) ) return 0;
		return queryPost(text);
	}

	public int query (TextFragment frag) {
		if ( !frag.hasText(false) ) return 0;
		return queryPost(frag.toString());
	}
	
	private int queryPost (String text) {
		current = -1;
		OutputStreamWriter wr = null;
		BufferedReader rd = null;

//Only EN-FR for now			
		if ( !pair.equals("en_fr") ) return 0;

		try {
			// Try to authenticate if needed
			if ( !Util.isEmpty(params.getUsername()) ) {
				Authenticator.setDefault(new Authenticator() {
				    protected PasswordAuthentication getPasswordAuthentication() {
				        return new PasswordAuthentication(params.getUsername(), params.getPassword().toCharArray());
				    }
				});
			}
			// Open a connection
			URLConnection conn = url.openConnection();
			
			// Set the parameters
			//DirId=string&TplId=string&Text=string
			//524289
			String data = String.format("DirId=%s&TplId=%s&Text=%s",
				"524289", "General", URLEncoder.encode(text, "UTF-8"));

			// Post the data
			conn.setDoOutput(true);
			wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
	        
	        // Get the response
	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));
	        String buffer;
	        StringBuilder tmp = new StringBuilder();
	        while (( buffer = rd.readLine() ) != null ) {
	            tmp.append(buffer);
	        }
	        
	        // Treat the output 
	        Matcher m = RESULTPATTERN.matcher(tmp.toString());
	        if ( m.find() ) {
	        	buffer = m.group(2);
	        	if ( !Util.isEmpty(buffer) ) {
	        		result = new QueryResult();
	        		result.source = new TextFragment(text);
	        		result.target = new TextFragment(buffer);
	        		result.score = 95; // Arbitrary score for MT
	        		result.origin = Util.ORIGIN_MT;
	    			current = 0;
	        	}
	        }
		}
		catch ( MalformedURLException e ) {
			throw new RuntimeException("Error during the query.", e);
		}
		catch ( IOException e ) {
e.printStackTrace();			
			throw new RuntimeException("Error during the query.", e);
		}
		finally {
        	try {
        		if ( wr != null ) wr.close();
    	        if ( rd != null ) rd.close();
   	        }
       		catch ( IOException e ) {
       			// Ignore this exception
	        }
		}
		return current+1;
	}
	
	public void removeAttribute (String name) {
		//TODO: use domain
	}

	public void clearAttributes () {
		//TODO: use domain
	}

	public void setAttribute (String name,
		String value)
	{
		//TODO: use domain
	}

	public void setLanguages (LocaleId sourceLocale,
		LocaleId targetLocale)
	{
		srcLang = toInternalCode(sourceLocale);
		trgLang = toInternalCode(targetLocale);
		pair = srcLang + "_" + trgLang; 
	}
		
	private String toInternalCode (LocaleId locale) {
		// Reduce the locale code to its language part
		return locale.getLanguage();
	}

	public IParameters getParameters () {
		return params;
	}

	public void setParameters (IParameters params) {
		params = (Parameters)params;
	}

}

