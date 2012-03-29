/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.steps.xsltransform;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.ReferenceParameter;

public class Parameters extends BaseParameters {

	private static final String XSLTPATH = "xsltPath";
	
	private String xsltPath;
	public String paramList;
	public boolean useCustomTransformer;
	public String factoryClass;
	public String xpathClass;

	public Parameters () {
		reset();
	}
	
	public void reset () {
		xsltPath = ""; //$NON-NLS-1$
		paramList = ""; //$NON-NLS-1$
		useCustomTransformer = false;
		// Example: net.sf.saxon.TransformerFactoryImpl
		factoryClass = ""; //$NON-NLS-1$
		xpathClass = "";
	}
	
	public void setXsltPath (String xsltPath) {
		this.xsltPath = xsltPath;
	}
	
	@ReferenceParameter
	public String getXsltPath () {
		return xsltPath;
	}

	public void fromString (String data) {
		reset();
		buffer.fromString(data);
		xsltPath = buffer.getString(XSLTPATH, xsltPath); //$NON-NLS-1$
		paramList = buffer.getString("paramList", paramList); //$NON-NLS-1$
		useCustomTransformer = buffer.getBoolean("useCustomTransformer", useCustomTransformer); //$NON-NLS-1$
		factoryClass = buffer.getString("transformerClass", factoryClass); //$NON-NLS-1$
		xpathClass = buffer.getString("xpathClass", xpathClass);
	}

	@Override
	public String toString () {
		buffer.reset();
		buffer.setString(XSLTPATH, xsltPath); //$NON-NLS-1$
		buffer.setString("paramList", paramList); //$NON-NLS-1$
		buffer.setBoolean("useCustomTransformer", useCustomTransformer); //$NON-NLS-1$
		buffer.setString("transformerClass", factoryClass); //$NON-NLS-1$
		buffer.setString("xpathClass", xpathClass);
		return buffer.toString();
	}

}
