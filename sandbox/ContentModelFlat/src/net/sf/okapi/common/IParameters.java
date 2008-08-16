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

package net.sf.okapi.common;

/**
 * This interface provides a way for a component to expose its
 * parameters in a generic way.  
 */
public interface IParameters {

	/**
	 * Reset the parameters to their default values.
	 */
	public void reset ();
	
	/**
	 * Converts the options into a string.
	 * @return The string holding all the options.
	 */
	public String toString ();
	
	/**
	 * Parses the parameters stored into a string back into the object.  
	 * @param data The string holding the options. It must be formatted as 
	 * the string generated by toString(). It can also be null or empty, in
	 * such case the parameters are left with their current values.
	 * Use reset() to reset the parameters to their defaults.
	 */
	public void fromString (String data);
	
	/**
	 * Gets the string value of a given parameter.
	 * @param name Name of the parameter (ignores cases).
	 * @return The value of the given parameter.
	 */
	public String getParameter (String name);

	/**
	 * Sets the value of a given parameter.
	 * @param name The name of the parameter (ignores cases).
	 * @param value The new value. 
	 */
	public void setParameter (String name,
		String value);

	/**
	 * Sets the boolean value of a given parameter.
	 * @param name The name of the parameter (ignores cases).
	 * @param value The new value. 
	 */
	public void setParameter (String name,
		boolean value);

	/**
	 * Sets the int value of a given parameter.
	 * @param name The name of the parameter.
	 * @param value The new value. 
	 */
	public void setParameter (String name,
		int value);

	/**
	 * Gets the boolean value of the given parameter.
	 * @param name Name of the parameter.
	 * @return Value of the given parameter (false if it does not exists).
	 */
	public boolean getBoolean (String name);
	
	/**
	 * Gets the int value of the given parameter.
	 * @param name Name of the parameter.
	 * @return Value of the given parameter (0 if it does not exists).
	 */
	public int getInt (String name);
	
	/**
	 * Loads the parameters from a file.
	 * @param filePath The full path of the parameters file to load.
	 * @param ignoreErrors True if the load should ignore any error
	 * such as file not found. If an error occurs and this is set to true,
	 * the method should create the parameters object with its default values.
	 */
	public void load (String filePath,
		boolean ignoreErrors);
	
	/**
	 * Saves the parameters to a file.
	 * @param filePath The full path of the parameters file to save.
	 * @param multiFilesPrefix Optional prefix for saving multiple files. If not 
	 * null, it must be placed in front of any settings-related additional file(s)
	 * the that need to be saved along with the parameters file.
	 * @throws Exception
	 */
	public void save (String filePath,
		String multiFilesPrefix);

	/**
	 * Gets the full path of the last file where the parameters where loaded 
	 * from or saved to.
	 * @return The full path of the last load() or save(), or null if object
	 * was not loaded nor saved.
	 */
	public String getPath ();
}
