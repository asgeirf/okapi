/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.common;

import net.sf.okapi.common.exceptions.OkapiUnexpectedResourceTypeException;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextUnit;

/**
 * Represents an event generated by a filter that implements IFilter.
 */
public class Event {

	public final static Event END_BATCH_EVENT = new Event(EventType.END_BATCH);
	public final static Event NOOP_EVENT = new Event(EventType.NO_OP);

	private EventType filterEventType;
	private IResource resource;

	/**
	 * Creates a new event without any associated resource. Used for filter events that have no resources such as START
	 * and FINISH.
	 * 
	 * @param filterEventType
	 *            the type of event to create.
	 */
	public Event(EventType filterEventType) {
		this.filterEventType = filterEventType;
	}

	/**
	 * Creates a new event with an associated resource.
	 * 
	 * @param filterEventType
	 *            the type of event to create.
	 * @param resource
	 *            the resource to associate to the event.
	 */
	public Event(EventType filterEventType, IResource resource) {
		this.filterEventType = filterEventType;
		this.resource = resource;
	}

	/**
	 * Creates a new event with an associated resource and a skeleton object.
	 * 
	 * @param filterEventType
	 *            the type of event to create.
	 * @param resource
	 *            the resource to associate to the event.
	 * @param skeleton
	 *            the skeleton to associate to the event.
	 */
	public Event(EventType filterEventType,
		IResource resource,
		ISkeleton skeleton)
	{
		this.filterEventType = filterEventType;
		this.resource = resource;
		this.resource.setSkeleton(skeleton);
	}

	/**
	 * Gets the type of this event.
	 * 
	 * @return the type of this event.
	 */
	public EventType getEventType () {
		return filterEventType;
	}

	/**
	 * Gets the resource associated to this event.
	 * 
	 * @return the resource associated to this event, or null if there is none.
	 */
	public IResource getResource () {
		return resource;
	}

	/**
	 * Sets the resource associated to this event.
	 */
	public void setResource (IResource resource) {
		this.resource = resource;
	}

	/**
	 * Convenience method to tell if this Event carries a {@link TextUnit}
	 * 
	 * @return true if {@link TextUnit}, false otherwise
	 */
	public boolean isTextUnit () {
		return (filterEventType == EventType.TEXT_UNIT);
	}

	/**
	 * Convenience method to tell if this Event carries a {@link DocumentPart}
	 * 
	 * @return true if {@link DocumentPart}, false otherwise
	 */
	public boolean isDocumentPart () {
		return (filterEventType == EventType.DOCUMENT_PART);
	}

	/**
	 * Convenience method to tell if this Event carries a {@link StartGroup}
	 * 
	 * @return true if {@link StartGroup}, false otherwise
	 */
	public boolean isStartGroup () {
		return (filterEventType == EventType.START_GROUP);
	}

	/**
	 * Convenience method to tell if this Event carries a group {@link Ending}
	 * 
	 * @return true if group {@link Ending}, false otherwise
	 */
	public boolean isEndGroup () {
		return (filterEventType == EventType.END_GROUP);
	}

	/**
	 * Convenience method to tell if this Event carries a {@link RawDocument}
	 * 
	 * @return true if {@link RawDocument}, false otherwise
	 */
	public boolean isRawDocument () {
		return (filterEventType == EventType.RAW_DOCUMENT);
	}
	
	/**
	 * Convenience method to tell if this Event carries a {@link StartDocument}
	 * 
	 * @return true if {@link StartDocument}, false otherwise
	 */
	public boolean isStartDocument () {
		return (filterEventType == EventType.START_DOCUMENT);
	}

	/**
	 * Convenience method returns the {@link IResource} as a {@link TextUnit}.
	 * The caller should confirm the {@link Event} type using isTextUnit before
	 * calling this method.
	 * 
	 * @return the {@link TextUnit}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 *             if the {@link IResource} is not a {@link TextUnit}
	 */
	public TextUnit getTextUnit () {
		if ( isTextUnit() ) {
			return (TextUnit) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not a TextUnit");
	}

	/**
	 * Convenience method returns the {@link IResource} as a {@link DocumentPart}.
	 * The caller should confirm the {@link Event} type using isDocumentPart before calling
	 * this method.
	 * 
	 * @return the {@link DocumentPart}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 * 		if the {@link IResource} is not a {@link DocumentPart}
	 */
	public DocumentPart getDocumentPart () {
		if ( isDocumentPart() ) {
			return (DocumentPart) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not a DocumentPart");
	}
	
	/**
	 * Convenience method returns the {@link IResource} as a {@link StartGroup}. The 
	 * caller should confirm the {@link Event} type using isStartGroup before calling this
	 * method.
	 * 
	 * @return the {@link StartGroup}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 *             if the {@link IResource} is not a {@link StartGroup}
	 */
	public StartGroup getStartGroup () {
		if ( isStartGroup() ) {
			return (StartGroup) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not a StartGroup");
	}
	
	/**
	 * Convenience method returns the {@link IResource} as a {@link Ending}. The caller
	 * should confirm the {@link Event} type using isEndGroup before calling this method.
	 * 
	 * @return the {@link Ending}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 *             if the {@link IResource} is not a {@link Ending}
	 */
	public Ending getEndGroup () {
		if ( isEndGroup() ) {
			return (Ending) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not an Ending");
	}
	
	/**
	 * Convenience method returns the {@link IResource} as a {@link RawDocument}. The
	 * caller should confirm the {@link Event} type using isRawDocument before calling
	 * this method.
	 * 
	 * @return the {@link RawDocument}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 *             if the {@link IResource} is not a {@link RawDocument}
	 */
	public RawDocument getRawDocument() {
		if ( isRawDocument() ) {
			return (RawDocument) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not a RawDocument");
	}
	
	/**
	 * Convenience method returns the {@link IResource} as a {@link StartDocument}. The
	 * caller should confirm the {@link Event} type using isStartDocument before calling
	 * this method.
	 * 
	 * @return the {@link RawDocument}
	 * 
	 * @throws OkapiUnexpectedResourceTypeException
	 *             if the {@link IResource} is not a {@link RawDocument}
	 */
	public StartDocument getStartDocument() {
		if ( isStartDocument() ) {
			return (StartDocument) resource;
		}
		throw new OkapiUnexpectedResourceTypeException("Event resource is not a StartDocument");
	}
	
	@Override
	public String toString() {
		return filterEventType.toString();
	}
}
