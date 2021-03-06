/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff;

import java.io.Serializable;

import org.oasisopen.xliff.v2.IExtendedAttributes;
import org.oasisopen.xliff.v2.IWithExtendedAttributes;

public class EventData implements Serializable, IWithExtendedAttributes {

	private static final long serialVersionUID = 0100L;
	
	private String id;
	private IExtendedAttributes xattrs;

	public String getId () {
		return id;
	}
	
	public void setId (String id) {
		this.id = id;
	}
	
	@Override
	public void setExtendedAttributes (IExtendedAttributes attributes) {
		this.xattrs = attributes;
	}

	@Override
	public IExtendedAttributes getExtendedAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtendedAttributes();
		}
		return xattrs;
	}

	@Override
	public boolean hasExtendedAttribute () {
		if ( xattrs == null ) return false;
		return (xattrs.size() > 0);
	}

}
