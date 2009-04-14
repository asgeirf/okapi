/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.applications.rainbow;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.applications.rainbow.lib.FilterAccess;
import net.sf.okapi.applications.rainbow.lib.ILog;
import net.sf.okapi.applications.rainbow.plugins.PluginItem;
import net.sf.okapi.applications.rainbow.plugins.PluginsAccess;
import net.sf.okapi.applications.rainbow.utilities.CancelEvent;
import net.sf.okapi.applications.rainbow.utilities.CancelListener;
import net.sf.okapi.applications.rainbow.utilities.IFilterDrivenUtility;
import net.sf.okapi.applications.rainbow.utilities.ISimpleUtility;
import net.sf.okapi.applications.rainbow.utilities.IUtility;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.ui.Dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class UtilityDriver implements CancelListener {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private ILog log;
	private Project prj;
	private FilterAccess fa;
	private IFilter filter;
	private IUtility utility;
	private IParametersEditor editor;
	private PluginItem pluginItem;
	private PluginsAccess plugins;
	private String outputFolder;
	private boolean stopProcess;
	private IHelp help;
	private boolean canPrompt;
	
	public UtilityDriver (ILog log,
		FilterAccess fa,
		PluginsAccess plugins,
		IHelp help,
		boolean canPrompt)
	{
		this.log = log;
		this.fa = fa;
		this.plugins = plugins;
		this.help = help;
		this.canPrompt = canPrompt;
	}
	
	/**
	 * Gets the current utility.
	 * @return The last utility loaded, or null.
	 */
	public IUtility getUtility () {
		return utility;
	}

	public void setData (Project project,
		String utilityName) 
	{
		try {
			prj = project;
			if ( !plugins.containsID(utilityName) )
				throw new RuntimeException("Utility not found: "+utilityName);
			pluginItem = plugins.getItem(utilityName);
			utility = (IUtility)Class.forName(pluginItem.pluginClass).newInstance();
			// Feedback event handling
			utility.addCancelListener(this);
			
			if ( pluginItem.editorClass.length() > 0 ) {
				editor = (IParametersEditor)Class.forName(pluginItem.editorClass).newInstance();
			}
			else editor = null;

			if ( utility.hasParameters() ) {
				// Get any existing parameters for the utility in the project
				String tmp = prj.getUtilityParameters(utility.getName());
				if (( tmp != null ) && ( tmp.length() > 0 )) {
					utility.getParameters().fromString(tmp);
				}
			}
		}
		catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		}
		catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean checkParameters (Shell shell) {
		try {
			if ( pluginItem == null ) return false;
			// If there are no options to ask for,
			// ask confirmation to launch the utility
			if ( utility.hasParameters() ) {
				// Invoke the editor if there is one
				if ( editor != null ) {
					if ( !editor.edit(utility.getParameters(), shell, help, prj.getProjectFolder()) ) return false;
					// Save the parameters in memory
					prj.setUtilityParameters(utility.getName(),
						utility.getParameters().toString());
				}
			}
			else {
				MessageBox dlg = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
				dlg.setMessage(String.format("You are about to execute the utility: %s\nDo you want to proceed?",
					pluginItem.name));
				dlg.setText(Util.getNameInCaption(shell.getText()));
				if ( dlg.open() != SWT.YES ) return false;
			}
		}
		catch ( Exception e ) {
			Dialogs.showError(shell, e.getMessage(), null);
			return false;
		}
		return true;
	}
	
	public void execute (Shell shell) {
		try {
			log.beginTask(pluginItem.name);
			stopProcess = false;

			// Set the run-time parameters
			utility.setFilterAccess(fa, prj.getParametersFolder());
			utility.setContextUI(shell, help, "rainbow="+Res.getString("VERSION"),
				prj.getProjectFolder(), canPrompt);
			if ( utility.needsRoots() ) {
				utility.setRoots(prj.getInputRoot(0), prj.buildOutputRoot(0));
			}
			utility.setOptions(prj.getSourceLanguage(), prj.getTargetLanguage());
			
			// All is initialized, now run the pre-process 
			utility.preprocess();
			
			// Last check to warning for empty list
			if ( prj.getList(0).size() == 0 ) {
				log.warning("There is no input document.");
			}

			// Process each input file
			int f = -1;
			for ( Input item : prj.getList(0) ) {
				f++;
				log.message("\n-- Input: "+item.relativePath);

				// Initialize the main input
				utility.resetLists();
				String inputPath = prj.getInputRoot(0) + File.separator + item.relativePath;
				utility.addInputData(inputPath, prj.buildSourceEncoding(item), item.filterSettings);
				// Initialize the main output
				String outputPath = prj.buildTargetPath(0, item.relativePath);
				utility.addOutputData(outputPath, prj.buildTargetEncoding(item));

				// Add input/output data from other input lists if requested
				for ( int j=1; j<prj.inputLists.size(); j++ ) {
					// Does the utility requests this list?
					if ( j >= utility.requestInputCount() ) break; // No need to loop more
					// Do we have a corresponding input?
					if ( prj.inputLists.get(j).size() > f ) {
						// Data is available
						Input addItem = prj.getList(j).get(f);
						// Input
						utility.addInputData(
							prj.getInputRoot(j) + File.separator + addItem.relativePath,
							prj.buildSourceEncoding(addItem),
							addItem.filterSettings);
						// Output
						utility.addOutputData(
							prj.buildTargetPath(j, addItem.relativePath),
							prj.buildTargetEncoding(addItem));
					}
					// Else: don't add anything
					// The lists will return null and that is up to the utility to check.
				}
				
				// Executes the utility
				if ( utility.isFilterDriven() ) {
					((IFilterDrivenUtility)utility).processFilterInput();
				}
				else {
					((ISimpleUtility)utility).processInput();
				}
				
				// Handle user cancellation
				if ( stopProcess ) break;
			}			
			
			// All is done, now run the post-process 
			utility.postprocess();
		}
		catch ( Throwable e ) {
			if ( filter != null ) filter.close();
			if ( utility != null ) utility.postprocess();
			logger.log(Level.SEVERE, "Error with utility.", e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			logger.info(sw.toString());
		}
		finally {
			if ( stopProcess ) {
				logger.warning("Process interrupted by user.");
			}
			if ( utility != null ) {
				outputFolder = utility.getFolderAfterProcess();
			}
			log.endTask(null);
		}
	}
	
	String getFolderAfterProcess () {
		return outputFolder;
	}

	public void cancelOccurred (CancelEvent event) {
		stopProcess = true;
		if ( filter != null ) filter.cancel();
	}

}
