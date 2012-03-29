/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.applications.longhorn.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.applications.rainbow.Project;
import net.sf.okapi.applications.rainbow.batchconfig.BatchConfiguration;
import net.sf.okapi.applications.rainbow.lib.LanguageManager;
import net.sf.okapi.applications.rainbow.pipeline.PipelineWrapper;
import net.sf.okapi.applications.rainbow.pipeline.StepInfo;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.plugins.PluginsManager;
import net.sf.okapi.filters.rainbowkit.Manifest;
import net.sf.okapi.filters.rainbowkit.RainbowKitFilter;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;
import net.sf.okapi.steps.rainbowkit.creation.Parameters;
import net.sf.okapi.steps.rainbowkit.postprocess.MergingStep;

public class ProjectUtils {
	private static final Logger LOGGER = Logger.getLogger(ProjectUtils.class.getName());
	private static final String CURRENT_PROJECT_PIPELINE = "currentProjectPipeline";

	public static int createNewProject() {
		
		int projId = WorkspaceUtils.determineNewProjectId();
		
		File workingDir = new File(WorkspaceUtils.getWorkingDirectory());
		if (!workingDir.exists())
			LOGGER.log(Level.INFO, "The working directory " + workingDir.getAbsolutePath() + " doesn't exist. " +
					"It will be created.");
		
		Util.createDirectories(WorkspaceUtils.getInputDirPath(projId) + File.separator);
		Util.createDirectories(WorkspaceUtils.getConfigDirPath(projId) + File.separator);
		Util.createDirectories(WorkspaceUtils.getOutputDirPath(projId) + File.separator);
		
		return projId;
	}

	public static void addBatchConfig(int projId, File tmpFile) {
		PluginsManager plManager = new PluginsManager();
		try {
		File targetFile = WorkspaceUtils.getBatchConfigurationFile(projId);
		Util.copyFile(tmpFile, targetFile);
		
			PipelineWrapper pipelineWrapper = preparePipelineWrapper(projId, plManager);
		
		// install batch configuration to config directory
		BatchConfiguration bconf = new BatchConfiguration();
		bconf.installConfiguration(targetFile.getAbsolutePath(),
				WorkspaceUtils.getConfigDirPath(projId), pipelineWrapper);
		} finally {
			plManager.releaseClassLoader();
	}
	}

	/**
	 * Loads the default filter configurations from Okapi and also the custom filter
	 * configurations and plug-ins from the project's configuration directory
	 * (where the batch configuration should have been installed to).
	 * 
	 * @param projId The id of a local project
	 * @param plug-in manager for this wrapper
	 * @return A PipelineWrapper using all available filter configurations and plug-ins
	 */
	private static PipelineWrapper preparePipelineWrapper(int projId, PluginsManager plManager) {				
		// Load local plug-ins
		plManager.discover(new File(WorkspaceUtils.getConfigDirPath(projId)), true);

		// Initialize filter configurations
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, false, true);
		fcMapper.addFromPlugins(plManager);
		fcMapper.setCustomConfigurationsDirectory(WorkspaceUtils.getConfigDirPath(projId));
		fcMapper.updateCustomConfigurations();

		// Load pipeline
		PipelineWrapper pipelineWrapper = new PipelineWrapper(fcMapper, WorkspaceUtils.getConfigDirPath(projId),
				plManager, WorkspaceUtils.getInputDirPath(projId), WorkspaceUtils.getInputDirPath(projId), null);
		pipelineWrapper.addFromPlugins(plManager);
		return pipelineWrapper;
	}

	public static void addInputFile(int projId, File tmpFile, String filename) {
		File targetFile = WorkspaceUtils.getInputFile(projId, filename);
		Util.createDirectories(targetFile.getAbsolutePath());
		Util.copyFile(tmpFile, targetFile);
	}

	public static void executeProject(int projId) throws IOException{
		executeProject(projId, null, null);
	}
	
	public static void executeProject(int projId, String sourceLanguage, String targetLanguage) throws IOException {
		PluginsManager plManager = new PluginsManager();
		try {
		// Create a new, empty rainbow project
		Project rainbowProject = new Project(new LanguageManager());
		rainbowProject.setCustomParametersFolder(WorkspaceUtils.getConfigDirPath(projId));
		rainbowProject.setUseCustomParametersFolder(true);
		
		if (sourceLanguage != null){
			rainbowProject.setSourceLanguage(new LocaleId(sourceLanguage, true));
		}
		if (targetLanguage != null){
			rainbowProject.setTargetLanguage(new LocaleId(targetLanguage, true));
		}
			
		// Create a pipeline wrapper
			PipelineWrapper pipelineWrapper = preparePipelineWrapper(projId, plManager);
		
		// Load pipeline into the rainbow project
		File pipelineFile = WorkspaceUtils.getPipelineFile(projId);
		pipelineWrapper.load(pipelineFile.getAbsolutePath());
		rainbowProject.setUtilityParameters(CURRENT_PROJECT_PIPELINE, pipelineWrapper.getStringStorage());

		// Set new input and output root
		rainbowProject.setInputRoot(0, WorkspaceUtils.getInputDirPath(projId), true);
		rainbowProject.setOutputRoot(WorkspaceUtils.getOutputDirPath(projId));
		rainbowProject.setUseOutputRoot(true);
		
		// Adjust paths from specific steps
		adjustStepsPaths(projId, pipelineWrapper);
		
		// Load mapping of filter configs to file extensions
		HashMap<String, String> filterConfigByExtension = loadFilterConfigurationMapping(projId);

		// Add files to project input list
		if (!isTKitMergePipeline(pipelineWrapper)) {
			addDocumentsToProject(projId, rainbowProject, filterConfigByExtension);
		}
		else {
			addManifestToProject(projId, rainbowProject, filterConfigByExtension);
		}
		
		rainbowProject.getPathBuilder().setUseExtension(false);

		// Execute pipeline
		pipelineWrapper.execute(rainbowProject);
		} finally {
			plManager.releaseClassLoader();
	}
	}

	/**
	 * Adjusts input and output paths from specific steps
	 * 
	 * @param pipelineWrapper The pipeline wrapper with the pipeline to be executed
	 */
	private static void adjustStepsPaths(int projId, PipelineWrapper pipelineWrapper) {
		for (StepInfo step : pipelineWrapper.getSteps()) {
			// TKit Creation
			if (step.stepClass.equals(ExtractionStep.class.getName())) {
				Parameters tkitParams = new Parameters();
				tkitParams.fromString(step.paramsData);
				tkitParams.setPackageDirectory(WorkspaceUtils.getOutputDirPath(projId));
				step.paramsData = tkitParams.toString();
			}
			//TKit Merge
			if (step.stepClass.equals(MergingStep.class.getName())) {
				net.sf.okapi.steps.rainbowkit.postprocess.Parameters tkitParams = 
						new net.sf.okapi.steps.rainbowkit.postprocess.Parameters();
				tkitParams.fromString(step.paramsData);
				tkitParams.setOverrideOutputPath(WorkspaceUtils.getOutputDirPath(projId));
				step.paramsData = tkitParams.toString();
			}
		}
	}

	/**
	 * @param pipelineWrapper The pipeline wrapper with the pipeline to execute
	 * @return true if one of the step is the Rainbow TKit Merging Step, false otherwise
	 */
	private static boolean isTKitMergePipeline(PipelineWrapper pipelineWrapper) {
		for (StepInfo step : pipelineWrapper.getSteps()) {
			if (step.stepClass.equals(MergingStep.class.getName()))
				return true;
		}
		return false;
	}

	/**
	 * Adds all input files from the local project directory with the specified projId to the Rainbow project.
	 * The HashMap will be used to assign filter configurations to the files (by the file's extension).
	 * 
	 * @param projId The id of the local project in which the input files are located
	 * @param rainbowProject The Rainbow project to which the input files shall be added
	 * @param filterConfigByExtension The mapping from file extensions (including dot, ".html" for example)
	 * 			to filter configurations (e.g. "okf_html@Customized")
	 */
	private static void addDocumentsToProject(int projId, Project rainbowProject,
			HashMap<String, String> filterConfigByExtension) {
		
		for (File inputFile : WorkspaceUtils.getInputFiles(projId)) {
			
			String extension = Util.getExtension(inputFile.getName());
			String filterConfigurationId = filterConfigByExtension.get(extension);

			int status = rainbowProject.addDocument(
					0, inputFile.getAbsolutePath(), null, null, filterConfigurationId, false);
			
			if (status == 1)
				throw new RuntimeException("Adding document " + inputFile.getName() + " to list of input files failed");
		}
	}

	/**
	 * Adds all Rainbow TKit manifest files from the local project directory with the specified projId to the Rainbow project.
	 * The HashMap will be used to assign the correct filter configuration.
	 * 
	 * @param projId The id of the local project in which the input files are located
	 * @param rainbowProject The Rainbow project to which the input files shall be added
	 * @param filterConfigByExtension The mapping from file extensions (including dot, ".html" for example)
	 * 			to filter configurations (e.g. "okf_html@Customized")
	 */
	private static void addManifestToProject(int projId, Project rainbowProject,
			HashMap<String, String> filterConfigByExtension) {
		
		for (File inputFile : WorkspaceUtils.getInputFiles(projId)) {
			
			String extension = Util.getExtension(inputFile.getName());
			String filterConfigurationId = filterConfigByExtension.get(extension);
			
			if (inputFile.getName().endsWith(RainbowKitFilter.RAINBOWKIT_PACKAGE_EXTENSION) ||					
				inputFile.getName().equals(Manifest.MANIFEST_FILENAME + Manifest.MANIFEST_EXTENSION)) {

				int status = rainbowProject.addDocument(
						0, inputFile.getAbsolutePath(), null, null, filterConfigurationId, false);
				
				if (status == 1)
					throw new RuntimeException("Adding document " + inputFile.getName() + " to list of input files failed");
				
				break;
			}
		}
	}

	/**
	 * Loads project's file extension to filter configuration mapping.
	 * 
	 * @param projId The id of the project that has the mapping file in it's configuration sub-folder
	 * @return A HashMap with the file extension (keys) to filter configuration (values) mapping
	 * @throws IOException If the file could not be read or it doesn't exist
	 */
	private static HashMap<String, String> loadFilterConfigurationMapping(int projId)
			throws IOException {
		
		BufferedReader fh = new BufferedReader(new FileReader(WorkspaceUtils.getFilterMappingFile(projId)));
		HashMap<String, String> filterConfigByExtension = new HashMap<String, String>();
		
		String s;
		
		while ((s = fh.readLine()) != null) {
			String fields[] = s.split("\t");
			String ext = fields[0];
			String fc = fields[1];
			
			filterConfigByExtension.put(ext, fc);
		}
		fh.close();

		return filterConfigByExtension;
	}

	public static void addInputFilesFromArchive(int projId, File zipFile) throws IOException {
		WorkspaceUtils.unzip(zipFile, WorkspaceUtils.getInputDirPath(projId));
	}
}
