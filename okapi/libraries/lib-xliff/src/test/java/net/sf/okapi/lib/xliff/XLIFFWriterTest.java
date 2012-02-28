package net.sf.okapi.lib.xliff;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;
import org.oasisopen.xliff.v2.IFragment;
import org.oasisopen.xliff.v2.ISegment;
import org.oasisopen.xliff.v2.InlineType;

public class XLIFFWriterTest {

	@Test
	public void testEmptyDoc () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAOUTSIDE);

		writer.writeEndDocument();
		
		writer.close();
		assertEquals("",
			strWriter.toString());
	}

	@Test
	public void testOneUnitWithEmpties () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAOUTSIDE);

		// Empty so no output
		writer.writeUnit(new Unit("id1")); 
		// Empty part, so output
		Unit unit = new Unit("id2");
		unit.appendNewIgnorable();
		writer.writeUnit(unit);
		// One empty segment so output
		unit = new Unit("id3");
		unit.appendNewSegment(); 
		writer.writeUnit(unit);
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id2\">\n"
			+ "<ignorable>\n"
			+ "<source></source>\n"
			+ "</ignorable>\n"
			+ "</unit>\n"
			+ "<unit id=\"id3\">\n"
			+ "<segment>\n"
			+ "<source></source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n"
			+ "</xliff>\n",
			strWriter.toString());
	}

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
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source 1.</source>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>Source 2.</source>\n"
			+ "</segment>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentWithMatches () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAOUTSIDE);
		
		Unit unit = new Unit("id");
		ISegment seg = unit.appendNewSegment();
		seg.setSource("Source 1.");
		seg.addCandidate(createAlternate("seg"));
		unit.addCandidate(createAlternate("unit"));
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source 1.</source>\n"
			+ "<matches>\n"
			+ "<match similarity=\"77\">\n"
			+ "<source>seg-text<ph id=\"1\" nid=\"d1\"/></source>\n"
			+ "<target>SEG-TEXT<ph id=\"1\" nid=\"d1\"/></target>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">&lt;br/></data>\n"
			+ "</originalData>\n"
			+ "</match>\n"
			+ "</matches>\n"
			+ "</segment>\n"
			+ "<matches>\n"
			+ "<match similarity=\"77\">\n"
			+ "<source>unit-text<ph id=\"1\" nid=\"d1\"/></source>\n"
			+ "<target>UNIT-TEXT<ph id=\"1\" nid=\"d1\"/></target>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">&lt;br/></data>\n"
			+ "</originalData>\n"
			+ "</match>\n"
			+ "</matches>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentWithNotes () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAOUTSIDE);
		
		Unit unit = new Unit("id");
		ISegment seg = unit.appendNewSegment();
		seg.setSource("Source 1.");
		seg.addNote(new Note("seg-note1", Note.AppliesTo.SOURCE));
		seg.addNote(new Note("seg-note2", Note.AppliesTo.DEFAULT));
		unit.addNote(new Note("unit-note1", Note.AppliesTo.TARGET));
		unit.addNote(new Note("unit-note2", Note.AppliesTo.DEFAULT));
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source 1.</source>\n"
			+ "<notes>\n"
			+ "<simpleNote appliesTo=\"source\">seg-note1</simpleNote>\n"
			+ "<simpleNote>seg-note2</simpleNote>\n"
			+ "</notes>\n"
			+ "</segment>\n"
			+ "<notes>\n"
			+ "<simpleNote appliesTo=\"target\">unit-note1</simpleNote>\n"
			+ "<simpleNote>unit-note2</simpleNote>\n"
			+ "</notes>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentOutputWithCodesOutside () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAOUTSIDE);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\" nidEnd=\"d2\" nidStart=\"d1\">source</pc></source>\n"
			+ "<target><pc id=\"1\" nidEnd=\"d3\" nidStart=\"d1\">target</pc></target>\n"
			+ "</segment>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">&lt;b></data>\n"
			+ "<data id=\"d2\">&lt;/b></data>\n"
			+ "<data id=\"d3\">&lt;/B></data>\n"
			+ "</originalData>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentOutputWithCodesInside () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_DATAINSIDE);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><sc id=\"1\">&lt;b></sc>source<ec rid=\"1\">&lt;/b></ec></source>\n"
			+ "<target><sc id=\"1\">&lt;b></sc>target<ec rid=\"1\">&lt;/B></ec></target>\n"
			+ "</segment>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentOutputWithoutOriginalData () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setInlineStyle(Fragment.STYLE_NODATA);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\">source</pc></source>\n"
			+ "<target><pc id=\"1\">target</pc></target>\n"
			+ "</segment>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	@Test
	public void testSegmentOrder () {
		Unit unit = new Unit("id");
		// Source = "Source A. Source B."
		// Target = "Target B. Target A."
		ISegment seg = unit.appendNewSegment();
		seg.setSource("Source A.");
		seg.setTarget("Target A.");
		seg.setTargetOrder(3);
		unit.appendNewIgnorable().setSource(" ");
		seg = unit.appendNewSegment();
		seg.setSource("Source B.");
		seg.setTarget("Target B");
		seg.setTargetOrder(1);
		
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.writeUnit(unit);
		writer.close();
		
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file>\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source A.</source>\n"
			+ "<target order=\"3\">Target A.</target>\n"
			+ "</segment>\n"
			+ "<ignorable>\n"
			+ "<source> </source>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source>Source B.</source>\n"
			+ "<target order=\"1\">Target B</target>\n"
			+ "</segment>\n"
			+ "</unit>\n",
			strWriter.toString());
	}

	private void createSegment (Unit unit) {
		ISegment seg = unit.appendNewSegment();
		seg.getSource().append(InlineType.OPENING, "1", "<b>");
		seg.getSource().append("source");
		seg.getSource().append(InlineType.CLOSING, "1", "</b>");
		IFragment frag = seg.getTarget(true);
		frag.append(InlineType.OPENING, "1", "<b>");
		frag.append("target");
		frag.append(InlineType.CLOSING, "1", "</B>");
	}
	
	private Candidate createAlternate (String prefix) {
		Candidate alt = new Candidate();
		alt.setSimilarity(77);
		Fragment frag = new Fragment(alt.getDataStore());
		frag.append(prefix+"-text");
		frag.append(InlineType.PLACEHOLDER, "1", "<br/>");
		alt.setSource(frag);
		
		frag = new Fragment(alt.getDataStore());
		frag.append(prefix.toUpperCase()+"-TEXT");
		frag.append(InlineType.PLACEHOLDER, "1", "<br/>");
		alt.setTarget(frag);
		
		return alt;
	}
	
}
