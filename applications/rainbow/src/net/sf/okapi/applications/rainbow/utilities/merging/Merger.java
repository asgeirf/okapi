/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel                                          */
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

package net.sf.okapi.applications.rainbow.utilities.merging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.applications.rainbow.lib.FilterAccess;
import net.sf.okapi.applications.rainbow.lib.Utils;
import net.sf.okapi.applications.rainbow.packages.IReader;
import net.sf.okapi.applications.rainbow.packages.Manifest;
import net.sf.okapi.applications.rainbow.packages.ManifestItem;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipeline.ThrougputPipeBase;
import net.sf.okapi.common.resource.LocaleData;
import net.sf.okapi.common.resource.TextUnit;

public class Merger extends ThrougputPipeBase {

	private Manifest         manifest;
	private IReader          reader;
	private FilterAccess     fa;
	private final Logger     logger = LoggerFactory.getLogger("net.sf.okapi.logging");
	private boolean          skipNoTranslate;

	public Merger () {
		fa = new FilterAccess();

		// Get the location of the class source
		File file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
	    String rootFolder = file.getAbsolutePath();
	    // Remove the JAR file if running an installed version
	    if ( rootFolder.endsWith(".jar") ) rootFolder = Util.getDirectoryName(rootFolder);
	    // Remove the application folder in all cases
	    rootFolder = Util.getDirectoryName(rootFolder);
		String sharedFolder = Utils.getOkapiSharedFolder(rootFolder);

		//Load the FilterAccess list
		fa.loadList(sharedFolder + File.separator + "filters.xml");
	}

	public void initialize (Manifest manifest) {
		// Close any previous reader
		if ( reader != null ) {
			reader.closeDocument();
			reader = null;
		}
		// Set the manifest and the options
		this.manifest = manifest;
		skipNoTranslate = "omegat".equals(manifest.getPackageType());
	}
	
	public void merge (int docID) {
		try {
			ManifestItem item = manifest.getItem(docID);
			// Skip items not selected for merge
			if ( !item.selected() ) return;
			
			// File to merge
			String fileToMerge = manifest.getFileToMergePath(docID);
			// Instantiate a package reader of the proper type
			if ( reader == null ) {
				reader = (IReader)Class.forName(manifest.getReaderClass()).newInstance();
			}
			logger.info("Merging: " + fileToMerge);

			// Original and parameters files
			String originalFile = manifest.getRoot() + File.separator + manifest.getOriginalLocation()
				+ File.separator + String.format("%d.ori", docID);
			String paramsFile = manifest.getRoot() + File.separator + manifest.getOriginalLocation()
				+ File.separator + String.format("%d.fprm", docID);
			// Load the relevant filter
			fa.loadFilter(item.getFilterID(), paramsFile);
			
			reader.openDocument(fileToMerge);
			
			// Initializes the input
			InputStream input = new FileInputStream(originalFile);
			fa.inputFilter.initialize(input, originalFile, "TODO:filterSettings???", item.getInputEncoding(),
				manifest.getSourceLanguage(), manifest.getTargetLanguage());
			
			// Initializes the output
			String outputFile = manifest.getFileToGeneratePath(docID);
			Util.createDirectories(outputFile);
			OutputStream output = new FileOutputStream(outputFile);
			fa.outputFilter.initialize(output, item.getOutputEncoding(), manifest.getTargetLanguage());

			// Set the pipeline: inputFilter -> merger -> outputFilter 
			fa.inputFilter.setOutput(this);
			this.setOutput(fa.outputFilter);
			
			// Do it
			fa.inputFilter.process();
		}
		catch ( Exception e ) {
			// Log and move on to the next file
			logger.error("Merging error. " + e.getCause().getLocalizedMessage(), e);
		}
		finally {
			if ( fa.outputFilter != null ) fa.outputFilter.close();
			if ( reader != null ) reader.closeDocument();
			if ( fa.inputFilter != null ) fa.inputFilter.close();
		}
	}

	@Override
    public void endExtractionItem (TextUnit item) {
		processItem(item);
		if ( item.hasChild() ) {
			for ( TextUnit tu : item.childTextUnitIterator() ) {
				processItem(tu);
			}
		}

		// Call output filter
		super.endExtractionItem(item);
	}

	private void processItem (TextUnit item) {
		// Skip the non-translatable if they are not included in the package
		if ( skipNoTranslate && !item.isTranslatable() ) return;
			
		// Get item from the package document
		if ( !reader.readItem() ) {
			// Problem: 
			logger.warn("There is no more package item to merge (for id=\"{}\")",
				item.getID());
			// Keep the source
			return;
		}

		// Update the item if needed
		if ( item.isTranslatable() ) {
			TextUnit srcPkgItem = reader.getItem();
			
			if ( !item.getID().equals(srcPkgItem.getID()) ) {
				// Problem: different IDs
				logger.warn("ID mismatch: original item id=\"{}\" package item id=\"{}\"",
					item.getID(), srcPkgItem.getID());
				// Keep the source
				return;
			}

			if ( srcPkgItem.hasTarget() ) {
				if ( !item.hasTarget() ) {
					// Create the target entry for the output if it does not exist yet
					item.setTarget(new LocaleData(item));
				}
				// Set the codedText part of the content only. Do not modify the codes.
				//TODO: in-line could be clones: the code should come from the translation not the original then.
				try {
					item.getTargetContent().setCodedText(
						srcPkgItem.getTargetContent().getCodedText(),
						item.getSourceContent().getCodes());
				}
				catch ( RuntimeException e ) {
					logger.error("Error with item id=\"{}\".", item.getID());
					// Use the source instead, continue the merge
					item.setTarget(item.getSource());
				}
			}
			else { // No translation in package
				if ( !item.isEmpty() ) {
					logger.warn("Item id=\"{}\": No translation provided.", item.getID());
					item.setTarget(item.getSource());
				}
			}
		}
	}

}
