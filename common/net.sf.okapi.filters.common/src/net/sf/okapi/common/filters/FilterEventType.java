/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.filters;

/**
 * The type of events returned by the classes implementing IFilter.
 * <p>The minimum events generated by a filter are:
 * <ul><li>START
 * <li>START_DOCUMENT
 * <li>END_DOCUMENT
 * <li>FINISHED</li></ul>
 */
public enum FilterEventType {
	/**
	 * The first event generated by the filter when processing a input document. Use this event to
	 * initialize data before the START_DOCUMENT event.
	 * No resource are associated with this event.
	 */
	START,
	
	/**
	 * Indicates the start of an input document.
	 * A StartDocument resource should be associated with this event.  
	 */
	START_DOCUMENT,
	
	/**
	 * Indicates the end of an input document.
	 * An Ending resource should be associated with this event.  
	 */
	END_DOCUMENT,
	
	/**
	 * Indicates the start of a sub-document.
	 * A StartSubDocument resource should be associated with this event. 
	 */
	START_SUBDOCUMENT,
	
	/**
	 * Indicates the end of a sub-document.
	 * An Ending resource should be associated with this event.  
	 */
	END_SUBDOCUMENT,
	
	/**
	 * Indicates the start of a group. For example, the start tag of the &lt;table> element in HTML.
	 * A StartGroup resource should be associated with this event. 
	 */
	START_GROUP,
	
	/**
	 * Indicates the end of a group.
	 * An Ending resource should be associated with this event.  
	 */
	END_GROUP,
	
	/**
	 * Indicates a text unit. For example, a paragraph in an HTML document.
	 * A TextUnit resource should be associated with this event. 
	 */
	TEXT_UNIT,
	
	/**
	 * Indicates a document part. Document parts are used to carry chunks of the input document that
	 * have no translatable data, but may have properties.
	 * A DocumentPart resource should be associated with this event. 
	 */
	DOCUMENT_PART,
	
	/**
	 * The last event generated by the filter when processing a document. Use this event to clean up data.
	 * No resource are associated with this event.  
	 */
	FINISHED,
	
	/**
	 * Indicates that the user has canceled the processing.
	 * No resource are associated with this event.  
	 */
	CANCELED,
	
	/**
	 * File level event. A File resource should be associated with this event.
	 */
	FILE_RESOURCE
}
