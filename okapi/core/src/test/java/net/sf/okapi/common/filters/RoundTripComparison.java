/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.filters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.resource.RawDocument;

public class RoundTripComparison {
	private static final Logger LOGGER = Logger.getLogger(RoundTripComparison.class.getName());

	private IFilter filter;
	private ArrayList<Event> extraction1Events;
	private ArrayList<Event> extraction2Events;
	private ArrayList<Event> subDocEvents;
	private Event subDocEvent;
	private IFilterWriter writer;
	private ByteArrayOutputStream writerBuffer;
	private String defaultEncoding;
	private LocaleId srcLoc;
	private LocaleId trgLoc;
	private boolean includeSkeleton;

	public RoundTripComparison (boolean includeSkeleton) {
		extraction1Events = new ArrayList<Event>();
		extraction2Events = new ArrayList<Event>();
		subDocEvents = new ArrayList<Event>();
		this.includeSkeleton = includeSkeleton;
	}

	public RoundTripComparison () {
		this(true);
	}

	public boolean executeCompare (IFilter filter, List<InputDocument> inputDocs,
			String defaultEncoding, LocaleId srcLoc, LocaleId trgLoc) {
		this.filter = filter;
		this.defaultEncoding = defaultEncoding;
		this.srcLoc = srcLoc;
		this.trgLoc = trgLoc;

		// Create the filter-writer for the provided filter
		writer = filter.createFilterWriter();

		for (InputDocument doc : inputDocs) {
			LOGGER.fine("Processing Document: " + doc.path);
			
			// Reset the event lists
			extraction1Events.clear();
			extraction2Events.clear();
			subDocEvents.clear();
			// Load parameters if needed
			if (doc.paramFile != null && !doc.paramFile.equals("")) {
				String root = Util.getDirectoryName(doc.path);
				IParameters params = filter.getParameters();
				if (params != null) {
					params.load(Util.toURI(root + File.separator + doc.paramFile), false);
				}
			}
			// Execute the first extraction and the re-writing
			executeFirstExtraction(doc);
			// Execute the second extraction from the output of the first
			executeSecondExtraction();
			// Compare the events
			if ( !FilterTestDriver.compareEvents(extraction1Events, extraction2Events, includeSkeleton) ) {
				throw new RuntimeException("Events are different for " + doc.path);
			}
		}
		return true;
	}

	public boolean executeCompare (IFilter filter,
		List<InputDocument> inputDocs,
		String defaultEncoding,
		LocaleId srcLoc,
		LocaleId trgLoc,
		String dirSuffix)
	{
		//return executeCompare(filter, inputDocs, defaultEncoding, srcLoc, trgLoc, dirSuffix, (IPipelineStep[]) null);
		this.filter = filter;
		this.defaultEncoding = defaultEncoding;
		this.srcLoc = srcLoc;
		this.trgLoc = trgLoc;

		// Create the filter-writer for the provided filter
		writer = filter.createFilterWriter();

		for (InputDocument doc : inputDocs) {
			LOGGER.info("Processing Document: " + doc.path);
			// Reset the event lists
			extraction1Events.clear();
			extraction2Events.clear();
			subDocEvents.clear();
			// Load parameters if needed, !!! no reset is called for parameters
			if (doc.paramFile != null && !doc.paramFile.equals("")) {
				String root = Util.getDirectoryName(doc.path);
				IParameters params = filter.getParameters();
				if (params != null) {
					params.load(Util.toURI(root + File.separator + doc.paramFile), false);
				}
			}
			// Execute the first extraction and the re-writing
			String outPath = executeFirstExtractionToFile(doc, dirSuffix, (IPipelineStep[]) null);
			// Execute the second extraction from the output of the first
			executeSecondExtractionFromFile(outPath);
			// Compare the events
			if (!FilterTestDriver.compareEvents(extraction1Events, extraction2Events, subDocEvents, includeSkeleton)) {
				throw new RuntimeException("Events are different for " + doc.path);
			}
		}
		return true;
	}

	public boolean executeCompare(IFilter filter, List<InputDocument> inputDocs,
			String defaultEncoding, LocaleId srcLoc, LocaleId trgLoc, String dirSuffux, IPipelineStep... steps) {
		
		this.filter = filter;
		this.defaultEncoding = defaultEncoding;
		this.srcLoc = srcLoc;
		this.trgLoc = trgLoc;
		
//		Pipeline pipeline = new Pipeline();
//		for (IPipelineStep step : steps) {
//			pipeline.addStep(step);
//		}
		
		// Create the filter-writer for the provided filter
		writer = filter.createFilterWriter();

		for (InputDocument doc : inputDocs) {
			LOGGER.fine("Processing Document: " + doc.path);
			// Reset the event lists
			extraction1Events.clear();
			extraction2Events.clear();
			subDocEvents.clear();
			// Load parameters if needed
			if (doc.paramFile == null) {
				IParameters params = filter.getParameters();
				if (params != null)
					params.reset();
			} else {
				String root = Util.getDirectoryName(doc.path);
				IParameters params = filter.getParameters();
				if (params != null)
					params.load(Util.toURI(root + File.separator + doc.paramFile), false);
			}
			// Execute the first extraction and the re-writing
			String outPath = executeFirstExtractionToFile(doc, dirSuffux, steps);
			// Execute the second extraction from the output of the first
			executeSecondExtractionFromFile(outPath, steps);
			// Compare the events
			if ( !FilterTestDriver.compareEvents(extraction1Events, extraction2Events, includeSkeleton) ) {
				throw new RuntimeException("Events are different for " + doc.path);
			}
		}
		return true;
	}
	
	private void executeFirstExtraction(InputDocument doc) {
		try {
			// Open the input
			filter.open(new RawDocument(Util.toURI(doc.path), defaultEncoding, srcLoc,
					trgLoc));

			// Prepare the output
			writer.setOptions(trgLoc, "UTF-16");
			writerBuffer = new ByteArrayOutputStream();
			writer.setOutput(writerBuffer);

			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				switch (event.getEventType()) {
				case START_DOCUMENT:
				case END_DOCUMENT:				
				case END_SUBDOCUMENT:
					break;
				case START_SUBDOCUMENT:
					subDocEvent = event;
					break;
				case START_GROUP:
				case END_GROUP:
				case TEXT_UNIT:
					extraction1Events.add(event);
					subDocEvents.add(subDocEvent);
					break;
				}
				writer.handleEvent(event);
		}
		} finally {
			if (filter != null)
				filter.close();
			if (writer != null)
				writer.close();
		}
	}

	private void executeSecondExtraction() {
		try {
			// Set the input (from the output of first extraction)
			String input;
			try {
				input = new String(writerBuffer.toByteArray(), "UTF-16");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			filter.open(new RawDocument(input, srcLoc, trgLoc));

			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				switch (event.getEventType()) {
				case START_DOCUMENT:
				case END_DOCUMENT:
				case START_SUBDOCUMENT:
				case END_SUBDOCUMENT:
					break;
				case START_GROUP:
				case END_GROUP:
				case TEXT_UNIT:
					extraction2Events.add(event);
					break;
				}
			}
		} finally {
			if (filter != null)
				filter.close();
		}
	}

	private String executeFirstExtractionToFile(InputDocument doc, String outputDir, IPipelineStep... steps) {
		String outPath = null;
		try {
			// Open the input
			filter.open(new RawDocument(Util.toURI(doc.path), defaultEncoding, srcLoc,
					trgLoc));

			// Prepare the output
			writer.setOptions(trgLoc, "UTF-8");
			outPath = Util.getDirectoryName(doc.path);
			if ( Util.isEmpty(outputDir) ) {
				outPath += (File.separator + Util.getFilename(doc.path, true));
			}
			else {
				outPath += (File.separator + outputDir + File.separator + Util.getFilename(doc.path, true));
			}
			writer.setOutput(Util.fixPath(outPath));
			
			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				switch (event.getEventType()) {
				case START_DOCUMENT:
				case END_DOCUMENT:
				case END_SUBDOCUMENT:
					break;
				case START_SUBDOCUMENT:
					subDocEvent = event;
					break;
				case START_GROUP:
				case END_GROUP:
				case TEXT_UNIT:
					if (event.isTextUnit()) {
						// Steps can modify the event, but we need to compare events as were from the filter, so we are cloning 
						extraction1Events.add(new Event(EventType.TEXT_UNIT, event.getTextUnit().clone()));
					}
					else {
						extraction1Events.add(event);
					}
					
					subDocEvents.add(subDocEvent);
					break;
				}
				if (steps != null) {
					for (IPipelineStep step : steps) {
						event = step.handleEvent(event);						
					}
				}					
				writer.handleEvent(event);
			}
		} finally {
			if (filter != null)
				filter.close();
			if (writer != null)
				writer.close();
		}
		return outPath;
	}

	private void executeSecondExtractionFromFile(String input, IPipelineStep... steps) {
		try {
			// Set the input (from the output of first extraction)
			filter.open(new RawDocument(Util.toURI(input), "UTF-8", srcLoc, trgLoc));

			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				switch (event.getEventType()) {
				case START_DOCUMENT:
				case END_DOCUMENT:
				case START_SUBDOCUMENT:
				case END_SUBDOCUMENT:
					break;
				case START_GROUP:
				case END_GROUP:
				case TEXT_UNIT:
					extraction2Events.add(event);
					break;
				}
			}
		} finally {
			if (filter != null)
				filter.close();
		}
	}
	
	private void executeSecondExtractionFromFile(String input) {
		try {
			// Set the input (from the output of first extraction)
			filter.open(new RawDocument(Util.toURI(input), "UTF-8", srcLoc, trgLoc));

			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				switch (event.getEventType()) {
				case START_DOCUMENT:
				case END_DOCUMENT:
				case START_SUBDOCUMENT:
				case END_SUBDOCUMENT:
					break;
				case START_GROUP:
				case END_GROUP:
				case TEXT_UNIT:
					extraction2Events.add(event);
					break;
				}
			}
		} finally {
			if (filter != null)
				filter.close();
		}
	}
}
