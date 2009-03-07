package net.sf.okapi.filters.html.tests;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.filters.html.HtmlFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//TODO: Test for properties
public class HtmlSnippetsTest {
	private HtmlFilter htmlFilter;
	private URL parameters;

	@Before
	public void setUp() {
		htmlFilter = new HtmlFilter();
		parameters = HtmlSnippetsTest.class.getResource("testConfiguration1.yml");
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testMETATag1() {
		String snippet = "<meta http-equiv=\"keywords\" content=\"one,two,three\"/>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testPWithAttributes() {
		String snippet = "<p title='my title' dir='rtl'>Text of p</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testLang() {
		String snippet = "<p lang='en'>Text of p</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testComplexEmptyElement() {
		String snippet = "<dummy write=\"w\" readonly=\"ro\" trans=\"tu1\" />";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testPWithInlines() {
		String snippet = "<p>Before <b>bold</b> <a href=\"there\"/> after.</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testMETATag2() {
		String snippet = "<meta http-equiv=\"Content-Language\" content=\"en\"/>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testPWithInlines2() {
		String snippet = "<p>Before <img href=\"img.png\" alt=\"text\"/> after.</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testPWithInlineTextOnly() {
		String snippet = "<p>Before <img alt=\"text\"/> after.</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testTableGroups() {
		String snippet = "<table id=\"100\"><tr><td>text</td></tr></table>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testGroupInPara() {
		String snippet = "<p>Text before list:" + "<ul>" + "<li>Text of item 1</li>" + "<li>Text of item 2</li>"
				+ "</ul>" + "and text after the list.</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testInput() {
		String snippet = "<p>Before <input type=\"radio\" name=\"FavouriteFare\" value=\"spam\" checked=\"checked\"/> after.</p>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testCollapseWhitespaceWithPre() {
		String snippet = "<pre>   \n   \r <x/>  \f    </pre>";
		assertEquals(snippet, generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testCollapseWhitespaceWithoutPre() {
		String snippet = " <b>   \t\n\r\f    </b> ";
		assertEquals("<b> </b>", generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testEscapeEntities() {
		String snippet = "&nbsp;M&#x0033;";
		assertEquals("\u00A0M\u0033", generateOutput(getEvents(snippet), snippet));
	}

	@Test
	public void testNormalizeNewlines() {
		String snippet = "\r\nX\rY\n";
		URL originalParameters = parameters;
		parameters = HtmlSnippetsTest.class.getResource("collapseWhitespaceOff.yml");
		assertEquals("\nX\nY\n", generateOutput(getEvents(snippet), snippet));
		parameters = originalParameters; 
	}

	@Test
	public void testNormalizeNewlinesInPre() {
		String snippet = "<pre>\r\nX\rY\n</pre>";
		assertEquals("<pre>\r\nX\rY\n</pre>", generateOutput(getEvents(snippet), snippet));
	}

	private ArrayList<Event> getEvents(String snippet) {
		ArrayList<Event> list = new ArrayList<Event>();
		htmlFilter.setParametersFromURL(parameters);
		htmlFilter.open(snippet);
		while (htmlFilter.hasNext()) {
			Event event = htmlFilter.next();
			list.add(event);
		}
		htmlFilter.close();
		return list;
	}

	private String generateOutput(ArrayList<Event> list, String original) {
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		StringBuilder tmp = new StringBuilder();
		for (Event event : list) {
			switch (event.getEventType()) {
			case START_DOCUMENT:
				writer.processStartDocument("en", "utf-8", null, new EncoderManager(), (StartDocument) event
						.getResource());
				break;
			case TEXT_UNIT:
				TextUnit tu = (TextUnit) event.getResource();
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

}
