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

package net.sf.okapi.steps.formatconversion;

import java.io.File;
import java.net.URI;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.filterwriter.TMXFilterWriter;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.filters.po.POFilterWriter;

public class FormatConversionStep extends BasePipelineStep {

	private static final int PO_OUTPUT = 0;
	private static final int TMX_OUTPUT = 1;
	private static final int PENSIEVE_OUTPUT = 2;
	
	private Parameters params;
	private IFilterWriter writer;
	private boolean firstOutputCreated;
	private int outputType;
	private URI outputURI;
	private String targetLanguage;

	public FormatConversionStep () {
		params = new Parameters();
	}

	@StepParameterMapping(parameterType = StepParameterType.OUTPUT_URI)
	public void setOutputURI (URI outputURI) {
		this.outputURI = outputURI;
	}
	
	@StepParameterMapping(parameterType = StepParameterType.TARGET_LANGUAGE)
	public void setTargetLanguage (String targetLanguage) {
		this.targetLanguage = targetLanguage;
	}
	
	public String getDescription () {
		return "Converts the output of a filter into a specified file format.";
	}

	public String getName () {
		return "Format Conversion";
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters) params;
	}

	@Override
	public Parameters getParameters () {
		return params;
	}
	
	public Event handleEvent (Event event) {
		switch (event.getEventType()) {
		case START_BATCH:
			firstOutputCreated = false;
			if ( params.getOutputFormat().equals(Parameters.FORMAT_PO) ) {
				outputType = PO_OUTPUT;
			}
			else if ( params.getOutputFormat().equals(Parameters.FORMAT_TMX) ) {
				outputType = TMX_OUTPUT;
			}
			else if ( params.getOutputFormat().equals(Parameters.FORMAT_PENSIEVE) ) {
				outputType = PENSIEVE_OUTPUT;
			}
			break;
			
		case END_BATCH:
			if ( params.isSingleOutput() ) {
				Ending ending = new Ending("end");
				writer.handleEvent(new Event(EventType.END_DOCUMENT, ending));
			}
			break;
			
		case START_DOCUMENT:
			if ( !firstOutputCreated || !params.isSingleOutput() ) {
				switch ( outputType ) {
				case PO_OUTPUT:
					startPOOutput();
					break;
				case TMX_OUTPUT:
					startTMXOutput();
					break;
				case PENSIEVE_OUTPUT:
					startPensieveOutput();
					break;
				}
			}
			writer.handleEvent(event);
			break;
			
		case END_DOCUMENT:
			if ( !params.isSingleOutput() ) {
				writer.handleEvent(event);
			}
			// Else: Do nothing
			break;
			
		case START_SUBDOCUMENT:
		case END_SUBDOCUMENT:
		case START_GROUP:
		case END_GROUP:
			writer.handleEvent(event);
			break;

		case TEXT_UNIT:
			//TODO: Filter empty tu, non-target tu, etc.
			writer.handleEvent(event);
			break;
			
		case START_BATCH_ITEM:
		case END_BATCH_ITEM:
		case RAW_DOCUMENT:
		case DOCUMENT_PART:
		case CUSTOM:
			// Do nothing
			break;
		}
		return event;
	}

	private void startPOOutput () {
		writer = new POFilterWriter();
		net.sf.okapi.filters.po.Parameters outParams = (net.sf.okapi.filters.po.Parameters)writer.getParameters();
		outParams.outputGeneric = params.getUseGenericCodes();
		File outFile;
		if ( isLastOutputStep() ) {
			if ( params.isSingleOutput() ) {
				outFile = new File(params.getOutputPath());
			}
			else {
				outFile = new File(outputURI);
			}
		}
		else {
			try {
				outFile = File.createTempFile("okp-fc_", ".tmp");
			}
			catch ( Throwable e ) {
				throw new OkapiIOException("Cannot create temporary output.", e);
			}
			outFile.deleteOnExit();
		}
		// Not needed, writer does this: Util.createDirectories(outFile.getAbsolutePath());
		writer.setOutput(outFile.getPath());
		writer.setOptions(targetLanguage, "UTF-8");
		firstOutputCreated = true;
	}

	private void startTMXOutput () {
		writer = new TMXFilterWriter();
//		net.sf.okapi.filters.po.Parameters outParams = (net.sf.okapi.filters.po.Parameters)writer.getParameters();
//		outParams.outputGeneric = params.getUseGenericCodes();
		File outFile;
		if ( isLastOutputStep() ) {
			if ( params.isSingleOutput() ) {
				outFile = new File(params.getOutputPath());
			}
			else {
				outFile = new File(outputURI);
			}
			// Not needed, writer does this: Util.createDirectories(outFile.getAbsolutePath());
			writer.setOutput(outFile.getPath());
			writer.setOptions(targetLanguage, "UTF-8");
		}
		else {
			try {
				outFile = File.createTempFile("okp-fc_", ".tmp");
			}
			catch ( Throwable e ) {
				throw new OkapiIOException("Cannot create temporary output.", e);
			}
			outFile.deleteOnExit();
		}
		firstOutputCreated = true;
	}

	private void startPensieveOutput () {
		writer = new PensieveFilterWriter();
		writer.setOutput(params.getOutputPath());
		writer.setOptions(targetLanguage, "UTF-8");
	}

}
