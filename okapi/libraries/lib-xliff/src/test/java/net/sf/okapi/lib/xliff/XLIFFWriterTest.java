package net.sf.okapi.lib.xliff;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;

public class XLIFFWriterTest {

	@Test
	public void testTwoSegments () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		
		Unit unit = new Unit("id");
		unit.appendNewSegment().setSource("Source 1.");
		unit.appendNewSegment().setSource("Source 2.");
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<file source=\"en\">\n<unit id=\"id\">\n<segment>\n<source>Source 1.</source>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source>\n</segment>\n</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentWithMatch () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		
		Unit unit = new Unit("id");
		Segment seg = unit.appendNewSegment();
		seg.setSource("Source 1.");
		seg.addCandidate(new Alternate("Source candidate 1.", "Target candidate 1."));
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<file source=\"en\">\n<unit id=\"id\">\n<segment>\n<source>Source 1.</source>\n"
			+ "<matches>\n<match>\n<source>Source candidate 1.</source>\n"
			+ "<target>Target candidate 1.</target>\n</match>\n</matches>\n</segment>\n</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentOrder () {
		Unit unit = new Unit("id");
		// Source = "Source A. Source B."
		// Target = "Target B. Target A."
		Segment seg = unit.appendNewSegment();
		seg.setSource("Source A.");
		seg.settarget("Target A.");
		seg.setTargetOrder(3);
		unit.appendNewIgnorable().setSource(" ");
		seg = unit.appendNewSegment();
		seg.setSource("Source B.");
		seg.settarget("Target B");
		seg.setTargetOrder(1);
		
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.writeUnit(unit);
		writer.close();
		
		assertEquals("<file source=\"en\">\n<unit id=\"id\">\n<segment>\n<source>Source A.</source>\n"
			+ "<target order=\"3\">Target A.</target>\n"
			+ "</segment>\n<ignorable>\n<source> </source>\n</ignorable>\n"
			+ "<segment>\n<source>Source B.</source>\n"
			+ "<target order=\"1\">Target B</target>\n</segment>\n</unit>\n",
			strWriter.toString());
	}
	
}
