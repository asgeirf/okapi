package net.sf.okapi.applications.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.filters.IInputFilter;
import net.sf.okapi.common.filters.IOutputFilter;
import net.sf.okapi.common.resource.CodeFragment;
import net.sf.okapi.common.resource.Container;
import net.sf.okapi.common.resource.ExtractionItem;
import net.sf.okapi.common.resource.IContainer;
import net.sf.okapi.common.resource.IExtractionItem;
import net.sf.okapi.common.resource.IFragment;
import net.sf.okapi.filters.xml.XMLReader;

public class Main {

	private static void testContainer () {
		try {
			System.out.println("---start testContainer---");
			IContainer cnt = new Container();
			cnt.append("t1");
			cnt.append(new CodeFragment(IContainer.CODE_ISOLATED, 1, "<br/>"));
			cnt.append("t2");
			System.out.println("out 1: " + cnt.toString());
			String s1 = cnt.getCodedText();
			cnt.setContent(s1);
			System.out.println("out 2: " + cnt.toString());
			Map<Integer, IFragment> codes = cnt.getCodes();
			cnt.setContent(s1, codes);
			System.out.println("out 3: " + cnt.toString());
			
			List<IFragment> list = cnt.getFragments();
			for ( IFragment frag : list ) {
				System.out.println(String.format("istext=%s, content='%s'",
					(frag.isText() ? "yes" : "no "),
					frag.toString()));
			}
			
			cnt.setProperty("test1", "value1");
			System.out.println(String.format("name='test1' value='%s'",
				cnt.getProperty("test1")));
			cnt.setProperty("test1", null);
			System.out.println(String.format("name='test1' value='%s'",
				cnt.getProperty("test1")));
			cnt.setProperty("test1", "value1 again");
			System.out.println(String.format("name='test1' value='%s'",
				cnt.getProperty("test1")));
			cnt.clearProperties();
			System.out.println(String.format("name='test1' value='%s'",
				cnt.getProperty("test1")));
		}		
		catch ( Exception e ) {
			System.out.println(e.getLocalizedMessage());
		}
		System.out.println("---end testContainer---");
	}
	
	private static void testXMLReader () {
		try {
			System.out.println("---start testXMLReader---");
			XMLReader reader = new XMLReader();
			String inputName = "Test01.xml";
			InputStream input = new FileInputStream(inputName);
			reader.open(input, inputName);
			int n;
			do {
				n = reader.read();
				IExtractionItem item = reader.getSourceItem();
				switch ( n ) {
				case XMLReader.RESULT_STARTTRANSUNIT:
					System.out.println("sTU:"+item.getName()+",'"+item.getContent().toString()+"'");
					break;
				case XMLReader.RESULT_ENDTRANSUNIT:
					System.out.println("eTU:"+item.getName()+",'"+item.getContent().toString()+"'");
					break;
				}
			} while ( n > XMLReader.RESULT_ENDINPUT );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		System.out.println("---end testXMLReader---");
	}
	
	private static void testItem () {
		try {
			System.out.println("---start testItem---");
			IExtractionItem item = new ExtractionItem();
			
			item.addSegment(new Container("This is segment 1. "));
			item.addSegment(new Container("This is segment 2. "));
			List<IContainer> list = item.getSegments();
			for ( IContainer seg : list ) {
				System.out.println("seg='"+seg.toString()+"'");
			}
			System.out.println("all segs= '"+item.toString()+"'");
			item.addSegment(new Container("This is segment 3."));
			list = item.getSegments();
			for ( IContainer seg : list ) {
				System.out.println("seg='"+seg.toString()+"'");
			}
			System.out.println("all segs= '"+item.toString()+"'");
			
			item.removeSegmentation();
			System.out.println("After removing segs:");
			list = item.getSegments();
			for ( IContainer seg : list ) {
				System.out.println("seg='"+seg.toString()+"'");
			}
			System.out.println("all segs= '"+item.toString()+"'");
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		System.out.println("---end testItem---");
	}
	
	private static void testFilter () {
		try {
			System.out.println("---start testContainer---");
		
			String inputFile = "test.properties";
			IInputFilter inputFlt = new net.sf.okapi.filters.properties.InputFilter();
			FileInputStream input = new FileInputStream(inputFile);
			inputFlt.initialize(input, inputFile, null, "utf-16", null, null);
			
			IOutputFilter outputFlt = new net.sf.okapi.filters.properties.OutputFilter();
			FileOutputStream output = new FileOutputStream("test.out.properties");
			outputFlt.initialize(output, "us-ascii", null);
			
			inputFlt.setOutput(outputFlt);
			inputFlt.process();
		}
		catch ( Exception e ) {
			System.out.println(e.getLocalizedMessage());
		}
		System.out.println("---end testContainer---");
	}

	public static void main (String[] args)
		throws Exception
	{
		testXMLReader();
		//testItem();
		//testContainer();
		//testFilter();
	}		
		
}
