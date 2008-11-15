/*===========================================================================*/
/* Copyright (C) 2008 By the Okapi Framework contributors                    */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.lib.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

public class QueryManager {

	LinkedHashMap<Integer, ResourceItem> resList;
	ArrayList<QueryResult> results;
	int current = -1;
	int lastId = 0;
	String srcLang;
	String trgLang;
	LinkedHashMap<String, String> attributes;
	
	public QueryManager () {
		resList = new LinkedHashMap<Integer, ResourceItem>();
		results = new ArrayList<QueryResult>();
		attributes = new LinkedHashMap<String, String>();
	}
	
	public int addResource (IQuery connector, String name) {
		assert(connector!=null);
		ResourceItem ri = new ResourceItem();
		ri.query = connector;
		ri.enabled = true;
		ri.name = name;
		resList.put(++lastId, ri);
		return lastId;
	}
	
	public int addAndInitializeResource (IQuery connector,
		String resourceName,
		String connectionString)
	{
		// Add the resource
		int id = addResource(connector, resourceName);
		// open it and set the current options
		connector.open(connectionString);
		if (( srcLang != null ) && ( trgLang != null )) {
			connector.setLanguages(srcLang, trgLang);
		}
		for ( String name : attributes.keySet() ) {
			connector.setAttribute(name, attributes.get(name));
		}
		// Set the connection string
		ResourceItem ri = resList.get(id);
		ri.connectionString = connectionString;
		return id;
	}
	
	public void setEnabled (int resourceId,
		boolean enabled)
	{
		resList.get(resourceId).enabled = enabled;
	}
	
	public void remove (int resourceId) {
		resList.remove(resourceId);
	}
	
	public IQuery getInterface (int resourceId) {
		return resList.get(resourceId).query;
	}
	
	public ResourceItem getResource (int resourceId) {
		return resList.get(resourceId);
	}
	
	public String getName (int resourceId) {
		return resList.get(resourceId).name;
	}
	
	public Map<Integer, ResourceItem> getResources () {
		return resList;
	}
	
	public void close () {
		for ( ResourceItem ri : resList.values() ) {
			ri.query.close();
		}
	}

	public ArrayList<QueryResult> getResults () {
		return results;
	}
	
	public boolean hasNext() {
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	public QueryResult next () {
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	public void open (String connectionString) {
		for ( ResourceItem ri : resList.values() ) {
			ri.query.open(ri.connectionString);
		}
	}

	public int query (String plainText) {
		TextFragment tf = new TextFragment(plainText);
		return query(tf);
	}

	public int query (TextFragment text) {
		results.clear();
		ResourceItem ri;
		for ( int id : resList.keySet() ) {
			ri = resList.get(id);
			if ( !ri.enabled ) continue; // Skip disabled entries
			if ( ri.query.query(text) > 0 ) {
				QueryResult res;
				while ( ri.query.hasNext() ) {
					res = ri.query.next();
					res.connectorId = id;
					results.add(res);
				}
			}
		}
		if ( results.size() > 0 ) current = 0;
		Collections.sort(results); // Sort by weights
		return results.size();
	}

	public void setAttribute (String name,
		String value)
	{
		attributes.put(name, value);
		for ( ResourceItem ri : resList.values() ) {
			ri.query.setAttribute(name, value);
		}
	}
	
	public void removeAttribute (String name) {
		attributes.remove(name);
		for ( ResourceItem ri : resList.values() ) {
			ri.query.removeAttribute(name);
		}
	}
	
	public void setLanguages (String sourceLang,
		String targetLang)
	{
		srcLang = sourceLang;
		trgLang = targetLang;
		for ( ResourceItem ri : resList.values() ) {
			ri.query.setLanguages(srcLang, trgLang);
		}
	}

	public String getSourceLanguage () {
		return srcLang;
	}
	
	public String getTargetLanguage () {
		return trgLang;
	}

	/**
	 * Leverage a text unit (segmented or not) based on the current settings.
	 * Any options or attributes needed must be set before calling this method.
	 * @param tu The text unit to modify.
	 */
	public void leverage (TextUnit tu) {
		if ( !tu.isTranslatable() ) return;
		if ( tu.hasTarget() ) return;
		
		TextContainer tc = tu.getSourceContent().clone();
		tu.setTargetContent(tc);
		QueryResult qr;
		if ( tc.isSegmented() ) {
			List<TextFragment> segList = tc.getSegments();
			for ( int i=0; i<segList.size(); i++ ) {
				if ( query(segList.get(i)) != 1 ) continue;
				qr = next();
				if ( qr.score != 100 ) continue;
				segList.set(i, qr.target);
			}
		}
		else {
			if ( query(tc) != 1 ) return;
			qr = next();
			if ( qr.score != 100 ) return;
			tc.setCodedText(qr.target.getCodedText(), false);
		}
	}
}
