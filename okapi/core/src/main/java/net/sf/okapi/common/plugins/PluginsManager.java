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
============================================================================*/

package net.sf.okapi.common.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import net.sf.okapi.common.ClassInfo;
import net.sf.okapi.common.DefaultFilenameFilter;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.IEmbeddableParametersEditor;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

/**
 * Provides a way to discover and list plug-ins for a given location or file.   
 */
public class PluginsManager {

	private ArrayList<URL> urls;
	private List<PluginItem> plugins;
	private URLClassLoader loader;
	private File pluginsDir;
	
	/**
	 * Explores the given file or directory for plug-ins and add them to
	 * this manager.
	 * @param pluginsDir the directory where the plugins are located.
	 * @param append true to preserve any plug-ins already existing in this
	 * manager, false to reset and start with no plug-in.
	 */
	public void discover (File pluginsDir,
		boolean append)
	{
		try {
			if ( plugins == null ) {
				plugins = new ArrayList<PluginItem>();
			}
			
			if ( pluginsDir == null ) return;
			if ( !pluginsDir.isDirectory() ) return;

			this.pluginsDir = pluginsDir;
			
			ArrayList<URL> existingUrls = new ArrayList<URL>();
			if ( append && ( urls != null )) {
				existingUrls.addAll(urls);
			}
			urls = new ArrayList<URL>();
			loader = null;
	
			// The plug-ins directory can contain single plug-in jars, and/or first-level sub-directories containing plug-in jars
			FilenameFilter filter = new DefaultFilenameFilter(".jar");
			
			// Inspect single jars
			File[] files = pluginsDir.listFiles(filter);
			for ( File file : files ) {
				// Skip over any sub-directories in the plugins directory 
				if ( file.isDirectory() ) continue;
				inspectFile(file);
			}
			
			// Inspect sub-directories entries
			File[] dirs = pluginsDir.listFiles();
			for ( File dir : dirs ) {
				// Skip over any file at that level
				if ( !dir.isDirectory() ) continue;
				// Else explore all .jar just under the sub-folder
				files = dir.listFiles(filter);
				for ( File file : files ) {
					inspectFile(file);
				}
			}

			// Add existing URLs, make sure they are unique 
			existingUrls.removeAll(urls);
			urls.addAll(existingUrls);
			// Set the loader
			if ( urls.size() > 0 ) {
				URL[] tmp = new URL[urls.size()];
				for ( int i=0; i<urls.size(); i++ ) {
					tmp[i] = urls.get(i);
				}
				loader = new URLClassLoader(tmp);
			}
			
			// Associate the editor-type plugins with their action-type plugins
			for ( PluginItem item1 : plugins ) {
				Class<?> cls1 = Class.forName(item1.className, false, loader);
				switch ( item1.type ) {
				case PluginItem.TYPE_IFILTER:
				case PluginItem.TYPE_IPIPELINESTEP:
					// Get the getParameters() method
					UsingParameters usingParams = cls1.getAnnotation(UsingParameters.class);
					if ( usingParams == null ) continue;
					// Skip if the class does not use parameters
					if ( usingParams.value().equals(IParameters.class) ) continue;
					// Look at all plug-ins to see if any can be associated with that type
					for ( PluginItem item2 : plugins ) {
						switch ( item2.type ) {
						case PluginItem.TYPE_IPARAMETERSEDITOR:
						case PluginItem.TYPE_IEMBEDDABLEPARAMETERSEDITOR:
						case PluginItem.TYPE_IEDITORDESCRIPTIONPROVIDER:
							Class<?> cls2 = Class.forName(item2.className, false, loader);
							// Get the type of parameters for which this editor works  
							EditorFor editorFor = cls2.getAnnotation(EditorFor.class);
							if ( editorFor == null ) continue;
							if ( editorFor.value().equals(usingParams.value()) ) {
								if ( IParametersEditor.class.isAssignableFrom(cls2) ) {
									item1.paramsEditor = new ClassInfo(item2.className, loader);
								}
								if ( IEmbeddableParametersEditor.class.isAssignableFrom(cls2) ) {
									item1.embeddableParamsEditor = new ClassInfo(item2.className, loader);
								}
								if ( IEditorDescriptionProvider.class.isAssignableFrom(cls2) ) {
									item1.editorDescriptionProvider = new ClassInfo(item2.className, loader);
								}
							}
							cls2 = null; // Try to help unlocking the file
							break;
						}
					}
					break;
				}
				cls1 = null; // Try to help unlocking the file
			}
		}
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException("Class not found", e);
		}
		catch ( SecurityException e ) {
			throw new RuntimeException("Error when looking for getParameters() method.", e);
		}
		finally {
			System.gc(); // Try freeing locks as soon as possible
		}
	}
	
	/**
	 * Gets the list of the class names of all available plug-ins 
	 * of a given type currently available in this manager.
	 * The method {@link #discover(File, boolean)} must be called once before
	 * calling this method.
	 * @param type the type of plug-ins to list.
	 * @return the list of available plug-ins for the given type.
	 */
	public List<String> getList (int type) {
		ArrayList<String> list = new ArrayList<String>();
		for ( PluginItem item : plugins ) {
			if ( item.type == type ) list.add(item.className);
		}
		return list;
	}

	/**
	 * Gets the list of all the plug-ins currently in this manager.
	 * @return the list of all the plug-ins currently in this manager.
	 */
	public List<PluginItem> getList () {
		return plugins;
	}
	
	/**
	 * Gets the list of URLs of the jars containing plug-ins currently in this manager. 
	 * @return the list of URLs.
	 */
	public ArrayList<URL> getURLs() {
		return urls;
	}

	/**
	 * Gets the URLClassLoader to use for creating new instance of the
	 * components listed in this manager. 
	 * The method {@link #discover(File, boolean)} must be called once before
	 * calling this method.
	 * @return the URLClassLoader for this manager.
	 */
	public URLClassLoader getClassLoader () {
		return loader;
	}
	
	private void inspectFile (File file) {
		try {
			// Make sure there is something to discover
			if (( file == null ) || !file.exists() ) return;
			
			// Create a temporary class loader
			URL[] tmpUrls = new URL[1]; 
			URL url = file.toURI().toURL();
			tmpUrls[0] = url;
			URLClassLoader loader = URLClassLoader.newInstance(tmpUrls);
		
			// Introspect the classes
			JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
			JarEntry entry;
			Class<?> cls = null;
			while ( true ) {
				cls = null; // Try to help unlocking the file
				if ( (entry = jarFile.getNextJarEntry()) == null ) break;
				String name = entry.getName();
				if ( name.endsWith(".class") ) {
					name = name.substring(0, name.length()-6).replace('/', '.');
					try {
						cls = Class.forName(name, false, loader);
						// Skip interfaces
						if ( cls.isInterface() ) continue;
						// Skip abstract
						if ( Modifier.isAbstract(cls.getModifiers()) ) continue;
						// Check class type
						if ( IFilter.class.isAssignableFrom(cls) ) {
							// Skip IFilter classes that should not be use directly
							if ( cls.getAnnotation(UsingParameters.class) == null ) continue;
							if ( !urls.contains(url) ) urls.add(url);
							plugins.add(new PluginItem(PluginItem.TYPE_IFILTER, name));
						}
						else if ( IPipelineStep.class.isAssignableFrom(cls) ) {
							// Skip IPipelineStep classes that should not be use directly
							if ( cls.getAnnotation(UsingParameters.class) == null ) continue;
							if ( !urls.contains(url) ) urls.add(url);
							plugins.add(new PluginItem(PluginItem.TYPE_IPIPELINESTEP, name));
						}
						else if ( IParametersEditor.class.isAssignableFrom(cls) ) {
							// Skip IParametersEditor classes that should not be use directly
							if ( cls.getAnnotation(EditorFor.class) == null ) continue;
							if ( !urls.contains(url) ) urls.add(url);
							plugins.add(new PluginItem(PluginItem.TYPE_IPARAMETERSEDITOR, name));
						}
						else if ( IEmbeddableParametersEditor.class.isAssignableFrom(cls) ) {
							// Skip IEmbeddableParametersEditor classes that should not be use directly
							if ( cls.getAnnotation(EditorFor.class) == null ) continue;
							if ( !urls.contains(url) ) urls.add(url);
							plugins.add(new PluginItem(PluginItem.TYPE_IEMBEDDABLEPARAMETERSEDITOR, name));
						}
						else if ( IEditorDescriptionProvider.class.isAssignableFrom(cls) ) {
							// Skip IEditorDescriptionProvider classes that should not be use directly
							if ( cls.getAnnotation(EditorFor.class) == null ) continue;
							if ( !urls.contains(url) ) urls.add(url);
							plugins.add(new PluginItem(PluginItem.TYPE_IEDITORDESCRIPTIONPROVIDER, name));
						}
					}
					catch ( Throwable e ) {
						// If the class cannot be create for some reason, we skip it silently
					}
				}
			}
			if ( jarFile != null ) {
				jarFile.close();
				jarFile = null;
			}
			loader = null; // Try to help unlocking the file
		}
		catch ( IOException e ) {
			throw new RuntimeException("IO error when inspecting a file for plugins.", e);
		}
	}

	/**
	 * Gets the directory where the plug-ins are located.
	 * @return full directory path.
	 */
	public String getPluginsDir() {
		return pluginsDir.getPath();
	}

}
