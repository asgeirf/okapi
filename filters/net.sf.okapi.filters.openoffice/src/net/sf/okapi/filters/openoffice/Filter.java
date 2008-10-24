package net.sf.okapi.filters.openoffice;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.filters.IParser.ParserTokenType;
import net.sf.okapi.common.resource.Group;
import net.sf.okapi.common.resource.IResource;

public class Filter implements IFilter {

	private enum StateType {
		OPENZIP, NEXTINZIP, NEXTINSUBDOC, DONE
	}
	
	private InputStream input;
	private Parser parser;
	private ZipFile zipFile;
	private StateType nextAction = StateType.OPENZIP;
	private String inputPath;
	private Enumeration<? extends ZipEntry> entries;
	private FilterEvent event;
	private Group subDocResource;

	public Filter () {
		parser = new Parser();
	}
	
	public void cancel () {
		parser.cancel();
	}

	public void close () {
		try {
			if ( zipFile != null ) {
				zipFile.close();
				zipFile = null;
			}
			parser.close();
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public String getName () {
		return "OpenOfficeFilter";
	}

	public IParameters getParameters () {
		return parser.resource.getParameters();
	}

	public IResource getResource () {
		//TODO: fix type
		return (IResource)parser.getResource();
	}

	public boolean hasNext () {
		return (nextAction != StateType.DONE);
	}

	//TODO: need to change to event when interface changes
	public Enum<?> next () {
		try {
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
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public void open (String inputPath) {
		this.inputPath = inputPath;
	}

	public void open (CharSequence inputText) {
		// Not supported for this filter
		throw new RuntimeException("CharSequence input not supported for this filter.");
	}

	public void setOptions (String language,
		String defaultEncoding)
	{
		// TODO Auto-generated method stub
	}

	public void setParameters (IParameters params) {
		parser.resource.params = (Parameters)params;
	}

	private Enum<?> openZipFile () throws IOException {
		zipFile = new ZipFile(inputPath);
		entries = zipFile.entries();
		event = new FilterEvent(FilterEventType.START_DOCUMENT, parser.resource);
		nextAction = StateType.NEXTINZIP;
		return event.getEventType();
	}

	private Enum<?> nextInZipFile () throws IOException {
		// Find the relevant zip entries and process them
		ZipEntry entry;
		while( entries.hasMoreElements() ) {
			entry = entries.nextElement();
			if ( entry.getName().equals("content.xml")
				|| entry.getName().equals("meta.xml")
				|| entry.getName().equals("styles.xml") )
			{
				return openSubDocument(entry);
			}
			else continue;
		}

		// No more sub-documents: end of the document
		close();
		event = new FilterEvent(FilterEventType.END_DOCUMENT, parser.resource);
		nextAction = StateType.DONE;
		return event.getEventType();
	}

	private Enum<?> openSubDocument (ZipEntry zipEntry) throws IOException {
		// Start of the sub-document
		// Get the input stream
		input = new BufferedInputStream(zipFile.getInputStream(zipEntry));
		parser.open(input);
		// Send the start sub-document event
		subDocResource = new Group();
		subDocResource.setName(zipEntry.getName());
		subDocResource.setType(zipEntry.getName());
		event = new FilterEvent(FilterEventType.START_SUBDOCUMENT, subDocResource);
		nextAction = StateType.NEXTINSUBDOC;
		return event.getEventType();
	}
	
	private Enum<?> nextInSubDocument () throws IOException {
		ParserTokenType tok;
		do {
			switch ( (tok = parser.parseNext()) ) {
			case TRANSUNIT:
				event = new FilterEvent(FilterEventType.TEXT_UNIT, parser.getResource());
				return event.getEventType();
			case SKELETON:
				event = new FilterEvent(FilterEventType.SKELETON_UNIT, parser.getResource());
				return event.getEventType();
			case STARTGROUP:
				event = new FilterEvent(FilterEventType.START_GROUP, parser.getResource());
				return event.getEventType();
			case ENDGROUP:
				event = new FilterEvent(FilterEventType.END_GROUP, parser.getResource());
				return event.getEventType();
			default:
				assert(false); // Catch problem
			}
		}
		while ( tok != ParserTokenType.ENDINPUT );

		// Send the end sub-document even
		parser.close();
		// input.close(); Not needed as the reader is set to do it automatically
		event = new FilterEvent(FilterEventType.END_SUBDOCUMENT, subDocResource);
		nextAction = StateType.NEXTINZIP;
		return event.getEventType();
	}

}
