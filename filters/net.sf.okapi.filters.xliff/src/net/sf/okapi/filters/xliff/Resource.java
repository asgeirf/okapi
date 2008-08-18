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

import java.util.ArrayList;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.filters.xliff.Parameters;

public class Resource extends net.sf.okapi.common.resource.Document {

	private static final long serialVersionUID = 1L;

	protected Parameters          params;
	protected boolean             needTargetElement;
	protected ArrayList<Code>     srcCodes;
	protected ArrayList<Code>     trgCodes;
	

	public Resource () {
		params = new Parameters();
		srcCodes = new ArrayList<Code>();
		trgCodes = new ArrayList<Code>();		
	}
	
	@Override
	public IParameters getParameters () {
		return params;
	}

}
