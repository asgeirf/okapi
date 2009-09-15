/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiBadFilterParametersException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.TextUnitUtils;
import net.sf.okapi.filters.table.csv.CommaSeparatedValuesFilter;
import net.sf.okapi.filters.table.csv.Parameters;
import net.sf.okapi.lib.extra.filters.AbstractLineFilter;
import net.sf.okapi.lib.extra.filters.WrapMode;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.TestUtil; //maven

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CommaSeparatedValuesFilterTest {

	private CommaSeparatedValuesFilter filter;
	private FilterTestDriver testDriver;
	private String root; // for maven move; deletions marked with //m
	
	@Before
	public void setUp() {
		filter = new CommaSeparatedValuesFilter();
		assertNotNull(filter);
		
		testDriver = new FilterTestDriver();
		assertNotNull(testDriver);
		
		testDriver.setDisplayLevel(0);
		testDriver.setShowSkeleton(true);
		
        root = TestUtil.getParentDir(this.getClass(), "/csv_test1.txt");
	}

	@Test
	public void testThreeColumnsSrcTrgData () {
		String snippet = "\"src\",\"trg\",data\n"
			+ "\"source1\",\"target1\",data1\n"
			+ "\"source2\",\"target2\",data2\n";

		// Set the parameters
		Parameters params = new Parameters();
		params.fieldDelimiter = ",";
		params.textQualifier = "\"";
		params.sendColumnsMode = Parameters.SEND_COLUMNS_LISTED;
		params.sourceColumns = "1";
		params.targetColumns = "2";
		params.targetLanguages = "FR-CA";
		params.targetSourceRefs = "1";
		filter.setParameters(params);
		
		String result = FilterTestDriver.generateOutput(getEvents(snippet, "EN", "FR-CA"), "FR-CA");
//		assertEquals(snippet, result);
	}
	
	@Test
	public void testFileEvents96_2() {
		
		//Parameters params = (Parameters) filter.getParameters();
			
		InputStream input = TableFilterTest.class.getResourceAsStream("/CSVTest_96_2.txt"); // issue 96
		assertNotNull(input);
	
		// Set the parameters
		Parameters params = new Parameters();
		params.fieldDelimiter = ",";
		params.textQualifier = "\"";
		params.sendColumnsMode = Parameters.SEND_COLUMNS_LISTED;
		params.sourceColumns = "1";
		params.targetColumns = "2";
		params.targetLanguages = "FR-CA";
		params.targetSourceRefs = "1";
		filter.setParameters(params);
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "src");
		testEvent(EventType.TEXT_UNIT, "trg");
		testEvent(EventType.TEXT_UNIT, "data");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "source1", "", "target1", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "source2", "", "target2", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
				filter.close();
		
	}
	
	
	@Test
	public void testEmptyInput() {
		
		// Empty input, check exceptions
				
		// Empty stream, OkapiBadFilterInputException expected, no other		
		InputStream input = null;
		try {			
			filter.open(new RawDocument(input, "UTF-8", "en"));
			fail("OkapiIOException should've been trown");
		}	
		catch (OkapiIOException e) {
		}
		finally {
			filter.close();
		}
				
		// Empty URI, OkapiBadFilterInputException expected, no other
		URI uri = null;
		try {
			filter.open(new RawDocument(uri, "UTF-8", "en"));
			fail("OkapiIOException should've been trown");
		}	
		catch (OkapiIOException e) {
		}
		finally {
			filter.close();
		}
		
		// Empty char seq, OkapiBadFilterInputException expected, no other		
		String st = null;
		try {
			filter.open(new RawDocument(st, "UTF-8", "en"));
			fail("OkapiIOException should've been trown");
		}	
		catch (OkapiIOException e) {
		}
		finally {
			filter.close();
		}
		
		// Empty raw doc, open(RawDocument), OkapiBadFilterInputException expected, no other		
		try {
			filter.open(null);
			fail("OkapiBadFilterInputException should've been trown");
		}	
		catch (OkapiBadFilterInputException e) {
		}
		finally {
			filter.close();
		}
	
		// Empty raw doc, open(RawDocument, boolean), OkapiBadFilterInputException expected, no other
		try {
			filter.open(null, true);
			fail("OkapiBadFilterInputException should've been trown");
		}	
		catch (OkapiBadFilterInputException e) {
		}
		finally {
			filter.close();
		}
	
		// Empty filter parameters, OkapiBadFilterParametersException expected		
			filter.setParameters(null);
			
			InputStream input2 = TableFilterTest.class.getResourceAsStream("/csv_test6.txt");
		try {
			filter.open(new RawDocument(input2, "UTF-8", "en"));
			fail("OkapiBadFilterParametersException should've been trown");
		}
		catch (OkapiBadFilterParametersException e) {
		}
		finally {
			filter.close();
		}		
	}		
		
	@Test
	public void testNameAndMimeType() {
		assertEquals("text/csv", filter.getMimeType());
		assertEquals("okf_table_csv", filter.getName());
		
		// Read lines from a file, check mime types 
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv_test1.txt");
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		while (filter.hasNext()) {
			Event event = filter.next();
			assertNotNull(event);
			
			IResource res = event.getResource();
			assertNotNull(res);
			
			switch (event.getEventType()) {
				case TEXT_UNIT:
					assertTrue(res instanceof TextUnit);
					assertEquals(((TextUnit) res).getMimeType(), filter.getMimeType());
					break;
					
				case DOCUMENT_PART:
					assertTrue(res instanceof DocumentPart);
					assertEquals(((DocumentPart) res).getMimeType(), null);
					break;
			}
		}
		filter.close();
	}
	
	@Test
	public void testParameters() {
		
		// Check if PlainTextFilter params are set for inherited fields
		Parameters params = (Parameters) filter.getParameters();
						
		assertEquals(params.unescapeSource, true);
		assertEquals(params.trimLeading, true);
		assertEquals(params.trimTrailing, true);
		assertEquals(params.preserveWS, true);
		assertEquals(params.useCodeFinder, false);
		assertEquals(				
				"#v1\ncount.i=2\nrule0=%(([-0+#]?)[-0+#]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn]\n" +
				"rule1=(\\\\r\\\\n)|\\\\a|\\\\b|\\\\f|\\\\n|\\\\r|\\\\t|\\\\v\nsample=\nuseAllRulesWhenTesting.b=false", 
				params.codeFinderRules);
									
		// Check if defaults are set
		params = new Parameters();
		filter.setParameters(params);
		
		params.columnNamesLineNum = 1;
		params.valuesStartLineNum = 1;
		params.detectColumnsMode = 1;
		params.numColumns = 1;
		params.sendHeaderMode = 1;
		params.trimMode = 1;
		params.fieldDelimiter = "1";
		params.textQualifier = "1";
		params.sourceIdColumns = "1";
		params.sourceColumns = "1";
		params.targetColumns = "1";
		params.commentColumns = "1";
		params.preserveWS = true;
		params.useCodeFinder = true;
		
		params = getParameters();
		
		assertEquals(params.fieldDelimiter, "1");
		assertEquals(params.columnNamesLineNum, 1);
		assertEquals(params.numColumns, 1);
		assertEquals(params.sendHeaderMode, 1);
		assertEquals(params.textQualifier, "1");
		assertEquals(params.trimMode, 1);
		assertEquals(params.valuesStartLineNum, 1);
		assertEquals(params.preserveWS, true);
		assertEquals(params.useCodeFinder, true);
		
		// Load filter parameters from a file, check if params have changed
//		URL paramsUrl = TableFilterTest.class.getResource("/test_params1.txt");
//		assertNotNull(paramsUrl);  
		

		try {
		String st = "file:" + getFullFileName("test_params1.txt");
		params.load(new URI(st), false);
	} catch (URISyntaxException e) {
	}

		
		assertEquals("2", params.fieldDelimiter);
		assertEquals(params.columnNamesLineNum, 2);
		assertEquals(params.numColumns, 2);
		assertEquals(params.sendHeaderMode, 2);
		assertEquals("2", params.textQualifier);
		assertEquals(params.trimMode, 2);
		assertEquals(params.valuesStartLineNum, 2);
		assertEquals(params.preserveWS, false);
		assertEquals(params.useCodeFinder, false);
		
		// Save filter parameters to a file, load and check if params have changed
		URL paramsUrl = TableFilterTest.class.getResource("/test_params2.txt");
		assertNotNull(paramsUrl);
		
		params.save(paramsUrl.getPath());
		
		// Change params before loading them
		params = (Parameters) filter.getParameters();
		params.fieldDelimiter = "3";
		params.columnNamesLineNum = 3;
		params.numColumns = 3;
		params.sendHeaderMode = 3;
		params.textQualifier = "3";
		params.trimMode = 3;
		params.valuesStartLineNum = 3;
		params.preserveWS = true;
		params.useCodeFinder = true;
		
		params.load(Util.toURI(paramsUrl.getPath()), false);
		
		assertEquals(params.fieldDelimiter, "2");
		assertEquals(params.columnNamesLineNum, 2);
		assertEquals(params.numColumns, 2);
		assertEquals(params.sendHeaderMode, 2);
		assertEquals(params.textQualifier, "2");
		assertEquals(params.trimMode, 2);
		assertEquals(params.valuesStartLineNum, 2);
		assertEquals(params.preserveWS, false);
		assertEquals(params.useCodeFinder, false);
		
		// Check if parameters type is controlled
		
		filter.setParameters(new net.sf.okapi.filters.plaintext.base.Parameters());
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv_test1.txt");
		try {
			filter.open(new RawDocument(input, "UTF-8", "en"));
			fail("OkapiBadFilterParametersException should've been trown");
		}
		catch (OkapiBadFilterParametersException e) {
		}
		
		filter.close();
	
		filter.setParameters(new net.sf.okapi.filters.table.csv.Parameters());
		input = TableFilterTest.class.getResourceAsStream("/csv_test1.txt");
		try {
			filter.open(new RawDocument(input, "UTF-8", "en"));
		}
		catch (OkapiBadFilterParametersException e) {
			fail("OkapiBadFilterParametersException should NOT have been trown");
		}
			filter.close();
	}

	@Test
	public void testSkeleton () {
		String st = null;
		String expected = null;
		
		try {
			st = getSkeleton(getFullFileName("csv_test1.txt"));
		} 
		catch (UnsupportedEncodingException e) {
		}	
//debug		System.out.println(String.format("Skeleton of %s\n---\n", "csv_test1.txt") + st + "\n----------");
		
		try {
			expected = streamAsString(CommaSeparatedValuesFilterTest.class.getResourceAsStream("/csv_test1.txt"));			
		} 
		catch (IOException e) {
		}
		assertEquals(expected, st);
	}
	
	
	@Test
	public void testSkeleton2 () {
		String st = null;
		String expected = null;
		
		try {
			st = getSkeleton(getFullFileName("csv_test2.txt"));
		} 
		catch (UnsupportedEncodingException e) {
		}	
//debug		System.out.println(String.format("Skeleton of %s\n---\n", "csv_test2.txt") + st + "\n----------");
		
		try {
			expected = streamAsString(CommaSeparatedValuesFilterTest.class.getResourceAsStream("/csv_test2.txt"));			
		} 
		catch (IOException e) {
		}
		assertEquals(expected, st);
	}
	
	@Test
	public void testSkeleton3 () {
		String st = null;
		String expected = null;
		
		try {
			st = getSkeleton(getFullFileName("csv_test3.txt"));
		} 
		catch (UnsupportedEncodingException e) {
		}	
		//debug System.out.println(String.format("Skeleton of %s\n---\n", "csv_test3.txt") + st + "\n----------");
		
		try {
			expected = streamAsString(CommaSeparatedValuesFilterTest.class.getResourceAsStream("/csv_test3.txt"));			
		} 
		catch (IOException e) {
		}
		assertEquals(expected, st);
	}
	
	@Test
	public void testFileEvents() {
		testDriver.setDisplayLevel(0);
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv_test1.txt");
		assertNotNull(input);
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "FieldName1");
		testEvent(EventType.TEXT_UNIT, "FieldName2");
		testEvent(EventType.TEXT_UNIT, "FieldName3");
		testEvent(EventType.TEXT_UNIT, "FieldName4");
		testEvent(EventType.TEXT_UNIT, "FieldName5");
		testEvent(EventType.TEXT_UNIT, "FieldName6");
		testEvent(EventType.TEXT_UNIT, "FieldName7");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value11");
		testEvent(EventType.TEXT_UNIT, "Value12");
		testEvent(EventType.TEXT_UNIT, "Value13");
		testEvent(EventType.TEXT_UNIT, "Value14");
		testEvent(EventType.TEXT_UNIT, "Value15");
		testEvent(EventType.TEXT_UNIT, "Value16");
		testEvent(EventType.TEXT_UNIT, "Value17");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value21");
		testEvent(EventType.TEXT_UNIT, "Value22");
		testEvent(EventType.TEXT_UNIT, "Value23");
		testEvent(EventType.TEXT_UNIT, "Value24");
		testEvent(EventType.TEXT_UNIT, "Value25");
		testEvent(EventType.TEXT_UNIT, "Value26");
		testEvent(EventType.TEXT_UNIT, "Value27");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value31");
		testEvent(EventType.TEXT_UNIT, "Value32");
		testEvent(EventType.TEXT_UNIT, "Value33");
		testEvent(EventType.TEXT_UNIT, "Value34");
		testEvent(EventType.TEXT_UNIT, "Value35");
		testEvent(EventType.TEXT_UNIT, "Value36");
		testEvent(EventType.TEXT_UNIT, "Value37");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
		
		// List events		
		String filename = "csv_test1.txt";
		input = TableFilterTest.class.getResourceAsStream("/" + filename);
		assertNotNull(input);
		
		// System.out.println(filename);
		filter.open(new RawDocument(input, "UTF-8", "en"));
		if ( !testDriver.process(filter) ) Assert.fail();
		filter.close();
	}
	
	
	@Test
	public void testFileEvents106() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv.txt"); // issue 106
		assertNotNull(input);
		
		params.sendHeaderMode = Parameters.SEND_HEADER_ALL;
		params.valuesStartLineNum = 2;
		params.columnNamesLineNum = 1;
		params.detectColumnsMode = Parameters.DETECT_COLUMNS_COL_NAMES;
		params.trimLeading = false;
		params.trimTrailing = false;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "id");
		testEvent(EventType.TEXT_UNIT, "value");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "01");
		testEvent(EventType.TEXT_UNIT, "one");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "02");
		testEvent(EventType.TEXT_UNIT, "first,\nsecond\n");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "03");
		testEvent(EventType.TEXT_UNIT, "three");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testFileEvents106_2() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv2.txt"); // issue 106
		assertNotNull(input);
		
		params.sendHeaderMode = Parameters.SEND_HEADER_ALL;
		params.valuesStartLineNum = 2;
		params.columnNamesLineNum = 1;
		//params.detectColumnsMode = Parameters.DETECT_COLUMNS_COL_NAMES;
		params.detectColumnsMode = Parameters.DETECT_COLUMNS_NONE;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "id");
		testEvent(EventType.TEXT_UNIT, "value");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "01");
		testEvent(EventType.TEXT_UNIT, "one");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "02");
		testEvent(EventType.TEXT_UNIT, "first,\nsecond");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "03");
		testEvent(EventType.TEXT_UNIT, "three");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testFileEvents106_3() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv3.txt"); // issue 106
		assertNotNull(input);
		
//		params.sendHeaderMode = Parameters.SEND_HEADER_ALL;
//		params.valuesStartLineNum = 2;
//		params.columnNamesLineNum = 1;
//		params.detectColumnsMode = Parameters.DETECT_COLUMNS_COL_NAMES;
		
		params.trimLeading = true;
		params.trimTrailing = false;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "02");
		testEvent(EventType.TEXT_UNIT, "first,\nsecond\n");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
		
		input = TableFilterTest.class.getResourceAsStream("/csv3.txt"); // issue 106
		assertNotNull(input);
		
		params.trimLeading = true;
		params.trimTrailing = true;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "02");
		testEvent(EventType.TEXT_UNIT, "first,\nsecond");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testFileEvents106_4() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv.txt"); // issue 106
		assertNotNull(input);
		
		URL paramsUrl = TableFilterTest.class.getResource("/okf_table@copy-of-csv._106.fprm");
		assertNotNull(paramsUrl);  
		
		try {
			params.load(paramsUrl.toURI(), false);
		} catch (URISyntaxException e) {
		}
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		//testEvent(EventType.TEXT_UNIT, "one");
		testEvent(EventType.TEXT_UNIT, "one", "01", null, null, null);
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "first,\nsecond\n", "02", null, null, null);
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "three", "03", null, null, null);
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testFileEvents96() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/CSVTest_96.txt"); // issue 96
		assertNotNull(input);
		
		params.loadFromResource("/okf_table@copy-of-csv_96.fprm");
		params.sendHeaderMode = Parameters.SEND_HEADER_ALL;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source");
		testEvent(EventType.TEXT_UNIT, "Target");
		testEvent(EventType.TEXT_UNIT, "Data");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 1", "", "Target text 1", "FR-FR", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 2", "", "Target text 2", "FR-FR", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
		
		input = TableFilterTest.class.getResourceAsStream("/CSVTest_96.txt"); // issue 96
		assertNotNull(input);
		
		params.loadFromResource("/okf_table@copy-of-csv_96.fprm");
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 1", "", "Target text 1", "FR-FR", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 2", "", "Target text 2", "FR-FR", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testFileEvents97() {
		
		Parameters params = (Parameters) filter.getParameters();
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/CSVTest_97.txt"); // issue 97
		assertNotNull(input);
		
		params.loadFromResource("/okf_table@copy-of-csv_97.fprm");
		params.sendHeaderMode = Parameters.SEND_HEADER_ALL;
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source");
		testEvent(EventType.TEXT_UNIT, "Target");
		testEvent(EventType.TEXT_UNIT, "Data");
		testEvent(EventType.TEXT_UNIT, "Source");
		testEvent(EventType.TEXT_UNIT, "Target");		
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 1", "", "Target text 1", "FR-FR", "");
		testEvent(EventType.TEXT_UNIT, "SourceB1", "", "TargetB1", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 2", "", "Target text 2", "FR-FR", "");
		testEvent(EventType.TEXT_UNIT, "SourceB2", "", "TargetB2", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
		
		
		input = TableFilterTest.class.getResourceAsStream("/CSVTest_97.txt"); // issue 97
		assertNotNull(input);
		
		params.loadFromResource("/okf_table@copy-of-csv_97.fprm");
		
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
						
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 1", "", "Target text 1", "FR-FR", "");
		testEvent(EventType.TEXT_UNIT, "SourceB1", "", "TargetB1", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Source text 2", "", "Target text 2", "FR-FR", "");
		testEvent(EventType.TEXT_UNIT, "SourceB2", "", "TargetB2", "FR-CA", "");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		
		filter.close();
	}
	
	@Test
	public void testQualifiedValues() {
				
		//_getParameters().detectColumnsMode = Parameters.DETECT_COLUMNS_NONE;
		//_getParameters().compoundTuDelimiter = "\n";
		
		Parameters params = (Parameters) filter.getParameters();
		params.detectColumnsMode = Parameters.DETECT_COLUMNS_NONE;
		
		InputStream input = TableFilterTest.class.getResourceAsStream("/csv_test3.txt");
		assertNotNull(input);
		
		params.wrapMode = WrapMode.NONE; // !!!
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
		
		// Line 1
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value11");
		testEvent(EventType.TEXT_UNIT, "Value12");
		testEvent(EventType.TEXT_UNIT, "Value13");
		testEvent(EventType.TEXT_UNIT, "Value14");		
		testEvent(EventType.END_GROUP, null);
		
		// Line 2
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value21");
		testEvent(EventType.TEXT_UNIT, "Value22.1,Value22.2, Value22.3");
		testEvent(EventType.TEXT_UNIT, "Value23");
		testEvent(EventType.TEXT_UNIT, "Value24");		
		testEvent(EventType.END_GROUP, null);
				
		// Correct multiline chunk
		// Line 4-7
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value31");
		testEvent(EventType.TEXT_UNIT, "Value32");
		testEvent(EventType.TEXT_UNIT, "Value33");
		testEvent(EventType.TEXT_UNIT, "Value34.1\nValue34.2\nValue34.3\nValue34.4,Value34.5");
		testEvent(EventType.TEXT_UNIT, "Value35");
		testEvent(EventType.END_GROUP, null);
		
		// Incorrect multiline chunk 1
		// Line 9
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value41");
		testEvent(EventType.TEXT_UNIT, "Value42");
		testEvent(EventType.TEXT_UNIT, "Value43");
		testEvent(EventType.TEXT_UNIT, "\"Value44.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 10
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 11
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 12
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.4");
		testEvent(EventType.TEXT_UNIT, "Value45.1,Value45.2");
		testEvent(EventType.END_GROUP, null);		
		
		// Incorrect multiline chunk 2
		// Line 14
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value51");
		testEvent(EventType.TEXT_UNIT, "Value52");
		testEvent(EventType.TEXT_UNIT, "Value53");
		testEvent(EventType.TEXT_UNIT, "\"Value54.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 15
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 16
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 17-18
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.4");
		testEvent(EventType.TEXT_UNIT, "Value55.1,Value55.2\nValue55.3,Value55.4");
		testEvent(EventType.END_GROUP, null);
		
		// Incorrect multiline chunk 3
		// Line 20
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value61");
		testEvent(EventType.TEXT_UNIT, "Value62");
		testEvent(EventType.TEXT_UNIT, "Value63");
		testEvent(EventType.TEXT_UNIT, "\"Value64.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 21
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 22
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 23
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.4");
		testEvent(EventType.TEXT_UNIT, "\"Value65.1");
		testEvent(EventType.TEXT_UNIT, "Value65.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 24
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value65.3");
		testEvent(EventType.TEXT_UNIT, "Value65.4");
		testEvent(EventType.END_GROUP, null);
		
		// Line 25
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value65.5");
		testEvent(EventType.TEXT_UNIT, "Value66");
		testEvent(EventType.END_GROUP, null);
		// -------------------------------------------------
		
		// Line 27
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value71");
		testEvent(EventType.TEXT_UNIT, "Value72 \"quoted part 1\"\"quoted part 2\" value");
		testEvent(EventType.TEXT_UNIT, "Value73");
		testEvent(EventType.TEXT_UNIT, "Value74");		
		testEvent(EventType.END_GROUP, null);
		
		// Line 28
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value81");
		testEvent(EventType.TEXT_UNIT, "\"Value82 with unclosed quote"); // Preserve the wrong quotation
		testEvent(EventType.END_GROUP, null);
		
		// Line 29
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value91");
		testEvent(EventType.TEXT_UNIT, "Value92");
		testEvent(EventType.END_GROUP, null);
		
		// Line 30
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueA1");
		testEvent(EventType.TEXT_UNIT, "ValueA2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 31
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueB1");
		testEvent(EventType.TEXT_UNIT, "Value\"B2,Va\"lueB3");		// If quotation marks are not around field, preserve them 
		testEvent(EventType.TEXT_UNIT, "Va\"lueB4\"");
		testEvent(EventType.END_GROUP, null);
		
		// Line 32
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueC1");
		testEvent(EventType.TEXT_UNIT, "\"ValueC2");		 
		testEvent(EventType.TEXT_UNIT, "ValueC3");			
		testEvent(EventType.TEXT_UNIT, "\"ValueC4");
		testEvent(EventType.TEXT_UNIT, "ValueC5");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		filter.close();
		
		
		// Unwrap lines
		input = TableFilterTest.class.getResourceAsStream("/csv_test3.txt");
		assertNotNull(input);
		
		params.wrapMode = WrapMode.SPACES; // !!!
		filter.open(new RawDocument(input, "UTF-8", "en"));
		
		testEvent(EventType.START_DOCUMENT, null);
		
		// Line 1
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value11");
		testEvent(EventType.TEXT_UNIT, "Value12");
		testEvent(EventType.TEXT_UNIT, "Value13");
		testEvent(EventType.TEXT_UNIT, "Value14");		
		testEvent(EventType.END_GROUP, null);
		
		// Line 2
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value21");
		testEvent(EventType.TEXT_UNIT, "Value22.1,Value22.2, Value22.3");
		testEvent(EventType.TEXT_UNIT, "Value23");
		testEvent(EventType.TEXT_UNIT, "Value24");		
		testEvent(EventType.END_GROUP, null);
				
		// Correct multiline chunk
		// Line 4-7
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value31");
		testEvent(EventType.TEXT_UNIT, "Value32");
		testEvent(EventType.TEXT_UNIT, "Value33");
		testEvent(EventType.TEXT_UNIT, "Value34.1 Value34.2 Value34.3 Value34.4,Value34.5");
		testEvent(EventType.TEXT_UNIT, "Value35");
		testEvent(EventType.END_GROUP, null);
		
		// Incorrect multiline chunk 1
		// Line 9
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value41");
		testEvent(EventType.TEXT_UNIT, "Value42");
		testEvent(EventType.TEXT_UNIT, "Value43");
		testEvent(EventType.TEXT_UNIT, "\"Value44.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 10
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 11
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 12
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value44.4");
		testEvent(EventType.TEXT_UNIT, "Value45.1,Value45.2");
		testEvent(EventType.END_GROUP, null);		
		
		// Incorrect multiline chunk 2
		// Line 14
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value51");
		testEvent(EventType.TEXT_UNIT, "Value52");
		testEvent(EventType.TEXT_UNIT, "Value53");
		testEvent(EventType.TEXT_UNIT, "\"Value54.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 15
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 16
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 17-18
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value54.4");
		testEvent(EventType.TEXT_UNIT, "Value55.1,Value55.2 Value55.3,Value55.4");
		testEvent(EventType.END_GROUP, null);
		
		// Incorrect multiline chunk 3
		// Line 20
		testEvent(EventType.START_GROUP, null);
		testEvent(EventType.TEXT_UNIT, "Value61");
		testEvent(EventType.TEXT_UNIT, "Value62");
		testEvent(EventType.TEXT_UNIT, "Value63");
		testEvent(EventType.TEXT_UNIT, "\"Value64.1");
		testEvent(EventType.END_GROUP, null);
		
		// Line 21
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 22
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.3");
		testEvent(EventType.END_GROUP, null);
		
		// Line 23
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value64.4");
		testEvent(EventType.TEXT_UNIT, "\"Value65.1");
		testEvent(EventType.TEXT_UNIT, "Value65.2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 24
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value65.3");
		testEvent(EventType.TEXT_UNIT, "Value65.4");
		testEvent(EventType.END_GROUP, null);
		
		// Line 25
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value65.5");
		testEvent(EventType.TEXT_UNIT, "Value66");
		testEvent(EventType.END_GROUP, null);
		// -------------------------------------------------
		
		// Line 27
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value71");
		testEvent(EventType.TEXT_UNIT, "Value72 \"quoted part 1\"\"quoted part 2\" value");
		testEvent(EventType.TEXT_UNIT, "Value73");
		testEvent(EventType.TEXT_UNIT, "Value74");		
		testEvent(EventType.END_GROUP, null);
		
		// Line 28
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value81");
		testEvent(EventType.TEXT_UNIT, "\"Value82 with unclosed quote"); // Preserve the wrong quotation
		testEvent(EventType.END_GROUP, null);
		
		// Line 29
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "Value91");
		testEvent(EventType.TEXT_UNIT, "Value92");
		testEvent(EventType.END_GROUP, null);
		
		// Line 30
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueA1");
		testEvent(EventType.TEXT_UNIT, "ValueA2");
		testEvent(EventType.END_GROUP, null);
		
		// Line 31
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueB1");
		testEvent(EventType.TEXT_UNIT, "Value\"B2,Va\"lueB3");		// If quotation marks are not around field, preserve them 
		testEvent(EventType.TEXT_UNIT, "Va\"lueB4\"");
		testEvent(EventType.END_GROUP, null);
		
		// Line 32
		testEvent(EventType.START_GROUP, null);		
		testEvent(EventType.TEXT_UNIT, "ValueC1");
		testEvent(EventType.TEXT_UNIT, "\"ValueC2");		 
		testEvent(EventType.TEXT_UNIT, "ValueC3");			
		testEvent(EventType.TEXT_UNIT, "\"ValueC4");
		testEvent(EventType.TEXT_UNIT, "ValueC5");
		testEvent(EventType.END_GROUP, null);
		
		testEvent(EventType.END_DOCUMENT, null);
		filter.close();
		
		
//		input = TableFilterTest.class.getResourceAsStream("/csv_test3.txt");
//		assertNotNull(input);
//		
//		params.wrapMode = WrapMode.SPACE;
//		filter.open(new RawDocument(input, "UTF-8", "en"));
//		
//		testEvent(EventType.START_DOCUMENT, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value11");
//		testEvent(EventType.TEXT_UNIT, "Value12");
//		testEvent(EventType.TEXT_UNIT, "Value13");
//		testEvent(EventType.TEXT_UNIT, "Value14");		
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value21");
//		testEvent(EventType.TEXT_UNIT, "Value22.1,Value22.2, Value22.3");
//		testEvent(EventType.TEXT_UNIT, "Value23");
//		testEvent(EventType.TEXT_UNIT, "Value24");		
//		testEvent(EventType.END_GROUP, null);
//		
//		// Correct multiline chunk
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value31");
//		testEvent(EventType.TEXT_UNIT, "Value32");
//		testEvent(EventType.TEXT_UNIT, "Value33");
////		testEvent(EventType.TEXT_UNIT, "Value34.1\nValue34.2\nValue34.3\nValue34.4,Value34.5");
//		testEvent(EventType.TEXT_UNIT, "Value34.1,Value34.2,Value34.3,Value34.4,Value34.5");
//		testEvent(EventType.TEXT_UNIT, "Value35");
//		testEvent(EventType.END_GROUP, null);
//		
//		// Incorrect multiline chunk 1
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value31");
//		testEvent(EventType.TEXT_UNIT, "Value32");
//		testEvent(EventType.TEXT_UNIT, "Value33");
//		testEvent(EventType.TEXT_UNIT, "\"Value34.1");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.2");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.3");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.4");
//		testEvent(EventType.TEXT_UNIT, "Value35.1,Value35.2");
//		testEvent(EventType.END_GROUP, null);		
//		
//		// Incorrect multiline chunk 2
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value31");
//		testEvent(EventType.TEXT_UNIT, "Value32");
//		testEvent(EventType.TEXT_UNIT, "Value33");
//		testEvent(EventType.TEXT_UNIT, "\"Value34.1");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.2");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.3");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.4");
//		testEvent(EventType.TEXT_UNIT, "Value35.1,Value35.2,Value35.3,Value35.4");
//		testEvent(EventType.END_GROUP, null);
//		
//		// Incorrect multiline chunk 3
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value31");
//		testEvent(EventType.TEXT_UNIT, "Value32");
//		testEvent(EventType.TEXT_UNIT, "Value33");
//		testEvent(EventType.TEXT_UNIT, "\"Value34.1");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.2");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.3");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value34.4");
//		testEvent(EventType.TEXT_UNIT, "\"Value35.1,Value35.2");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value35.3");
//		testEvent(EventType.TEXT_UNIT, "Value35.4");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value35.5");
//		testEvent(EventType.TEXT_UNIT, "Value35.6");
//		testEvent(EventType.END_GROUP, null);
//		// -------------------------------------------------
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value41");
//		testEvent(EventType.TEXT_UNIT, "Value42 \"quoted part 1\" \"quoted part 2\" value");
//		testEvent(EventType.TEXT_UNIT, "Value43");
//		testEvent(EventType.TEXT_UNIT, "Value44");		
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value51");
//		testEvent(EventType.TEXT_UNIT, "\"Value52 with unclosed quote"); // Preserve the wrong quotation
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value61");
//		testEvent(EventType.TEXT_UNIT, "Value62");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value71");
//		testEvent(EventType.TEXT_UNIT, "Value72");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value81");
//		testEvent(EventType.TEXT_UNIT, "\"Value\"82");		// If quotation marks are not around field, preserve them 
//		testEvent(EventType.TEXT_UNIT, "Value83");			// If quotation marks are not around field, preserve them
//		testEvent(EventType.TEXT_UNIT, "Va\"lue84\"");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.START_GROUP, null);
//		testEvent(EventType.TEXT_UNIT, "Value91");
//		testEvent(EventType.TEXT_UNIT, "\"Value92");		 
//		testEvent(EventType.TEXT_UNIT, "Value93");			
//		testEvent(EventType.TEXT_UNIT, "\"Value94");
//		testEvent(EventType.TEXT_UNIT, "Value95");
//		testEvent(EventType.END_GROUP, null);
//		
//		testEvent(EventType.END_DOCUMENT, null);
//		
//		filter.close();
	}

	
	@Test
	public void testDoubleExtraction () {
		// Read all files in the data directory
//		URL url = TableFilterTest.class.getResource("/csv_test1.txt");
//		String root = Util.getDirectoryName(url.getPath());
//		root = Util.getDirectoryName(root) + "/data/";
		
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();
		
		list.add(new InputDocument(root + "csv_test1.txt", ""));
		list.add(new InputDocument(root + "csv_test2.txt", ""));
		list.add(new InputDocument(root + "csv_test3.txt", ""));
		list.add(new InputDocument(root + "csv.txt", ""));
		list.add(new InputDocument(root + "csv2.txt", ""));
		list.add(new InputDocument(root + "csv3.txt", ""));
		list.add(new InputDocument(root + "CSVTest_96.txt", ""));
		list.add(new InputDocument(root + "CSVTest_97.txt", ""));
		list.add(new InputDocument(root + "CSVTesting01.csv", ""));
		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", "en", "fr"));
	}


	
	// Helpers
	private String getFullFileName(String fileName) {
//		URL url = TableFilterTest.class.getResource("/csv_testb.txt");
//		String root = Util.getDirectoryName(url.getPath());
//		root = Util.getDirectoryName(root) + "/data/";
		return root + fileName;
	}
	
	private void testEvent(EventType expectedType, String expectedText) {
		assertNotNull(filter);
		
		Event event = filter.next();		
		assertNotNull(event);
		
		assertTrue(event.getEventType() == expectedType);
		
		switch (event.getEventType()) {
		case TEXT_UNIT:
			IResource res = event.getResource();
			assertTrue(res instanceof TextUnit);
			
			// assertEquals(expectedText, ((TextUnit) res).toString());
			assertEquals(expectedText, TextUnitUtils.getSourceText((TextUnit) res, true));
			break;
			
		case DOCUMENT_PART:
			if (expectedText == null) break;
			res = event.getResource();
			assertTrue(res instanceof DocumentPart);
			
			ISkeleton skel = res.getSkeleton();
			if (skel != null) {
				assertEquals(expectedText, skel.toString());
			}
			break;
		}
	}
	
	private void testEvent(EventType expectedType, String source, String expName, String target, String language, String comment) {
		
		assertNotNull(filter);
		
		Event event = filter.next();		
		assertNotNull(event);
		
		assertTrue(event.getEventType() == expectedType);
		
		switch (event.getEventType()) {
		
		case TEXT_UNIT:
			IResource res = event.getResource();
			assertTrue(res instanceof TextUnit);
			TextUnit tu = (TextUnit) res;
			
			assertEquals(source, tu.toString());
			
			Property prop = tu.getSourceProperty(AbstractLineFilter.LINE_NUMBER);
			assertNotNull(prop);
			
			if (!Util.isEmpty(expName)) {
				assertEquals(expName, tu.getName());
			}
			
			if (!Util.isEmpty(target) && !Util.isEmpty(language)) {
				
				TextContainer trg = tu.getTarget(language);
				assertNotNull(trg);
				assertEquals(target, trg.toString());
			}
			
			if (!Util.isEmpty(comment)) {
				
				prop = tu.getProperty(Property.NOTE);
				assertNotNull(prop);
				assertEquals(comment, prop.toString());
			}
			
			break;
		}
			
	}

	private Parameters getParameters() {
		IParameters punk = filter.getParameters();
		
		if (punk instanceof Parameters)
			return (Parameters) punk;
		else
			return null;
	}

	private String getSkeleton (String fileName) throws UnsupportedEncodingException {
		IFilterWriter writer;
		ByteArrayOutputStream writerBuffer;
										
		writer = filter.createFilterWriter();		
		try {						
			// Open the input
			filter.open(new RawDocument((new File(fileName)).toURI(), "UTF-8", "en", "fr"));
			
			// Prepare the output
			writer.setOptions("fr", "UTF-16");
			writerBuffer = new ByteArrayOutputStream();
			writer.setOutput(writerBuffer);
			
			// Process the document
			Event event;
			while ( filter.hasNext() ) {
				event = filter.next();
				writer.handleEvent(event);
			}
		}
		finally {
			if ( filter != null ) filter.close();
			if ( writer != null ) writer.close();
		}
		return new String(writerBuffer.toByteArray(), "UTF-16");
	}
	
	private String streamAsString(InputStream input) throws IOException {
		BufferedReader reader = null;
		reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

		StringBuilder tmp = new StringBuilder();
		char[] buf = new char[2048];
		int count = 0;
		while (( count = reader.read(buf)) != -1 ) {
			tmp.append(buf, 0, count);
		}
		
        return tmp.toString();
    }

	private ArrayList<Event> getEvents (String snippet,
		String srcLang,
		String trgLang)
	{
		ArrayList<Event> list = new ArrayList<Event>();
		filter.open(new RawDocument(snippet, srcLang, trgLang));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}

	
}
