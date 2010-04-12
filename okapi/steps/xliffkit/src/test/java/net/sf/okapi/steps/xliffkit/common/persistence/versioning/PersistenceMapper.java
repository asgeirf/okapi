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

package net.sf.okapi.steps.xliffkit.common.persistence.versioning;

import java.util.LinkedHashMap;

import net.sf.okapi.common.ClassUtil;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.BaseNameable;
import net.sf.okapi.common.resource.BaseReferenceable;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.Document;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.InlineAnnotation;
import net.sf.okapi.common.resource.MultiEvent;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TargetPropertiesAnnotation;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.BaseNameableBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.BaseReferenceableBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.CodeBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.DocumentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.DocumentPartBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.EndingBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.EventBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.FilterWriterBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.GenericSkeletonBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.GenericSkeletonPartBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.InlineAnnotationBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.MultiEventBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.ParametersBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.PropertyBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.RawDocumentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.SegmentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.StartDocumentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.StartGroupBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.StartSubDocumentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.TargetPropertiesAnnotationBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.TextContainerBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.TextFragmentBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.TextUnitBean;
import net.sf.okapi.steps.xliffkit.common.persistence.versioning.ZipSkeletonBean;

public class PersistenceMapper {
	
	private static final String MSG1 = "PersistenceFactory: bean mapping is not initialized";
	private static final String MSG2 = "PersistenceFactory: unknown class: %s";
	private static final String MSG3 = "PersistenceFactory: class %s is not registered";
	private static final String MSG4 = "PersistenceFactory: cannot instantiate %s";	
	private static final String MSG5 = "PersistenceFactory: Class reference cannot be empty";
	
	// !!! LinkedHashMap to preserve registration order
	private static LinkedHashMap<Class<?>, Class<? extends IPersistenceBean>> beanMapping;
	//private static ConcurrentHashMap<Class<? extends IPersistenceBean>, IPersistenceBean> persistenceCache;
	
	static {
		beanMapping = new LinkedHashMap<Class<?>, Class<? extends IPersistenceBean>> ();
		//persistenceCache = new ConcurrentHashMap<Class<? extends IPersistenceBean>, IPersistenceBean> ();
		
		registerBeans();
	}
	
	public static void registerBean(
			Class<?> classRef, 
			Class<? extends IPersistenceBean> beanClassRef) {
		if (classRef == null || beanClassRef == null)
			throw(new IllegalArgumentException());
		
		if (beanMapping == null)
			throw(new RuntimeException(MSG1));
		
		beanMapping.put(classRef, beanClassRef);		
	}
	
	public static Class<? extends IPersistenceBean> getBeanClass(Class<?> classRef) {
		if (classRef == null)
			throw(new IllegalArgumentException(MSG5));
	
		if (beanMapping == null)
			throw(new RuntimeException(MSG1));
		
		Class<? extends IPersistenceBean> beanClass = beanMapping.get(classRef);
		
		// If not found explicitly, try to find a matching bean
		if (beanClass == null)
			for (Class<?> cls : beanMapping.keySet())
				if (cls.isAssignableFrom(classRef)) {
					beanClass = beanMapping.get(cls);
					break;
				}
		
		return beanClass;		
	}
	
	public static Class<? extends IPersistenceBean> getBeanClass(String className) {
			
		Class<? extends IPersistenceBean> res = null;
		try {
			res = getBeanClass(Class.forName(className));
		} catch (ClassNotFoundException e) {
			throw(new RuntimeException(String.format(MSG2, className)));
		}
		return res;		
	}
	
	public static IPersistenceBean getBean(Class<?> classRef) {
		Class<? extends IPersistenceBean> beanClass = 
			getBeanClass(classRef); // Checks for skelClass == null, beanMapping == null
		
		if (beanClass == null)
			throw(new RuntimeException(String.format(MSG3, classRef.getName())));
		
//		if (persistenceCache == null)
//			throw(new RuntimeException(MSG2));
		
		IPersistenceBean bean = null; //persistenceCache.get(beanClass); 
		//if (bean == null) {
			try {
				bean = ClassUtil.instantiateClass(beanClass);
				//persistenceCache.put(beanClass, bean);
			} catch (Exception e) {
				throw new RuntimeException(String.format(MSG4, beanClass.getName()), e);
			}
		//}		
		return bean;		
	}
	
	private static void registerBeans() {
		// General purpose beans
		registerBean(IParameters.class, ParametersBean.class);
		registerBean(IFilterWriter.class, FilterWriterBean.class);
		registerBean(Object.class, TypeInfoBean.class); // If no bean was found, use just this one to store class info
		
		// Specific class beans
		registerBean(Event.class, EventBean.class);
		registerBean(TextUnit.class, TextUnitBean.class);
		registerBean(RawDocument.class, RawDocumentBean.class);
		registerBean(Property.class, PropertyBean.class);
		registerBean(TextFragment.class, TextFragmentBean.class);
		registerBean(TextContainer.class, TextContainerBean.class);
		registerBean(Code.class, CodeBean.class);
		registerBean(Document.class, DocumentBean.class);
		registerBean(DocumentPart.class, DocumentPartBean.class);
		registerBean(Ending.class, EndingBean.class);
		registerBean(MultiEvent.class, MultiEventBean.class);
		registerBean(Segment.class, SegmentBean.class);
		registerBean(BaseNameable.class, BaseNameableBean.class);
		registerBean(BaseReferenceable.class, BaseReferenceableBean.class);
		registerBean(StartDocument.class, StartDocumentBean.class);
		registerBean(StartGroup.class, StartGroupBean.class);
		registerBean(StartSubDocument.class, StartSubDocumentBean.class);
		registerBean(TargetPropertiesAnnotation.class, TargetPropertiesAnnotationBean.class);
		registerBean(GenericSkeleton.class, GenericSkeletonBean.class);
		registerBean(GenericSkeletonPart.class, GenericSkeletonPartBean.class);
		registerBean(ZipSkeleton.class, ZipSkeletonBean.class);		
		registerBean(InlineAnnotation.class, InlineAnnotationBean.class);		
	}
}
