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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.okapi.common.DefaultFilenameFilter;
import net.sf.okapi.common.Util;

/**
 * Utilities for the web-service's project and file handling.
 */
public final class WorkspaceUtils {
	private static final String PLUGINS = "plugins";
	private static final String BATCH_CONF = "settings.bconf";
	private static final String EXTENSIONS_MAPPING = "extensions-mapping.txt";
	private static final String INPUT = "input";
	private static final String CONFIG = "config";
	private static final String OUTPUT = "output";
	private static final FilenameFilter DIRECTORY_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			File file = new File(dir.getAbsolutePath() + File.separator + name);
			return file.isDirectory();
		}
	};
	private static final FilenameFilter ANY_FILE_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			File file = new File(dir.getAbsolutePath() + File.separator + name);
			return file.isFile();
		}
	};
	private static final int BUFFER = 102400;

	public static final String BATCH_CONF_PARAM = "batchConfiguration";
	public static final String INPUT_FILE_PARAM = "inputFile";

	/**
	 * @return The directory where the local projects will be created and saved temporarily
	 */
	public static String getWorkingDirectory() {
		//TODO Don't re-load that every time!
		Configuration conf = loadConfig();
		return conf.getWorkingDirectory();
	}
	
	/**
	 * TODO update JavaDoc!
	 * 
	 * @return The user's configuration (if <code>System.getProperty("user.home") + "/okapi-longhorn-configuration.xml"</code>
	 * 		was found) or the default configuration
	 */
	private static Configuration loadConfig() {
		
		Configuration config = null;

		String workdirEnvVar = System.getProperty("LONGHORN_WORKDIR");
		String userHome = System.getProperty("user.home");
		File userConfig = new File(userHome + "/okapi-longhorn-configuration.xml");
		
		if (workdirEnvVar != null) {
			config = new Configuration(workdirEnvVar);
		}
		else if (userConfig.exists()) {
			try {
				config = new Configuration(new FileInputStream(userConfig));
			}
			catch (FileNotFoundException e) {
				// This should be impossible, because we checked for the existence of the file
				throw new RuntimeException(e);
			}
		}
		else {
			config = new Configuration();
		}
		
		return config;
	}

	/**
	 * @param projId The id of a local project
	 * @return The project's absolute path on the file system
	 * 		(without a trailing path separator)
	 */
	public static String getProjectPath(int projId) {
		
		return getWorkingDirectory() + File.separator + projId;
	}

	/**
	 * @param projId The id of a local project
	 * @return The project's batch configuration file
	 */
	public static File getBatchConfigurationFile(int projId) {
		
		return new File(getProjectPath(projId) + File.separator + BATCH_CONF);
	}
	
	/**
	 * @param projId The id of a local project
	 * @return The absolute path of the project's input files directory on the file system
	 * 		(without a trailing path separator)
	 */
	public static String getInputDirPath(int projId) {
		
		return getProjectPath(projId) + File.separator + INPUT;
	}
	
	/**
	 * @param projId The id of a local project
	 * @param filename The name of the file to return
	 * @return The input file with the specified file name that belongs to the project
	 */
	public static File getInputFile(int projId, String filename) {
		
		return new File(getInputDirPath(projId) + File.separator + filename);
	}
	
	/**
	 * @param projId The id of a local project
	 * @return All input files belonging to the project
	 */
	public static List<File> getInputFiles(int projId) {
		
		return getFilesRecursivly(new File(getInputDirPath(projId)));
	}
	
	/**
	 * @param projId The id of a local project
	 * @return A (string) list of the file names of the project's input files
	 */
	public static ArrayList<String> getInputFileNames(int projId) {
		
		Collection<File> files = getInputFiles(projId);
		return getFileNames(files, getInputDirPath(projId) + File.separator);
	}
	
	/**
	 * @param projId The id of a local project
	 * @return The absolute path of the project's output files directory on the file system
	 * 		(without a trailing path separator)
	 */
	public static String getOutputDirPath(int projId) {
		
		return getProjectPath(projId) + File.separator + OUTPUT;
	}
	
	/**
	 * @param projId The id of a local project
	 * @return The absolute path of the project's configuration files directory on the file system
	 * 		(without a trailing path separator)
	 */
	public static String getConfigDirPath(int projId) {
		
		return getProjectPath(projId) + File.separator + CONFIG;
	}

	/**
	 * @param projId The id of a local project
	 * @return The first file in the project's configuration directory with the extension ".pln" or null if none exists
	 */
	public static File getPipelineFile(int projId) {
		File[] pipelineFiles = getFilteredFiles(getConfigDirPath(projId), ".pln");
		if (pipelineFiles.length == 0)
			return null;
		return pipelineFiles[0];
	}

	/**
	 * @param projId The id of a local project
	 * @return The project's file extension-to-filter-configuration mapping file
	 */
	public static File getFilterMappingFile(int projId) {
		
		return new File(getConfigDirPath(projId) + File.separator + EXTENSIONS_MAPPING);
	}
	
	/**
	 * @param projId The id of a local project
	 * @param filename The name of the file to return
	 * @return The output file with the given file name that belongs to the project
	 */
	public static File getOutputFile(int projId, String filename) {
		
		return new File(getOutputDirPath(projId) + File.separator + filename);
	}
	
	/**
	 * @param projId The id of a local project
	 * @return All output files belonging to the project
	 */
	public static List<File> getOutputFiles(int projId) {
		
		return getFilesRecursivly(new File(getOutputDirPath(projId)));
	}
	
	/**
	 * @param projId The id of a local project
	 * @return A (string) list of the file names of the project's output files
	 */
	public static ArrayList<String> getOutputFileNames(int projId) {
		
		Collection<File> files = getOutputFiles(projId);
		return getFileNames(files, getOutputDirPath(projId) + File.separator);
	}
	
	/**
	 * @param files Any collection of files
	 * @param rootDir The path that is the starting point for the returned relative file paths (WITH trailing path separator)
	 * @return The names of the given files (including their extension and their relative path from the root directory with '/' as path separator)
	 */
	public static ArrayList<String> getFileNames(Collection<File> files, String rootDir) {
		
		ArrayList<String> relFilePaths = new ArrayList<String>();
		for (File file : files) {
			String relativePath = file.getAbsolutePath().substring(rootDir.length());
			relativePath = relativePath.replace("\\", "/");
			relFilePaths.add(relativePath);
		}
		return relFilePaths;
	}

	/**
	 * @return A list of all project ids that are currently in use (in numerical order)
	 */
	public static ArrayList<Integer> getProjectIds() {

		ArrayList<Integer> projectIds = new ArrayList<Integer>();
		File[] subDirs = getSubdirectories(getWorkingDirectory());
		
		if(subDirs == null)
			// The Directory has not yet been created
			return projectIds;
		
		Collection<File> directories = Arrays.asList(subDirs);
		
		for (File dir : directories) {
			if (PLUGINS.equals(dir.getName()))
				continue;
			projectIds.add(Integer.parseInt(dir.getName()));
		}
		
		// Sort list, so new project folders are at the end.
		// The OS may put 10 in between 1 and 2.
		Collections.sort(projectIds);
		
		return projectIds;
	}
	
	/**
	 * @return The highest project id currently in use, increased by 1
	 */
	public static int determineNewProjectId() {
		
		ArrayList<Integer> takenProjectIds = getProjectIds();
		
		if (takenProjectIds.isEmpty())
			return 1;
		
		// List is in numerical order, so we can simply increase the last value by 1
		Integer highestId = takenProjectIds.get(takenProjectIds.size() - 1);
		return highestId + 1;
	}
	
	
	/**
	 * Filter files in a directory by their extension.
	 * 
	 * @param directory - directory where files are located
	 * @param extension - the extension used to filter files
	 * @return - list of files matching the suffix, all files in the directory if suffix is null
	 */
	private static File[] getFilteredFiles(String directory, String extension) {
		File dir = new File(directory);
		return dir.listFiles(new DefaultFilenameFilter(extension));
	}
	
	
	/**
	 * Get all files in a directory (and it's sub-directories).
	 * 
	 * @param directory - root directory
	 * @return - list all files in that directory and it's sub-directories
	 */
	private static ArrayList<File> getFilesRecursivly(File dir) {
		
		ArrayList<File> allFiles = new ArrayList<File>();
		
		for (File subDir : dir.listFiles(DIRECTORY_FILTER)) {
			allFiles.addAll(getFilesRecursivly(subDir));
		}
		
		File[] files = dir.listFiles(ANY_FILE_FILTER);
		allFiles.addAll(Arrays.asList(files));
		return allFiles;
	}
	
	/**
	 * @param directory - directory where sub-directories are located
	 * @return - list of the directory's sub-directories or null if the directory does not exist
	 */
	private static File[] getSubdirectories(String directory) {
		File dir = new File(directory);
		File[] directories = dir.listFiles(DIRECTORY_FILTER);
		return directories;
	}

	public static File getOutputFilesAsArchive(int projId) throws IOException {
		
		File tempZip = File.createTempFile("Okapi-Longhorn-Files", ".zip");
		if (0 == zip(getOutputFiles(projId), getOutputDirPath(projId) + File.separator, tempZip))
			throw new RuntimeException("Error while addind the output files to an archive.");
		
		return tempZip;
	}
	
	public static void unzip(File zipFile, String targetDirectory) throws IOException {

		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			
			// Skip directories here. They will be created anyway.
			if (entry.isDirectory())
				continue;

			// Skip Mac specific directories
			if (entry.getName().contains("__MACOSX"))
				continue;
			
			int count;
			byte data[] = new byte[BUFFER];
			String targetFilePath = targetDirectory + File.separator + entry.getName();
			
			// Normalize separators
			targetFilePath = targetFilePath.replace("\\", File.separator);
			targetFilePath = targetFilePath.replace("/", File.separator);
			Util.createDirectories(targetFilePath);
			
			FileOutputStream fos = new FileOutputStream(targetFilePath);
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				fos.write(data, 0, count);
			}
			fos.flush();
			fos.close();
		}
		zis.close();
	}
	
	/**
	 * @param files Files to be added to the new zip archive
	 * @param rootDirectory Root directory of the files WITH trailing path separator (directories below will be in the archive)
	 * @param destZip The zip archive to be created
	 * @return 1 if the zip archive was created, 0 if some of the files were outside of the specified root directory
	 * @throws IOException
	 */
	public static int zip(Collection<File> files, String rootDirectory, File destZip) throws IOException {
		
		for (File file : files) {
			if (!file.getAbsolutePath().startsWith(rootDirectory))
				return 0;
		}
		
		FileOutputStream dest = new FileOutputStream(destZip);
		BufferedInputStream origin = null;
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		
		byte data[] = new byte[BUFFER];

		for (File file : files) {
			FileInputStream fi = new FileInputStream(file);
			origin = new BufferedInputStream(fi, BUFFER);
			String entryName = file.getAbsolutePath().substring(rootDirectory.length());
			ZipEntry entry = new ZipEntry(entryName);
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();
		}
		out.close();
		
		return 1;
	}
}
