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

package net.sf.okapi.steps.xliffkit.common.persistence;

import net.sf.okapi.common.ClassUtil;

public class FactoryBean implements IPersistenceBean {	
	private String className;
	private Object content;		
	
	@Override
	public <T> T get(T obj) {
		return obj;
	}
	
	@Override
	public <T> T get(Class<T> classRef) {
		return classRef.cast(validateContent() ? ((IPersistenceBean) content).get(content.getClass()) : null);
	}
	
	@Override
	public IPersistenceBean set(Object obj) {		
		className = ClassUtil.getQualifiedClassName(obj);
		//System.out.println(className);
		
		IPersistenceBean bean = PersistenceMapper.getBean(ClassUtil.getClass(obj));
		content = bean;
		
		return (bean instanceof FactoryBean) ? this : bean.set(obj); 
	}
	
	private boolean validateContent() {
		if (content == null) return false;
		if (className == null) return false;
		
		String cname = ClassUtil.getQualifiedClassName(content);
		if (!className.equals(cname))			
			content = JSONObjectConverter.convert(content, PersistenceMapper.getBeanClass(className));
		if (content == null) return false;
		
		return className.equals(cname);
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	public Object getContent() {
		return content;
	}

}
