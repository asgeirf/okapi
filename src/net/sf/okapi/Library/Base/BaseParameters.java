/*===========================================================================*/
/* Copyright (C) 2008 ENLASO Corporation, Okapi Development Team             */
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

package net.sf.okapi.Library.Base;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public abstract class BaseParameters implements IParameters {

	public abstract void reset ();
	
	public abstract String toString ();
	
	public abstract void fromString (String p_sData);
	
	public String getParameter (String p_sName) {
		//TODO: Find a faster/better way to implement getOption()
		FieldsString FS = new FieldsString(toString());
		return FS.get(p_sName, null);
	}

	public void setParameter (String p_sName,
		String p_sValue)
	{
		//TODO: Find a faster/better way to implement setOption()
		FieldsString FS = new FieldsString(toString());
		FS.set(p_sName, p_sValue);
		fromString(FS.toString());
	}

	public void load (String p_sPath,
		boolean p_bIgnoreErrors)
		throws Exception
	{
		try {
			// Reset the parameters to their defaults
			reset();
			// Open the file
			Reader SR = new InputStreamReader(
				new BufferedInputStream(new FileInputStream(p_sPath)),
				"UTF-8");
				
			// Read the file in one string
			StringBuffer sbTmp = new StringBuffer(1024);
			char[] aBuf = new char[1024];
			@SuppressWarnings("unused")
			int nCount;
			while ((nCount = SR.read(aBuf)) > -1) {
				sbTmp.append(aBuf.toString());	
			}
			SR.close();
			SR = null;

			// Parse it
			fromString(sbTmp.toString());
		}
		catch ( Exception E ) {
			if ( !p_bIgnoreErrors ) throw E;
		}
	}

	public void save (String p_sPath)
		throws Exception
	{
		Writer SW = null;
		try {
			// Save the fields on file
			SW = new OutputStreamWriter(
				new BufferedOutputStream(new FileOutputStream(p_sPath)),
				"UTF-8");
			SW.write(this.toString());
		}
		catch ( Exception E ) {
			throw E;
		}
		finally {
			if ( SW != null ) SW.close();
		}
	}
}
