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

package net.sf.okapi.steps.formatconversion;

import net.sf.okapi.common.BaseParameters;

public class Parameters extends BaseParameters {

	public static final String FORMAT_TMX = "tmx";
	public static final String FORMAT_PO = "po";
	public static final String FORMAT_TABLE = "table";
	public static final String FORMAT_PENSIEVE = "pensieve";
	
	public static final int TRG_TARGETOREMPTY = 0; 
	public static final int TRG_FORCESOURCE = 1; 
	public static final int TRG_FORCEEMPTY = 2; 

	static final String SINGLEOUTPUT = "singleOutput";
	static final String OUTPUTPATH = "outputPath";
	static final String TARGETSTYLE = "targetStyle";
	static final String OUTPUTFORMAT = "outputFormat";
	static final String FORMATOPTIONS = "formatOptions";
	static final String USEGENERICCODES = "useGenericCodes";
	
	private boolean singleOutput;
	private String outputPath;
	private int targetStyle;
	private String outputFormat;
	private boolean useGenericCodes;
	private String formatOptions;
	
	public Parameters () {
		reset();
	}

	public int getTargetStyle () {
		return targetStyle;
	}

	public void setTargetStyle (int targetStyle) {
		this.targetStyle = targetStyle;
	}

	public boolean isSingleOutput () {
		return singleOutput;
	}

	public void setSingleOutput (boolean singleOutput) {
		this.singleOutput = singleOutput;
	}

	public String getOutputPath () {
		return outputPath;
	}

	public void setOutputPath (String outputPath) {
		this.outputPath = outputPath;
	}

	public String getOutputFormat () {
		return outputFormat;
	}

	public void setOutputFormat (String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public boolean getUseGenericCodes () {
		return useGenericCodes;
	}

	public String getFormatOptions () {
		return formatOptions;
	}

	public void setFormatOptions (String formatOptions) {
		this.formatOptions = formatOptions;
	}

	public void setUseGenericCodes (boolean useGenericCodes) {
		this.useGenericCodes = useGenericCodes;
	}

	public void reset () {
		singleOutput = true;
		targetStyle = TRG_TARGETOREMPTY;
		outputPath = "output";
		outputFormat = FORMAT_TMX;
		formatOptions = null;
		useGenericCodes = false;
	}

	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		singleOutput = buffer.getBoolean(SINGLEOUTPUT, singleOutput);
		targetStyle = buffer.getInteger(TARGETSTYLE, targetStyle);
		outputPath = buffer.getString(OUTPUTPATH, outputPath);
		outputFormat = buffer.getString(OUTPUTFORMAT, outputFormat);
		formatOptions = buffer.getGroup(FORMATOPTIONS, formatOptions);
		useGenericCodes = buffer.getBoolean(USEGENERICCODES, useGenericCodes);
	}

	public String toString() {
		buffer.reset();
		buffer.setBoolean(SINGLEOUTPUT, singleOutput);
		buffer.setInteger(TARGETSTYLE, targetStyle);
		buffer.setString(OUTPUTPATH, outputPath);
		buffer.setString(OUTPUTFORMAT, outputFormat);
		buffer.setGroup(FORMATOPTIONS, formatOptions);
		buffer.setBoolean(USEGENERICCODES, useGenericCodes);
		return buffer.toString();
	}

//	@Override
//	public ParametersDescription getParametersDescription () {
//		ParametersDescription desc = new ParametersDescription(this);
//		desc.add(SINGLEOUTPUT, "Create a single output document", null);
//		desc.add(OUTPUTPATH, "Output path", "Full path of the single output document to generate");
//		desc.add(OUTPUTFORMAT, "Output format", "Format to generate in output");
//		desc.add(USEGENERICCODES, "Output generic inline codes", null);
//		return desc;
//	}
//
//	public EditorDescription createEditorDescription(ParametersDescription paramDesc) {
//		EditorDescription desc = new EditorDescription("Format Conversion", true, false);
//
//		String[] choices = {FORMAT_PO, FORMAT_TMX, FORMAT_TABLE, FORMAT_PENSIEVE};
//		String[] choicesLabels = {"PO File", "TMX Document", "Tab-Delimited Table", "Pensieve TM"};
//		ListSelectionPart lsp = desc.addListSelectionPart(paramDesc.get(OUTPUTFORMAT), choices);
//		lsp.setChoicesLabels(choicesLabels);
//		
//		desc.addCheckboxPart(paramDesc.get(USEGENERICCODES));
//
//		CheckboxPart cbp = desc.addCheckboxPart(paramDesc.get(SINGLEOUTPUT));
//		PathInputPart pip = desc.addPathInputPart(paramDesc.get(OUTPUTPATH), "Output File", true);
//		pip.setMasterPart(cbp, true);
//		
//		return desc;
//	}

}
