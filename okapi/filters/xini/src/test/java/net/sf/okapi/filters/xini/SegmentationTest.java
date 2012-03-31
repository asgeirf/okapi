package net.sf.okapi.filters.xini;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.filters.xini.jaxb.INITR;
import net.sf.okapi.filters.xini.jaxb.Seg;
import net.sf.okapi.filters.xini.jaxb.Xini;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * segmentation rules of the defaultSegmentation.srx:
 * 1. first sentence has to end with a period
 * 2. second segment has to start with a capital letter
 * 3. between the sentences has to be a whitespace character
 *
 * ...otherwise the default-segmentation-rule is not working correct
 *
 * correct example: 'Sentence1. Sentence2.' -> 2 segments after segmentation
 * wrong example: 'Sentence1. sentence2.' -> 1 segment after segmentation
 */
public class SegmentationTest {

	private XINIFilter filter = new XINIFilter();
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locDE = LocaleId.fromString("de");
	private StepHelper segmentizer;
	private XiniTestHelper xiniHelper = new XiniTestHelper();
	private String startSnippetForTable =
		"<?xml version=\"1.0\" ?>" +
		"<Xini SchemaVersion=\"1.0\" xsi:noNamespaceSchemaLocation=\"http://www.ontram.com/xsd/xini.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
		"	<Main>" +
		"		<Page PageID=\"1\">" +
		"			<Elements>" +
		"				<Element ElementID=\"10\" Size=\"50\">" +
		"					<ElementContent>";
	private String endSnippetForTable =
		"					</ElementContent>" +
		"				</Element>" +
		"			</Elements>" +
		"		</Page>" +
		"	</Main>" +
		"</Xini>";
	private String startSnippet =
		startSnippetForTable +
		"						<Fields>" +
		"							<Field FieldID=\"0\">";
	private String endSnippet =
		"							</Field>" +
		"						</Fields>" +
		endSnippetForTable;

	@Before
	public void prepare() {
		URL url =  ClassLoader.getSystemResource("defaultSegmentation.srx");
		String path = url.getFile();
		((net.sf.okapi.filters.xini.Parameters) filter.getParameters()).setUseOkapiSegmentation(true);
		Parameters params = new Parameters();
		params.setSourceSrxPath(path);
		SegmentationStep segmentationStep = new SegmentationStep();
		segmentationStep.setParameters(params);
		segmentationStep.setSourceLocale(locDE);
		segmentationStep.setTargetLocale(locEN);
		segmentizer = new StepHelper(segmentationStep);
	}

	@Test
	public void formattingsAreNotBreakingApart() {
		String segSnippet = "<Seg SegID=\"0\"><sub>Don't</sub><sub> break these sentences. Apart.</sub></Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);

		checkContent(firstSeg, "<sub>Don't</sub><sub> break these sentences. Apart.</sub>");
	}

	@Test
	public void formattingsAreNotBreakingApart2() {
		String segSnippet = "<Seg SegID=\"0\"><sub>Don't</sub><i> break these sentences. Apart.</i></Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);

		checkContent(firstSeg, "<sub>Don't</sub><i> break these sentences. Apart.</i>");
	}

	@Test
	public void formattingsAreNotBreakingApart3() {
		String segSnippet = "<Seg SegID=\"0\"><sub>Don't break. Don't</sub><i> break these sentences. Apart.</i></Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "<sub>Don't break. Don't</sub><i> break these sentences. Apart.</i>");
	}

	@Test
	public void sentencesAreSegmentedAndWhitespaceIsSavedInAttribute() {
		String segSnippet = "<Seg SegID=\"0\">T1. T2.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		assertEquals(2, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);

		checkContent(firstSeg, "T1.");
		checkContent(secondSeg, "T2.");
	}

	@Test
	public void newSegmentsHaveIncreasingIDs() {
		String segSnippet = "<Seg SegID=\"0\">T1. T2.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);

		assertEquals(2, segsOfFirstField.size());

		assertEquals(0, firstSeg.getSegID());
		assertEquals(1, secondSeg.getSegID());

		assertEquals(0, firstSeg.getSegmentIDBeforeSegmentation().intValue());
		assertEquals(0, secondSeg.getSegmentIDBeforeSegmentation().intValue());
	}

	@Test
	public void originalSegmentIdIsSavedInAttribute() {
		String segSnippet = "<Seg SegID=\"0\">T1. T2.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);

		assertEquals(2, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);

		assertEquals(0, firstSeg.getSegmentIDBeforeSegmentation().intValue());
		assertEquals(0, secondSeg.getSegmentIDBeforeSegmentation().intValue());
	}

	@Test
	public void placeholderDoesntChange() {
		String segSnippet = "<Seg SegID=\"0\"><ph ID=\"1\" type=\"style\">A Sentence.</ph></Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;


		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);

		assertEquals(1, segsOfFirstField.size());
		checkContent(firstSeg, "<ph ID=\"1\" type=\"style\">A Sentence.</ph>");
	}

	@Test
	public void placeholdersAreNotBrokenApart() {
		String segSnippet = "<Seg SegID=\"0\">Sentence1<ph ID=\"1\"> with ph. Sentence2</ph> with closing ph.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);

		assertEquals(1, segsOfFirstField.size());
		checkContent(firstSeg, "Sentence1<ph ID=\"1\"> with ph. Sentence2</ph> with closing ph.");
	}

	@Test
	public void formattingsAreNotBrokenApart(){
		String segSnippet = "<Seg SegID=\"0\">Sentence1<b> with b. Sentence2</b> with closing b.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);

		assertEquals(1, segsOfFirstField.size());
		checkContent(firstSeg, "Sentence1<b> with b. Sentence2</b> with closing b.");
	}

	@Test
	public void formattingTagsAndPlaceholdersDontChange(){
		String segSnippet = "<Seg SegID=\"0\">Sentence1<b><i><u><sup><sub><ph ID=\"1\"> with many formatting. Sentence2</ph></sub></sup></u></i></b> with closing formatting.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);

		assertEquals(1, segsOfFirstField.size());
		checkContentWithoutReorderAttributes(firstSeg, "Sentence1<b><i><u><sup><sub><ph ID=\"1\"> with many formatting. Sentence2</ph></sub></sup></u></i></b> with closing formatting.");
	}

	@Test
	public void lineBreaksArePreserved(){
		String segSnippet = "<Seg SegID=\"0\">Sentence 1. A new <br/> sentence with <br/> line break.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);

		assertEquals(2, segsOfFirstField.size());
		checkContent(firstSeg, "Sentence 1.");
		checkContent(secondSeg, "A new <br/> sentence with <br/> line break.");
	}

	@Test
	public void surroundingWhitespacesAreMovedIntoAttributes(){
		String segSnippet = "<Seg SegID=\"0\"> Sentence1. </Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);

		assertEquals(1, segsOfFirstField.size());
		checkContent(firstSeg, "Sentence1.");
		checkLeadingSpacer(firstSeg, " ");
		checkTrailingSpacer(firstSeg, " ");
	}

	@Test
	public void whitespacesFromInBetweenAreMovedIntoAttributes(){
		String segSnippet = "<Seg SegID=\"0\"> Sentence1. Sentence2. </Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(2, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "Sentence1.");
		checkLeadingSpacer(firstSeg, " ");
		checkTrailingSpacer(firstSeg, " ");

		Seg secondSeg = segsOfFirstField.get(1);
		checkContent(secondSeg, "Sentence2.");
		checkLeadingSpacer(secondSeg, null);
		checkTrailingSpacer(secondSeg, " ");
	}

	@Test
	public void codesAreNotMovedIntoAttributes(){
		String segSnippet = "<Seg SegID=\"0\"> <ph ID=\"1\"/> Sentence1 <sph ID=\"2\"/> </Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "<ph ID=\"1\"/> Sentence1 <sph ID=\"2\"/>");
		checkLeadingSpacer(firstSeg, " ");
		checkTrailingSpacer(firstSeg, " ");
	}

	@Test
	public void isolatedPlaceholdersArePreserved(){
		String segSnippet = "<Seg SegID=\"0\">Sentence <sph ID=\"2\"/>one. Sentence <eph ID=\"3\"/>two.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(2, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);

		checkContent(firstSeg, "Sentence <sph ID=\"2\"/>one.");
		checkContent(secondSeg, "Sentence <eph ID=\"3\"/>two.");
	}

	@Test
	@Ignore("This it not implemented yet")
	public void placeholderIDsStartAt1InEachSegment(){
		String segSnippet = "<Seg SegID=\"0\"><ph ID=\"1\">Sentence 1.</ph> <ph ID=\"2\">Sentence 2.</ph> <ph ID=\"3\">Sentence 3.</ph></Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		Seg firstSeg = segsOfFirstField.get(0);
		Seg secondSeg = segsOfFirstField.get(1);
		Seg thirdSeg = segsOfFirstField.get(2);

		assertEquals(3, segsOfFirstField.size());

		checkContent(firstSeg, "<ph ID=\"1\">Sentence 1.</ph>");
		checkContent(secondSeg, "<ph ID=\"1\">Sentence 2.</ph>");
		checkContent(thirdSeg, "<ph ID=\"1\">Sentence 3.</ph>");
	}

	@Test
	public void nestedPlaceholdersWithSameIdArePreservedUnchanged(){
		String segSnippet = "<Seg SegID=\"0\">Click <ph ID=\"1\" type=\"style\"><ph ID=\"1\" type=\"link\">here</ph></ph> to read more.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "Click <ph ID=\"1\" type=\"style\"><ph ID=\"1\" type=\"link\">here</ph></ph> to read more.");
	}

	@Test
	public void emptyPlaceholdersWithSameIdArePreservedUnchanged(){
		String segSnippet = "<Seg SegID=\"0\"><ph ID=\"1\"/>List item 1 <br/><ph ID=\"1\"/>List item 2</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "<ph ID=\"1\"/>List item 1 <br/><ph ID=\"1\"/>List item 2");
	}

	@Test
	public void placeholdersWithSameIdArePreservedUnchanged(){
		String segSnippet = "<Seg SegID=\"0\">Click <ph ID=\"1\" type=\"style\">save</ph> or <ph ID=\"1\" type=\"style\">save as...</ph>.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;

		List<Seg> segsOfFirstField = doSegmentation(xiniSnippet);
		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, "Click <ph ID=\"1\" type=\"style\">save</ph> or <ph ID=\"1\" type=\"style\">save as...</ph>.");
	}

	@Test
	public void desegmentizedXiniContainsTrailingWhitespaces(){
		String segSnippet = "<Seg LeadingSpacer=\" \" TrailingSpacer=\" \" SegmentIDBeforeSegmentation=\"0\" SegID=\"0\">Sentence 1.</Seg>" +
                "<Seg TrailingSpacer=\" \" SegmentIDBeforeSegmentation=\"0\" SegID=\"1\">Sentence 2.</Seg>" +
                "<Seg TrailingSpacer=\" \" SegmentIDBeforeSegmentation=\"0\" SegID=\"2\">Sentence 3.</Seg>";
		String xiniSnippet = startSnippet + segSnippet + endSnippet;
		
		((net.sf.okapi.filters.xini.Parameters)filter.getParameters()).setUseOkapiSegmentation(false);

		List<Seg> segsOfFirstField = doDesegmentation(xiniSnippet);
		assertEquals(1, segsOfFirstField.size());

		Seg firstSeg = segsOfFirstField.get(0);
		checkContent(firstSeg, " Sentence 1. Sentence 2. Sentence 3. ");
	}

	private List<INITR> getRowsOfFirstTable(Xini segmentizedXini) {
		List<INITR> segsOfFirstTable = segmentizedXini.getMain().getPage().get(0)
		.getElements().getElement().get(0).getElementContent()
		.getINITable().getTR();
		return segsOfFirstTable;
	}

	private List<Seg> getSegListOfFirstField(Xini segmentizedXini){
		List<Seg> segList = segmentizedXini.getMain().getPage().get(0)
		.getElements().getElement().get(0).getElementContent()
		.getFields().getField().get(0).getSeg();
		return segList;
	}

	private List<Seg> doSegmentation(String xiniSnippet) {
		Xini segmentizedXini = makeSegmentizedXiniFrom(xiniSnippet);
		List<Seg> segsOfFirstField = getSegListOfFirstField(segmentizedXini);
		return segsOfFirstField;
	}

	private List<Seg> doDesegmentation(String xiniSnippet) {
		Xini segmentizedXini = makeDesegmentizedXiniFrom(xiniSnippet);
		List<Seg> segsOfFirstField = getSegListOfFirstField(segmentizedXini);
		return segsOfFirstField;
	}

	private Xini makeSegmentizedXiniFrom(String xiniSnippet) {
		List<Event> before = toEvents(xiniSnippet);
		List<Event> segmentized = segmentize(before);
		Xini segmentizedXini = toXini(segmentized);
		return segmentizedXini;
	}

	private Xini makeDesegmentizedXiniFrom(String xiniSnippet) {
		List<Event> events = toEvents(xiniSnippet);
		Xini segmentizedXini = toXini(events);
		return segmentizedXini;
	}

	/**
	 * Compares the content of a segment with the expected content.
	 * For that compare, the segment will get serialized into a String.
	 * Also the xiniHelper is used to reorder the attributes of a xini element for assertion reasons.
	 *
	 * @param seg
	 * @param expectedContent
	 */
	private void checkContent(Seg seg, String expectedContent){
		String segContent = xiniHelper.serializeTextContent(seg);
		xiniHelper.assertEquivalent(expectedContent, segContent);
	}

	/**
	 * Compares the content of a segment with the expected content.
	 * For that compare, the segment will get serialized into a String.
	 *
	 * @param seg
	 * @param expectedContent
	 */
	private void checkContentWithoutReorderAttributes(Seg seg, String expectedContent){
		String segContent = xiniHelper.serializeTextContent(seg);
		assertEquals(expectedContent, segContent);
	}

	private void checkLeadingSpacer(Seg seg, String expectedLeadSpacer){
		String leadSpacerFirstSeg = seg.getLeadingSpacer();
		assertEquals(expectedLeadSpacer, leadSpacerFirstSeg);
	}

	private void checkTrailingSpacer(Seg seg, String expectedTrailSpacer){
		String trailSpacerFirstSeg = seg.getTrailingSpacer();
		assertEquals(expectedTrailSpacer, trailSpacerFirstSeg);
	}

	private List<Event> segmentize(List<Event> events) {
		return segmentizer.process(events);
	}

	private List<Event> toEvents(String snippet) {
		return xiniHelper.toEvents(snippet, filter, locEN, locDE);
	}

	private Xini toXini(List<Event> events) {
		return xiniHelper.toXini(events, filter);
	}
}