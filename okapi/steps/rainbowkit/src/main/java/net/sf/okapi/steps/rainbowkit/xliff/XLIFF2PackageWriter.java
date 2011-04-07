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

package net.sf.okapi.steps.rainbowkit.xliff;

import java.io.File;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.filterwriter.XLIFFContent;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.rainbowkit.Manifest;
import net.sf.okapi.filters.rainbowkit.MergingInfo;
import net.sf.okapi.lib.xliff.Fragment;
import net.sf.okapi.lib.xliff.Unit;
import net.sf.okapi.lib.xliff.XLIFFWriter;
import net.sf.okapi.steps.rainbowkit.common.BasePackageWriter;

public class XLIFF2PackageWriter extends BasePackageWriter {

	private XLIFFWriter writer;
	private XLIFFContent fmt;

	public XLIFF2PackageWriter () {
		super(Manifest.EXTRACTIONTYPE_XLIFF2);
		fmt = new XLIFFContent();
	}

	@Override
	protected void processStartBatch () {
		manifest.setSubDirectories("original", "work", "work", "done", null, false);
		setTMXInfo(true, null, null, null, null);
		super.processStartBatch();
	}
	
	@Override
	protected void processStartDocument (Event event) {
		super.processStartDocument(event);
		
		writer = new XLIFFWriter();

//		writer.setOptions(manifest.getTargetLocale(), "UTF-8");
		MergingInfo item = manifest.getItem(docId);
		String path = manifest.getSourceDirectory() + item.getRelativeInputPath() + ".xlf";
		
		writer.create(new File(path));

		// Set the writer's options
		// Get the options from the parameters
		Options options = new Options();
		if ( !Util.isEmpty(params.getWriterOptions()) ) {
			options.fromString(params.getWriterOptions());
		}
//		//TODO: Would be easier to use IParameters in XLIFFWriter.
//		writer.setPlaceholderMode(options.getPlaceholderMode());
//		writer.setCopySource(options.getCopySource());
//		writer.setIncludeAltTrans(options.getIncludeAltTrans());
//		writer.setSetApprovedasNoTranslate(options.getSetApprovedAsNoTranslate());
//		writer.setIncludeNoTranslate(options.getIncludeNoTranslate());
		
		StartDocument sd = event.getStartDocument();
//		writer.create(path, null, manifest.getSourceLocale(), manifest.getTargetLocale(),
//			sd.getMimeType(), item.getRelativeInputPath(), null);
		writer.writeStartDocument();
	}
	
	@Override
	protected void processEndDocument (Event event) {
		writer.writeEndDocument();
		writer.close();
		writer = null;
		super.processEndDocument(event);
	}

	@Override
	protected void processStartSubDocument (Event event) {
//		writer.handleEvent(event);
	}
	
	@Override
	protected void processEndSubDocument (Event event) {
//		writer.handleEvent(event);
	}
	
	@Override
	protected void processStartGroup (Event event) {
//		writer.handleEvent(event);
	}
	
	@Override
	protected void processEndGroup (Event event) {
//		writer.handleEvent(event);
	}
	
	@Override
	protected void processTextUnit (Event event) {
		
		Unit unit = toXLIFF2Unit(event.getTextUnit());
		writer.writeUnit(unit);
		
//		event = writer.handleEvent(event);
		writeTMXEntries(event.getTextUnit());
	}

	@Override
	public void close () {
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
	}

	@Override
	public String getName () {
		return getClass().getName();
	}

	private Unit toXLIFF2Unit (TextUnit tu) {
		Unit unit = new Unit(tu.getId());

		ISegments srcSegs = tu.getSource().getSegments();
		ISegments trgSegs = null;
		if ( tu.hasTarget(manifest.getTargetLocale()) ) {
			trgSegs = tu.getTarget(manifest.getTargetLocale()).getSegments();
		}
		
		for ( Segment srcSeg : srcSegs ) {
			Segment trgSeg = null;
			if ( trgSegs != null ) {
				trgSeg = trgSegs.get(srcSeg.getId());
			}
			net.sf.okapi.lib.xliff.Segment xSeg = new net.sf.okapi.lib.xliff.Segment(
				fmt.setContent(srcSeg.text).toString(true));
			if ( trgSeg != null ) {
				xSeg.setTarget(new Fragment(
					fmt.setContent(trgSeg.text).toString(true)));
			}
			unit.add(xSeg);
		}
		
		return unit;
	}
}