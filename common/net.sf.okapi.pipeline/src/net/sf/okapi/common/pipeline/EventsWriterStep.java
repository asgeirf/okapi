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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filterwriter.IFilterWriter;

/**
 * Outputs filters events into a document.
 * This class implements the {@link IPipelineStep} interface for a step that takes 
 * filter events and creates an output document using a provided {@link IFilterWriter} 
 * implementation. Each event and its resource are passed on to the next step.
 * @see RawDocumentToEventsStep
 * @see EventsToRawDocumentStep 
 */
public class EventsWriterStep extends BasePipelineStep {

	private IFilterWriter filterWriter;

	/**
	 * Creates a new EventsWriterStep object.
	 * This constructor is needed to be able to instantiate an object from newInstance()
	 */
	public EventsWriterStep () {
	}
	
	public EventsWriterStep(IFilterWriter filterWriter) {
		this.filterWriter = filterWriter;
	}

	public IFilterWriter getFilterWriter() {
		return filterWriter;
	}
	
	/**
	 * Sets the filter writer for this EventsWriterStep object.
	 * @param filterWriter the filter writer to use.
	 */
	public void setFilterWriter (IFilterWriter filterWriter) {
		this.filterWriter = filterWriter;
	}

	public String getName() {
		return filterWriter.getName();
	}

	public String getDescription () {
		return "Writes out filter events into a document.";
	}
	
	@Override
	public Event handleEvent(Event event) {
		return filterWriter.handleEvent(event);
	}	
	
	@Override
	public void destroy() {
		filterWriter.close();
	}

	public boolean hasNext() {		
		return false;
	}	
}
