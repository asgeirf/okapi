package net.sf.okapi.filters.xini;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadFilterParametersException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.xini.jaxb.TextContent;
import net.sf.okapi.filters.xini.jaxb.Xini;

public class XiniTestHelper implements Serializable {
	private static Pattern STARTING_TAG = Pattern.compile("<(\\w+ [^>]+?)(/?)>");
	private static Pattern ATTRIBUTES = Pattern.compile("\\w+?=\".*?\"");
	private Marshaller m;

	public XiniTestHelper() {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(Xini.class.getPackage().getName());
			m = jc.createMarshaller();
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public void assertEquivalent(String expected, String actual) {
		expected = normalize(expected);
		actual = normalize(actual);
		assertEquals(expected, actual);
	}

	private String normalize(String str) {
		String result = str;
		Matcher startTagMatcher = STARTING_TAG.matcher(str);

		while (startTagMatcher.find()) {
			String tagContent = startTagMatcher.group(1);
			String suffix = startTagMatcher.group(2);
			String newTagContent = orderAttributes(tagContent);
			newTagContent += suffix;

			String before = result.substring(0, startTagMatcher.start());
			String after = result.substring(startTagMatcher.end(), result.length());
			result = before + "<" + newTagContent + ">" + after;
		}
		return result;
	}

	private String orderAttributes(String tagContent) {
		String tagContentWithOrderedAttributes = "";
		Matcher attributeMatcher = ATTRIBUTES.matcher(tagContent);
		List<String> attributeDeclarations = new ArrayList<String>();
		boolean firstMatch = true;
		while (attributeMatcher.find()) {
			if (firstMatch) {
				tagContentWithOrderedAttributes += tagContent.substring(0, attributeMatcher.start());
				tagContentWithOrderedAttributes = tagContentWithOrderedAttributes.trim();
				firstMatch = false;
			}
			String attDeclaration = attributeMatcher.group();
			attributeDeclarations.add(attDeclaration);
		}
		Collections.sort(attributeDeclarations);
		for (String attributeDeclaration : attributeDeclarations) {
			tagContentWithOrderedAttributes += " " + attributeDeclaration;
		}
		return tagContentWithOrderedAttributes;
	}

	/**
	 * Converts {@link TextContent} into an xml string.
	 *
	 * @param xiniTextContent
	 * @return
	 */
	public String serializeTextContent(TextContent xiniTextContent) {
		String serializedSeg = null;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			m.marshal(xiniTextContent, baos);
			serializedSeg = baos.toString("UTF-8");
			serializedSeg = removeXmlDeclaration(serializedSeg);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		int beginOffset = serializedSeg.indexOf(">") + 1;
		int endOffset = serializedSeg.lastIndexOf("<");
		
		serializedSeg = serializedSeg.substring(beginOffset, endOffset);
		
		return serializedSeg;
	}

	private String removeXmlDeclaration(String content) {
		if (content.startsWith("<?xml"))
			content = content.substring(content.indexOf(">") + 1);
		return content;
	}

	public Xini toXini(List<Event> events, IFilter filter) {
		XINIWriter writer = (XINIWriter) filter.createFilterWriter();
		try {
			for (Event event : events) {
				writer.handleEvent(event);
			}
		}
		catch (OkapiBadFilterParametersException e) {
			// Output path is not set when tests are run. That's ok here.
		}
		return writer.getXini();
	}

	public List<Event> toEvents(String snippet, IFilter filter, LocaleId inputLoc, LocaleId outputLoc) {
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, inputLoc, outputLoc));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}
}
