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

package net.sf.okapi.steps.xliffkit.common.persistence.beans;

import net.sf.okapi.common.ClassUtil;
import net.sf.okapi.steps.xliffkit.common.persistence.BeanMapper;
import net.sf.okapi.steps.xliffkit.common.persistence.IPersistenceBean;
import net.sf.okapi.steps.xliffkit.common.persistence.IPersistenceSession;
import net.sf.okapi.steps.xliffkit.common.persistence.PersistenceBean;

public class ReferenceBean extends PersistenceBean<Object> {

	private long reference;
	private String className;

	@Override
	protected Object createObject(IPersistenceSession session) {
		Object obj = session.getObject(reference);
		if (obj == null) {
			IPersistenceBean proxy = BeanMapper.getProxy(className);
			if (proxy != null)
				// Create an object and put to cache so getObject() can find it from PersistenceBean.get()
				obj = proxy.get(ClassUtil.getClass(className), session); 
		}
		return obj;
	}

	@Override
	protected void fromObject(Object obj, IPersistenceSession session) {
		if (obj == null) return;
		
		className = ClassUtil.getQualifiedClassName(obj);
		session.setRefIdForObject(this, this.getRefId()); // To find the ref parent's root
		
		long rid = session.getRefIdForObject(obj);
		if (rid != 0) {
			reference = rid;
			//session.setRefIdForObject(this, this.getRefId()); // To find the ref parent's root
			session.setReference(this.getRefId(), rid);
			return;
		}
		
		IPersistenceBean bean = session.createBean(ClassUtil.getClass(obj));
		session.cacheBean(obj, bean); // for a FactoryBean to hook up later
		reference = bean.getRefId();
		// session.setRefIdForObject(obj, bean.getRefId());
		session.setReference(this.getRefId(), bean.getRefId());		
	}

	@Override
	protected void setObject(Object obj, IPersistenceSession session) {
	}

	public void setReference(long reference) {
		this.reference = reference;
	}

	public long getReference() {
		return reference;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}
}