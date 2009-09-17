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

package net.sf.okapi.common.pipeline;

import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IContext;
import net.sf.okapi.common.resource.RawDocument;

/**
 * Common set of methods to drive an event-driven process. A pipeline is made of
 * a chain of {@link IPipelineStep} objects through which documents are
 * processed.
 */
public interface IPipeline {

	/**
	 * Starts {@link IPipeline} processing with a {@link RawDocument} as input.
	 * This is a convenience method that calls {@link #process(Event)}.
	 * 
	 * @param input
	 *            the RawDocument to process.
	 */
	public Event process(RawDocument input);

	/**
	 * Starts {@link IPipeline} processing with a {@link Event} as input.
	 * 
	 * @param input
	 *            event that primes the {@link IPipeline}
	 */
	public Event process(Event input);

	/**
	 * Gets the current pipeline state.
	 * 
	 * @return the current state of the pipeline.
	 */
	public PipelineReturnValue getState();

	/**
	 * Cancels processing on this pipeline.
	 */
	public void cancel();

	/**
	 * Adds a step to this pipeline. Steps are executed in the order they are
	 * added.
	 * 
	 * @param step
	 *            the step to add.
	 */
	public void addStep(IPipelineStep step);

	/**
	 * Gets the list of all steps in this pipeline.
	 * 
	 * @return a list of all steps in this pipeline, the list may be empty.
	 */
	public List<IPipelineStep> getSteps();

	/**
	 * Gets the current {@link IContext} for this pipeline.
	 * 
	 * @return the current {@link IContext} for this pipeline.
	 */
	@Deprecated
	public IContext getContext();

	/**
	 * Sets the {@link IContext} for this pipeline.
	 * 
	 * @param context
	 *            the new {@link IContext} for this pipeline.
	 */
	@Deprecated
	public void setContext(IContext context);

	/**
	 * Starts a batch of inputs.
	 */
	public void startBatch();

	/**
	 * Finishes a batch of inputs and return the final {@link Event}
	 * 
	 */
	public void endBatch();

	/**
	 * Indicates the highest number of inputs that was requested by any of the
	 * steps
	 * in this pipeline.
	 * 
	 * @return highest number of input requested by this pipeline.
	 */
	@Deprecated
	public int inputCountRequested();

	/**
	 * Indicates if an output is needed for a given input.
	 * 
	 * @param inputIndex
	 *            the index of the input to query. Use 0 for the main input.
	 * @return true if an output is needed for the given input.
	 */
	@Deprecated
	public boolean needsOutput(int inputIndex);

	/**
	 * Frees all resources from all steps in this pipeline.
	 */
	public void destroy();

	/**
	 * Remove all the {@link IPipelineStep}s from the pipeline. Also calls the
	 * destroy() method on each step.
	 */
	public void clearSteps();
}
