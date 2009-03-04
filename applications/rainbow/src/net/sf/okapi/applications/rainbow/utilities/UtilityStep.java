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

package net.sf.okapi.applications.rainbow.utilities;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.IPipelineStep;

public class UtilityStep implements IPipelineStep {

	private IFilterDrivenUtility utility;

	public UtilityStep (IFilterDrivenUtility utility) {
		this.utility = utility;
	}

	public Event handleEvent (Event event) {
		utility.handleEvent(event);
		return event;
	}
	
	public void cancel () {
		// Cancel needed here
	}

	public String getName () {
		return utility.getName();
	}

	public void pause () {
	}

	public void postprocess () {
		utility.postprocess();
	}

	public void preprocess () {
		utility.preprocess();
	}

	public void close () {
		//TODO: Figure out what needs to be done. Thought this was done in postprocess 
	}
}
