/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.copysourcetotarget;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.TextUnit;

@UsingParameters(Parameters.class)
public class CopySourceToTargetStep extends BasePipelineStep {

	private Parameters params;
	private LocaleId targetLocale;

	public CopySourceToTargetStep() {
		params = new Parameters();
	}

	public String getDescription() {
		return "Copy the source segments to the specified target. Create the target if needed."
			+ " Expects: filter events. Sends back: filter events.";
	}

	public String getName() {
		return "Copy Source To Target";
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters) params;
	}

	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale(LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	@Override
	public Parameters getParameters() {
		return params;
	}

	@Override
	public Event handleTextUnit(Event event) {
		TextUnit tu = event.getTextUnit();

		// Initialize the copy options
		int copyOptions = IResource.CREATE_EMPTY;

		if (params.isCopyContent()) {
			copyOptions |= IResource.COPY_CONTENT;
		}
		if (params.isCopyProperties()) {
			copyOptions |= IResource.COPY_PROPERTIES;
		}

		tu.createTarget(targetLocale, params.isOverwriteExisting(), copyOptions);
		return event;
	}

}
