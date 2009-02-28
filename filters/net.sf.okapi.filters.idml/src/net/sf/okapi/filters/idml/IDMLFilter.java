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

package net.sf.okapi.filters.idml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

public class IDMLFilter implements IFilter {

	private enum NextAction {
		OPENZIP, NEXTINZIP, NEXTINSUBDOC, DONE
	}

	private final String MIMETYPE = "application/vnd.adobe.indesign-idml-package";
	private final String docId = "sd";
	
	private ZipFile zipFile;
	private ZipEntry entry;
	private NextAction nextAction;
	private URI docURI;
	private Enumeration<? extends ZipEntry> entries;
	private int subDocId;
	private LinkedList<Event> queue;
	private String srcLang;
	private IDMLContentFilter filter;
	private Parameters params;

	public IDMLFilter () {
		params = new Parameters();
	}
	
	public void cancel () {
		// TODO Auto-generated method stub
	}

	public void close () {
		try {
			nextAction = NextAction.DONE;
			if ( zipFile != null ) {
				zipFile.close();
				zipFile = null;
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ISkeletonWriter createSkeletonWriter () {
		return null; // There is no corresponding skeleton writer
	}
	
	public IFilterWriter createFilterWriter () {
		return new ZipFilterWriter();
	}

	public String getName () {
		return "okf_idml";
	}

	public String getMimeType () {
		return MIMETYPE;
	}

	public IParameters getParameters () {
		return params;
	}

	public boolean hasNext () {
		return ((( queue != null ) && ( !queue.isEmpty() )) || ( nextAction != NextAction.DONE ));
	}

	public Event next () {
		// Send remaining event from the queue first
		if ( queue.size() > 0 ) {
			return queue.poll();
		}
		
		// When the queue is empty: process next action
		switch ( nextAction ) {
		case OPENZIP:
			return openZipFile();
		case NEXTINZIP:
			return nextInZipFile();
		case NEXTINSUBDOC:
			return nextInSubDocument();
		default:
			throw new RuntimeException("Invalid next() call.");
		}
	}

	public void open (InputStream input) {
		// Not supported for this filter
		throw new UnsupportedOperationException(
			"Method is not supported for this filter.");
	}

	public void open (CharSequence inputText) {
		// Not supported for this filter
		throw new UnsupportedOperationException(
			"Method is not supported for this filter.");
	}

	public void open (URI inputURI) {
		close();
		docURI = inputURI;
		nextAction = NextAction.OPENZIP;
		queue = new LinkedList<Event>();
		filter = new IDMLContentFilter();
	}

	public void setOptions (String sourceLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		setOptions(sourceLanguage, null, defaultEncoding, generateSkeleton);
	}

	public void setOptions (String sourceLanguage,
		String targetLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		srcLang = sourceLanguage;
	}

	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	private Event openZipFile () {
		try {
			zipFile = new ZipFile(new File(docURI));
			entries = zipFile.entries();
			subDocId = 0;
			nextAction = NextAction.NEXTINZIP;
			
			StartDocument startDoc = new StartDocument(docId);
			startDoc.setName(docURI.getPath());
			startDoc.setLanguage(srcLang);
			startDoc.setMimeType(MIMETYPE);
			ZipSkeleton skel = new ZipSkeleton(zipFile);
			queue.add(new Event(EventType.START_DOCUMENT, startDoc, skel));
			
			return new Event(EventType.START);
		}
		catch ( ZipException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private Event nextInZipFile () {
		while( entries.hasMoreElements() ) {
			entry = entries.nextElement();
			if ( entry.getName().startsWith("Stories/")
				&& entry.getName().endsWith(".xml") ) {
				return openSubDocument();
			}
			else {
				DocumentPart dp = new DocumentPart(entry.getName(), false);
				ZipSkeleton skel = new ZipSkeleton(entry);
				return new Event(EventType.DOCUMENT_PART, dp, skel);
			}
		}

		// No more sub-documents: end of the ZIP document
		close();
		queue.add(new Event(EventType.FINISHED));
		Ending ending = new Ending("ed");
		return new Event(EventType.END_DOCUMENT, ending);
	}
	
	private Event openSubDocument () {
		filter.close(); // Make sure the previous is closed
		filter.setParameters(params);
		filter.setOptions(srcLang, "UTF-8", true);
		Event event;
		try {
			filter.open(zipFile.getInputStream(entry));
			filter.next(); // START
			event = filter.next(); // START_DOCUMENT
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// Change the START_DOCUMENT event to START_SUBDOCUMENT
		StartSubDocument sd = new StartSubDocument(docId, String.valueOf(++subDocId));
		sd.setName(entry.getName());
		nextAction = NextAction.NEXTINSUBDOC;
		ZipSkeleton skel = new ZipSkeleton(
			(GenericSkeleton)event.getResource().getSkeleton(), entry);
		return new Event(EventType.START_SUBDOCUMENT, sd, skel);
	}
	
	private Event nextInSubDocument () {
		Event event;
		while ( filter.hasNext() ) {
			event = filter.next();
			switch ( event.getEventType() ) {
			case END_DOCUMENT:
				// Read the FINISHED event
				filter.next();
				// Change the END_DOCUMENT to END_SUBDOCUMENT
				Ending ending = new Ending(String.valueOf(subDocId));
				nextAction = NextAction.NEXTINZIP;
				ZipSkeleton skel = new ZipSkeleton(
					(GenericSkeleton)event.getResource().getSkeleton(), entry);
				return new Event(EventType.END_SUBDOCUMENT, ending, skel);
			
			default: // Else: just pass the event through
				return event;
			}
		}
		return null; // Should not get here
	}

}
