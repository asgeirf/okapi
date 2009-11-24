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
============================================================================*/

package net.sf.okapi.filters.ttx;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

public class Parameters extends BaseParameters implements IEditorDescriptionProvider {

	protected static final String FORCESEGMENTS = "forceSegments";
	
	private boolean forceSegments;

	public Parameters () {
		reset();
		toString(); // fill the list
	}
	
	public boolean getForceSegments () {
		return forceSegments;
	}

	public void setForcesegments (boolean forceSegments) {
		this.forceSegments = forceSegments;
	}

	public void reset () {
		forceSegments = false;
	}

	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		forceSegments = buffer.getBoolean(FORCESEGMENTS, forceSegments);
	}

	@Override
	public String toString () {
		buffer.reset();
		buffer.setBoolean(FORCESEGMENTS, forceSegments);
		return buffer.toString();
	}
	
	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(FORCESEGMENTS, "Force un-segmented entries to be output as a segment", null);
		return desc;
	}

	public EditorDescription createEditorDescription (ParametersDescription paramDesc) {
		EditorDescription desc = new EditorDescription("TTX Filter Parameters", true, false);
		desc.addCheckboxPart(paramDesc.get("escapeGT"));
		return desc;
	}

}
