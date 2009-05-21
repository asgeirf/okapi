package net.sf.okapi.common.filters;

import java.util.List;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.exceptions.OkapiFilterCreationException;

public interface IFilterConfigurationMapper {

	/**
	 * Adds a new configuration to this mapper.
	 * @param config the configuration to add.
	 * @param mimeType the MIME type associated with the new configuration.
	 */
	public void addConfiguration (FilterConfiguration config, String mimeType);
	
	/**
	 * Removes a given configuration from this mapper.
	 * @param configId the identifier of the configuration to remove.
	 */
	public void removeConfiguration (String configId);
	
	/**
	 * Removes all configuration mappings from this mapper.
	 */
	public void clearConfigurations ();
	
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
	 * Gets a list of all FilterConfiguration objects for a given MIME type.
	 * @param mimeType mimeType MIME type to search for.
	 * @return a list of all FilterConfiguration objects found for the
	 * given MIME type (it may be empty).
	 */
	public List<FilterConfiguration> getConfigurations (String mimeType);

	/**
	 * Gets the parameters for a given custom filter configuration.
	 * This method provides a way for this mapper to implements how it retrieves
	 * custom filter parameters.  
	 * @param the custom configuration for which the method should return the 
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
	 * @param the custom configuration for which the method should return the 
	 * filter parameters.
	 * @return the parameters for the given custom filter configuration, or null
	 * if the parameters could not be provided, or if the corresponding filter does not have
	 * parameters.
	 * @throws OkapiFilterCreationException if the filter of the given configuration
	 * could not be created to load the parameters.
	 */
	public IParameters getCustomParameters (FilterConfiguration config);
	
}
