/*===========================================================================*/
/* Copyright (C) 2008 by the Okapi Framework contributors                    */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.pipeline;

import java.util.LinkedList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.InputResource;

public class Pipeline implements IPipeline {
	LinkedList<IPipelineStep> steps;
	LinkedList<IPipelineStep> finishedSteps;
	volatile boolean cancel = false;
	private boolean done = false;
	private boolean destroyed = false;
	private Event event;

	public Pipeline() {
		steps = new LinkedList<IPipelineStep>();
		finishedSteps = new LinkedList<IPipelineStep>();
	}

	private void initialize() {
		// copy all the finished steps from previous run
		for (IPipelineStep step : finishedSteps) {
			steps.add(step);
		}
		finishedSteps.clear();

		cancel = false;
		done = false;
		destroyed = false;
	}

	public void addStep(IPipelineStep step) {
		if (destroyed) {
			throw new RuntimeException("Pipeline has been destroyed and must be reinitialized");
		}
		steps.add(step);
	}

	public void cancel() {
		cancel = true;
	}

	private Event execute(Event input) {

		// special processing for FINISHED event
		// Non-terminal steps will return FINISHED after receiving FINISHED
		// (simple pass through).
		// Terminal steps return an event which may be anything,
		// including a CUSTOM event. The pipeline returns this final event.
		if (input.getEventType() == EventType.FINISHED) {
			for (IPipelineStep step : steps) {
				event = step.handleEvent(event);
			}
			return event;
		}

		event = input;

		// loop through the events until we run out of steps or hit cancel
		while (!steps.isEmpty() && !cancel) {
			// cycle through the steps in order, pulling off steps that run out
			// of events.
			while (steps.getFirst().hasNext() && !cancel) {
				// go to each active step and call handleEvent
				// the event returned is used as input to the next pass
				for (IPipelineStep step : steps) {
					event = step.handleEvent(event);
				}
				// get ready for another pass down the pipeline
				// overwrite the terminal steps event
				event = Event.NOOP_EVENT;
			}
			// as each step exhausts its events remove it from the list and move
			// on to the next
			finishedSteps.add(steps.remove());
		}

		// FINSHED event was not sent we just return NOOP event
		return Event.NOOP_EVENT;
	}

	public PipelineReturnValue getState() {
		if (destroyed)
			return PipelineReturnValue.DESTROYED;
		else if (cancel)
			return PipelineReturnValue.CANCELLED;
		else if (done)
			return PipelineReturnValue.SUCCEDED;
		else
			return PipelineReturnValue.RUNNING;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipeline#process(FileResource)
	 */
	public Event process(InputResource input) {
		return process(new Event(EventType.FILE_RESOURCE, input));
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipeline#process(Event)
	 */
	public Event process(Event input) {
		// initialize the pipeline for a single run
		initialize();

		// preprocess is called on each step
		preprocess();

		// prime the pipeline with the input Event and run it to completion.
		Event e = execute(input);

		// copy any remaining steps into finishedSteps - makes initialization
		// process easier down the road if we use the pipeline again
		for (IPipelineStep step : steps) {
			finishedSteps.add(step);
		}
		steps.clear();

		// postprocess is always called on each step, even if the pipeline is
		// canceled.
		postprocess();

		done = true;

		// return the terminal event generated by the last step in the pipeline
		// after receiving the FINISHED event
		return e;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipeline#postprocess()
	 */
	private void postprocess() {
		for (IPipelineStep step : finishedSteps) {
			step.postprocess();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipeline#preprocess()
	 */
	private void preprocess() {
		// finishedSteps is empty - we preprocess on the steps waiting to be
		// processed.
		for (IPipelineStep step : steps) {
			step.preprocess();
		}
	}

	/*
	 * Destroy this pipeline and call destroy on each pipeline step. Cleanup
	 * code should go here.
	 */
	public void destroy() {
		for (IPipelineStep step : finishedSteps) {
			step.destroy();
		}
		destroyed = true;
	}

	public Event finishBatch() {		
		return process(Event.FINISHED_EVENT);
	}
}
