/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.searchandreplace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;

/**
 * This step performs search and replace actions on either the text units or the full content of input documents. Source
 * and/or target content can be searched and replaced.
 * 
 * Takes: Raw document or Filter events. Sends: same as the input.
 * 
 * The step provides a way to define a list of search entries and corresponding replacements. You can use regular
 * expressions if needed.
 * 
 * The step can take as input either a raw document or filter events.
 * 
 * If the step receives filter events, the search and replace is done on the content of the text units, and the step
 * sends updated filter events to the next step. If the step receives a raw document, the search and replace is done on
 * the whole file, and the step sends an updated raw document to the next step. Note that in this case, the raw document
 * must be in some text-based file format for the search and replace to work: The document is seen exactly like it would
 * be in a text editor (no conversion of escaped characters is done for example).
 * 
 * @author Fredrik L.
 * @author Yves S.
 * @author HargraveJE
 * 
 */
@UsingParameters(Parameters.class)
public class SearchAndReplaceStep extends BasePipelineStep {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private Parameters params;
	private boolean isDone;
	private Matcher matcher;
	private Pattern patterns[];
	private URI outputURI;
	private LocaleId targetLocale;

	public enum ProcType {
		UNSPECIFIED, PLAINTEXT, FILTER;
	}

	private boolean firstEventDone = false;
	private ProcType procType = ProcType.UNSPECIFIED;

	@Override
	public void destroy() {
		// Nothing to do
	}

	public SearchAndReplaceStep() {
		params = new Parameters();
	}

	@StepParameterMapping(parameterType = StepParameterType.OUTPUT_URI)
	public void setOutputURI(URI outputURI) {
		this.outputURI = outputURI;
	}

	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale(LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	public String getDescription() {
		return "Performs search and replace on the entire file or the text units. "
				+ "Expects raw document or filter events. Sends back: raw document or filter events.";
	}

	public String getName() {
		return "Search and Replace";
	}

	@Override
	public boolean isDone() {

		if (procType == ProcType.UNSPECIFIED) {
			return false;
		} else if (procType == ProcType.PLAINTEXT) { // Expects RawDocument
			return isDone;
		} else {
			return true;
		}
	}

	@Override
	public IParameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters) params;
	}

	@Override
	protected Event handleStartBatch(Event event) {		
		if (params.regEx) {
			int flags = 0;
			patterns = new Pattern[params.rules.size()];

			// --initialize patterns--
			if (params.dotAll)
				flags |= Pattern.DOTALL;
			if (params.ignoreCase)
				flags |= Pattern.CASE_INSENSITIVE;
			if (params.multiLine)
				flags |= Pattern.MULTILINE;

			for (int i = 0; i < params.rules.size(); i++) {
				String s[] = params.rules.get(i);
				if (params.regEx) {
					patterns[i] = Pattern.compile(s[1], flags);
				}
			}
		}

		return event;
	}

	@Override
	protected Event handleStartBatchItem(Event event) {
		isDone = false;
		return event;
	}

	@Override
	protected Event handleRawDocument(Event event) {
		// --first event determines processing type--
		if (!firstEventDone) {
			procType = ProcType.PLAINTEXT;
			firstEventDone = true;
		}

		RawDocument rawDoc;
		String encoding = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;

		String result = null;
		StringBuilder assembled = new StringBuilder();

		try {
			rawDoc = (RawDocument) event.getResource();

			// Detect the BOM (and the encoding) if possible
			BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(
					rawDoc.getStream(), rawDoc.getEncoding());
			detector.detectAndRemoveBom();
			encoding = detector.getEncoding();
			// Create the reader from the BOM-aware stream, with the possibly new encoding
			reader = new BufferedReader(new InputStreamReader(detector.getInputStream(), encoding));

			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				assembled.append(buf, 0, numRead);
			}
			reader.close();
			result = assembled.toString();
			assembled = null;

			// Open the output
			File outFile;
			if (isLastOutputStep()) {
				outFile = new File(outputURI);
				Util.createDirectories(outFile.getAbsolutePath());
			} else {
				try {
					outFile = File.createTempFile("okp-snr_", ".tmp");
				} catch (Throwable e) {
					throw new OkapiIOException("Cannot create temporary output.", e);
				}
				outFile.deleteOnExit();
			}

			result = searchAndReplace(result);

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile),
					encoding));
			Util.writeBOMIfNeeded(writer, detector.hasUtf8Bom(), encoding);

			writer.write(result);
			writer.close();

			event.setResource(new RawDocument(outFile.toURI(), encoding, rawDoc.getSourceLocale(),
					rawDoc.getTargetLocale()));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			isDone = true;
			try {
				if (writer != null) {
					writer.close();
					writer = null;
				}
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return event;
	}

	/*
	 * un-escape unicode escape sequences and other things.
	 * TODO: add more escape patterns like in Properties.loadConvert(char[], int, int, char[])??
	 */
	private String unescape(String s, boolean isRegex) {		
		s = s.replace("\\N", System.getProperty("line.separator"));
		s = s.replace("\\n", "\n");
		s = s.replace("\\r", "\r");
		s = s.replace("\\t", "\t");
		
		int i = 0, len = s.length();
		char c;
		StringBuffer sb = new StringBuffer(len);
		while (i < len) {
			c = s.charAt(i++);
			if (c == '\\') {
				if (i < len) {
					c = s.charAt(i++);
					if (c == 'u') {
						c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
						i += 4;
					}else if (c == '\\'){
						if(isRegex){
							//--add both slashes to the regex
							sb.append(c);							
						}
					}// add other cases here as desired...
					 // perhaps throw exception here to notify incomplete escaping?
				}
			} // fall through: \ escapes itself, quotes any character but u
			sb.append(c);
		}
		
		return sb.toString();
	}

	@Override
	protected Event handleTextUnit(Event event) {

		// --first event determines processing type--
		if (!firstEventDone) {
			procType = ProcType.FILTER;
			firstEventDone = true;
		}

		TextUnit tu = (TextUnit) event.getResource();
		// Skip non-translatable
		if (!tu.isTranslatable())
			return event;

		String tmp = null;
		try {
			if (params.source) {
				// search and replace source
				TextContainer tc = tu.getSource();
				for (Segment seg : tc.getSegments()) {
					String r = searchAndReplace(seg.text.toString());
					seg.text.setCodedText(r);
				}
			}

			if (params.target) {
				// search and replace source
				TextContainer tc = tu.createTarget(targetLocale, false, IResource.COPY_ALL);
				for (Segment seg : tc.getSegments()) {
					String r = searchAndReplace(seg.text.toString());
					seg.text.setCodedText(r);
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, String.format("Error when updating content: '%s'.", tmp), e);
		}

		return event;
	}

	private String searchAndReplace(String result) {
		if (params.regEx) {
			for (int i = 0; i < params.rules.size(); i++) {
				String s[] = params.rules.get(i);
				if (s[0].equals("true")) {
					matcher = patterns[i].matcher(result);
					result = matcher.replaceAll(unescape(s[2], true));
				}
			}
		} else {
			for (String[] s : params.rules) {
				if (s[0].equals("true")) {
					result = result.replace(unescape(s[1], false), unescape(s[2], false));
				}
			}
		}

		return result;
	}
}
