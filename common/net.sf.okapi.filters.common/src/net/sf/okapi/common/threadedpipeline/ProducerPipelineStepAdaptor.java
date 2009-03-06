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

package net.sf.okapi.common.threadedpipeline;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.BlockingQueue;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.MemMappedCharSequence;
import net.sf.okapi.common.pipeline.IInitialStep;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.pipeline.PipelineReturnValue;

public class ProducerPipelineStepAdaptor extends BaseThreadedPipelineStepAdaptor implements IProducer, IInitialStep {	
	private BlockingQueue<Event> producerQueue;
	
	public ProducerPipelineStepAdaptor(IPipelineStep step) {
		super(step);		
	}
	
	public void setProducerQueue(BlockingQueue<Event> producerQueue) {
		this.producerQueue = producerQueue;
	}
	
	protected void addToQueue(Event event) {
		if (producerQueue == null) {
			throw new RuntimeException("This class is a consumer not a producer");
		}
		try {
			producerQueue.put(event);
		} catch (InterruptedException e) {
			throw new RuntimeInterruptedException(e);
		}
	}
	
	public Event handleEvent(Event event) {			
		Event e = step.handleEvent(event);
		addToQueue(e);
		return e;		
	}
	
	@Override
	protected PipelineReturnValue processBlockingQueue() {
		Event event = handleEvent(null);
		if (event == null) {
			return PipelineReturnValue.SUCCEDED;
		}
		return PipelineReturnValue.RUNNING;
	}

	/* (non-Javadoc)
	 * @see net.sf.okapi.common.pipeline.IInitialStep#hasNext()
	 */
	public boolean hasNext() {
		return ((IInitialStep)step).hasNext();
	}

	/* (non-Javadoc)
	 * @see net.sf.okapi.common.pipeline.IInitialStep#setInput(java.net.URI)
	 */
	public void setInput(URI input) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.okapi.common.pipeline.IInitialStep#setInput(java.io.InputStream)
	 */
	public void setInput(InputStream input) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.okapi.common.pipeline.IInitialStep#setInput(net.sf.okapi.common.MemMappedCharSequence)
	 */
	public void setInput(MemMappedCharSequence input) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.sf.okapi.common.pipeline.IInitialStep#setInput(java.lang.CharSequence)
	 */
	public void setInput(CharSequence input) {
		// TODO Auto-generated method stub
		
	}
}
