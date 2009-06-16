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

import java.util.Iterator;
import java.util.List;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.exceptions.OkapiFilterCreationException;

/**
 * Common set of methods to manage filter configurations.
 */
public interface IFilterConfigurationMapper {

	/**
	 * Adds a new configuration to this mapper.
	 * @param config the configuration to add.
	 */
	public void addConfiguration (FilterConfiguration config);
	
	/**
	 * Removes a given configuration from this mapper.
	 * @param configId the identifier of the configuration to remove.
	 */
	public void removeConfiguration (String configId);
	
	/**
	 * Adds all the predefined configurations of a given filter to this mapper.
	 * @param filterClass the class name of the filter to lookup.
	 */
	public void addConfigurations (String filterClass);

	/**
	 * Removes all the configurations (predefined and custom) of a given
	 * filter from this mapper. 
	 * @param filterClass the class name of the filter to lookup.
	 */
	public void removeConfigurations (String filterClass);

	/**
	 * Removes configuration mappings from this mapper.
	 * @param customOnly true to clear only custom configuration, false to 
	 * clear all configurations from this mapper.
	 */
	public void clearConfigurations (boolean customOnly);
	
	/**
	 * Adds a new editor mapping to this mapper.
	 * @param editorClass the class name of the editor to add.
	 * @param parametersClass the class name of the parameters this editor can edit.
	 */
	public void addEditor (String editorClass,
		String parametersClass);
	
	/**
	 * Removes a given editor from this mapper.
	 * @param editorClass the class name of the editor to remove.
	 */
	public void removeEditor (String editorClass);
	
	/**
	 * Removes all editor mappings for this mapper.
	 */
	public void clearEditors ();
	
	/**
	 * Creates an instance of the filter for a given configuration identifier
	 * and loads its corresponding parameters.
	 * @param configId the configuration identifier to use for look-up.
	 * @param existingFilter an optional existing instance of a filter. This argument can be null.
	 * If this argument is not null, it is checked against the requested filter and re-use
	 * if the requested filter and the provided instance are the same. If the provided
	 * instance is re-used, its parameters are always re-loaded.
	 * Providing an existing instance of the requested filter may allow for better
	 * efficiency.  
	 * @return a new IFilter object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 * @throws OkapiFilterCreationException if the filter could not be created.
	 */
	public IFilter createFilter (String configId,
		IFilter existingFilter);

	/**
	 * Creates an instance of the filter for a given configuration identifier
	 * and loads its corresponding parameters.
	 * @param configId the configuration identifier to use for look-up.
	 * @return a new IFilter object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 * @throws OkapiFilterCreationException if the filter could not be created.
	 */
	public IFilter createFilter (String configId);
	
	/**
	 * Creates an instance of the filter's parameters editor for a given 
	 * configuration identifier.
	 * @param configId the configuration identifier to use for look-up.
	 * @param existingFilter an optional existing instance of a filter. This 
	 * argument can be null. If this argument is not null and matches the filter
	 * of the given configuration it is used instead of a temporay instance, to
	 * get an instance of the parameters object for which the editor is requested.
	 * @return a new IParametersEditor object for the given
	 * configuration identifier, or null if no editor is available or if
	 * the object could not be created.
	 * @throws OkapiFilterCreationException if a filter needed to be created
	 * and could not.
	 * @throws OkapiEditorCreationException if the editor could not be created.
	 */
	public IParametersEditor createParametersEditor (String configId,
		IFilter existingFilter);
	
	/**
	 * Creates an instance of the filter's parameters editor for a given 
	 * configuration identifier.
	 * @param configId the configuration identifier to use for look-up.  
	 * @return a new IParametersEditor object for the given
	 * configuration identifier, or null if no editor is available or if
	 * the object could not be created.
	 * @throws OkapiFilterCreationException if a filter needed to be created
	 * and could not.
	 * @throws OkapiEditorCreationException if the editor could not be created.
	 */
	public IParametersEditor createParametersEditor (String configId);
	
	/**
	 * Gets the FilterConfiguration object for a given configuration identifier.
	 * @param configId the configuration identifier to search for.
	 * @return the FilterConfiguration object for the given configuration identifier,
	 * or null if a match could not be found.
	 */
	public FilterConfiguration getConfiguration (String configId);
	
	/**
	 * Gets the first filter configuration for a given MIME type.
	 * @param mimeType MIME type to search for.
	 * @return the filter configuration for the given MIME type.
	 */
	public FilterConfiguration getDefaultConfiguration (String mimeType);
	
	/**
	 * Gets an iterator on all configurations objects for this mapper.
	 * @return an iterator on all configurations for this mapper.
	 */
	public Iterator<FilterConfiguration> getAllConfigurations ();
	
	/**
	 * Gets a list of all FilterConfiguration objects for a given MIME type.
	 * @param mimeType mimeType MIME type to search for.
	 * @return a list of all FilterConfiguration objects found for the
	 * given MIME type (the list may be empty).
	 */
	public List<FilterConfiguration> getMimeConfigurations (String mimeType);

	/**
	 * Gets a list of all FilterConfiguration objects for a given filter class.
	 * @param filterClass the class name of the filter to search for.
	 * @return a list of all FilterConfiguration objects found for the
	 * given filter class name (the list may be empty).
	 */
	public List<FilterConfiguration> getFilterConfigurations (String filterClass);

	public IParameters getParameters (FilterConfiguration config);
	
	public IParameters getParameters (FilterConfiguration config,
		IFilter existingFilter);

	/**
	 * Gets the parameters for a given custom filter configuration.
	 * This method provides a way for this mapper to implements how it retrieves
	 * custom filter parameters.  
	 * @param config the custom configuration for which the method should return the 
	 * filter parameters.
	 * @param existingFilter optional existing instance of the filter for the given
	 * configuration. this argument can be null. If it not null, the provided filter
	 * may be used to load the parameters (if it matches the appropriate class).
	 * Providing this argument may allow the method to be more efficient by not 
	 * creating a temporary filter to get an instance of the parameters to load. 
	 * @return the parameters for the given custom filter configuration, or null
	 * if the parameters could not be provided, or if the corresponding filter does not have
	 * parameters.
	 * @throws OkapiFilterCreationException if the filter of the given configuration
	 * could not be created to load the parameters.
	 */
	public IParameters getCustomParameters (FilterConfiguration config,
		IFilter existingFilter);
	
	/**
	 * Gets the parameters for a given custom filter configuration.
	 * This method provides a way for this mapper to implements how it retrieves
	 * custom filter parameters.  
	 * @param config the custom configuration for which the method should return the 
	 * filter parameters.
	 * @return the parameters for the given custom filter configuration, or null
	 * if the parameters could not be provided, or if the corresponding filter does not have
	 * parameters.
	 * @throws OkapiFilterCreationException if the filter of the given configuration
	 * could not be created to load the parameters.
	 */
	public IParameters getCustomParameters (FilterConfiguration config);

	public void saveCustomParameters (FilterConfiguration config, IParameters params);
	
	public void deleteCustomParameters (FilterConfiguration config);

}
