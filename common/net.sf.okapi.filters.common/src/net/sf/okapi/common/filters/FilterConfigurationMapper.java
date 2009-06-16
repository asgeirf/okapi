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

package net.sf.okapi.common.filters;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ParametersEditorMapper;
import net.sf.okapi.common.exceptions.OkapiEditorCreationException;
import net.sf.okapi.common.exceptions.OkapiFilterCreationException;

/**
 * Default implementation of the {@link IFilterConfigurationMapper} interface.
 */
public class FilterConfigurationMapper extends ParametersEditorMapper implements IFilterConfigurationMapper {

	private LinkedHashMap<String, FilterConfiguration> configMap;
	
	/**
	 * Creates a new FilterConfigurationMapper object with no mappings.
	 */
	public FilterConfigurationMapper () {
		super();
		configMap = new LinkedHashMap<String, FilterConfiguration>();
	}

	public void addConfigurations (String filterClass) {
		// Instantiate the filter to get the available configurations
		IFilter filter = null;
		try {
			filter = (IFilter)Class.forName(filterClass).newInstance();
		}
		catch ( InstantiationException e ) {
			throw new OkapiFilterCreationException("Cannot instantiate filter.", e);
		}
		catch ( IllegalAccessException e ) {
			throw new OkapiFilterCreationException("Cannot instantiate filter.", e);
		}
		catch ( ClassNotFoundException e ) {
			throw new OkapiFilterCreationException("Cannot instantiate filter.", e);
		}
		// Get the available configurations for this filter
		List<FilterConfiguration> list = filter.getConfigurations();
		// Add the configurations to the mapper
		for ( FilterConfiguration config : list ) {
			configMap.put(config.configId, config);
		}
	}

	public void addConfiguration (FilterConfiguration config) {
		configMap.put(config.configId, config);
	}

	public IFilter createFilter (String configId) {
		return createFilter(configId, null);
	}
	
	public IFilter createFilter (String configId,
		IFilter existingFilter)
	{
		// Get the configuration object for the given configId
		FilterConfiguration fc = configMap.get(configId);
		if ( fc == null ) return null;
		
		// Instantiate the filter (or re-use one)
		IFilter filter = instantiateFilter(fc, existingFilter);
		
		// Always load the parameters (if there are parameters)
		if ( fc.parameters != null ) {
			IParameters params = filter.getParameters();
			if ( params == null ) {
				throw new RuntimeException(String.format(
					"Cannot create default parameters for '%s'.", fc.configId));
			}
			if ( fc.custom ) {
				params = getCustomParameters(fc, filter);
			}
			else {
				// Note that we cannot assume the parameters are the same
				// if we re-used an existing filter, as we cannot compare the 
				// configuration identifiers
				URL url = filter.getClass().getResource(fc.parameters);
				try {
					params.load(url.toURI(), false);
				}
				catch ( URISyntaxException e ) {
					throw new RuntimeException(String.format(
						"URI syntax error '%s'.", url.getPath()));
				}
			}
		}
		
		return filter;
	}

	public IParameters getParameters (FilterConfiguration config) {
		return getParameters(config, null);
	}
	
	public IParameters getParameters (FilterConfiguration config,
		IFilter existingFilter)
	{
		if ( config.parameters == null ) return null; // Nothing to load
		
		IFilter filter = instantiateFilter(config, existingFilter);
		IParameters params = filter.getParameters();
		if ( params == null ) {
			throw new RuntimeException(String.format(
				"Cannot create default parameters for '%s'.", config.configId));
		}
		if ( config.custom ) {
			params = getCustomParameters(config, filter);
		}
		else {
			// Note that we cannot assume the parameters are the same
			// if we re-used an existing filter, as we cannot compare the 
			// configuration identifiers
			URL url = filter.getClass().getResource(config.parameters);
			try {
				params.load(url.toURI(), false);
			}
			catch ( URISyntaxException e ) {
				throw new RuntimeException(String.format(
					"URI syntax error '%s'.", url.getPath()));
			}
		}
		return params;
	}
	
	@Override
	public IParametersEditor createParametersEditor (String configId) {
		return createParametersEditor(configId, null);
	}
	
	public IParametersEditor createParametersEditor (String configId,
		IFilter existingFilter)
	{
		FilterConfiguration fc = configMap.get(configId);
		if ( fc == null ) return null;
		if ( fc.parameters == null ) return null;

		IFilter filter = instantiateFilter(fc, existingFilter);

		// Get the default parameters object
		IParameters params = filter.getParameters();
		if ( params == null ) {
			return null; // This filter does not have parameters
		}
		
		// Lookup the editor class based on the parameters class
		String editorClass = editorMap.get(params.getClass().getName());
		if ( editorClass == null ) return null;
		
		// Else: instantiate the editor
		IParametersEditor editor = null;
		try {
			editor = (IParametersEditor)Class.forName(editorClass).newInstance();
		}
		catch ( InstantiationException e ) {
			throw new OkapiEditorCreationException(
				String.format("Cannot instantiate the editor '%s'", editorClass), e);
		}
		catch ( IllegalAccessException e ) {
			throw new OkapiEditorCreationException(
				String.format("Cannot instantiate the editor '%s'", editorClass), e);
		}
		catch ( ClassNotFoundException e ) {
			throw new OkapiEditorCreationException(
				String.format("Cannot instantiate the editor '%s'", editorClass), e);
		}
		return editor;
	}

	public FilterConfiguration getConfiguration (String configId) {
		return configMap.get(configId);
	}

	public Iterator<FilterConfiguration> getAllConfigurations () {
		return configMap.values().iterator();
	}
	
	public List<FilterConfiguration> getMimeConfigurations (String mimeType) {
		ArrayList<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
		for ( FilterConfiguration config : configMap.values() ) {
			if ( config.mimeType != null ) {
				if ( config.equals(mimeType) ) {
					list.add(config);
				}
			}
		}
		return list;
	}

	public List<FilterConfiguration> getFilterConfigurations(String filterClass) {
		ArrayList<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
		for ( FilterConfiguration config : configMap.values() ) {
			if ( config.filterClass != null ) {
				if ( config.equals(filterClass) ) {
					list.add(config);
				}
			}
		}
		return list;
	}

	public FilterConfiguration getDefaultConfiguration (String mimeType) {
		for ( FilterConfiguration config : configMap.values() ) {
			if ( config.mimeType != null ) {
				if ( config.mimeType.equals(mimeType) ) {
					return config;
				}
			}
		}
		return null;
	}

	public void removeConfiguration (String configId) {
		configMap.remove(configId);
	}

	public void removeConfigurations (String filterClass) {
		Entry<String, FilterConfiguration> entry;
		Iterator<Entry<String, FilterConfiguration>> iter = configMap.entrySet().iterator();
		while ( iter.hasNext() ) {
			entry = iter.next();
			if ( entry.getValue().filterClass.equals(filterClass) ) {
				iter.remove();
			}
		}
	}

	public IParameters getCustomParameters (FilterConfiguration config) {
		return getCustomParameters(config, null);
	}

	/**
	 * Gets the parameters for a given custom filter configuration. This
	 * default implementation gets the custom data from a file located
	 * in the current directory at the time the method is called. 
	 */
	public IParameters getCustomParameters (FilterConfiguration config,
		IFilter existingFilter)
	{
		// Instantiate a filter (or re-use one)
		IFilter filter = instantiateFilter(config, existingFilter);

		// Get the default parameters object
		IParameters params = filter.getParameters();
		if ( params == null ) {
			return null; // This filter does not have parameters
		}

		// Load the provided parameter file
		// In this implementation we assume it is in the current directory
		File file = new File(config.parameters);
		params.load(file.toURI(), false);
		return params;
	}

	public void deleteCustomParameters (FilterConfiguration config) {
		// In this implementation we assume it is in the current directory
		File file = new File(config.parameters);
		file.delete();
	}

	public void saveCustomParameters (FilterConfiguration config,
		IParameters params)
	{
		// In this implementation we assume it is in the current directory
		params.save(config.parameters);
	}

	public void clearConfigurations (boolean customOnly) {
		if ( customOnly ) {
			Entry<String, FilterConfiguration> entry;
			Iterator<Entry<String, FilterConfiguration>> iter = configMap.entrySet().iterator();
			while ( iter.hasNext() ) {
				entry = iter.next();
				if ( entry.getValue().custom ) {
					iter.remove();
				}
			}
		}
		else {
			configMap.clear();
		}
	}

	/**
	 * Instantiate a filter from a given configuration, trying to re-use an existing one.
	 * @param config the configuration corresponding to the filter to load.
	 * @param existingFilter an optional existing filter we can try to reuse.
	 * @return the instance of the requested filter, or null if an error occurred.
	 * @throws OkapiFilterCreationException if the filter could not be instantiated.
	 */
	protected IFilter instantiateFilter (FilterConfiguration config,
		IFilter existingFilter)
	{
		IFilter filter = null;
		if ( existingFilter != null ) {
			if ( config.filterClass.equals(existingFilter.getClass().getName()) ) {
				filter = existingFilter;
			}
		}
		if ( filter == null ) {
			try {
				filter = (IFilter)Class.forName(config.filterClass).newInstance();
			}
			catch ( InstantiationException e ) {
				throw new OkapiFilterCreationException(
					String.format("Cannot instantiate afilter from the configuration '%s'", config.configId), e);
			}
			catch ( IllegalAccessException e ) {
				throw new OkapiFilterCreationException(
					String.format("Cannot instantiate afilter from the configuration '%s'", config.configId), e);
			}
			catch ( ClassNotFoundException e ) {
				throw new OkapiFilterCreationException(
					String.format("Cannot instantiate afilter from the configuration '%s'", config.configId), e);
			}
		}
		return filter;
	}

}
