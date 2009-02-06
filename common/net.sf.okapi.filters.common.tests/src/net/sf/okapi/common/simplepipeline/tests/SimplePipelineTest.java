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

package net.sf.okapi.common.simplepipeline.tests;

import net.sf.okapi.common.eventpipeline.EventPipeline;
import net.sf.okapi.common.eventpipeline.IEventPipeline;
import net.sf.okapi.common.pipeline.tests.Consumer;
import net.sf.okapi.common.pipeline.tests.ConsumerProducer;
import net.sf.okapi.common.pipeline.tests.Producer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimplePipelineTest {

	@Before
	public void setUp() {

	}
	
	@Test
	public void runPipeline() {
		IEventPipeline pipeline = new EventPipeline();
		pipeline.addStep(new Producer());
		pipeline.addStep(new ConsumerProducer());
		pipeline.addStep(new Consumer());

		System.out.println("START PIPELINE");
		pipeline.execute();		
	}

	@After
	public void cleanUp() {
		System.out.println("CLEANUP PIPELINE");
	}

}
