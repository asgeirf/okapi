/*===========================================================================*/
/* Copyright (C) 2008 Jim Hargrave                                           */
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
/* Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA              */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.pipeline;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;

public class ConsumerProducer extends BasePipelineStep {
	
	private static final Logger LOGGER = Logger.getLogger(ConsumerProducer.class.getName());
	
	public String getName() {
		return "ProducerConsumer";
	}

	public String getDescription() {
		return "Description";
	}

	@Override
	protected Event handleEndBatchItem (Event event) {		
		LOGGER.log(Level.FINEST, getName() + " end-batch-item");
		return event;
	}

	@Override
	protected Event handleStartBatchItem (Event event) {		
		LOGGER.log(Level.FINEST, getName() + " start-batch-item");
		return event;
	}
	
	@Override
	protected Event handleTextUnit(Event event) {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {}
		return event;
	}

}
