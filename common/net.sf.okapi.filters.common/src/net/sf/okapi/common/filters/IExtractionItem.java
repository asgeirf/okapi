/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel (at ENLASO Corporation)                  */
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
/* Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA              */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.filters;

import java.util.List;

public interface IExtractionItem {

	/**
	 * Gets the un-segmented content of the item.
	 * @return The IContainer object with the un-segmented content.
	 */
	IContainer getContent();
	
	/**
	 * Sets the content of the item.
	 * @param data An IContainer object with the new content.
	 */
	void setContent (IContainer data);
	
	/**
	 * Gets the list of the segments for the item.
	 * @return A list of IContainer object corresponding to each segment.
	 */
	List<IContainer> getSegments();

	/**
	 * Gets the list of children items for this item.
	 * @return A list of IExtractionItem objects, one for each child item.
	 */
	List<IExtractionItem> getChildren();
	
	/**
	 * Adds a child item to this item.
	 * @param child The IExtractionItem object to add.
	 */
	void addChild (IExtractionItem child);
}
