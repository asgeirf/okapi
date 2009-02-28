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

package net.sf.okapi.common.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

/**
 * Implements the IFilterWriter interface for filters that use the
 * GenericSkeleton skeleton.
 */
public class GenericFilterWriter implements IFilterWriter {

	private ISkeletonWriter skelWriter;
	private OutputStream output;
	private String outputPath;
	private OutputStreamWriter writer;
	private String language;
	private String encoding;
	private EncoderManager encoderManager;
	private File tempFile;
	
	public GenericFilterWriter (ISkeletonWriter skelWriter) {
		this.skelWriter = skelWriter;
		encoderManager = new EncoderManager();
	}
	
	public void close () {
		if ( writer == null ) return;
		IOException err = null;
		InputStream orig = null;
		OutputStream dest = null;
		try {
			// Close the output
			writer.close();
			writer = null;

			// If it was in a temporary file, copy it over the existing one
			// If the IFilter.close() is called before IFilterWriter.close()
			// this should allow to overwrite the input.
			if ( tempFile != null ) {
				dest = new FileOutputStream(outputPath);
				orig = new FileInputStream(tempFile); 
				byte[] buffer = new byte[2048];
				int len;
				while ( (len = orig.read(buffer)) > 0 ) {
					dest.write(buffer, 0, len);
				}
			}
		}
		catch ( IOException e ) {
			err = e;
		}
		finally {
			// Make sure we close both files
			if ( dest != null ) {
				try {
					dest.close();
				}
				catch ( IOException e ) {
					err = e;
				}
				dest = null;
			}
			if ( orig != null ) {
				try {
					orig.close();
				} catch ( IOException e ) {
					err = e;
				}
				orig = null;
				if ( err != null ) throw new RuntimeException(err);
				else {
					if ( tempFile != null ) {
						tempFile.delete();
						tempFile = null;
					}
				}
			}
		}
	}

	public String getName () {
		return "GenericFilterWriter";
	}

	public IParameters getParameters () {
		return null;
	}

	public Event handleEvent (Event event) {
		try {
			switch ( event.getEventType() ) {
			case START:
				processStart();
				break;
			case FINISHED:
				processFinished();
				break;
			case START_DOCUMENT:
				processStartDocument(language, encoding, null, encoderManager, (StartDocument)event.getResource());
				break;
			case END_DOCUMENT:
				processEndDocument((Ending)event.getResource());
				close();
				break;
			case START_SUBDOCUMENT:
				processStartSubDocument((StartSubDocument)event.getResource());
				break;
			case END_SUBDOCUMENT:
				processEndSubDocument((Ending)event.getResource());
				break;
			case START_GROUP:
				processStartGroup((StartGroup)event.getResource());
				break;
			case END_GROUP:
				processEndGroup((Ending)event.getResource());
				break;
			case TEXT_UNIT:
				processTextUnit((TextUnit)event.getResource());
				break;
			case DOCUMENT_PART:
				processDocumentPart((DocumentPart)event.getResource());
				break;
			}
		}
		catch ( FileNotFoundException e ) {
			throw new RuntimeException(e);
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return event;
	}

	private void processStart () {
		skelWriter.processStart();
	}

	private void processFinished () {
		skelWriter.processFinished();
		close();
	}
	
	private void processStartDocument(String outputLanguage,
		String outputEncoding,
		ILayerProvider layer,
		EncoderManager encoderManager,
		StartDocument resource) throws IOException
	{
		// Create the output
		createWriter(resource);
		writer.write(skelWriter.processStartDocument(outputLanguage,
			outputEncoding, layer, encoderManager, resource));
	}

	private void processEndDocument(Ending resource) throws IOException {
		writer.write(skelWriter.processEndDocument(resource));
	}

	private void processStartSubDocument (StartSubDocument resource) throws IOException {
		writer.write(skelWriter.processStartSubDocument(resource));
	}

	private void processEndSubDocument (Ending resource) throws IOException {
		writer.write(skelWriter.processEndSubDocument(resource));
	}

	private void processStartGroup (StartGroup resource) throws IOException {
		writer.write(skelWriter.processStartGroup(resource));
	}

	private void processEndGroup (Ending resource) throws IOException {
		writer.write(skelWriter.processEndGroup(resource));
	}

	private void processTextUnit (TextUnit resource) throws IOException {
		writer.write(skelWriter.processTextUnit(resource));
	}

	private void processDocumentPart (DocumentPart resource) throws IOException {
		writer.write(skelWriter.processDocumentPart(resource));
	}

	public void setOptions (String language,
		String defaultEncoding)
	{
		this.language = language;
		this.encoding = defaultEncoding;
	}

	public void setOutput (String path) {
		close(); // Make sure previous is closed
		this.outputPath = path;
	}

	public void setOutput (OutputStream output) {
		close(); // Make sure previous is closed
		this.output = output; // then assign the new stream
	}

	public void setParameters (IParameters params) {
	}

	private void createWriter (StartDocument resource) {
		try {
			tempFile = null;
			// If needed, create the output stream from the path provided
			if ( output == null ) {
				boolean useTemp = false;
				File f = new File(outputPath);
				if ( f.exists() ) {
					// If the file exists, try to remove
					useTemp = !f.delete();
				}
				if ( useTemp ) {
					// Use a temporary output if we can overwrite for now
					// If it's the input file, IFilter.close() will free it before we
					// call close() here (that is if IFilter.close() is called correctly
					tempFile = File.createTempFile("gfwTmp", null);
					output = new BufferedOutputStream(new FileOutputStream(tempFile.getAbsolutePath()));
				}
				else { // Make sure the directory exists
					Util.createDirectories(outputPath);
					output = new BufferedOutputStream(new FileOutputStream(outputPath));
				}
			}
			
			// Get the encoding of the original document
			String originalEnc = resource.getEncoding();
			// If it's undefined, assume it's the default of the system
			if ( originalEnc == null ) {
				originalEnc = Charset.defaultCharset().name();
			}
			// Check if the output encoding is defined
			if ( encoding == null ) {
				// if not: Fall back on the encoding of the original
				encoding = originalEnc;
			}
			// Create the output
			writer = new OutputStreamWriter(output, encoding);
			// Set default UTF-8 BOM usage
			boolean useUTF8BOM = false; //TODO: use platform default
			// Check if the output encoding is UTF-8
			if ( "utf-8".equalsIgnoreCase(encoding) ) {
				// If the original was UTF-8 too
				if ( "utf-8".equalsIgnoreCase(originalEnc) ) {
					// Check whether it had a BOM or not
					useUTF8BOM = resource.hasUTF8BOM();
				}
			}
			// Write out the BOM if needed
			Util.writeBOMIfNeeded(writer, useUTF8BOM, encoding);
		}
		catch ( FileNotFoundException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

}
