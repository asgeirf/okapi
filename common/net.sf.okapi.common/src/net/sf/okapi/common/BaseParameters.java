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

package net.sf.okapi.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;

/**
 * Base class for properties-like parameters that implement IParameters.
 * See the {@link ParametersString} documentation for details on the storage format.
 */
public abstract class BaseParameters implements IParameters {

	/**
	 * Current path of the parameter file.
	 */
	protected String path;
	
	/**
	 * Buffer where the parameters are stored during conversion.
	 */
	protected ParametersString buffer;

	/**
	 * Creates a new BaseParameters object with a null path and an empty buffer.
	 */
	public BaseParameters () {
		path = null;
		buffer = new ParametersString();
	}
	
	public String getPath () {
		return path;
	}

	public void setPath (String filePath) {
		path = filePath;
	}
	
	public void load (URI inputURI,
		boolean p_bIgnoreErrors)
	{
		char[] aBuf;
		try {
			// Reset the parameters to their defaults
			reset();
			// Open the file. use a URL so we can do openStream() and load
			// predefined files from JARs.
			URL url = inputURI.toURL();
			Reader SR = new InputStreamReader(
				new BufferedInputStream(url.openStream()), "UTF-8");

			// Read the file in one string
			StringBuilder sbTmp = new StringBuilder(1024);
			aBuf = new char[1024];
			int nCount;
			while ((nCount = SR.read(aBuf)) > -1) {
				sbTmp.append(aBuf, 0, nCount);	
			}
			SR.close();
			SR = null;

			// Parse it
			String tmp = sbTmp.toString().replace("\r\n", "\n");
			fromString(tmp.replace("\r", "\n"));
			path = inputURI.getPath();
		}
		catch ( IOException e ) {
			if ( !p_bIgnoreErrors ) throw new RuntimeException(e);
		}
		finally {
			aBuf = null;
		}
	}

	public void save (String newPath) {
		Writer SW = null;
		try {
			// Save the fields on file
			SW = new OutputStreamWriter(
				new BufferedOutputStream(new FileOutputStream(newPath)),
				"UTF-8");
			SW.write(toString());
			path = newPath;
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		finally {
			if ( SW != null )
				try { SW.close(); } catch ( IOException e ) {};
		}
	}
	
	public boolean getBoolean (String name) {
		if ( buffer == null ) return false;
		else return buffer.getBoolean(name);
	}
	
	public String getString (String name) {
		if ( buffer == null ) return null;
		else return buffer.getString(name);
	}
	
	public int getInteger (String name) {
		if ( buffer == null ) return 0;
		else return Integer.parseInt(buffer.getString(name));
	}

}
