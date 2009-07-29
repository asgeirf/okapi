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

package net.sf.okapi.steps.linebreakconversion;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipelinedriver.PipelineContext;
import net.sf.okapi.common.resource.RawDocument;

public class LineBreakConversionStep extends BasePipelineStep {

	private static final int BUFFER_SIZE = 1024;
	
	private boolean isDone;
	private Parameters params;

	public LineBreakConversionStep () {
		params = new Parameters();
	}
	
	@Override
	/**
	 * FIXME: Steps should only depend on the IPipeline, IPipelineStep and IContext interfaces. 
	 * This step depends on the pipeline driver project. 
	 */
	public PipelineContext getContext() {		
		return (PipelineContext)super.getContext();
	}
	
	public String getDescription () {
		return "Convert the type of line-breaks in a text-based file.";
	}

	public String getName () {
		return "Line-Break Conversion";
	}

	@Override
	public boolean isDone () {
		return isDone;
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		params = (Parameters)params;
	}
 
	@Override
	public boolean needsOutput (int inputIndex) {
		return isLastStep();
	}

	@Override
	protected void handleStartBatchItem (Event event) {
		isDone = false;
	}

	@Override
	protected void handleRawDocument (Event event) {
		RawDocument rawDoc;
		BufferedReader reader = null;
		OutputStreamWriter writer = null;
		try {
			rawDoc = (RawDocument)event.getResource();
			
			BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(rawDoc.getStream(), rawDoc.getEncoding());
			detector.detectAndRemoveBom();
			rawDoc.setEncoding(detector.getEncoding());
			reader = new BufferedReader(new InputStreamReader(detector.getInputStream(), rawDoc.getEncoding()));
			
			// Open the output
			File outFile;
			if ( isLastStep() ) {
				outFile = new File(getContext().getOutputURI(0));
				Util.createDirectories(outFile.getAbsolutePath());
			}
			else {
				try {
					outFile = File.createTempFile("okp-lbc_", ".tmp");
				}
				catch ( Throwable e ) {
					throw new OkapiIOException("Cannot create temporary output.", e);
				}
				outFile.deleteOnExit();
			}
			OutputStream output = new FileOutputStream(outFile);
			
			writer = new OutputStreamWriter(new BufferedOutputStream(output), rawDoc.getEncoding());
			// Write BOM if there was one
			Util.writeBOMIfNeeded(writer, detector.hasUtf8Bom(), rawDoc.getEncoding());
			
			// Set the variables
			char[] buf = new char[BUFFER_SIZE];
			int length = 0;
			int i;
			int done = 0;
			
			// Process the file
			while ( (length = reader.read(buf, 0, BUFFER_SIZE-1)) > 0 ) {
				// Check if you need to read the next char to avoid splitting cases
				if ( buf[length-1] == '\r'  ) {
					int count = reader.read(buf, length, 1);
					if ( count > -1 ) length++;
				}
				// Reset 'done' flag on second pass after it was set
				if ( done == 1 ) done++; else done = 0;
				// Replace line-breaks
				int start = 0;
				for ( i=0; i<length; i++ ) {
					if ( buf[i] == '\n') {
						if (( i != 0 ) || ( done == 0 )) {
							writer.write(buf, start, i-start);
							writer.write(params.lineBreak);
						}
						start = i+1;
					}
					else if ( buf[i] == '\r') {
						writer.write(buf, start, i-start);
						writer.write(params.lineBreak);
						// Check if it's a \r\n
						if ( i+1 < length ) {
							if ( buf[i+1] == '\n' ) {
								i++; // Skip it
							}
						}
						start = i+1;
						// We could be splitting a \r\n, so let's remember
						done = 1;
					}
				}
				// Write out the remainder of the buffer
				if ( length-start > 0 ) {
					writer.write(buf, start, length-start);
				}
			}
			
			// Done: close the output
			writer.close();
			// Creates the new RawDocument
			event.setResource(new RawDocument(outFile.toURI(), rawDoc.getEncoding(), 
				rawDoc.getSourceLanguage(), rawDoc.getTargetLanguage()));
		}
		catch ( IOException e ) {
			throw new OkapiIOException("IO error while converting.", e);
		}
		finally {
			isDone = true;
			try {
				if ( writer != null ) {
					writer.close();
				}
				if ( reader != null ) {
					reader.close();
				}
			}
			catch ( IOException e ) {
				throw new OkapiIOException("IO error while closing.", e);
			}
		}
	}

}
