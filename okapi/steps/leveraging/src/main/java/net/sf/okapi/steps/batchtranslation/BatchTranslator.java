/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.batchtranslation;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.XMLWriter;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.TMXWriter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.lib.segmentation.SRXDocument;
import net.sf.okapi.lib.translation.QueryUtil;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.tm.pensieve.common.TranslationUnitVariant;
import net.sf.okapi.tm.pensieve.seeker.ITmSeeker;
import net.sf.okapi.tm.pensieve.seeker.TmSeekerFactory;
import net.sf.okapi.tm.pensieve.writer.ITmWriter;
import net.sf.okapi.tm.pensieve.writer.TmWriterFactory;

public class BatchTranslator {

	private static final Logger LOGGER = Logger.getLogger(BatchTranslator.class.getName());

	private IFilterConfigurationMapper fcMapper;
	private IFilter filter;
	private RawDocument rawDoc;
	private QueryUtil qutil;
	private File htmlSourceFile;
	private File htmlTargetFile;
	private File originalStoreFile;
	private Parameters params;
	private ITmWriter tmWriter;
	private TMXWriter tmxWriter;
	private LocaleId srcLoc;
	private LocaleId trgLoc;
	private int subDocId;
	private int currentSubDocId;
	private boolean initDone;
	private Map<String, String> attributes;
	private SimpleStore store;
	private ITmSeeker existingTm;
	private ITmSeeker currentTm;
	private int docInternalMatches;
	private int totalInternalMatches;
	private int docExternalMatches;
	private int totalExternalMatches;
	private int docEntries;
	private int totalEntries;
	private ISegmenter segmenter;
	private String rootDir;

	public BatchTranslator (IFilterConfigurationMapper fcMapper,
		Parameters params,
		String rootDir)
	{
		this.fcMapper = fcMapper;
		this.params = params;
		this.rootDir = rootDir;
		
		if ( this.params == null ) {
			this.params = new Parameters();
		}
		qutil = new QueryUtil();
		initDone = false;
	}
	
	@Override
	protected void finalize () {
		closeAll();
	}
	
	private void closeAll () {
		if ( tmxWriter != null ) {
			tmxWriter.writeEndDocument();
			tmxWriter.close();
			tmxWriter = null;
		}
		if ( existingTm != null ) {
			existingTm.close();
			existingTm = null;
		}
		if ( currentTm != null ) {
			currentTm.close();
			currentTm = null;
		}
		initDone = false;
	}

	public void endBatch () {
		LOGGER.info("");
		if ( currentTm != null ) {
			LOGGER.info(String.format("Total matches from TM being built = %d", totalInternalMatches));
			LOGGER.info(String.format("Total matches from existing TM = %d", totalExternalMatches));
		}
		LOGGER.info(String.format("Total entries sent to translation = %d", totalEntries));
		
		// Then close all files/TMs
		closeAll();
	}

	// Call this method at the first document
	private void initialize () {
		if ( params.getMakeTMX() ) {
			String tmxOutputPath = Util.fillRootDirectoryVariable(params.getTmxPath(), rootDir);
			tmxOutputPath = LocaleId.replaceVariables(tmxOutputPath, srcLoc, trgLoc);
			tmxWriter = new TMXWriter(tmxOutputPath);
			tmxWriter.writeStartDocument(srcLoc, trgLoc, getClass().getCanonicalName(), "1", "sentence",
				(params.getMarkAsMT() ? "MT-based" : null), "unknown");
		}
		
		attributes = new Hashtable<String, String>();
		if ( params.getMarkAsMT() ) {
			attributes.put("creationid", Util.MTFLAG);
		}
		if ( !Util.isEmpty(params.getOrigin()) ) {
			attributes.put("Txt::Origin", params.getOrigin());
		}
		initDone = true;

		store = new SimpleStore();
		totalInternalMatches = 0;
		totalExternalMatches = 0;
		totalEntries = 0;

		// Initialize existing TM if needed
		if ( params.getCheckExistingTm() ) {
			String existingTMPath = Util.fillRootDirectoryVariable(params.getExistingTm(), rootDir);
			existingTMPath = LocaleId.replaceVariables(existingTMPath, srcLoc, trgLoc);
			existingTm = TmSeekerFactory.createFileBasedTmSeeker(existingTMPath);
		}
		
		segmenter = null;
		if ( params.getSegment() ) {
			SRXDocument srxDoc = new SRXDocument();
			String srxPath = Util.fillRootDirectoryVariable(params.getSrxPath(), rootDir);
			srxPath = LocaleId.replaceVariables(srxPath, srcLoc, trgLoc);
			srxDoc.loadRules(srxPath);
			//if ( srxDoc.hasWarning() ) logger.warning(srxDoc.getWarning());
			segmenter = srxDoc.compileLanguageRules(srcLoc, null);
		}
	}
	
	public void processDocument (RawDocument rd) {
		rawDoc = rd;
		srcLoc = rawDoc.getSourceLocale();
		trgLoc = rawDoc.getTargetLocale();
		
		if ( !initDone ) {
			initialize();
		}
		
		filter = fcMapper.createFilter(rd.getFilterConfigId(), filter);
		if ( filter == null ) {
			throw new RuntimeException(String.format(
				"No filter available for the configuration '%s'.",
				rd.getFilterConfigId()));
		}

		processInput();
	}
	
	private void processInput () {
		XMLWriter htmlWriter = null;
		try {
			// Open the document
			filter.open(rawDoc);

			// Create initial temporary file
			htmlSourceFile = File.createTempFile("hft_", ".html");
			if ( htmlSourceFile != null ) {
				htmlSourceFile.deleteOnExit();
			}

			// Open the TM if needed
			if ( tmWriter != null ) {				
				tmWriter.close();
				tmWriter = null;
			}
			if ( params.getMakeTM() ) {
				String tmDir = Util.fillRootDirectoryVariable(params.getTmDirectory(), rootDir);
				tmDir = LocaleId.replaceVariables(tmDir, srcLoc, trgLoc);
				Util.createDirectories(tmDir+File.separator);
				//TODO: Move this check at the pensieve package level
				File file = new File(tmDir+File.separator+"segments.gen");
				// Create a new index only if one does not exists yet
				// If one exists we pass false to append to it
				tmWriter = TmWriterFactory.createFileBasedTmWriter(tmDir, !file.exists());
				
				// Open TM seeker to check for repetitions
				// We have to close and re-open to take into account new entry from previous document
				// We currently cannot found entry being added before a close of the TM writer.
				if ( currentTm != null ) {
					currentTm.close();
				}
				currentTm = TmSeekerFactory.createFileBasedTmSeeker(tmDir);
			}
			
			// Process blocks for N entries
			docInternalMatches = 0;
			docExternalMatches = 0;
			docEntries = 0;
			int count = 0;
			int maxCount = params.getBlockSize();
			Event event;
			subDocId = 0;
			currentSubDocId = 0;

			while ( filter.hasNext() ) {
				event = filter.next();
				switch ( event.getEventType() ) {
				case START_SUBDOCUMENT:
					currentSubDocId = ++subDocId;
					break;
					
				case END_SUBDOCUMENT:
					currentSubDocId = 0; // Top-level
					break;
					
				case TEXT_UNIT:
					ITextUnit tu = event.getTextUnit();
					if ( !tu.isTranslatable() ) continue;

					TextContainer tc = tu.getSource();

					// Segment if needed
					if ( segmenter != null ) {
						if ( segmenter.computeSegments(tc) > 1 ) {
							tc.getSegments().create(segmenter.getRanges());
						}
					}

					// If we have no files ready yet, create them
					if ( htmlWriter == null ) {
						htmlWriter = startTemporaryFiles();
					}
					
					// Write out to source input
					boolean atLeastOne = false;
					
					for ( Segment seg : tc.getSegments() ) {
						// If needed, check if the entry is in the existing TM
						if ( currentTm != null ) {
							if ( currentTm.searchFuzzy(seg.text, 95, 1, null).size() > 0 ) {
								// If we have a hit, no need to query the MT
								docInternalMatches++;
								continue;
							}
						}
						if ( existingTm != null ) {
							if ( existingTm.searchFuzzy(seg.text, 95, 1, null).size() > 0 ) {
								// If we have a hit, no need to query the MT
								docExternalMatches++;
								continue;
							}
						}
						// Store
						store.write(seg.text);
						htmlWriter.writeStartElement("p");
						htmlWriter.writeAttributeString("id", String.format("%d:%s:%s", currentSubDocId, tu.getId(), seg.id));
						htmlWriter.writeRawXML(qutil.toCodedHTML(seg.text));
						htmlWriter.writeEndElementLineBreak(); // p
						atLeastOne = true;
						docEntries++;
					}
					
					if ( atLeastOne ) count++;
				}
				
				// Check if we reached the number of entries per block
				if ( count >= maxCount ) {
					// Close the temporary files
					finishTemporaryFiles(htmlWriter);
					// Run the batch process
					runBatchTranslation();
					// Retrieve the translations
					retrieveTranslation();
					// Reset the counter for next block
					count = 0;
					htmlWriter = null;
				}
			}

			// Process the last block if needed
			if ( count > 0 ) {
				// Close the temporary files
				finishTemporaryFiles(htmlWriter);
				htmlWriter = null;
				// Run the batch process
				runBatchTranslation();
				// Retrieve the translations
				retrieveTranslation();
			}
			else {
				// Close the temporary files
				// But no need to process them
				finishTemporaryFiles(htmlWriter);
				htmlWriter = null;
			}
		}
		catch ( Throwable e ) {
			throw new RuntimeException(String.format("Error when processing a file.\nSource='%s'\nTarget='%s'",
				htmlSourceFile.toURI(), htmlTargetFile.toURI()), e);
		}
		finally {
			if ( htmlWriter != null ) {
				finishTemporaryFiles(htmlWriter);
			}
			if ( filter != null ) {
				filter.close();
			}
			if ( tmWriter != null ) {
				tmWriter.close();
			}

			if ( currentTm != null ) {
				LOGGER.info(String.format("Existing matches from TM being built = %d", docInternalMatches));
				LOGGER.info(String.format("Existing matches from existing TM = %d", docExternalMatches));
			}
			LOGGER.info(String.format("Entries sent to translation = %d", docEntries));

			totalInternalMatches += docInternalMatches;
			totalExternalMatches += docExternalMatches;
			totalEntries += docEntries;
		}
	}
	
	private XMLWriter startTemporaryFiles () {
		// Create the HTML source file
		XMLWriter htmlWriter = new XMLWriter(htmlSourceFile.getPath());
		// Start building the source file
		htmlWriter.writeStartElement("html");
		htmlWriter.writeStartElement("meta");
		htmlWriter.writeAttributeString("http-equiv", "Content-Type");
		htmlWriter.writeAttributeString("content", "text/html; charset=UTF-8");
		htmlWriter.writeEndElementLineBreak();

		// Set the output name and make sure it's deleted
		String path = htmlSourceFile.getAbsolutePath();
		path = Util.getDirectoryName(path) + File.separator + Util.getFilename(path, false) + ".trg.html";
		htmlTargetFile = new File(path);
		if ( htmlTargetFile.exists() ) {
			htmlTargetFile.delete();
		}

		// Create the store for the original source
		path = htmlSourceFile.getAbsolutePath();
		path = Util.getDirectoryName(path) + File.separator + Util.getFilename(path, false) + ".ori.bin";
		originalStoreFile = new File(path);
		store.create(originalStoreFile);
		
		return htmlWriter;
	}

	private void finishTemporaryFiles (XMLWriter htmlWriter) {
		// Close the temporary source input
		if ( htmlWriter != null ) {
			htmlWriter.writeEndElement(); // html
			htmlWriter.writeEndDocument();
			htmlWriter.close();
			// htmlWriter should be reset to null by the caller 
		}
		
		// Close the original entry store
		if ( store != null ) {
			store.close();
		}
	}
	

	private void runBatchTranslation () {
		String cmd = params.getCommand();
		try {
			cmd = cmd.replace("${inputURI}", htmlSourceFile.toString());
			cmd = cmd.replace("${inputPath}", htmlSourceFile.getPath());
			cmd = cmd.replace("${outputPath}", htmlTargetFile.getPath());
			cmd = cmd.replace(Util.ROOT_DIRECTORY_VAR, rootDir);
			
			Locale loc = rawDoc.getSourceLocale().toJavaLocale();
			cmd = cmd.replace("${srcLangName}", loc.getDisplayLanguage(Locale.ENGLISH));
			loc = rawDoc.getTargetLocale().toJavaLocale();
			cmd = cmd.replace("${trgLangName}", loc.getDisplayLanguage(Locale.ENGLISH));
			
			cmd = LocaleId.replaceVariables(cmd, srcLoc, trgLoc);
			
			LOGGER.info("Command line: "+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			
			// Make sure we empty the output buffers 
	    	StreamGobbler errGobbler = new StreamGobbler(p.getErrorStream(), "err");            
	    	StreamGobbler outGobbler = new StreamGobbler(p.getInputStream(), "out");
	    	errGobbler.start();
	    	outGobbler.start();
			
	    	// Wait for the external program to be done 
	    	p.waitFor();
		}
		catch ( IOException e ) {
			throw new RuntimeException("Error during the batch translation.\nCommand line was:\n"+cmd, e);
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException("Program interrupted.", e);
		}
	}
	
	private void retrieveTranslation () {
		Source html = null;
		try {
			// Open the original store for read
			store.openForRead(originalStoreFile);

			// Open the translated file
			html = new Source(htmlTargetFile.toURI().toURL());
			html.fullSequentialParse();
			// Get the elements
			List<Element> paragraphs = html.getAllElements(HTMLElementName.P);

			// Process
			// The element should be in the same order as the event of the original file
			int htmlSubDocId;
			String htmlTuId;
			String htmlSegId;
			
			for ( Element elem : paragraphs ) {
				String id = elem.getAttributeValue("id");
				if ( id == null ) continue; // No id means we can't match
				// Decompose the html id in its sub-doc, tu and seg parts
				String parts[] = id.split(":", -1);
				htmlSubDocId = Integer.valueOf(parts[0]);
				htmlTuId = parts[1];
				htmlSegId = parts[2];
				
				TextFragment srcFrag = store.readNext();
				if ( srcFrag == null ) {
					// Not found, out of synchronization
					break; // No need to continue
				}
				
				TextFragment trgFrag;
				try {
					String ctext = qutil.fromCodedHTML(elem.getContent().toString(), srcFrag, true);
					trgFrag = new TextFragment(ctext, srcFrag.getCodes());
				}
				catch ( Throwable e ) {
					// Catch issues with inline codes
					LOGGER.warning(String.format("Skipping entry '%d:%s:%s'.\n", htmlSubDocId, htmlTuId, htmlSegId)
						+ e.getMessage());
					continue; // Skip this entry
				}

				if ( tmWriter != null ) {
					TranslationUnit unit = new TranslationUnit(
						new TranslationUnitVariant(srcLoc, srcFrag),
						new TranslationUnitVariant(trgLoc, trgFrag));
					tmWriter.indexTranslationUnit(unit);							
				}
				
				if ( tmxWriter != null ) {
					tmxWriter.writeTU(srcFrag, trgFrag, null, attributes);
				}
//				System.out.println("");
//				System.out.println("SRC="+srcFrag.toString());
//				System.out.println("TRG="+trgFrag.toString());
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException(String.format("Error reading the translations.\nSource='%s'\nTarget='%s'",
				htmlSourceFile.toURI(), htmlTargetFile.toURI()), e);
		}
		finally {
			if ( html != null ) html.clearCache();
			htmlTargetFile.deleteOnExit();
			store.close();
		}
	}

}
