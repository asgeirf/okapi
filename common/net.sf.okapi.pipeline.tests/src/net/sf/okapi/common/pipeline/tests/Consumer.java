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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.FileResource;

public class Consumer extends BasePipelineStep {
	public String getName() {
		return "Consumer";
	}

	@Override
	public void postprocess() {
		System.out.println(getName() + " postprocess");
	}

	@Override
	public void preprocess() {
		System.out.println(getName() + " preprocess");
	}
	
	@Override
	protected void handleTextUnit(Event event) {
		System.out.println("EventType: " + event.getEventType().name());
	}
	
	@Override
	protected void handleFileResource(Event event) {
		System.out.println("EventType: " + event.getEventType().name());
	/*	Reader r = ((FileResource)event.getResource()).getReader();	
		StringWriter out = new StringWriter();
		try {
			while(true) {
				char c = (char)r.read();
				if (c == -1) {
					break;
				}
				out.append(c);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("Resource is: " + out.toString());		*/
	}
}
