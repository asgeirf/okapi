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

package net.sf.okapi.lib.verification;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;

public class QualityCheckSession {

	Map<URI, RawDocument> rawDocs; // Temporary solution waiting for the DB
	IFilterConfigurationMapper fcMapper;
	private Parameters params;
	private List<Issue> issues;
	private QualityChecker checker;
	private LocaleId sourceLocale = LocaleId.ENGLISH;
	private LocaleId targetLocale = LocaleId.FRENCH;
	private IFilter filter;
	
	public QualityCheckSession () {
		resetData();
	}
	
	public List<Issue> getIssues () {
		return issues;
	}
	
	public Parameters getParameters () {
		return params;
	}
	
	public void setParameters (Parameters params) {
		this.params = params;
	}
	
	public void addRawDocument (RawDocument rawDoc) {
		URI uri = rawDoc.getInputURI();
		rawDocs.put(uri, rawDoc);
	}

	public void setFilterConfigurationMapper (IFilterConfigurationMapper fcMapper) {
		this.fcMapper = fcMapper;
	}

	private void resetData () {
		rawDocs = new HashMap<URI, RawDocument>();
		issues = new ArrayList<Issue>();
		params = new Parameters();
		checker = new QualityChecker();
	}
	
	public void refreshAll () {
		startProcess(targetLocale, null);
		for ( RawDocument rd : rawDocs.values() ) {
			executeRefresh(rd);
		}
		completeProcess();
	}
	
	private void executeRefresh (RawDocument rd) {
		try {
			// Reset the issues for that document
			
			// Process the document
			filter = fcMapper.createFilter(rd.getFilterConfigId(), filter);
			if ( filter == null ) {
				throw new RuntimeException("Unsupported filter type.");
			}
			filter.open(rd);
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case START_DOCUMENT:
					StartDocument sd = (StartDocument)event.getResource();
					clearIssues(Util.makeId(sd.getName()));
					processStartDocument(sd);
					break;
				case TEXT_UNIT:
					processTextUnit(event.getTextUnit());
					break;
				}
			}
		}
		finally {
			if ( filter != null ) filter.close();
		}
	}
	
	private void clearIssues (String docId) {
		Iterator<Issue> iter = issues.iterator();
		while ( iter.hasNext() ) {
			Issue issue = iter.next();
			if ( issue.docId.equals(docId) ) {
				iter.remove();
			}
		}
	}
	
	public void saveSession (String path) {
		// Temporary code, waiting for DB
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(path));
			dos.writeUTF(sourceLocale.toBCP47());
			dos.writeUTF(targetLocale.toBCP47());
			dos.writeUTF(params.toString());
			dos.writeInt(rawDocs.size());
			for ( RawDocument rd : rawDocs.values() ) {
				dos.writeUTF(rd.getInputURI().toString());
				dos.writeUTF(rd.getFilterConfigId());
				dos.writeUTF(rd.getEncoding());
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error while saving session.", e);
		}
		finally {
			if ( dos != null ) {
				try {
					dos.close();
				}
				catch ( IOException e ) {
					throw new OkapiIOException("Error closing session file.", e);
				}
			}
		}
	}
	
	public void loadSession (String path) {
		resetData();
		// Temporary code, waiting for DB
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(path));
			String tmp = dis.readUTF(); // Source
			sourceLocale = LocaleId.fromBCP47(tmp);
			tmp = dis.readUTF(); // Target
			targetLocale = LocaleId.fromBCP47(tmp);
			tmp = dis.readUTF(); // Parameters
			params.fromString(tmp);
			int count = dis.readInt();
			for ( int i=0; i<count; i++ ) {
				tmp = dis.readUTF();
				URI uri = new URI(tmp);
				String configId = dis.readUTF();
				String encoding = dis.readUTF();
				RawDocument rd = new RawDocument(uri, encoding, sourceLocale, targetLocale);
				rd.setFilterConfigId(configId);
				rawDocs.put(uri, rd);
			}
		}
		catch ( Throwable e ) {
			throw new OkapiIOException("Error reading session file.", e);
		}
		finally {
			if ( dis != null ) {
				try {
					dis.close();
				}
				catch ( IOException e ) {
					throw new OkapiIOException("Error closing session file.", e);
				}
			}
		}
	}

	public void startProcess (LocaleId locId,
		String rootDir)
	{
		checker.startProcess(locId, rootDir, params, issues);
	}
	
	public void processStartDocument (StartDocument startDoc) {
		checker.processStartDocument(startDoc);
	}

	public void processTextUnit (TextUnit textUnit) {
		checker.processTextUnit(textUnit);
	}

	public void completeProcess () {
		checker.completeProcess();
	}

}
