/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xini;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.ParametersDescription;

public class Parameters extends BaseParameters {
	/**
	 * Name of the parameter that enables/disables output segmentation
	 */
	public static final String USE_OKAPI_SEGMENTATION = "useOkapiSegmentation";
	
	private boolean useOkapiSegmentation;

	public Parameters () {
		reset();
	}
	
	public void reset () {
		useOkapiSegmentation = true;
	}

	@Override
	public String toString () {
		buffer.reset();
		buffer.setBoolean(USE_OKAPI_SEGMENTATION, useOkapiSegmentation);
		return buffer.toString();
	}
	
	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		useOkapiSegmentation = buffer.getBoolean(USE_OKAPI_SEGMENTATION, useOkapiSegmentation);
	}

	public boolean isUseOkapiSegmentation() {
		return useOkapiSegmentation;
	}

	public void setUseOkapiSegmentation(boolean useOkapiSegmentation) {
		this.useOkapiSegmentation = useOkapiSegmentation;
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(
				USE_OKAPI_SEGMENTATION, 
				"Use Okapi segmentation for output", 
				"If disabled, all XINI segments with the same value\n" +
				"of the attribute 'SegmentIDBeforeSegmentation' will be merged.\n" +
				"If the XINI was not segmented, it will remain unsegmented.\n" +
				"If this option is enabled, new segmentation\n" +
				"(i.e. from segmentation step) will be used for the output.");
		return desc;
	}
}
