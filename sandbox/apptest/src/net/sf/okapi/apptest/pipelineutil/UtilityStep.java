package net.sf.okapi.apptest.pipelineutil;

import java.util.concurrent.BlockingQueue;

import net.sf.okapi.apptest.filters.FilterEvent;
import net.sf.okapi.apptest.filters.FilterEvent.FilterEventType;
import net.sf.okapi.apptest.pipeline.BasePipelineStep;
import net.sf.okapi.apptest.pipeline.IConsumer;
import net.sf.okapi.apptest.pipeline.IProducer;
import net.sf.okapi.apptest.pipeline.PipelineReturnValue;
import net.sf.okapi.apptest.utilities.IUtility;

public class UtilityStep extends BasePipelineStep implements IConsumer<FilterEvent>, IProducer<FilterEvent> {

	private BlockingQueue<FilterEvent> producerQueue;
	private BlockingQueue<FilterEvent> consumerQueue;
	private IUtility utility;

	public void setUtility (IUtility utility) {
		this.utility = utility;
	}
	
	public void setConsumerQueue (BlockingQueue<FilterEvent> consumerQueue) {
		this.consumerQueue = consumerQueue;
	}

	public void setProducerQueue (BlockingQueue<FilterEvent> producerQueue) {
		this.producerQueue = producerQueue;
	}

	public void finish () throws InterruptedException {
		if ( utility != null ) utility.doEpilog();
	}

	public String getName() {
		return utility.getName();
	}

	public void initialize () throws InterruptedException {
	}

	public PipelineReturnValue process() throws InterruptedException {
		// Get the event from the queue
		FilterEvent event = consumerQueue.take();
		// Feed it to the utility
		utility.handleEvent(event);
		// Pass it to the next step
		producerQueue.put(event);
		// End the process if it's the end of the document
		if ( event.getEventType() == FilterEventType.END_DOCUMENT ) {
			return PipelineReturnValue.SUCCEDED;
		}
		return PipelineReturnValue.RUNNING;
	}

}
