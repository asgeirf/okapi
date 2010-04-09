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

package net.sf.okapi.common.skeleton;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.IReferenceable;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

/**
 * Simple generic implementation of the ISkeleton interface.
 * This class implements a skeleton as a list of parts: some are simple text storage
 * string corresponding to the original code of the input document, others are
 * placeholders for the content the the text units, or the values of modifiable
 * properties. 
 */
public class GenericSkeleton implements ISkeleton {

	private ArrayList<GenericSkeletonPart> list;
	private boolean createNew = true;

	/**
	 * Creates a new empty GenericSkeleton object.
	 */
	public GenericSkeleton () {
		list = new ArrayList<GenericSkeletonPart>();
	}

	/**
	 * Creates a new GenericSkeleton object and append some data to it.
	 * @param data the data to append.
	 */
	public GenericSkeleton (String data) {
		list = new ArrayList<GenericSkeletonPart>();
		if ( data != null ) add(data);
	}
	
	/**
	 * Creates a new GenericSkeleton object and initialize it with the parts
	 * of an existing one passed as a parameter.
	 * @param skel the existing skeleton from which to copy the parts.
	 */
	public GenericSkeleton (GenericSkeleton skel) {
		list = new ArrayList<GenericSkeletonPart>();		
		if ( skel != null ) { 
			for ( GenericSkeletonPart part : skel.list ) {
				list.add(part);
			}
		}
	}

	/**
	 * Indicates if this skeleton is empty or not.
	 * @return true if this skeleton is empty, false if it has at least one part.
	 */
	public boolean isEmpty () {
		return (list.size()==0);
	}
	
	/**
	 * Indicates if this skeleton is empty or not, considering the white-spaces
	 * or not.
	 * @return true if this skeleton is empty, false if it has at least one part.
	 */
	public boolean isEmpty (boolean ignoreWhitespaces) {
		if ( ignoreWhitespaces ) {
			for ( GenericSkeletonPart part : list ) {
				for ( int i=0; i<part.data.length(); i++ ) {
					if ( !Character.isWhitespace(part.data.charAt(i)) ) {
						return false;
					}
				}
			}
			return true; // no parts, or only with white-spaces
		}
		else { // Just like isEmpty()
			return (list.size()==0);
		}
	}

	/**
	 * Adds a new part to this skeleton, and set a string data to it.
	 * @param data the data to add.
	 */
	public void add (String data) {
		if ( data.length() == 0 ) return;
		GenericSkeletonPart part = new GenericSkeletonPart(data);
		list.add(part);
		createNew = false;
	}
	
	/**
	 * Adds a new part to this skeleton, and set a character data to it.
	 * @param data the data to add.
	 */
	public void add (char data) {
		GenericSkeletonPart part = new GenericSkeletonPart(data);
		list.add(part);
		createNew = false;
	}
	
	/**
	 * Adds to this skeleton all the parts of a given skeleton.
	 * @param skel the existing skeleton from which to copy the parts.
	 */
	public void add (GenericSkeleton skel) {
		if ( skel != null ) { 
			for ( GenericSkeletonPart part : skel.list ) {
				list.add(part);
			}
		}
	}

	/**
	 * Appends a string of data to the first skeleton part, a new
	 * part is created is none exists already.
	 * @param data the string data to append.
	 */
	public void appendToFirstPart (String data) { // DWH 5-2-09
		if ( data.length() == 0 ) return;
		if ( list.isEmpty() ) {
			add(data);
		}
		else {
			list.get(0).append(data);
		}
	}

	/**
	 * Appends a string of data to this skeleton. The text is added to
	 * the current part if the current part is already a data part, a new
	 * part is created is necessary. 
	 * @param data the string data to append.
	 */
	public void append (String data) {
		if ( data.length() == 0 ) return;
		if ( createNew || list.isEmpty() ) {
			add(data);
		}
		else {
			list.get(list.size()-1).append(data);
		}
	}

	/**
	 * Appends a character data to this skeleton. The text is added to
	 * the current part if the current part is already a data part, a new
	 * part is created is necessary. 
	 * @param data the character data to append.
	 */
	public void append (char data) {
		if ( createNew || list.isEmpty() ) {
			add(data);
		}
		else {
			list.get(list.size()-1).data.append(data);
		}
	}

	/**
	 * Adds to this skeleton a placeholder for the source content of the resource
	 * to which this skeleton is attached.
	 * @param textUnit the resource object.
	 */
	public void addContentPlaceholder (TextUnit textUnit)
	{
		GenericSkeletonPart part = new GenericSkeletonPart(TextFragment.makeRefMarker("$self$"));
		part.parent = textUnit;
		part.locId = null;
		list.add(part);
		// Flag that the next append() should start a new part
		createNew = true;
	}
	
	/**
	 * Adds to this skeleton a placeholder for the content (in a given locale) of the resource
	 * to which this skeleton is attached.
	 * @param textUnit the resource object.
	 * @param locId the locale; use null if the reference is the source.
	 */
	public void addContentPlaceholder (TextUnit textUnit,
		LocaleId locId)
	{
		GenericSkeletonPart part = new GenericSkeletonPart(TextFragment.makeRefMarker("$self$"));
		part.parent = textUnit;
		part.locId = locId;
		list.add(part);
		// Flag that the next append() should start a new part
		createNew = true;
	}

	/**
	 * Adds to this skeleton a place-holder for the value of a property (in a given locale)
	 * of the resource to which this skeleton is attached.
	 * @param referent the resource object.
	 * @param propName the property name.
	 * @param locId the locale; use null for the source; LocaleId.EMPTY for resource-level property.
	 */
	public void addValuePlaceholder (INameable referent,
		String propName,
		LocaleId locId)
	{
		GenericSkeletonPart part = new GenericSkeletonPart(
			TextFragment.makeRefMarker("$self$", propName));
		part.locId = locId;
		part.parent = referent;
		list.add(part);
		// Flag that the next append() should start a new part
		createNew = true;
	}
	
	/**
	 * Updates all the self-references to use the given referent. 
	 * @param newReferent the new referent to use.
	 */
	public void changeSelfReferents (INameable newReferent) {
		String start = TextFragment.REFMARKER_START+"$self$";
		for ( GenericSkeletonPart part : list ) {
			if (( part.data != null ) && ( part.data.toString().startsWith(start) )) {
				part.parent = newReferent;
			}
		}
	}
	
	/**
	 * Adds to this skeleton a reference to an existing resource send before the one to 
	 * which this skeleton is attached to.
	 * @param referent the resource to refer to.
	 */
	public void addReference (IReferenceable referent) {
		GenericSkeletonPart part = new GenericSkeletonPart(
			TextFragment.makeRefMarker(((IResource)referent).getId()));
			part.locId = null;
			part.parent = null; // This is a reference to a real referent
			list.add(part);
			// Flag that the next append() should start a new part
			createNew = true;
		
	}
	
	/**
	 * Gets a list of all the parts of this skeleton.
	 * @return the list of all the parts of this skeleton.
	 */
	public List<GenericSkeletonPart> getParts () {
		return list;
	}
	
	/**
	 * Gets a string representation of the content of all the part of the skeleton.
	 * This should be used for display only.
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (GenericSkeletonPart part : list) {
			b.append(part.toString());
		}
		return b.toString();
	}

}