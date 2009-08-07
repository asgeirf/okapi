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

package net.sf.okapi.common.uidescription;

import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.okapi.common.ParameterDescriptor;

public class EditorDescription {

	private String caption;
	private boolean alignLabels;
	private LinkedHashMap<String, AbstractPart> descriptors;
	
	/**
	 * Creates a new EditorDescription object.
	 */
	public EditorDescription () {
		descriptors = new LinkedHashMap<String, AbstractPart>();
	}
	
	/**
	 * Creates a new EditorDescription object with a given caption.
	 * @param caption the caption of the editor.
	 */
	public EditorDescription (String caption) {
		descriptors = new LinkedHashMap<String, AbstractPart>();
		setCaption(caption);
	}
	
	/**
	 * Gets the caption for this editor.
	 * @return the caption for this editor.
	 */
	public String getCaption () {
		return caption;
	}

	/**
	 * Sets the caption for this editor.
	 * @param caption the caption for this editor.
	 */
	public void setCaption (String caption) {
		this.caption = caption;
	}
	
	/**
	 * Indicates if the labels for this editor should be aligned.
	 * @return true if the labels for this editor should be aligned, false otherwise.
	 */
	public boolean alignLabels () {
		return alignLabels;
	}

	/**
	 * Sets the flag indicating if the labels for this editor should be aligned.
	 * @param value true if the labels for this editor should be aligned, false otherwise.
	 */
	public void setAlignLabels (boolean value) {
		alignLabels = value;
	}

	/**
	 * Gets a map of the descriptor of all UI parts for this editor.
	 * @return a map of all descriptor of the UI parts.
	 */
	public Map<String, AbstractPart> getDescriptors () {
		return descriptors;
	}
	
	/**
	 * Gets the descriptor for a given UI part. 
	 * @param name the name of the UI part to lookup.
	 * @return the descriptor for the given UI part.
	 */
	public AbstractPart getDescriptor (String name) {
		return descriptors.get(name);
	}
	
	/**
	 * Adds a text input UI part to this editor description.
	 * @param paramDescriptor the parameter descriptor for this UI part.
	 * @param allowEmpty flag indicating if the text input can be empty.
	 * @param isPassword flag indicating if the text input should be treated as a password.
	 * @return the UI part created by this call.
	 */
	public TextInputPart addInputUIPart (ParameterDescriptor paramDescriptor,
		boolean allowEmpty,
		boolean isPassword)
	{
		TextInputPart desc = new TextInputPart(paramDescriptor, allowEmpty, isPassword);
		descriptors.put(desc.getName(), desc);
		return desc;
	}
	
	/**
	 * Adds a checkbox UI part to this editor description.
	 * @param paramDescriptor the parameter descriptor for this UI part.
	 * @return the UI part created by this call.
	 */
	public CheckboxPart addCheckboxUIPart (ParameterDescriptor paramDescriptor) {
		CheckboxPart desc = new CheckboxPart(paramDescriptor);
		descriptors.put(desc.getName(), desc);
		return desc;
	}

/*	public ListSelectionUIPart addListSelectionUIPart (ParameterDescriptor paramDescriptor,
		String[] choices)
	{
		ListSelectionUIPart desc = new ListSelectionUIPart(paramDescriptor, choices);
		descriptors.put(desc.getName(), desc);
		return desc;
	}

	public PathUIPart addPathUIPart (ParameterDescriptor paramDescriptor,
		String browseTitle,
		boolean saveAs)
	{
		PathUIPart desc = new PathUIPart(paramDescriptor, browseTitle, saveAs);
		descriptors.put(desc.getName(), desc);
		return desc;
	}
*/
}
