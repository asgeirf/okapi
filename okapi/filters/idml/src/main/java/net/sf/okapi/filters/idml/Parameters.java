/*===========================================================================
  Copyright (C) 2009-2012 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.SpinInputPart;

@EditorFor(Parameters.class)
public class Parameters extends BaseParameters implements IEditorDescriptionProvider {

	private static final String EXTRACTNOTES = "extractNotes";
	private static final String SIMPLIFYCODES = "simplifyCodes";
	private static final String EXTRACTMASTERSPREADS = "extractMasterSpreads";
	private static final String SKIPTHRESHOLD = "skipThreshold";

	private boolean extractNotes;
	private boolean simplifyCodes;
	private boolean extractMasterSpreads;
	private int skipThreshold;

	public Parameters () {
		reset();
		toString(); // fill the list
	}
	
	public void reset () {
		extractNotes = false;
		simplifyCodes = true;
		extractMasterSpreads = true;
		skipThreshold = 1000;
	}

	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		extractNotes = buffer.getBoolean(EXTRACTNOTES, extractNotes);
		simplifyCodes = buffer.getBoolean(SIMPLIFYCODES, simplifyCodes);
		extractMasterSpreads = buffer.getBoolean(EXTRACTMASTERSPREADS, extractMasterSpreads);
		skipThreshold = buffer.getInteger(SKIPTHRESHOLD, skipThreshold);
	}
	
	@Override
	public String toString () {
		buffer.reset();
		buffer.setBoolean(EXTRACTNOTES, extractNotes);
		buffer.setBoolean(SIMPLIFYCODES, simplifyCodes);
		buffer.setBoolean(EXTRACTMASTERSPREADS, extractMasterSpreads);
		buffer.setInteger(SKIPTHRESHOLD, skipThreshold);
		return buffer.toString();
	}

	public boolean getExtractNotes () {
		return extractNotes;
	}
	
	public void setExtractNotes (boolean extractNotes) {
		this.extractNotes = extractNotes;
	}

	public boolean getSimplifyCodes () {
		return simplifyCodes;
	}
	
	public void setSimplifyCodes (boolean simplifyCodes) {
		this.simplifyCodes = simplifyCodes;
	}

	public boolean getExtractMasterSpreads () {
		return extractMasterSpreads;
	}
	
	public void setExtractMasterSpreads (boolean extractMasterSpreads) {
		this.extractMasterSpreads = extractMasterSpreads;
	}
	
	public int getSkipThreshold () {
		return skipThreshold;
	}
	
	public void setSkipThreshold (int skipThreshold) {
		this.skipThreshold = skipThreshold;
	}

	@Override
	public ParametersDescription getParametersDescription() {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(EXTRACTNOTES, "Extract notes", null);
		desc.add(EXTRACTMASTERSPREADS, "Extract master spreads", null);
		desc.add(SIMPLIFYCODES, "Simplify inline codes when possible", null);
		desc.add(SKIPTHRESHOLD, "Maximum spread size", "Skip any spread larger than the given value (in Kbytes)");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription(ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("IDML Filter", true, false);
		
		desc.addCheckboxPart(paramsDesc.get(EXTRACTNOTES));
		desc.addCheckboxPart(paramsDesc.get(EXTRACTMASTERSPREADS));
		SpinInputPart sip = desc.addSpinInputPart(paramsDesc.get(SKIPTHRESHOLD));
		sip.setRange(1, 32000);
		desc.addSeparatorPart();
		desc.addCheckboxPart(paramsDesc.get(SIMPLIFYCODES));
		
		return desc;
	}

}
