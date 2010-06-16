/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.BaseParameters;

public class Parameters extends BaseParameters {
	
	private static final String OUTPUTPATH = "outputPath";
	private static final String AUTOOPEN = "autoOpen";
	private static final String LEADINGWS = "leadingWS";
	private static final String TRAILINGWS = "trailingWS";
	private static final String EMPTYTARGET = "emptyTarget";
	private static final String TARGETSAMEASSOURCE = "targetSameAsSource";
	private static final String TARGETSAMEASSOURCE_WITHCODES = "targetSameAsSourceWithCodes";
	private static final String CODEDIFFERENCE = "codeDifference";
	private static final String CHECKPATTERNS = "checkPatterns";
	private static final String PATTERNCOUNT = "patternCount";
	private static final String USEPATTERN = "usePattern";
	private static final String SOURCEPATTERN = "sourcePattern";
	private static final String TARGETPATTERN = "targetPattern";
	private static final String CHECKWITHLT = "checkWithLT";
	private static final String SERVERURL = "serverURL";

	String outputPath;
	boolean autoOpen;
	boolean leadingWS;
	boolean trailingWS;
	boolean emptyTarget;
	boolean targetSameAsSource;
	boolean targetSameAsSourceWithCodes;
	boolean codeDifference;
	boolean checkPatterns;
	List<PatternItem> patterns;
	boolean checkWithLT;
	String serverURL;

	public Parameters () {
		reset();
	}
	
	public String getOutputPath () {
		return outputPath;
	}

	public void setOutputPath (String outputPath) {
		this.outputPath = outputPath;
	}

	public boolean getAutoOpen () {
		return autoOpen;
	}

	public void setAutoOpen (boolean autoOpen) {
		this.autoOpen = autoOpen;
	}

	public boolean getLeadingWS () {
		return leadingWS;
	}

	public void setLeadingWS (boolean leadingWS) {
		this.leadingWS = leadingWS;
	}

	public boolean getTrailingWS () {
		return trailingWS;
	}

	public void setTrailingWS (boolean trailingWS) {
		this.trailingWS = trailingWS;
	}

	public boolean getEmptyTarget () {
		return emptyTarget;
	}

	public void setEmptyTarget (boolean emptyTarget) {
		this.emptyTarget = emptyTarget;
	}

	public boolean getTargetSameAsSource () {
		return targetSameAsSource;
	}

	public void setTargetSameAsSource (boolean targetSameAsSource) {
		this.targetSameAsSource = targetSameAsSource;
	}

	public boolean getTargetSameAsSourceWithCodes () {
		return targetSameAsSourceWithCodes;
	}

	public void setTargetSameAsSourceWithCodes (boolean targetSameAsSourceWithCodes) {
		this.targetSameAsSourceWithCodes = targetSameAsSourceWithCodes;
	}

	public boolean getCodeDifference () {
		return codeDifference;
	}

	public void setCodeDifference (boolean codeDifference) {
		this.codeDifference = codeDifference;
	}

	public boolean getCheckPatterns () {
		return checkPatterns;
	}

	public void setCheckPatterns (boolean patterns) {
		this.checkPatterns = patterns;
	}
	
	public List<PatternItem> getPatterns () {
		return this.patterns;
	}
	
	public void setPatterns (List<PatternItem> patterns) {
		this.patterns = patterns;
	}
	
	public boolean getCheckWithLT () {
		return this.checkWithLT;
	}

	public void setCheckWithLT (boolean checkWithLT) {
		this.checkWithLT = checkWithLT;
	}
	
	public String getServerURL () {
		return this.serverURL;
	}
	
	public void setServerURL (String serverURL) {
		this.serverURL = serverURL;
	}
	
	@Override
	public void reset () {
		outputPath = "${rootDir}/qa-report.html";
		autoOpen = false;
		leadingWS = true;
		trailingWS = true;
		emptyTarget = true;
		targetSameAsSource = true;
		targetSameAsSourceWithCodes = true;
		codeDifference = true;
		checkPatterns = true;
		patterns = new ArrayList<PatternItem>();
		checkWithLT = false;
		serverURL = "http://localhost:8081/"; // Default
	}

	@Override
	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		outputPath = buffer.getString(OUTPUTPATH, outputPath);
		autoOpen = buffer.getBoolean(AUTOOPEN, autoOpen);
		leadingWS = buffer.getBoolean(LEADINGWS, leadingWS);
		trailingWS = buffer.getBoolean(TRAILINGWS, trailingWS);
		emptyTarget = buffer.getBoolean(EMPTYTARGET, emptyTarget);
		targetSameAsSource = buffer.getBoolean(TARGETSAMEASSOURCE, targetSameAsSource);
		targetSameAsSourceWithCodes = buffer.getBoolean(TARGETSAMEASSOURCE_WITHCODES, targetSameAsSourceWithCodes);
		codeDifference = buffer.getBoolean(CODEDIFFERENCE, codeDifference);
		checkWithLT = buffer.getBoolean(CHECKWITHLT, checkWithLT);
		serverURL = buffer.getString(SERVERURL, serverURL);
		// Patterns
		checkPatterns = buffer.getBoolean(CHECKPATTERNS, checkPatterns);
		int count = buffer.getInteger(PATTERNCOUNT, 0);
		for ( int i=0; i<count; i++ ) {
			boolean enabled = buffer.getBoolean(String.format("%s%d", USEPATTERN, i), true);
			String source = buffer.getString(String.format("%s%d", SOURCEPATTERN, i), "");
			String target = buffer.getString(String.format("%s%d", TARGETPATTERN, i), PatternItem.SAME);
			patterns.add(new PatternItem(source, target, enabled));
		}
	}

	@Override
	public String toString() {
		buffer.reset();
		buffer.setString(OUTPUTPATH, outputPath);
		buffer.setBoolean(AUTOOPEN, autoOpen);
		buffer.setBoolean(LEADINGWS, leadingWS);
		buffer.setBoolean(TRAILINGWS, trailingWS);
		buffer.setBoolean(EMPTYTARGET, emptyTarget);
		buffer.setBoolean(TARGETSAMEASSOURCE, targetSameAsSource);
		buffer.setBoolean(TARGETSAMEASSOURCE_WITHCODES, targetSameAsSourceWithCodes);
		buffer.setBoolean(CODEDIFFERENCE, codeDifference);
		buffer.setBoolean(CHECKWITHLT, checkWithLT);
		buffer.setString(SERVERURL, serverURL);
		// Patterns
		buffer.setBoolean(CHECKPATTERNS, checkPatterns);
		buffer.setInteger(PATTERNCOUNT, patterns.size());
		for ( int i=0; i<patterns.size(); i++ ) {
			buffer.setBoolean(String.format("%s%d", USEPATTERN, i), patterns.get(i).enabled);
			buffer.setString(String.format("%s%d", SOURCEPATTERN, i), patterns.get(i).source);
			buffer.setString(String.format("%s%d", TARGETPATTERN, i), patterns.get(i).target);
		}
		return buffer.toString();
	}
	
}
