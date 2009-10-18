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

import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.tm.simpletm.Database;

public class GenerateSimpleTmStep extends BasePipelineStep {

	protected final Logger logger = Logger.getLogger(getClass().getName());

	private Database simpleTm = null;
	private Parameters params;
	private LocaleId targetLocale;
	private String fileName;
	private int countIsNotTranslatable;
	private int countTuNotAdded;
	private int countTusAdded;
	private int countSegsAdded;
	private boolean isMultilingual;

	public GenerateSimpleTmStep () {
		params = new Parameters();
	}
	
	@StepParameterMapping(parameterType = StepParameterType.TARGET_LANGUAGE)
	public void setTargetLocale (LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}
	
	public String getName () {
		return "Generate SimpleTM";
	}

	public String getDescription () {
		return "Generates a SimpleTM translation memory from multilingual input files.";
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	protected void handleStartBatch (Event event) {
		if ( Util.isEmpty(params.getTmPath()) ) {
			throw new OkapiBadStepInputException("Please provide a valid path and name for the TM.");
		}
	}
	
	@Override
	protected void handleStartBatchItem (Event event) {
		if(simpleTm == null){
			simpleTm = new Database();
			simpleTm.create(params.getTmPath(), true, targetLocale);
		}
	}
	
	@Override
	protected void handleEndBatchItem (Event event) {
		logger.info(String.format("\nSIMPLE TM GENERATION FILE: %s", fileName ));
		logger.info(String.format("Untranslatable text units = %d",countIsNotTranslatable));
		logger.info(String.format("Translatable text units but failed to add = %d", countTuNotAdded));
		logger.info(String.format("Text units added = %d", countTusAdded));
		logger.info(String.format("Segments added = %d",countSegsAdded));
	}

	@Override
	protected void handleEndBatch (Event event) {
		logger.info(String.format("\nSIMPLE TM GENERATION: "));
		logger.info(String.format("Total untranslatable text units = %d",countIsNotTranslatable));
		logger.info(String.format("Total text units (Translatable) that failed to add = %d", countTuNotAdded));
		logger.info(String.format("Total text units added = %d", countTusAdded));
		logger.info(String.format("Total segments added = %d",countSegsAdded));
		logger.info(String.format("Total entries in generated simpleTm = %d",simpleTm.getEntryCount()));
		simpleTm.close();
	}

	@Override
	protected void handleStartDocument (Event event) {
		StartDocument sd = (StartDocument)event.getResource();
		fileName = Util.getFilename(sd.getName(), true);
		isMultilingual = sd.isMultilingual();
		if(!isMultilingual){
			logger.warning("File "+fileName+ " is not processed as a multiLingual file and cannot be used to populate the SimpleTm.");
		} 
	}
	
	@Override
	protected void handleTextUnit (Event event) {
		
		//--skip file if not multilingual.
		if(!isMultilingual){
			countTuNotAdded++;
			return;
		} 
		
		TextUnit tu = (TextUnit)event.getResource();
		// Skip non-translatable
		if ( !tu.isTranslatable() ){
			countIsNotTranslatable++;
			return;
		} 
		
		if(tu.getSource()==null){
			logger.warning("TextUnit is missing source content.");
			countTuNotAdded++;
			return;
		}
		
		if( !tu.hasTarget(targetLocale) || ( tu.getTarget(targetLocale)==null )){
			logger.warning(String.format("TextUnit is missing '%s' target.", targetLocale));
			countTuNotAdded++;
			return;
		}
		
		// Check if the attributes for GroupName and FileName are available from the input
		Property propGName = tu.getProperty(Database.NGRPNAME);
		if ( propGName == null ) propGName = tu.getProperty("Txt::"+Database.NGRPNAME);
		Property propFName = tu.getProperty(Database.NFILENAME);
		if ( propFName == null ) propFName = tu.getProperty("Txt::"+Database.NFILENAME);

		// Add the entry
		int added = simpleTm.addEntry(tu,
			(propGName==null) ? tu.getName() : propGName.getValue(),
			(propFName==null) ? fileName : propFName.getValue());

		if ( added==0 ) {
			countTuNotAdded++;
		}
		else if( added>0 ) {
			countTusAdded++;
			countSegsAdded+=added;
		}
	}
}
