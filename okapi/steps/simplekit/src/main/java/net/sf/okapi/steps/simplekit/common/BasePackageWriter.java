/*===========================================================================
  Copyright (C) 2010-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.simplekit.common;

import java.io.OutputStream;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.steps.simplekit.creation.Parameters;

public abstract class BasePackageWriter implements IPackageWriter {

	protected Parameters params;
	protected Manifest manifest;
	protected int docId;
	protected String inputPath;
	protected String filterConfigId;
	protected String outputPath;
	protected String formatType;
	
	public BasePackageWriter (String formatType) {
		this.formatType = formatType;
		manifest = new Manifest();
	}
	
	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}
	
	@Override
	public void setBatchInformation (String packageRoot,
		LocaleId srcLoc,
		LocaleId trgLoc,
		String inputRoot)
	{
		manifest.setInformation(packageRoot, srcLoc, trgLoc, inputRoot);
	}

	public void setDocumentInformation (String inputPath,
		String filterConfigId,
		String outputPath)
	{
		this.inputPath = inputPath;
		this.filterConfigId = filterConfigId;
		this.outputPath = outputPath;
	}
	
	@Override
	public void cancel () {
		// TODO
	}

	@Override
	public void close () {
		// Do nothing by default
	}

	@Override
	public EncoderManager getEncoderManager () {
		// Not used
		return null;
	}

	@Override
	public Event handleEvent (Event event) {
		switch ( event.getEventType() ) {
		case START_BATCH:
			processStartBatch();
			break;
		case END_BATCH:
			processEndBatch();
			break;
		case START_BATCH_ITEM:
			processStartBatchItem();
			break;
		case END_BATCH_ITEM:
			processEndBatchItem();
			break;
		case START_DOCUMENT:
			processStartDocument(event);
			break;
		case END_DOCUMENT:
			processEndDocument(event);
			break;
		case START_SUBDOCUMENT:
			processStartSubDocument(event);
			break;
		case END_SUBDOCUMENT:
			processEndSubDocument(event);
			break;
		case START_GROUP:
			processStartGroup(event);
			break;
		case END_GROUP:
			processEndGroup(event);
			break;
		case TEXT_UNIT:
			processTextUnit(event);
			break;
		}
		// This writer is not supposed to change the event, so we return the same
		return event;
	}

	@Override
	public void setOptions (LocaleId locale,
		String defaultEncoding)
	{
		throw new UnsupportedOperationException("Use setDocumentInformation instead.");
	}

	@Override
	public void setOutput (String path) {
		throw new UnsupportedOperationException("Use setDocumentInformation instead.");
	}

	@Override
	public void setOutput (OutputStream output) {
		throw new UnsupportedOperationException("Output to stream not supported for now");
	}

	protected void processStartBatch () {
		docId = 0;
	}

	protected void processEndBatch () {
		// Do nothing by default
	}

	protected void processStartBatchItem () {
		// Do nothing by default
	}

	protected void processEndBatchItem () {
		if ( params.getOutputManifest() ) {
			manifest.Save();
		}
	}

	protected void processStartDocument (Event event) {
		StartDocument sd = event.getStartDocument();
		String relativeInput = inputPath.substring(manifest.getInputRoot().length());
//TODO: Make the relative output based on an output root, not input 
//		String relativeOutput = outputPath.substring(manifest.getInputRoot().length());
		
		String res[] = FilterConfigurationMapper.splitFilterFromConfiguration(filterConfigId);
		
		manifest.addDocument(++docId, relativeInput, relativeInput,
			sd.getEncoding(), res[0], formatType);
	}

	protected void processEndDocument (Event event) {
		// Do nothing by default
	}

	protected void processStartSubDocument (Event event) {
		// Do nothing by default
	}

	protected void processEndSubDocument (Event event) {
		// Do nothing by default
	}

	protected void processStartGroup (Event event) {
		// Do nothing by default
	}

	protected void processEndGroup (Event event) {
		// Do nothing by default
	}

	protected abstract void processTextUnit (Event event);

}
