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

package net.sf.okapi.common.ui;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Provides a way to manage a list of most recently used files (MRU).
 */
public class MRUList {

	private LinkedList<String> list;
	private int max;
	
	/**
	 * Creates a new MRUList object with a given maximum size.
	 * @param maxInList The maximum size.
	 */
	public MRUList (int maxInList) {
		if ( max < 2 ) max = 2;
		max = maxInList;
		list = new LinkedList<String>();
	}
	
	/**
	 * Reads the list from a properties object.
	 * @param properties The properties to read from.
	 */
	public void getFromProperties (Properties properties) {
		list = new LinkedList<String>();
		String tmp = properties.getProperty("mru.count");
		if ( tmp == null ) return;
		int count = Integer.valueOf(tmp);
		for ( int i=0; i<count; i++ ) {
			tmp = properties.getProperty(String.format("mru.path%d", i+1));
			if ( tmp != null ) {
				list.add(tmp);
			}
		}
	}
	
	/**
	 * Saves the list to a properties object. This also clears any old list.
	 * @param properties The properties where to copy the paths.
	 */
	public void copyToProperties (Properties properties) {
		// Clear any existing entries
		String tmp = properties.getProperty("mru.count");
		if ( tmp != null ) {
			properties.remove("mru.count");
			int count = Integer.valueOf(tmp);
			for ( int i=0; i<count; i++ ) {
				properties.remove(String.format("mru.path%d", i+1));
			}
		}
		// Then write the new list
		properties.setProperty("mru.count", String.format("%d", list.size()));
		for ( int i=0; i<list.size(); i++ ) {
			properties.setProperty(String.format("mru.path%d", i+1), list.get(i));
		}
	}
	
	/**
	 * Clears the list from any path.
	 */
	public void clear () {
		list = new LinkedList<String>();
	}

	/**
	 * Removes a given path from this MRU list. If the path does not
	 * exists in the list, nothing happens.
	 * @param path The path to remove.
	 */
	public void remove (String path) {
		list.remove(path);
	}
	
	/**
	 * Adds at the top of the list, pushes all the other path downward,
	 * and trim if the list gets larger than the maximum allowed.
	 * If the path exists already in the list, it is moved to the top.
	 * @param path The path to add.
	 */
	public void add (String path) {
		// Check if the path exists and if so, get its index
		int n = -1;
		for ( int i=0; i<list.size(); i++ ) {
			if ( list.get(i).equals(path) ) {
				n = i;
				break;
			}
		}
		if ( n > -1 ) {
			// In the list already
			if ( n == 0 ) return; // Already at the top
			// Else, remove it, so we can add it at the top
			list.remove(n);
		}
		// Add the path at the top
		list.addFirst(path);
		// Trim the list to the maximum allowed
		if ( list.size() > max ) {
			list.removeLast();
		}
	}
	
	/**
	 * Gets the first path in the list, or null if there is none.
	 * @return the first path in the list, or null if there is none.
	 */
	public String getfirst () {
		if ( list.size() > 0 ) return list.getFirst();
		else return null;
	}

	/**
	 * Gets an iterator for the paths in this MRU list.
	 * @return The iterator for the paths in this MRU list.
	 */
	public Iterator<String> getIterator () {
		return list.iterator();
	}
	
}
