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

package net.sf.okapi.steps.diffleverage;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.TextInputPart;

public class Parameters extends BaseParameters implements IEditorDescriptionProvider {
	private int fuzzyThreshold;
	
	@Override
	public void fromString(String data) {
		reset();
		buffer.fromString(data);
		fuzzyThreshold = buffer.getInteger("fuzzyThreshold", fuzzyThreshold);
	}

	@Override
	public void reset() {	
		fuzzyThreshold = 100;
	}
	
	@Override
	public String toString() {
		buffer.reset();
		buffer.setParameter("fuzzyThreshold", fuzzyThreshold);		
		return buffer.toString();
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add("fuzzyThreshold",	"Fuzy Threshold", "Fuzzy Threshold between 0 and 100");		
		return desc;
	}
	
	@Override
	public EditorDescription createEditorDescription(ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("Diff Leverager", true, false);	
		TextInputPart tp = desc.addTextInputPart(paramsDesc.get("fuzzyThreshold"));
		tp.setRange(0, 100);
		return desc;
	}
}
