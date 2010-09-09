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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.observer.BaseObservable;
import net.sf.okapi.common.observer.IObservable;
import net.sf.okapi.common.observer.IObserver;
import net.sf.okapi.common.resource.MultiEvent;
import net.sf.okapi.common.resource.RawDocument;

/**
 * Default implementations of the {@link IPipeline} interface.
 */
public class Pipeline implements IPipeline, IObservable, IObserver {
	public static final String DEFAULT_ID = "DEFAULT ID";

	private LinkedList<IPipelineStep> steps;
	private LinkedList<IPipelineStep> finishedSteps;
	private volatile PipelineReturnValue state;
	private String id;

	/**
	 * Creates a new Pipeline object.
	 */
	public Pipeline() {
		steps = new LinkedList<IPipelineStep>();
		finishedSteps = new LinkedList<IPipelineStep>();
		state = PipelineReturnValue.PAUSED;
		id = DEFAULT_ID;
	}

	private void initialize() {
		// Copy all the finished steps from previous run
		for (IPipelineStep step : finishedSteps) {
			steps.add(step);
		}
		finishedSteps.clear();
	}

	@Override
	public void startBatch() {
		state = PipelineReturnValue.RUNNING;

		initialize();

		Event event = new Event(EventType.START_BATCH);
		for (IPipelineStep step : steps) {
			step.handleEvent(event);
		}
		notifyObservers(event);
	}

	@Override
	public void endBatch() {
		// Non-terminal steps will return END_BATCH after receiving END_BATCH
		// Terminal steps return an event which may be anything,
		// including a CUSTOM event. The pipeline returns this final event.
		// We run this on finishedSteps since steps is empty by the time we get
		// here
		Event event = Event.END_BATCH_EVENT;
		for (IPipelineStep step : finishedSteps) {
			step.handleEvent(Event.END_BATCH_EVENT);
		}
		notifyObservers(event);

		state = PipelineReturnValue.SUCCEDED;
	}

	@Override
	public void addStep(IPipelineStep step) {
		steps.add(step);
	}

	@Override
	public List<IPipelineStep> getSteps() {
		return new ArrayList<IPipelineStep>(steps);
	}

	@Override
	public void cancel() {
		state = PipelineReturnValue.CANCELLED;
	}

	private Event execute(Event event) {
		boolean notifiedObserver = false;
		state = PipelineReturnValue.RUNNING;

		// loop through the events until we run out of steps or hit cancel
		while (!steps.isEmpty() && !(state == PipelineReturnValue.CANCELLED)) {
			// cycle through the steps in order, pulling off steps that run out
			// of events.
			do {
				// go to each active step and call handleEvent
				// the event returned is used as input to the next pass
				notifiedObserver = false;
				for (IPipelineStep step : steps) {
					event = step.handleEvent(event);
					// We send each of the events in MULTI_EVENT down the pipeline before
					// processing any other events but only if the event is configured for multi-event propagation
					if (event.getEventType() == EventType.MULTI_EVENT
							&& !(((MultiEvent) event.getResource()).isPropagateAsSingleEvent())) {
						// add the remaining steps to a temp list - these are the steps that will receive the expanded
						// MULT_EVENTS
						List<IPipelineStep> remainingSteps = steps.subList(steps.indexOf(step) + 1,
								steps.size());
						for (Event e : ((MultiEvent) event.getResource())) {
							event = e;
							// send the current event from MULTI_EVENT down the remaining steps in the pipeline
							for (IPipelineStep remainingStep : remainingSteps) {
								event = remainingStep.handleEvent(event);
							}
							// notify observers that the final step has sent an Event
							// always filter out NO_OP events
							if (!event.isNoop()) {
								notifyObservers(event);
							}
							notifiedObserver = true;
						}
						
						// the previous event has already been sent to all steps
						// start the next cycle with a NO_OP event
						event = Event.NOOP_EVENT;
						break;
					}
				}

				// notify observers that the final step has sent an Event
				if (!notifiedObserver && !event.isNoop()) {					
					notifyObservers(event);
				}
			} while (!steps.getFirst().isDone() && !(state == PipelineReturnValue.CANCELLED));
			// As each step exhausts its events remove it from the list and move
			// on to the next
			try {
				while (steps.getFirst().isDone()) {
					finishedSteps.add(steps.remove());
				}
			} catch (NoSuchElementException e) {	
				// ignore
			}
		}

		return event;
	}

	@Override
	public PipelineReturnValue getState() {
		return state;
	}

	@Override
	public Event process(RawDocument input) {
		return process(new Event(EventType.RAW_DOCUMENT, input));
	}

	@Override
	public Event process(Event input) {
		state = PipelineReturnValue.RUNNING;
		initialize();

		// Pre-process for this batch-item
		Event e = new Event(EventType.START_BATCH_ITEM);
		for (IPipelineStep step : steps) {
			step.handleEvent(e);
		}
		notifyObservers(e);

		// Prime the pipeline with the input Event and run it to completion.
		Event finalEvent = execute(input);

		// Copy any remaining steps into finishedSteps - makes initialization
		// process easier down the road if we use the pipeline again
		for (IPipelineStep step : steps) {
			finishedSteps.add(step);
		}
		steps.clear();

		// Post-process for this batch-item
		e = new Event(EventType.END_BATCH_ITEM);
		for (IPipelineStep step : finishedSteps) {
			step.handleEvent(e);
		}
		notifyObservers(e);

		return finalEvent;
	}

	@Override
	public void destroy() {
		for (IPipelineStep step : finishedSteps) {
			step.destroy();
		}
		state = PipelineReturnValue.DESTROYED;
	}

	@Override
	public void clearSteps() {
		destroy();
		steps.clear();
		finishedSteps.clear();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	//
	// implements IObserver interface
	//

	public void update(IObservable o, Object arg) {
		notifyObservers();
	}

	//
	// implements IObservable interface
	//

	/**
	 * Implements multiple inheritance via delegate pattern to an inner class
	 * 
	 * @see IObservable
	 * @see BaseObservable
	 */
	private IObservable delegatedObservable = new BaseObservable(this);

	public void addObserver(IObserver observer) {
		delegatedObservable.addObserver(observer);
	}

	public int countObservers() {
		return delegatedObservable.countObservers();
	}

	public void deleteObserver(IObserver observer) {
		delegatedObservable.deleteObserver(observer);
	}

	public void notifyObservers() {
		delegatedObservable.notifyObservers();
	}

	public void notifyObservers(Object arg) {
		delegatedObservable.notifyObservers(arg);
	}

	public void deleteObservers() {
		delegatedObservable.deleteObservers();
	}

	public List<IObserver> getObservers() {
		return delegatedObservable.getObservers();
	}
}
