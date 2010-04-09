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

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.steps.xliffkit.common.persistence.BeanMapper;
import net.sf.okapi.steps.xliffkit.common.persistence.FactoryBean;
import net.sf.okapi.steps.xliffkit.common.persistence.IPersistenceBean;

public class TextContainerBean implements IPersistenceBean {
	
	private List<PropertyBean> properties = new ArrayList<PropertyBean>();
	private List<FactoryBean> annotations = new ArrayList<FactoryBean>();
	private List<TextPartBean> parts = new ArrayList<TextPartBean>();
	private boolean segApplied;
	
	@Override
	public <T> T get(T obj) {				
		if (obj instanceof TextContainer) {
			TextContainer tc = (TextContainer) obj;
		
			for (PropertyBean prop : properties)
				tc.setProperty(prop.get(Property.class));
			
			for (FactoryBean annotationBean : annotations)
				tc.setAnnotation(annotationBean.get(IAnnotation.class));
			
			for (TextPartBean partBean : parts)
				tc.insertPart(tc.getPartCount(), partBean.get(TextPart.class));
			
			tc.setHasBeenSegmentedFlag(segApplied);
		}		
		return obj;
	}
	
	@Override
	public <T> T get(Class<T> classRef) {
		return classRef.cast(get(new TextContainer()));
	}
	
	@Override
	public IPersistenceBean set(Object obj) {
		if (obj instanceof TextContainer) {
			TextContainer tc = (TextContainer) obj;
						
			for (String propName : tc.getPropertyNames()) {
				PropertyBean propBean = new PropertyBean();
				propBean.set(tc.getProperty(propName));
				properties.add(propBean);
			}
			
			for (IAnnotation annotation : tc.getAnnotations()) {
				FactoryBean annotationBean = new FactoryBean();
				annotations.add(annotationBean);
				annotationBean.set(annotation);
			}
			
			for (int i = 0; i < tc.getPartCount(); i++) {
				//TextPartBean partBean = new TextPartBean();
				TextPartBean partBean = (TextPartBean) BeanMapper.getBean(tc.getPart(i).getClass());
				parts.add(partBean);
				partBean.set(tc.getPart(i));
			}
			
			segApplied = tc.hasBeenSegmented();
		}		
		return this;
	}

	public void setAnnotations(List<FactoryBean> annotations) {
		this.annotations = annotations;
	}

	public List<FactoryBean> getAnnotations() {
		return annotations;
	}

	public List<PropertyBean> getProperties() {
		return properties;
	}

	public void setProperties(List<PropertyBean> properties) {
		this.properties = properties;
	}

	public List<TextPartBean> getParts() {
		return parts;
	}

	public void setParts(List<TextPartBean> parts) {
		this.parts = parts;
	}

	public boolean isSegApplied() {
		return segApplied;
	}

	public void setSegApplied(boolean segApplied) {
		this.segApplied = segApplied;
	}
}
