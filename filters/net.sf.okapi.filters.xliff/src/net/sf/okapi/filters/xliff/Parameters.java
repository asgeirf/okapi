/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel                                          */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.filters.xliff;

import net.sf.okapi.common.BaseParameters;

public class Parameters extends BaseParameters {

	protected boolean   useStateValues;
	protected boolean   extractOnlyMatchingValues;
	protected String    stateValues;
	protected boolean   extractNoState;
	protected boolean   fallbackToID;
	protected boolean   escapeGT;
	

	public Parameters () {
		reset();
	}
	
	public void reset () {
		super.reset();
		useStateValues = true;
		stateValues = "new|needs-translation";
		extractOnlyMatchingValues = true;
		extractNoState = true;
		fallbackToID = true;
		escapeGT = false;
	}

	public String toString () {
		setBoolean("useStateValues", useStateValues);
		setBoolean("extractOnlyMatchingValues", extractOnlyMatchingValues);
		setString("stateValues", stateValues);
		setBoolean("extractNoState", extractNoState);
		setBoolean("fallbackToID", fallbackToID);
		setBoolean("escapeGT", escapeGT);
		return super.toString();
	}
	
	public void fromString (String data) {
		reset();
		super.fromString(data);
		useStateValues = getBoolean("useStateValues", useStateValues);
		extractOnlyMatchingValues = getBoolean("extractOnlyMatchingValues", extractOnlyMatchingValues);
		stateValues = getString("stateValues", stateValues);
		extractNoState = getBoolean("extractNoState", extractNoState);
		fallbackToID = getBoolean("fallbackToID", fallbackToID);
		escapeGT = getBoolean("escapeGT", escapeGT);
	}
}
