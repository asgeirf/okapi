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

package net.sf.okapi.steps.rainbowkit.postprocess;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

@EditorFor(Parameters.class)
public class Parameters extends BaseParameters implements IEditorDescriptionProvider {

	static final String PRESERVESEGMENTATION = "preserveSegmentation"; //$NON-NLS-1$
	
	private boolean preserveSegmentation;

	public Parameters () {
		reset();
	}
	
	@Override
	public void reset () {
		// Most of the times, this is the last step of the pipeline
		// so preserving the segmentation is not needed
		preserveSegmentation = false;
	}

	@Override
	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		preserveSegmentation = buffer.getBoolean(PRESERVESEGMENTATION, preserveSegmentation);
	}

	@Override
	public String toString () {
		buffer.reset();
		buffer.setParameter(PRESERVESEGMENTATION, preserveSegmentation);
		return buffer.toString();
	}

	public boolean getPreserveSegmentation () {
		return preserveSegmentation;
	}

	public void setPreserveSegmentation (boolean preserveSegmentation) {
		this.preserveSegmentation = preserveSegmentation;
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(PRESERVESEGMENTATION, "Preserve the segmentation for the next steps", null);
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription paramDesc) {
		EditorDescription desc = new EditorDescription(MergingStep.NAME, true, false);
		desc.addCheckboxPart(paramDesc.get(PRESERVESEGMENTATION));
		return desc;
	}

}
