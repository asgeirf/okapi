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

package net.sf.okapi.common.pipeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.resource.RawDocument;

/**
 * Default implementation of the {@link IPipelineDriver} interface.
 */
public class PipelineDriver implements IPipelineDriver {
	
	private IPipeline pipeline;
	private List<IBatchItemContext> batchItems;
	
	/**
	 * Creates an new PipelineDriver object with an empty pipeline.
	 */
	public PipelineDriver () {
		pipeline = new Pipeline();
		batchItems = new ArrayList<IBatchItemContext>();
	}

	public void setPipeline (IPipeline pipeline) {
		this.pipeline = pipeline;
	}

	public IPipeline getPipeline () {
		return pipeline;
	}

	public int inputCountRequested () {
		return pipeline.inputCountRequested();
	}
	
	public boolean needsOutput (int inputIndex) {
		return pipeline.needsOutput(inputIndex);
	}
	
	public void addStep (IPipelineStep step) {
		pipeline.addStep(step);
	}

	public void processBatch (List<IBatchItemContext> batchItems) {
		this.batchItems = batchItems;
		processBatch();
	}
	
	public void processBatch () {
		pipeline.startBatch();
		for ( IBatchItemContext item : batchItems ) {
			pipeline.getContext().setBatchItemContext(item);
			pipeline.process(item.getRawDocument(0));
		}
		pipeline.endBatch();
	}

	public void addBatchItem (IBatchItemContext item) {
		batchItems.add(item);
	}

	public void addBatchItem (String filterConfig,
		RawDocument... rawDocs)
	{
		BatchItemContext item = new BatchItemContext();
		for ( RawDocument rawDoc : rawDocs ) {
			DocumentData ddi = new DocumentData();
			ddi.rawDocument = rawDoc;
			ddi.filterConfigId = filterConfig;
			item.add(ddi);
		}
		batchItems.add(item);
	}
	
	public void addBatchItem (RawDocument rawDoc,
		String filterConfig,
		URI outputURI,
		String outputEncoding)
	{
		DocumentData ddi = new DocumentData();
		ddi.rawDocument = rawDoc;
		ddi.filterConfigId = filterConfig;
		ddi.outputURI = outputURI;
		ddi.outputEncoding = outputEncoding;
		BatchItemContext item = new BatchItemContext();
		item.add(ddi);
		batchItems.add(item);
	}
	
	public void addBatchItem (URI inputURI,
		String defaultEncoding,
		String filterConfigId,
		String srcLang,
		String trgLang)
	{
		DocumentData ddi = new DocumentData();
		ddi.rawDocument = new RawDocument(inputURI, defaultEncoding, srcLang, trgLang);
		ddi.filterConfigId = filterConfigId;
		BatchItemContext item = new BatchItemContext();
		item.add(ddi);
		batchItems.add(item);
	}
	
	public void clearItems () {
		batchItems.clear();
	}
	
}
