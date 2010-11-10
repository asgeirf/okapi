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
===========================================================================*/

package net.sf.okapi.steps.simplekit.xliff;

import java.io.File;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.steps.simplekit.common.BasePackageWriter;
import net.sf.okapi.steps.simplekit.common.ManifestItem;
import net.sf.okapi.steps.simplekit.creation.Parameters;

/**
 * Implements {@link IPackageWriter} for generic XLIFF translation packages.
 */
class XLIFFPackageWriter extends BasePackageWriter {

	private Parameters params;
	private XLIFFWriter xlfWriter;
	
	public XLIFFPackageWriter () {
		super();
		params = new Parameters();
	}
	
	@Override
	public String getPackageType () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getReaderClass () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close () {
		if ( xlfWriter != null ) {
			xlfWriter.close();
			xlfWriter = null;
		}
	}

	@Override
	public String getName () {
		return getClass().getName();
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public Event handleEvent (Event event) {
		switch ( event.getEventType() ) {
		case START_DOCUMENT:
			processStartDocument(event.getStartDocument());
			break;
		case END_DOCUMENT:
			processEndDocument();
			break;
		case START_SUBDOCUMENT:
			processStartSubDocument((StartSubDocument)event.getResource());
			break;
		case END_SUBDOCUMENT:
			processEndSubDocument();
			break;
		case START_GROUP:
			processStartGroup(event.getStartGroup());
			break;
		case END_GROUP:
			processEndGroup();
			break;
		case TEXT_UNIT:
			processTextUnit(event.getTextUnit());
			break;
		}
		return event;
	}

	@Override
	public void setParameters (IParameters params) {
		params = (Parameters)params;
	}
	
	private void processStartDocument (StartDocument resource) {
		String xlfPath = manifest.getRoot() + File.separator
			+ ((manifest.getSourceLocation().length() == 0 ) ? "" : (manifest.getSourceLocation() + File.separator))
			+ relativeWorkPath;
		
		String skeletonPath = null;
		if ( params.getUseManifest() ) {
			skeletonPath = manifest.getRoot() + File.separator
				+ (Util.isEmpty(manifest.getSkeletonLocation()) ? "" : manifest.getSkeletonLocation());
		}
		else {
			skeletonPath = manifest.getRoot() + File.separator
				+ (Util.isEmpty(manifest.getSkeletonLocation()) ? "" : (manifest.getSkeletonLocation() + File.separator)) 
				+ relativeSourcePath + ".skl";
		}
		// Create the XLIFF document
		xlfWriter.create(xlfPath, skeletonPath, resource.getLocale(), trgLoc,
			resource.getMimeType(), relativeSourcePath, params.getMessage());
	}

	private void processEndDocument () {
		// Finish and close the XLIFF document
		xlfWriter.close();
		// Add the document to the manifest
		manifest.addDocument(docID, relativeWorkPath, relativeSourcePath,
			relativeTargetPath, sourceEncoding, targetEncoding, filterId,
			ManifestItem.POSPROCESSING_TYPE_DEFAULT);
		// Create the skeleton corresponding to the XLIFF
		createFilesForMerging();
	}

	private void processStartSubDocument (StartSubDocument resource) {
		xlfWriter.writeStartFile(resource.getName(), resource.getMimeType(), resource.getName());		
	}
	
	private void processEndSubDocument () {
		xlfWriter.writeEndFile();
	}

	private void processStartGroup (StartGroup resource) {
		xlfWriter.writeStartGroup(resource.getId(), resource.getName(), resource.getType());
	}
	
	private void processEndGroup () {
		xlfWriter.writeEndGroup();
	}
	
	private void processTextUnit (TextUnit tu) {
		// Write the XLIFF trans-unit
		xlfWriter.writeTextUnit(tu);
		// Write out TMX entries
		super.writeTMXEntries(tu);
	}
}
