/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Collection of helper functions for working with classes.
 */
public class ClassUtil {
	
	private static String MSG_CANT_INSTANTIATE = "ClassUtil: cannot instantiate %s";
	private static String MSG_EMPTY_CLASSNAME = "ClassUtil: class name cannot be empty";
	private static String MSG_NULL_REF = "ClassUtil: class reference cannot be null";
	private static String MSG_NULL_LOADER = "ClassUtil: class loader cannot be null";
	private static String MSG_NONRESOLVABLE = "ClassUtil: cannot resolve class name %s";
	
	/**
	 * Gets the runtime class of the given object.
	 * @param obj The object
	 * @return The object's runtime class
	 */
	public static Class<?> getClass(Object obj) {
		if (obj == null) return null;
	
		return obj.getClass();
	}

	/**
	 * Gets a class reference for a qualified class name. 
	 * @param className the given class name
	 * @return class reference
	 */
	public static Class<?> getClass(String className) {
		if (Util.isEmpty(className))
			throw new IllegalArgumentException(MSG_EMPTY_CLASSNAME);

		Class<?> ref;
		try {
			ref = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(String.format(MSG_NONRESOLVABLE, className));
		}
		return ref;
	}
	
	/**
	 * Gets non-qualified (without package name prefix) class name for the given object.
	 * @param obj The object
	 * @return The object's class name (w/o package name prefix)
	 */
	public static String getClassName(Object obj) {
		if (obj == null) return "";
	
		return getClassName(obj.getClass());
	}
	
	/**
	 * Gets non-qualified (w/o package name prefix) name for the given class.
	 * @param classRef Class reference
	 * @return The name of the class (w/o package name prefix)
	 */
	public static String getClassName(Class<?> classRef) {
		if (classRef == null) return "";
	
		return classRef.getSimpleName();
	}

	/**
	 * Gets a qualified class name of the given object.
	 * @param obj The object
	 * @return Qualified class name
	 */
	public static String getQualifiedClassName(Object obj) {
		if (obj == null) return "";
		
		return getQualifiedClassName(obj.getClass());
	}
	
	/**
	 * Gets a qualified class name.
	 * @param classRef Class reference
	 * @return Qualified class name
	 */
	public static String getQualifiedClassName(Class<?> classRef) {
		if (classRef == null) return "";
		
		return classRef.getName();
	}
		
	/**
	 * Gets the name of the package containing the given object's class.
	 * @param obj The object
	 * @return Package name of the object's class (without the trailing dot), or an empty string
	 */
	public static String getPackageName(Object obj) {
		if (obj == null) return "";
		
		return getPackageName(obj.getClass());
	}
	
	/**
	 * Gets a package name for the given class. 
	 * @param classRef Class reference
	 * @return Package name of the class (without the trailing dot), or an empty string 
	 */
	public static String getPackageName(Class<?> classRef) {
		if (classRef == null) return "";
		
		Package pkg = classRef.getPackage();
		
		if (pkg == null) {
			
			String className = classRef.getName();
			String shortClassName = classRef.getSimpleName();
			
			int index = className.lastIndexOf(shortClassName);
            if (index != -1) {
            	
            	String res = className.substring(0, index);
            	return res.endsWith(".") ? res.substring(0, res.length() - 1) : res; 
            }
            else
            	return "";
		}
		
		return pkg.getName();
	}

	public static String getTargetPath(Class<?> cls) {
		try {
			return cls.getResource("").toURI().getPath();
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	/**
	 * Extracts the package name part of a qualified class name.
	 * @param className Qualified class name
	 * @return Package name (without the trailing dot)
	 */
	public static String extractPackageName(String className) {
		if (Util.isEmpty(className)) return "";
		
		int index = className.lastIndexOf(".");
		if (index > -1)
			return className.substring(0, index);
		
		return "";
	}

	/**
	 * Gets a qualified class name.  
	 * @param packageName Package name
	 * @param shortClassName Class name
	 * @return Qualified class name
	 */
	public static String qualifyName(String packageName, String shortClassName) {
		if (Util.isEmpty(packageName)) return "";
		if (Util.isEmpty(shortClassName)) return "";
		
		// Already qualified
		if (shortClassName.indexOf(".") != -1) return shortClassName;
			
		if (!packageName.endsWith("."))
			packageName += ".";
		
		return packageName + shortClassName;
	}

	/**
	 * Gets a qualified class name for the given class name. Package name is determined 
	 * from a reference to another class in the same package.
	 * @param siblingClassRef Reference to another class in the same package
	 * @param shortClassName Non-qualified name of the class to get a qualified name for
	 * @return Qualified class name
	 */
	public static String qualifyName(Class<?> siblingClassRef, String shortClassName) {		
		return qualifyName(getPackageName(siblingClassRef), shortClassName);
	}
	
	/**
	 * Gets a qualified class name for the given class name. Package name is determined 
	 * from an instance of another class in the same package.
	 * @param sibling Existing object, an instance of another class in the same package
	 * @param shortClassName Non-qualified name of the class to get a qualified name for
	 * @return Qualified class name
	 */
	public static String qualifyName(Object sibling, String shortClassName) {		
		if (sibling == null) return ""; 
		
		return qualifyName(sibling.getClass(), shortClassName);
	}
	
	/**
	 * Creates a new instance of a given class.
	 * @param classRef The given class
	 * @return a newly created instance of the given class
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static <T> T instantiateClass(Class<T> classRef) 
		throws InstantiationException, IllegalAccessException {
		if (classRef == null)
			throw new IllegalArgumentException(MSG_NULL_REF);
		
		return classRef.cast(classRef.newInstance());
	}
	
	/**
	 * Creates a new instance of the class with a given class name.
	 * @param className The given class name
	 * @return a newly created instance of the class with the given class name
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static Object instantiateClass(String className)
		throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (Util.isEmpty(className))
			throw new IllegalArgumentException(MSG_EMPTY_CLASSNAME);
		
		return instantiateClass(Class.forName(className));
	}
	
	/**
	 * Creates a new instance of the class with a given class name using a given class loader.
	 * @param className The given class name
	 * @param classLoader The class loader from which the class must be loaded
	 * @return A newly created instance of the desired class.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static Object instantiateClass(String className, ClassLoader classLoader) 
		throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (Util.isEmpty(className))
			throw new IllegalArgumentException(MSG_EMPTY_CLASSNAME);
		
		if (classLoader == null)
			throw new IllegalArgumentException(MSG_NULL_LOADER);
		
		Class<?> ref = Class.forName(className, true, classLoader);
		if (ref == null)
			throw new RuntimeException(String.format(MSG_NONRESOLVABLE, className));
			
		return ref.cast(ref.newInstance());
	}
	
	/**
	 * Creates a new instance of the class using a given class loader and initialization parameters.
	 * @param classRef The given class
	 * @param constructorParameters The initialization parameters for the class constructor
	 * @return A newly created instance of the desired class
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static <T> T instantiateClass(Class<T> classRef, Object... constructorParameters) 
		throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
			InstantiationException, IllegalAccessException, InvocationTargetException { 
		if (classRef == null)
			throw new IllegalArgumentException(MSG_NULL_REF);
		
		if (constructorParameters == null) 
			return instantiateClass(classRef);
				
		// Find a constructor matching the given parameters (constructors' ambiguity is impossible)
		Constructor<?>[] constructors = classRef.getConstructors();
		
		for (Constructor<?> constructor : constructors) {
			
			if (constructor == null) continue;
			
			Class<?>[] parameterTypes = constructor.getParameterTypes();			
			if (parameterTypes.length != constructorParameters.length) continue;

			boolean matches = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				
				Class<?> paramType = parameterTypes[i];
				Object constructorParameter = constructorParameters[i];
				
				if (!paramType.isInstance(constructorParameter)) {
					
					matches = false;
					break;
				}
			}
			
			if (matches)
				return classRef.cast(constructor.newInstance(constructorParameters));
		}
		throw new RuntimeException(String.format(MSG_CANT_INSTANTIATE, classRef.getName()));
	}
	
	/**
	 * Creates a new instance of the class with a given class name and initialization parameters.
	 * @param className The given class name
	 * @param constructorParameters The initialization parameters for the class constructor
	 * @return A newly created instance of the desired class
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public static Object instantiateClass(String className, Object... constructorParameters) 
		throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
			InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		
		if (Util.isEmpty(className))
			throw new IllegalArgumentException(MSG_EMPTY_CLASSNAME);
		
		Class<?> ref = Class.forName(className);		
		return ref.cast(instantiateClass(ref, constructorParameters));
	}

	/**
	 * Creates a new instance of the class with a given class name and initialization parameters using a given class loader.
	 * @param className The given class name
	 * @param classLoader The given class loader
	 * @param constructorParameters The initialization parameters for the class constructor
	 * @return A newly created instance of the desired class
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public static Object instantiateClass(String className, ClassLoader classLoader, Object... constructorParameters) 
	throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
		InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
	
		if (Util.isEmpty(className))
			throw new IllegalArgumentException(MSG_EMPTY_CLASSNAME);
		
		if (classLoader == null)
			throw new IllegalArgumentException(MSG_NULL_LOADER);
		
		Class<?> ref = classLoader.loadClass(className); 		
		return ref.cast(instantiateClass(ref, constructorParameters));
	}

	/**
	 * Gets a full path of a given resource. 
	 * @param cls Class containing the given resource.
	 * @param resourceName Name of the given resource. Should be prefixed with a leading slash ("/name.ext") 
	 * if the resource is located in the class root. If the resource is located in the same
	 * package as the class, then no leading slash is needed ("name.ext"). 
	 * @return Full path of the given resource.
	 */
	public static String getResourcePath(Class<?> cls, String resourceName) {
		return cls.getResource(resourceName).getPath();
	}
}
