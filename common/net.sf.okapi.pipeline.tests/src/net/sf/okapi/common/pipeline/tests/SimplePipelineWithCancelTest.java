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

package net.sf.okapi.common.pipeline.tests;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.pipeline.IPipeline;
import net.sf.okapi.common.pipeline.PipelineReturnValue;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimplePipelineWithCancelTest {
	
	@Test
	public void runPipelineAndCancel() throws URISyntaxException, InterruptedException {
		final IPipeline pipeline = new Pipeline();
		
		Runnable runnable = new Runnable() {
			public void run() {
				pipeline.addStep(new Producer());
				pipeline.addStep(new ConsumerProducer());
				pipeline.addStep(new Consumer());				
				try {
					pipeline.process(new URI("DUMMY"));
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}				
				pipeline.destroy();
			}
		};

		ExecutorService e = Executors.newSingleThreadExecutor();
		e.execute(runnable);
		Thread.sleep(2000);
		pipeline.cancel();
		assertEquals(PipelineReturnValue.CANCELLED, pipeline.getState());
		pipeline.destroy();
		assertEquals(PipelineReturnValue.DESTROYED, pipeline.getState());
	}
}
