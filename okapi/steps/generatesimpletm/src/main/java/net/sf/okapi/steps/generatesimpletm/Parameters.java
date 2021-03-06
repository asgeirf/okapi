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

package net.sf.okapi.steps.generatesimpletm;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.ParametersDescription;

public class Parameters extends BaseParameters {
	
	private String tmPath;
	
	public Parameters () {
		reset();
	}
	
	public String getTmPath () {
		return tmPath;
	}

	public void setTmPath (String tmPath) {
		this.tmPath = tmPath;
	}

	@Override
	public void reset() {
		tmPath = "";
	}

	@Override
	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		tmPath = buffer.getString("tmPath", tmPath);
	}

	@Override
	public String toString() {
		buffer.reset();
		buffer.setString("tmPath", tmPath);
		return buffer.toString();
	}
	
	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add("tmPath", "Path of the TM", "Full path of the TM to generate.");
		return desc;
	}
	
}
