/*===========================================================================*/
/* Copyright (C) 2008 Asgeir Frimannsson, Jim Hargrave, Yves Savourel        */
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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class TextUnit implements ITranslatable, IAnnotatable {

	protected String                        id;
	protected String                        name;
	protected String                        type;
	protected boolean                       isTranslatable;
	protected boolean                       preserveWS;
	protected SkeletonUnit                  sklBefore;
	protected SkeletonUnit                  sklAfter;
	protected ArrayList<ITranslatable>      children;
	protected ITranslatable                 parent;
	protected LocaleData                    source;
	protected ArrayList<LocaleData>         targets;
	protected Hashtable<String, String>     propList;
	protected Hashtable<String, IExtension> extList;
	private ArrayList<TextUnit>             allUnits;


	/**
	 * Creates a TextUnit object with an empty source content and no target content.
	 */
	public TextUnit () {
		source = new LocaleData(this);
		targets = new ArrayList<LocaleData>();
		targets.add(null);
	}

	/**
	 * Creates a TextUnit object with given ID and source text.
	 * @param id the ID to use.
	 * @param sourceText The source text to use.
	 */
	public TextUnit (String id,
		String sourceText)
	{
		this.id = id;
		source = new LocaleData(this);
		source.container = new TextContainer(this);
		if ( sourceText != null ) source.container.append(sourceText);
		source.container.id = id;
		targets = new ArrayList<LocaleData>();
		targets.add(null);
	}

	/** 
	 * Get the text representation of the source of this text unit.
	 * @return The text representation of the source of this text unit.
	 */
	@Override
	public String toString () {
		//TODO: Modify for real output, this is test only
		//TODO: support for children
		return (sklBefore==null ? "" : sklBefore.toString())
			+ source.container.toString() +
			(sklAfter==null ? "" : sklAfter.toString());
	}
	
	public String getID () {
		if ( id == null ) return "";
		return id;
	}

	public void setID (String value) {
		id = value;
	}

	public boolean isEmpty () {
		return source.container.isEmpty();
	}

	public String getName () {
		if ( name == null ) return "";
		return name;
	}

	public void setName (String value) {
		name = value;
	}

	public String getType () {
		if ( type == null ) return "";
		return type;
	}

	public void setType (String value) {
		type = value;
	}

	public String getProperty (String name) {
		if ( propList == null ) return null;
		return propList.get(name);
	}

	public void setProperty (String name,
		String value)
	{
		if ( propList == null ) propList = new Hashtable<String, String>();
		propList.put(name, value);
	}

	public Hashtable<String, String> getProperties () {
		if ( propList == null ) propList = new Hashtable<String, String>();
		return propList;
	}

	public IExtension getExtension (String name) {
		if ( extList == null ) return null;
		return extList.get(name);
	}

	public void setExtension (String name,
		IExtension value)
	{
		if ( extList == null ) extList = new Hashtable<String, IExtension>();
		extList.put(name, value);
	}

	public Hashtable<String, IExtension> getExtensions () {
		if ( extList == null ) extList = new Hashtable<String, IExtension>();
		return extList;
	}

	public boolean preserveWhitespaces() {
		return preserveWS;
	}

	public void setPreserveWhitespaces (boolean value) {
		preserveWS = value;
	}

	public boolean isTranslatable () {
		return isTranslatable;
	}

	public void setIsTranslatable (boolean value) {
		isTranslatable = value;
	}

	public ITranslatable getParent () {
		return parent;
	}

	public void setParent (ITranslatable value) {
		parent = value;
	}

	/**
	 * Gets the skeleton unit just before the resource. For example a "<p>"
	 * in a HTML paragraph. 
	 * @return The skeleton unit before the resource or null.
	 */
	public SkeletonUnit getSkeletonBefore () {
		return sklBefore;
	}
	
	/**
	 * Sets the skeleton unit just before the resource. For example a "<p>"
	 * in a HTML paragraph. 
	 * @param value The skeleton unit to set.
	 */
	public void setSkeletonBefore (SkeletonUnit value) {
		sklBefore = value;
	}

	/**
	 * Gets the skeleton unit just after the resource. For example a "</p>"
	 * in a HTML paragraph. 
	 * @return The skeleton unit after the resource or null.
	 */
	public SkeletonUnit getSkeletonAfter () {
		return sklAfter;
	}
	
	/**
	 * Sets the skeleton unit just after the resource. For example a "</p>"
	 * in a HTML paragraph. 
	 * @param value The skeleton unit to set.
	 */
	public void setSkeletonAfter (SkeletonUnit value) {
		sklAfter = value;
	}

	/**
	 * Gets the source object of the resource.
	 * @return The source object of the resource, never null.
	 */
	public LocaleData getSource () {
		return source;
	}

	/**
	 * Sets the source object of the resource.
	 * @param value The new source object. Must not be null.
	 */
	public void setSource (LocaleData value) {
		if ( value == null ) throw new InvalidParameterException();
		source = value;
		if ( source.container == null ) throw new InvalidParameterException();
		source.container.parent = this;
	}
	
	/**
	 * Gets the source content of the resource.
	 * @return The source content of the resource.
	 */
	public TextContainer getSourceContent () {
		return source.container;
	}
	
	/**
	 * Sets the source content of the resource.
	 * @param value The source content. Must not be null.
	 */
	public void setSourceContent (TextContainer value) {
		if ( value == null ) throw new InvalidParameterException();
		value.parent = this;
		source.container = value;
	}
	
	/**
	 * Indicates if the text unit has a (first) target.
	 * @return True if the unit has a (first) target, false otherwise.
	 */
	public boolean hasTarget () {
		if (( targets.get(0) == null )
			|| ( !(targets.get(0).container != null) )) return false;
		return !targets.get(0).container.isEmpty();
	}

	/**
	 * Gets the (first) target object of the resource. You can use
	 * {@link #hasTarget()} to know if there is a target available.
	 * @return The (first) target object, or null.
	 */
	public LocaleData getTarget () {
		return targets.get(0); 
	}
	
	/**
	 * Sets the (first) target object of the resource.
	 * @param value The object to assign (can be null).
	 */
	public void setTarget (LocaleData value) {
		if ( value == null ) throw new InvalidParameterException();
		targets.set(0, value);
	}
	
	/**
	 * Gets the (first) target content of the resource. You can use
	 * {@link #hasTarget()} to know if there is a target available.
	 * @return The (first) target content, or null.
	 */
	public TextContainer getTargetContent () {
		if ( targets.get(0) == null ) return null;
		return targets.get(0).container;
	}

	/**
	 * Sets the (first) target content of the resource.
	 * @param value The new content (can be null).
	 */
	public void setTargetContent (TextContainer value) {
		targets.get(0).container = value;
	}

	/**
	 * Gets the list of the target objects of the resource.
	 * @return The list of the targets.
	 */
	//For now the only way to access all targets
	public List<LocaleData> getTargets () {
		return targets;
	}
	
	/**
	 * Adds a child resource to the resource.
	 * @param child The child resource to add.
	 */
	public void addChild (ITranslatable child) {
		if ( children == null ) {
			children = new ArrayList<ITranslatable>();
		}
		child.setParent(this);
		children.add(child);
	}

	public boolean hasChild () {
		if ( children == null ) return false;
		else return !children.isEmpty();
	}

	/**
	 * Gets the list of all the children of the resource.
	 * @return The list of all the children of the resource.
	 */
	public List<ITranslatable> getChildren () {
		if ( children == null ) {
			children = new ArrayList<ITranslatable>();
		}
		return children;
	}

	/**
	 * Stores recursively all TextUnit items in the children for the given object.
	 * Only TextUnit objects are stored, Group and SkeletonUnit objects are not.
	 * @param parent The parent object.
	 */
	private void storeTextUnits (ITranslatable parent) {
		// Check if it's a TextUnit
		if ( parent instanceof TextUnit ) {
			allUnits.add((TextUnit)parent);
			if ( parent.hasChild() ) {
				for ( ITranslatable item : ((TextUnit)parent).getChildren() ) {
					storeTextUnits(item);
				}
			}
			return;
		}
		// Else: it is a Group
		if ( parent.hasChild() ) {
			for ( IContainable item : (Group)parent ) {
				if ( item instanceof ITranslatable ) { 
					storeTextUnits((ITranslatable)item);
				}
			}
		}
	}
	
	/**
	 * Gets the child object with a given ID.
	 * @param id The ID value of the child to return.
	 * @return The child object for the given ID, or null if it has not
	 * been found.
	 */
	public ITranslatable getChild (String id) {
		if ( id == null ) return null;
		if ( !hasChild() ) return null;
		//TODO: Fix this so it cannot match the initial parent...
		return findChild(this, id);
	}
	
	/**
	 * Creates an iteratable list of all children TextUnit objects for this
	 * TextUnit.
	 * @return An iteratable list of all the children TextUnit objects.
	 */
	public Iterable<TextUnit> childTextUnitIterator () {
		if ( allUnits == null ) {
			allUnits = new ArrayList<TextUnit>();
			if ( hasChild() ) {
				// Children only
				storeTextUnits(getChildren().get(0));
			}
		}
		return Collections.unmodifiableList(allUnits);
	}
	
	/**
	 * Finds the child of this text unit that corresponds to the given ID.
	 * Only children that implement ITranslatable are searched.
	 * @param parent The parent of the child to search for.
	 * @param id The ID to search for.
	 * @return The found child, or null if it not been found.
	 */
	private ITranslatable findChild (ITranslatable parent,
		String id)
	{
		if ( parent instanceof TextUnit ) {
			if ( parent.hasChild() ) {
				for ( ITranslatable item : ((TextUnit)parent).children ) {
					if ( id.equals(item.getID()) ) {
						return item;
					}
					ITranslatable res = findChild(item, id);
					if ( res != null ) return res;
				}
			}
			else if ( id.equals(parent.getID()) ) {
				return parent;
			}
		}
		else if ( parent instanceof Group ) {
			for ( IContainable item : (Group)parent ) {
				if ( item instanceof ITranslatable ) { 
					ITranslatable res = findChild((ITranslatable)item, id);
					if ( res != null ) return res;
				}
			}
		}
		return null;
	}
	
}
