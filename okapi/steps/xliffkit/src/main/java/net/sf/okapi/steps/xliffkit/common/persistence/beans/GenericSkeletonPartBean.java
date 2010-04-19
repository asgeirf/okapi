/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.xliffkit.common.persistence.beans;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.steps.xliffkit.common.persistence.IPersistenceSession;
import net.sf.okapi.steps.xliffkit.common.persistence.PersistenceBean;
import net.sf.okapi.steps.xliffkit.common.persistence.ReferenceBean;

public class GenericSkeletonPartBean extends PersistenceBean {

	private String data;
	private ReferenceBean parent = new ReferenceBean();
	//private FactoryBean parent = new FactoryBean();
	private String locId;

	@Override
	protected Object createObject(IPersistenceSession session) {
		LocaleId localeId = null;
		if (!Util.isEmpty(locId))
			localeId = new LocaleId(locId);
		
		return new GenericSkeletonPart(data, parent.get(IResource.class, session), localeId);
	}

	@Override
	protected void fromObject(Object obj, IPersistenceSession session) {
		if (obj instanceof GenericSkeletonPart) {
			GenericSkeletonPart part = (GenericSkeletonPart) obj;
			
			data = part.toString();
			parent.set(part.getParent(), session);
			LocaleId loc = part.getLocale();
			if (loc != null)
				locId = loc.toString();			
		}
	}

	@Override
	protected void setObject(Object obj, IPersistenceSession session) {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setLocId(String locId) {
		this.locId = locId;
	}

	public String getLocId() {
		return locId;
	}

//	public FactoryBean getParent() {
//		return parent;
//	}
//
//	public void setParent(FactoryBean parent) {
//		this.parent = parent;
//	}

	public ReferenceBean getParent() {
		return parent;
	}

	public void setParent(ReferenceBean parent) {
		this.parent = parent;
	}
}
