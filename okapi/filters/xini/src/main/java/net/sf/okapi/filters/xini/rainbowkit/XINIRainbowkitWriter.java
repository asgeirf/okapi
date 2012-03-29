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

package net.sf.okapi.filters.xini.rainbowkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

public class XINIRainbowkitWriter extends FilterEventToXiniTransformer implements IFilterWriter {

	private EncoderManager encodingManager;
	private IParameters params;
	private String xiniPath;
	private String nextPageName;

	public XINIRainbowkitWriter() {
		
	}

	@Override
	public void cancel() {
	}

	@Override
	public void close() {
	}

	@Override
	public String getName() {
		return "XINIRainbowKitWriter";
	}

	@Override
	public void setOptions(LocaleId locale, String defaultEncoding) {
	}

	@Override
	public void setOutput(String path) {
	}

	@Override
	public void setOutput(OutputStream output) {
	}

	@Override
	public Event handleEvent(Event event) {

		switch (event.getEventType()) {
		case START_DOCUMENT:
			startPage(nextPageName);
			break;
		case TEXT_UNIT:
			transformTextUnit(event.getTextUnit());
			break;
		}
		return event;
	}

	public void writeXINI() {
		
		try {
			File outputPath = new File(xiniPath);
			
			if ( !outputPath.getParentFile().exists() ) {
				outputPath.getParentFile().mkdirs();
			}
			
			if ( !outputPath.exists() )
				outputPath.createNewFile();
			
			FileOutputStream fos = new FileOutputStream(outputPath);
			marshall(fos);
			fos.close();
		}
		catch (FileNotFoundException e) {
			// Should be impossible. We created the file.
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public IParameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = params;

	}

	@Override
	public EncoderManager getEncoderManager() {
		return encodingManager;
	}

	public void setOutputPath(String path) {
		this.xiniPath = path;
	}

	@Override
	public ISkeletonWriter getSkeletonWriter() {
		return null;
	}

	public void setNextPageName(String nextPageName) {
		this.nextPageName = nextPageName;
	}

}
