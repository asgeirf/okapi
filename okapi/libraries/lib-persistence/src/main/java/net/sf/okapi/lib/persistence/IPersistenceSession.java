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

package net.sf.okapi.lib.persistence;

import java.io.InputStream;
import java.io.OutputStream;

public interface IPersistenceSession {

	void registerVersions();
	
	/**
	 * Gets the current session state.
	 * @return session state
	 */
	SessionState getState();
	
	/**
	 * Starts serialization.
	 * @param outStream output stream to serialize to
	 */
	void start(OutputStream outStream);
	
	/**
	 * Starts deserialization.
	 * @param inStream input stream to deserialize from
	 */
	void start(InputStream inStream);
	
	/**
	 * Ends serialization or deserialization.
	 */
	void end();
	
	/**
	 * Serializes a given object to the session output stream. 
	 * @param obj the given object to be serialized
	 */
	void serialize(Object obj);
	
	/**
	 * Serializes to the session output stream a given object, labeling it with a given field label if implementation allows.
	 * @param obj the given object to be serialized
	 * @param name field name of the object
	 */
	void serialize(Object obj, String name);
	
	/**
	 * Deserializes an object from the session input stream.
	 * @return the deserialized object
	 */
	<T> T deserialize(Class<T> classRef);

	<T> T readObject(String content, Class<T> classRef);
	
	String writeObject(Object obj);
	
	<T> IPersistenceBean<T> createBean(Class<T> classRef);
	
	void cacheBean(Object obj, IPersistenceBean<?> bean);
	
	IPersistenceBean<?> uncacheBean(Object obj);
	
	/**
	 * Converts a given object to an expected type.
	 * The given object can be serialized as is, and then deserialized as 
	 * an expected class instance. This helps if the object was initially deserialized incorrectly.
	 * Implementers can use different strategies to achieve the goal. 	
	 * @param obj the given object to be converted
	 * @param expectedClass new class of the given object
	 * @return the converted object
	 */
	<T extends IPersistenceBean<?>> T convert(Object obj, Class<T> expectedClass);

	String getVersion();
	
	String getMimeType();
	
	String getItemClass();
	
	String getDescription();
		
	long getRefIdForObject(Object obj);
	
	public Object getObject(long refId);
	
	void setRefIdForObject(Object obj, long refId);

	void setReference(long parentRefId, long childRefId);

	void setSerialized(Object obj);
	
	Class<?> getClass(String objClassName);
	
	Class<?> getObjectClass(Class<? extends IPersistenceBean<?>> beanClassRef);
	
	<T> Class<IPersistenceBean<T>> getBeanClass(Class<T> classRef);
	
	Class<? extends IPersistenceBean<?>> getBeanClass(String className);
	
	IPersistenceBean<?> getProxy(String objClassName);
	
	IPersistenceBean<?> getProxy(Class<?> objClassRef);
	
	void registerBean(
			Class<?> classRef, 
			Class<? extends IPersistenceBean<?>> beanClassRef);
}
