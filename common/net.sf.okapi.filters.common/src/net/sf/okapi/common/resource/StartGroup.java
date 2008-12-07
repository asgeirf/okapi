/*===========================================================================*/
/* Copyright (C) 2008 by the Okapi Framework contributors                    */
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

package net.sf.okapi.common.resource;

public class StartGroup extends BaseReferenceable {

	/**
	 * Creates a new StartGroup object.
	 * @param parentId The ID of the parent resource for this resource.
	 */
	public StartGroup (String parentId) {
		super();
		this.parentId = parentId;
	}

	public StartGroup (String parentId,
		String id)
	{
		super();
		this.parentId = parentId;
		this.id = id;
	}

	public StartGroup (String parentId,
		String id,
		boolean isReference)
	{
		super();
		this.parentId = parentId;
		this.id = id;
		this.isReferent = isReference;
	}

}
