package net.sf.okapi.filters.xmlstream.integration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;

public final class XmlStreamTestUtils {
	public static String[] getTestFiles(final String aFile, final String suffix)
			throws URISyntaxException {
		// read all files in the test xmlstream directory
		URL url = XmlStreamTestUtils.class.getResource(aFile);
		File dir = new File(url.toURI()).getParentFile();

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(suffix);
			}
		};
		return dir.list(filter);
	}

	public static String generateOutput(ArrayList<Event> list, String original, LocaleId trgLang,
			XmlStreamFilter filter) {
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		StringBuilder tmp = new StringBuilder();
		for (Event event : list) {
			switch (event.getEventType()) {
			case START_DOCUMENT:
				writer.processStartDocument(trgLang, "utf-8", null, filter.getEncoderManager(),
						(StartDocument) event.getResource());
				break;
			case TEXT_UNIT:
				ITextUnit tu = event.getTextUnit();
				tmp.append(writer.processTextUnit(tu));
				break;
			case DOCUMENT_PART:
				DocumentPart dp = (DocumentPart) event.getResource();
				tmp.append(writer.processDocumentPart(dp));
				break;
			case START_GROUP:
				StartGroup startGroup = (StartGroup) event.getResource();
				tmp.append(writer.processStartGroup(startGroup));
				break;
			case END_GROUP:
				Ending ending = (Ending) event.getResource();
				tmp.append(writer.processEndGroup(ending));
				break;
			}
		}
		writer.close();
		return tmp.toString();
	}

	public static ArrayList<Event> getEvents(String snippet, XmlStreamFilter filter, URL parameters) {
		ArrayList<Event> list = new ArrayList<Event>();
		filter.setParametersFromURL(parameters);
		filter.open(new RawDocument(snippet, LocaleId.ENGLISH));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine() method. We iterate until the
		 * BufferedReader return null which means there's no more data to read. Each line will appended to a
		 * StringBuilder and returned as String.
		 * From: http://www.kodejava.org/examples/266.html
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}
}
