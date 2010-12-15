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

package net.sf.okapi.filters.mosestext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiFileNotFoundException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;

/**
 * Implementation of the {@link IFilterWriter} interface for Moses Text files.
 * This class is not designed to be used with the Moses Text Filter, but as a 
 * standalone writer that can be driven by filter events. 
 */
public class MosesTextFilterWriter implements IFilterWriter {

	private OutputStream output;
	private String srcOutputPath;
	private String trgOutputPath;
	private OutputStreamWriter srcWriter;
	private OutputStreamWriter trgWriter;
	private LocaleId trgLoc;
	private FilterWriterParameters params;
	
	private final String lineBreak = System.getProperty("line.separator"); 
	
	public MosesTextFilterWriter () {
		params = new FilterWriterParameters();
	}

	@Override
	public void cancel () {
	}

	@Override
	public void close () {
		try {
			if ( srcWriter != null ) {
				srcWriter.close();
				srcWriter = null;
			}
			if ( output != null ) {
				output.close();
				output = null;
			}
			if ( trgWriter != null ) {
				trgWriter.close();
				trgWriter = null;
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public EncoderManager getEncoderManager () {
		return null; // Not used
	}

	@Override
	public String getName () {
		return "MosesTextFilterWriter";
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
			close();
			break;
		case TEXT_UNIT:
			processTextUnit(event.getTextUnit());
			break;
		}
		return event;
	}

	@Override
	public void setOptions (LocaleId locale,
		String defaultEncoding)
	{
		trgLoc = locale;
		// Default encoding is ignored: we always use UTF-8 for Moses Text files
	}

	@Override
	public void setOutput (String path) {
		close(); // Make sure previous is closed
		this.srcOutputPath = path;
	}

	@Override
	public void setOutput (OutputStream output) {
		close(); // Make sure previous is closed
		this.srcOutputPath = null; // If we use the stream, we can't use the path
		this.output = output; // then assign the new stream
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (FilterWriterParameters)params;
	}

	private void processStartDocument (StartDocument sd) {
		// Create the output file(s)
		// If needed, create the output stream from the path provided
		try {
			String srcLCode = sd.getLocale().toString();
			
			if ( output == null ) {
				// Path provided is used for the source only
				Util.createDirectories(srcOutputPath);
				output = new BufferedOutputStream(new FileOutputStream(srcOutputPath));
			}
			// Create the source output
			srcWriter = new OutputStreamWriter(output, "UTF-8");

			if ( params.getSourceAndTarget() ) {
				// Compute the output path from the input path and the target locale
				String ext = Util.getExtension(srcOutputPath);
				if ( ext.equals("."+srcLCode) ) {
					// If the extension of the source is the locale code
					// We do the same for the target
					trgOutputPath = Util.getDirectoryName(srcOutputPath)
						+ File.separator
						+ Util.getFilename(srcOutputPath, false)
						+ "."
						+ trgLoc.toString();
				}
				else {
					// Otherwise we use the same path as the source and add the target extension
					trgOutputPath = srcOutputPath + "." + trgLoc.toString();
				}
				// Create the target path
				trgWriter = new OutputStreamWriter(
					new BufferedOutputStream(new FileOutputStream(trgOutputPath)), "UTF-8");
			}
			else { // No target output to do
				trgWriter = null;
			}
			
		}
		catch ( FileNotFoundException e ) {
			throw new OkapiFileNotFoundException(e);
		}
		catch ( UnsupportedEncodingException e ) {
			throw new OkapiIOException(e);
		}
	}
	
	private void processTextUnit (TextUnit tu) {
		if ( !tu.isTranslatable() ) {
			return;
		}
		try {
			ISegments srcSegs = tu.getSource().getSegments();
			ISegments trgSegs = null;
			if ( tu.hasTarget(trgLoc) ) {
				trgSegs = tu.getTarget(trgLoc).getSegments();
			}

			// Process by segments
			for ( Segment seg : srcSegs ) {
				// Write the source
				srcWriter.write(toMosesText(seg.text));
				srcWriter.write(lineBreak);
				
				// Write the target if needed
				if ( trgWriter != null ) {
					if ( trgSegs != null ) {
						Segment trgSeg = trgSegs.get(seg.id);
						if ( trgSeg != null ) {
							trgWriter.write(toMosesText(trgSeg.text));
						}
					}
					trgWriter.write(lineBreak);
				}
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	/**
	 * Convert a segment into a Moses string.
	 * @param frag the fragment of the segment to convert.
	 * @return the Moses text for the given fragment.
	 */
	private String toMosesText (TextFragment frag) {
		boolean gMode = true;
		boolean escapeGT = false;
		int quoteMode = 0;
		
		String codedText = frag.getCodedText();
		List<Code> codes = frag.getCodes();
		
		StringBuilder tmp = new StringBuilder();
		int index;
		Code code;
		
		// Content
		for ( int i=0; i<codedText.length(); i++ ) {
			switch ( codedText.codePointAt(i) ) {
			case TextFragment.MARKER_OPENING:
				index = TextFragment.toIndex(codedText.charAt(++i));
				code = codes.get(index);
				if ( code.hasData() ) {
					if ( gMode ) {
						tmp.append(String.format("<g id=\"%d\">", code.getId()));
					}
					else {
						tmp.append(String.format("<bpt id=\"%d\">", code.getId()));
						tmp.append(Util.escapeToXML(code.toString(), quoteMode, escapeGT, null));
						tmp.append("</bpt>");
					}
				}
				else {
					// Marker
					tmp.append(code.getOuterData());
				}
				break;
			case TextFragment.MARKER_CLOSING:
				index = TextFragment.toIndex(codedText.charAt(++i));
				code = codes.get(index);
				if ( code.hasData() ) {
					if ( gMode ) {
						tmp.append("</g>");
					}
					else {
						tmp.append(String.format("<ept id=\"%d\">", code.getId()));
						tmp.append(Util.escapeToXML(code.toString(), quoteMode, escapeGT, null));
						tmp.append("</ept>");
					}
				}
				else {
					// Marker
					tmp.append(code.getOuterData());
				}
				break;
			case TextFragment.MARKER_ISOLATED:
				index = TextFragment.toIndex(codedText.charAt(++i));
				code = codes.get(index);
				if ( gMode ) {
					if ( code.getTagType() == TagType.OPENING ) {
						tmp.append(String.format("<bx id=\"%d\"/>", code.getId()));
					}
					else if ( code.getTagType() == TagType.CLOSING ) {
						tmp.append(String.format("<ex id=\"%d\"/>", code.getId()));
					}
					else {
						tmp.append(String.format("<x id=\"%d\"/>", code.getId()));
					}
				}
				else {
					if ( code.getTagType() == TagType.OPENING ) {
						tmp.append(String.format("<it id=\"%d\" pos=\"open\">", code.getId()));
						tmp.append(Util.escapeToXML(code.toString(), quoteMode, escapeGT, null));
						tmp.append("</it>");
					}
					else if ( code.getTagType() == TagType.CLOSING ) {
						tmp.append(String.format("<it id=\"%d\" pos=\"close\">", code.getId()));
						tmp.append(Util.escapeToXML(code.toString(), quoteMode, escapeGT, null));
						tmp.append("</it>");
					}
					else {
						tmp.append(String.format("<ph id=\"%d\">", code.getId()));
						tmp.append(Util.escapeToXML(code.toString(), quoteMode, escapeGT, null));
						tmp.append("</ph>");
					}
				}
				break;
			case '>':
				if ( escapeGT ) tmp.append("&gt;");
				else {
					if (( i > 0 ) && ( codedText.charAt(i-1) == ']' )) 
						tmp.append("&gt;");
					else
						tmp.append('>');
				}
				break;
			case '<':
				tmp.append("&lt;");
				break;
			case '&':
				tmp.append("&amp;");
				break;
			case '\r':
				tmp.append("&#13;");
				break;
			case '"':
				if ( quoteMode > 0 ) tmp.append("&quot;");
				else tmp.append('"');
				break;
			case '\'':
				switch ( quoteMode ) {
				case 1:
					tmp.append("&apos;");
					break;
				case 2:
					tmp.append("&#39;");
					break;
				default:
					tmp.append(codedText.charAt(i));
					break;
				}
				break;
			case '\n':
				tmp.append("<lb/>");
				break;
			default:
				tmp.append(codedText.charAt(i));
				break;
			}
		}
		
		return tmp.toString();
	}

}
