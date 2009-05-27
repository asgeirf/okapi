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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;

import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.RawDocument;

public class LineBreakConversionStep extends BasePipelineStep {

	private boolean isDone;
	private Parameters params;

	public LineBreakConversionStep () {
		params = new Parameters();
	}
	
	public String getDescription () {
		return "Convert the type of line-break in a document.";
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
		return pipeline.isLastStep(this);
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
			
			InputStream input;
			if ( rawDoc.getInputCharSequence() != null ) {
				throw new OkapiBadStepInputException("CharSequence input not supported for now.");
			}
			if ( rawDoc.getInputURI() != null ) {
				input = new FileInputStream(new File(rawDoc.getInputURI()));
			}
			else if ( rawDoc.getInputStream() != null ) {
				// Try to cast, in cast it's a FileInputStream
				try {
					input = (FileInputStream)rawDoc.getInputStream();
				}
				catch ( ClassCastException e ) {
					throw new OkapiBadStepInputException("RawDocument is set with an incompatible type of InputStream.");
				}
			}
			else {
				// Change this exception to more generic (not just filter)
				throw new OkapiBadStepInputException("RawDocument has no input defined.");
			}
			BOMAwareInputStream bis = new BOMAwareInputStream(input, rawDoc.getEncoding());
			String encoding = bis.detectEncoding(); // Update the encoding: it'll be use for the output
			reader = new BufferedReader(new InputStreamReader(bis, encoding));
			
			// Open the output
			File outFile;
			if ( pipeline.isLastStep(this) ) {
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
			
			writer = new OutputStreamWriter(new BufferedOutputStream(output), encoding);
			// Write BOM if there was one
			Util.writeBOMIfNeeded(writer, (bis.getBOMSize()>0), encoding);
			
			// Set the variables
			CharBuffer buffer = CharBuffer.allocate(1024);
			int length = 0;
			int start = 0;
			int i;
			int done = 0;
			
			// Process the file
			while ( (length = reader.read(buffer)) > 0 ) {
				buffer.position(0);
				// Reset 'done' flag on second pass after it was set
				if ( done == 1 ) done++; else done = 0;
				// Replace line-breaks
				for ( i=0; i<length; i++ ) {
					if ( buffer.charAt(i) == '\n') {
						if (( i != 0 ) || ( done == 0 )) {
							writer.write(buffer.array(), start, i-start);
							writer.write(params.lineBreak);
						}
						start = i+1;
					}
					else if ( buffer.charAt(i) == '\r') {
						writer.write(buffer.array(), start, i-start);
						writer.write(params.lineBreak);
						// Check if it's a \r\n
						if ( i+1 < length ) {
							if ( buffer.charAt(i+1) == '\n' ) {
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
					writer.write(buffer.array(), start, length-start);
				}
				
				// Set the new raw-document URI and the encoding (in case one was auto-detected)
				// Other info stays the same
				rawDoc.setInputURI(outFile.toURI());
				rawDoc.setEncoding(encoding);
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException("IO error while converting.", e);
		}
		finally {
			isDone = true;
			try {
				if ( writer != null ) {
					writer.close();
					writer = null;
				}
				if ( reader != null ) {
					reader.close();
					reader = null;
				}
			}
			catch ( IOException e ) {
				throw new OkapiIOException("IO error while closing.", e);
			}
		}
	}

}
