/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.common;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.skeleton.ResourceSimplifier;

/**
 * Converts events, i.e. splits the generic skeleton of a given event resource into parts to contain no references.
 * The skeleton parts are attached to newly created DOCUMENT_PART events.
 * Original references are converted either to skeleton parts or TEXT_UNIT events.
 * The sequence of DOCUMENT_PART and TEXT_UNIT events is packed into a single MULTI_EVENT event.
 */
@UsingParameters() // No parameters
public class ResourceSimplifierStep extends BasePipelineStep {
	
	private ResourceSimplifier simplifier;
	private LocaleId targetLocale;
	
	@Override
	public String getDescription() {
		return "Simplify events by converting references in generic skeleton parts. " +
				"Expects: filter events. Sends back: filter events.";
	}

	@Override
	public String getName() {
		return "Event Simplifier";
	}
	
	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale (LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	@Override
	public Event handleEvent(Event event) {
		switch (event.getEventType()) {
		case START_DOCUMENT:
			simplifier = new ResourceSimplifier(targetLocale);
		case END_DOCUMENT:
		case START_SUBDOCUMENT:
		case END_SUBDOCUMENT:
		case START_GROUP:
		case END_GROUP:
		case TEXT_UNIT:
		case DOCUMENT_PART:
			return simplifier.convert(event);
		default:
			return event;
		}
	}
}
