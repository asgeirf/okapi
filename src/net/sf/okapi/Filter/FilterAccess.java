/*===========================================================================*/
/* Copyright (C) 2008 ENLASO Corporation, Okapi Development Team             */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.Filter;

import java.io.File;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.okapi.Library.Base.FilterSettingsMarkers;
import net.sf.okapi.Library.Base.ILog;
import net.sf.okapi.Library.Base.IParameters;
import net.sf.okapi.Library.Base.IParametersEditor;
import net.sf.okapi.Library.Base.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FilterAccess {
	
	private ILog                                 log;
	private Hashtable<String, FilterAccessItem>  m_htFilters;
	private IFilter                              m_Flt;
	private IParametersEditor                    m_Editor;
	private String                               currentClass;
	
	/**
	 * Construct a filter settings string.
	 * @param filterID Filter identifier (cannot be null nor empty).
	 * @param paramsName Name of the parameters file (can be null or empty).
	 * @return Filter settings string.
	 */
	static public String buildFilterSettingsType1 (String filterID,
		String paramsName)
	{
		String sTmp = filterID;
		if (( paramsName != null ) && ( paramsName.length() > 0 ))
			sTmp += (FilterSettingsMarkers.PARAMETERSSEP + paramsName);
		return sTmp;
	}

	/**
	 * Splits a filter settings type 1 string into its different components, including
	 * the full path of the parameters file.
	 * A type 1 filter settings string is: filterID@paramatersName
	 * @param projectParamsFolder The project folder where the parameters files are stored.
	 * @param filterSettings The setting string to split.
	 * @return An array of 4 strings: 0=folder, 1=filter id, 2=parameters name
	 * and 3=full parameters file path (folder + parameters name + extension).
	 */
	static public String[] splitFilterSettingsType1 (String projectParamsFolder,
		String filterSettings) {
		String[] aOutput = new String[4];
		for ( int i=0; i<4; i++ ) aOutput[i] = "";

		if (( filterSettings == null ) || ( filterSettings.length() == 0 ))
			return aOutput;

		// Expand the parameters part into full path
		aOutput[3] = projectParamsFolder + File.separator + filterSettings
			+ FilterSettingsMarkers.PARAMETERS_FILEEXT;
		
		// Get the directory
		File F = new File(aOutput[3]);
		aOutput[0] = F.getParent();
		String sTmp;
		if ( aOutput[0] == null ) aOutput[0] = "";
		if ( aOutput[0].length() > 0 )
			sTmp = F.getName();
		else
			sTmp = aOutput[3];

		// Get the parameters name
		int n;
		if ( (n = sTmp.indexOf(FilterSettingsMarkers.PARAMETERSSEP)) > -1 ) {
			if ( n < sTmp.length()-1 ) {
				aOutput[2] = sTmp.substring(n+1);
				aOutput[2] = Utils.removeExtension(aOutput[2]);
			}
			sTmp = sTmp.substring(0, n);
		}

		// Get the filter identifier
		aOutput[1] = Utils.removeExtension(sTmp);
		
		return aOutput;
	}

	public FilterAccess (ILog newLog) {
		log = newLog;
		m_htFilters = new Hashtable<String, FilterAccessItem>();
	}
	
	public ILog getLog () {
		return log;
	}
	
	/**
	 * Gets an IFilter interface to the filter currently loaded.
	 * @return The IFilter interface, or null if no filter is loaded.
	 */
	public IFilter getFilter () {
		return m_Flt;
	}
	
	public String getFilterIdentifier () {
		if ( m_Flt == null ) return null;
		else return m_Flt.getIdentifier();
	}
	
	/**
	 * Loads the list of accessible filters.
	 * The list is stored in an XML file of the following format:
	 * <okapiFilters>
	 *  <filter id="okf_json"
	 *   filterClass="net.sf.okapi.Filter.JSON.Filter">
	 *   editorClass="net.sf.okapi.Filter.JSON.ParametersForm"
	 *  >JSON files</filter>
	 * </okapiFilters>
	 * @param p_sPath Full path of the list file to load.
	 */
	public void loadList (String p_sPath)
		throws Exception
	{
		try {
			currentClass = null;
			DocumentBuilderFactory Fact = DocumentBuilderFactory.newInstance();
			Fact.setValidating(false);
			Document Doc = Fact.newDocumentBuilder().parse(new File(p_sPath));
			
			NodeList NL = Doc.getElementsByTagName("filter");
			m_htFilters.clear();
			FilterAccessItem FAI;
			for ( int i=0; i<NL.getLength(); i++ ) {
				Node N = NL.item(i).getAttributes().getNamedItem("id");
				if ( N == null ) throw new Exception("The attribute 'id' is missing.");
				FAI = new FilterAccessItem();
				String sID = N.getTextContent();
				N = NL.item(i).getAttributes().getNamedItem("filterClass");
				if ( N == null ) throw new Exception("The attribute 'filterClass' is missing.");
				FAI.filterClass = N.getTextContent();
				N = NL.item(i).getAttributes().getNamedItem("editorClass");
				if ( N != null ) FAI.editorClass = N.getTextContent();
				m_htFilters.put(sID, FAI);
			}
		}
		catch ( Exception E ) {
			throw E;
		}
	}
	
	/**
	 * Loads a filter and its parameters (if necessary). If the filter is the 
	 * filter currently loaded, it is not re-loaded. 
	 * @param filterID Identifier of the filter to load.
	 * @param paramPath Full path of the parameters file to load. Use null
	 * for not loading any parameters file.
	 */
	public void loadFilter (String filterID,
		String paramPath)
		throws Exception
	{
		try {
			// If the filter ID starts with NNN. (e.g. 123.okf_xml...)
			// we remove the NNN. part. That part is reserved for multi-file storage info
			if ( Character.isDigit(filterID.charAt(0)) ) {
				int n = filterID.indexOf('.');
				if ( n != -1 ) filterID = filterID.substring(n+1);
			}

			// Map the ID to the class, and instantiate the filter
			if ( !m_htFilters.containsKey(filterID) )
				throw new Exception(String.format(Res.getString("UNDEF_FILTERID"), filterID)); 

			// Load if not already done
			boolean bLoad = true;
			if (( m_Flt != null ) && ( currentClass != null )) {
				bLoad = !currentClass.equals(m_htFilters.get(filterID).filterClass);
			}
			if ( bLoad ) {
				m_Flt = (IFilter)Class.forName(m_htFilters.get(filterID).filterClass).newInstance();
				m_Flt.initialize(log);
				currentClass = m_htFilters.get(filterID).filterClass;
			}

			// Load the parameters
			if ( paramPath != null ) {
				m_Flt.getParameters().load(paramPath, false);
			}
		}
		catch ( Exception E ) {
			throw E;
		}
	}

	public void loadFilterFromFilterSettingsType1 (String projectParamsFolder,
		String filterSettings)
		throws Exception
	{
		String[] aRes = splitFilterSettingsType1(projectParamsFolder, filterSettings);
		loadFilter(aRes[1], aRes[3]);
	}
	
	public void loadEditor (String filterID)
		throws Exception
	{
		try {
			// If the filter ID starts with NNN. (e.g. 123.okf_xml...)
			// we remove the NNN. part. That part is reserved for multi-file storage info
			if ( Character.isDigit(filterID.charAt(0)) ) {
				int n = filterID.indexOf('.');
				if ( n != -1 ) filterID = filterID.substring(n+1);
			}
	
			// Map the ID to the class, and instantiate the filter
			if ( !m_htFilters.containsKey(filterID) )
				throw new Exception(String.format(Res.getString("UNDEF_FILTERID"), filterID));
			m_Editor = (IParametersEditor)Class.forName(m_htFilters.get(filterID).editorClass).newInstance();
		}
		catch ( Exception E ) {
			throw E;
		}
	}
	
	/**
	 * Edits the parameters for a given filter. On success the 
	 * content of the parameters object is changed.
	 * @param filterID The identifier of the filter to use to edit the parameters.
	 * @param paramObject The parameter to edit.
	 * @param uiContext Object used by the editor to integrate itself with the 
	 * UI that called the method. In this implementation pass the SWT Shell object 
	 * of the calling application.
	 * @return True if the edit was successful, false if the use canceled
	 * or if an error occurred.
	 */
	public boolean editParameters (String filterID,
		IParameters paramObject,
		Object uiContext)
	{
		try {
			loadEditor(filterID);
			if ( m_Editor == null ) return false;
			return m_Editor.edit(paramObject, uiContext);
		}
		catch ( Exception E ) {
			return false;
		}
	}
	
}
