/*===========================================================================
  Copyright (C) 2008 by the Okapi Framework contributors
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

package net.sf.okapi.steps.xmlvalidation;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.ListSelectionPart;
import net.sf.okapi.common.uidescription.PathInputPart;


@EditorFor(Parameters.class)
public class Parameters extends BaseParameters implements IEditorDescriptionProvider{

	public static final int VALIDATIONTYPE_DTD = 0;
	public static final int VALIDATIONTYPE_SCHEMA = 1;
	
	private static String VALIDATE = "validate";
	private static String SCHEMAPATH = "schemaPath";
	private static final String VALIDATIONTYPE = "validationType";

	private boolean validate;	
	private String schemaPath;
	private int validationType;
	
	public Parameters () {
		reset();
	}
	
	public boolean isValidate () {
		return validate;
	}

	public void setValidate (boolean validate) {
		this.validate = validate;
	}
	
	public String getSchemaPath () {
		return schemaPath;
	}

	public void setSchemaPath (String schemaPath) {
		this.schemaPath = schemaPath;
	}
	
	public int getValidationType () {
		return this.validationType;
	}
	
	public void setValidationType (int validationType) {
		this.validationType = validationType;
	}
	
	public void reset () {
		schemaPath = "";
		validate = false;
		validationType = 0;
	}

	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		validate = buffer.getBoolean(VALIDATE, validate);
		schemaPath = buffer.getString(SCHEMAPATH, schemaPath);
		validationType = buffer.getInteger(VALIDATIONTYPE, validationType);
	}

	@Override
	public String toString () {
		buffer.reset();
		buffer.setParameter(VALIDATE, validate);
		buffer.setParameter(SCHEMAPATH, schemaPath);
		buffer.setInteger(VALIDATIONTYPE, validationType);
		return buffer.toString();
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(VALIDATE,
				"Validate the files [Well-formedness is checked regardless]", null);
		
		desc.add(VALIDATIONTYPE, "Type of validation", "Indicates which validation to use");

		desc.add(SCHEMAPATH,
				"Path of the XML Schema", "Full path of the XML Schema.");
	
		return desc;
	}

	
	@Override
	public EditorDescription createEditorDescription(ParametersDescription paramDesc) {
		EditorDescription desc = new EditorDescription("XML Validation", true, false);

		desc.addCheckboxPart(paramDesc.get(VALIDATE));

		String[] values = {
				"DTD",
				"XML Schema",
			};
		ListSelectionPart lsp = desc.addListSelectionPart(paramDesc.get(VALIDATIONTYPE), values);
		
		PathInputPart part = desc.addPathInputPart(paramDesc.get(SCHEMAPATH), "Schema", false);
		part.setBrowseFilters("Schema Files (*.xsd)\tAll Files (*.*)", "*.xsd\t*.*");
		part.setAllowEmpty(true);
	
		return desc;
	}

}
