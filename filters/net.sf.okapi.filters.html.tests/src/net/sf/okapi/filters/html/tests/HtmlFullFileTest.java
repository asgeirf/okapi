package net.sf.okapi.filters.html.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.markupfilter.Parameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HtmlFullFileTest {
	private HtmlFilter htmlFilter;
	private String[] testFileList;

	@Before
	public void setUp() throws Exception {
		htmlFilter = new HtmlFilter();
		htmlFilter.setParameters(new Parameters("/net/sf/okapi/filters/html/tests/minimalistConfiguration.yml"));

		// read all files in the test html directory
		URL url = HtmlFullFileTest.class.getResource("/simpleTest.html");
		File dir = new File(url.toURI()).getParentFile();

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".html") || name.endsWith(".hml");
			}
		};
		testFileList = dir.list(filter);
	}

	@After
	public void tearDown() {
		htmlFilter.close();
	}

	@Test
	public void testAll() throws URISyntaxException {
		for (String f : testFileList) {
			InputStream htmlStream = HtmlFullFileTest.class.getResourceAsStream("/" + f);
			htmlFilter.open(htmlStream);
			try {
				while (htmlFilter.hasNext()) {
					FilterEvent event = htmlFilter.next();
				}
			} catch (Exception e) {
				//System.err.println("Error for file: " + f + ": " + e.toString());
				throw new RuntimeException("Error for file: " + f + ": " + e.toString());
			}
		}
	}

	@Test
	public void testNonwellformed() {
		InputStream htmlStream = HtmlFullFileTest.class.getResourceAsStream("/nonwellformed.specialtest");
		htmlFilter.open(htmlStream);
		while (htmlFilter.hasNext()) {
			FilterEvent event = htmlFilter.next();
		}
	}
	
	@Test
	public void testTmx() throws FileNotFoundException {
		htmlFilter.setOptions("en", "UTF-16LE", true);
		InputStream in = new FileInputStream("D:/OKAPI/net.sf.okapi.filters.html.tests/html/FCH.tmx");
		htmlFilter.open(in);			
		while (htmlFilter.hasNext()) {
			FilterEvent event = htmlFilter.next();
			System.out.print(event.getEventType().toString());
		}
	}
}
