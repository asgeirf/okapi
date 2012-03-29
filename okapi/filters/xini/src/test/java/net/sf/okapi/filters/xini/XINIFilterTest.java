/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xini;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.filters.xini.jaxb.Element;
import net.sf.okapi.filters.xini.jaxb.Field;
import net.sf.okapi.filters.xini.jaxb.INITD;
import net.sf.okapi.filters.xini.jaxb.INITR;
import net.sf.okapi.filters.xini.jaxb.Page;
import net.sf.okapi.filters.xini.jaxb.Seg;
import net.sf.okapi.filters.xini.jaxb.TD;
import net.sf.okapi.filters.xini.jaxb.TR;
import net.sf.okapi.filters.xini.jaxb.TargetLanguages;
import net.sf.okapi.filters.xini.jaxb.TextContent;
import net.sf.okapi.filters.xini.jaxb.Xini;
import net.sf.okapi.filters.xini.jaxb.Element.ElementContent;

import org.junit.Test;

public class XINIFilterTest {

	private XINIFilter filter = new XINIFilter();
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locDE = LocaleId.fromString("de");
	private XiniTestHelper xiniHelper = new XiniTestHelper();

	private XINIFilter segmentationFilter = new XINIFilter();
	Parameters params = (Parameters)segmentationFilter.getParameters();
	private GenericContent fmt = new GenericContent();

	public XINIFilterTest() {
		params.setUseOkapiSegmentation(true);
	}

	@Test
	public void segmentBecomesTU () {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\"><Seg SegID=\"0\">tester</Seg></Field>" +
							"	</Fields>" +
						getEndSnippet();

		ITextUnit tu = FilterTestDriver.getTextUnit(toEvents(snippet), 1);
		assertNotNull(tu);
		TextContainer tuSrc = tu.getSource();
		assertTrue(tuSrc.contentIsOneSegment());

		TextFragment tf = tuSrc.getFirstContent();
		String firstTextFragmentContent = tf.getCodedText();
		assertEquals("tester", firstTextFragmentContent);
		List<Code> codes = tf.getCodes();
		assertTrue(codes.isEmpty());

		List<Segment> segments = tuSrc.getSegments().asList();
		assertEquals(1, segments.size());

		Segment segment = segments.get(0);
		assertEquals("0", segment.id);
		assertEquals("tester", segment.text.toText());
	}


	@Test
	public void formattingTagsBecomeCodes() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\"><b><i>a<sup>bb</sup>ccc</i>d</b></Seg>" +
							"			<Seg SegID=\"1\">e<sub>ff<br/>ggg</sub>hh<u>jj</u>k</Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		ITextUnit tu1 = FilterTestDriver.getTextUnit(toEvents(snippet), 1);
		TextContainer tuSrc1 = tu1.getSource();
		assertTrue(tuSrc1.contentIsOneSegment());
		TextFragment tf = tuSrc1.getFirstContent();
		assertEquals("abbcccd", tf.toText());

		List<Code> codes = tf.getClonedCodes();

		assertEquals(TagType.OPENING, codes.get(0).getTagType());
		assertEquals(Code.TYPE_BOLD, codes.get(0).getType());

		assertEquals(TagType.OPENING, codes.get(1).getTagType());
		assertEquals(Code.TYPE_ITALIC, codes.get(1).getType());

		assertEquals(TagType.OPENING, codes.get(2).getTagType());
		assertEquals("superscript", codes.get(2).getType());

		assertEquals(TagType.CLOSING, codes.get(3).getTagType());
		assertEquals("superscript", codes.get(3).getType());

		assertEquals(TagType.CLOSING, codes.get(4).getTagType());
		assertEquals(Code.TYPE_ITALIC, codes.get(4).getType());

		assertEquals(TagType.CLOSING, codes.get(5).getTagType());
		assertEquals(Code.TYPE_BOLD, codes.get(5).getType());

		ITextUnit tu2 = FilterTestDriver.getTextUnit(toEvents(snippet), 2);
		TextContainer tuSrc2 = tu2.getSource();
		tf = tuSrc2.getFirstContent();
		assertEquals("effggghhjjk", tf.toText());

		codes = tf.getClonedCodes();

		assertEquals(TagType.OPENING, codes.get(0).getTagType());
		assertEquals("subscript", codes.get(0).getType());

		assertEquals(TagType.PLACEHOLDER, codes.get(1).getTagType());
		assertEquals(Code.TYPE_LB, codes.get(1).getType());

		assertEquals(TagType.CLOSING, codes.get(2).getTagType());
		assertEquals("subscript", codes.get(2).getType());

		assertEquals(TagType.OPENING, codes.get(3).getTagType());
		assertEquals(Code.TYPE_UNDERLINED, codes.get(3).getType());

		assertEquals(TagType.CLOSING, codes.get(4).getTagType());
		assertEquals(Code.TYPE_UNDERLINED, codes.get(4).getType());
	}

	@Test
	public void segmentsAreGroupedInTUsByOriginalSegmentId() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\" SegmentIDBeforeSegmentation=\"0\">t1.</Seg>" +
							"			<Seg SegID=\"1\" SegmentIDBeforeSegmentation=\"0\">t2.</Seg>" +
							"			<Seg SegID=\"2\" SegmentIDBeforeSegmentation=\"1\">t3.</Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		ITextUnit tu1 = FilterTestDriver.getTextUnit(toEvents(snippet), 1);
		TextContainer tuSrc1 = tu1.getSource();
		assertFalse(tuSrc1.contentIsOneSegment());

		List<Segment> segments1 = tuSrc1.getSegments().asList();
		assertTrue(segments1.size() == 2);
		assertEquals("[t1.][t2.]", fmt.printSegmentedContent(tuSrc1, true));
		assertEquals("0", segments1.get(0).id);
		assertEquals("t1.", segments1.get(0).text.toText());
		assertEquals("1", segments1.get(1).id);
		assertEquals("t2.", segments1.get(1).text.toText());

		ITextUnit tu2 = FilterTestDriver.getTextUnit(toEvents(snippet), 2);
		TextContainer tuSrc2 = tu2.getSource();
		assertTrue(tuSrc2.contentIsOneSegment());

		List<Segment> segments2 = tuSrc2.getSegments().asList();
		assertTrue(segments2.size() == 1);
		assertEquals("[t3.]", fmt.printSegmentedContent(tuSrc2, true));
		assertEquals("0", segments2.get(0).id);
		assertEquals("t3.", segments2.get(0).text.toText());
	}

	@Test
	public void placeholdersBecomeCodes() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\"><ph ID=\"1\"><ph ID=\"2\" type=\"link\">a</ph>b</ph>cc<ph ID=\"3\"/></Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		//tu Content: phphabc
		ITextUnit tu = FilterTestDriver.getTextUnit(toEvents(snippet), 1);
		TextContainer tuSrc = tu.getSource();
		assertTrue(tuSrc.contentIsOneSegment());
		TextFragment tf = tuSrc.getFirstContent();

		List<Code> codes = tf.getClonedCodes();
		assertEquals(TagType.OPENING, codes.get(0).getTagType());
		assertEquals(1, codes.get(0).getId());

		assertEquals(TagType.OPENING, codes.get(1).getTagType());
		assertEquals(2, codes.get(1).getId());

		assertEquals(TagType.CLOSING, codes.get(2).getTagType());
		assertEquals(2, codes.get(2).getId());

		assertEquals(TagType.CLOSING, codes.get(3).getTagType());
		assertEquals(1, codes.get(3).getId());

		assertEquals(TagType.PLACEHOLDER, codes.get(4).getTagType());
		assertEquals(3, codes.get(4).getId());
	}

	@Test
	public void isolatedPlaceholdersBecomeCodes() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\">"+
							"				Inline placeholders <sph ID=\"1\" type=\"style\"/> must become codes<sph type=\"ph\" ID=\"2\"/>." +
							"				Has to work<eph ID=\"2\"/> with various types.<eph ID=\"1\" type=\"style\"/>" +
							"			</Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		ITextUnit tu1 = FilterTestDriver.getTextUnit(toEvents(snippet), 1);
		TextContainer tuSrc = tu1.getSource();
		assertTrue(tuSrc.contentIsOneSegment());
		TextFragment tf = tuSrc.getFirstContent();

		List<Code> codes = tf.getClonedCodes();

		assertEquals(TagType.OPENING, codes.get(0).getTagType());
		assertEquals(1, codes.get(0).getId());

		assertEquals(TagType.OPENING, codes.get(1).getTagType());
		assertEquals(2, codes.get(1).getId());

		assertEquals(TagType.CLOSING, codes.get(2).getTagType());
		assertEquals(2, codes.get(2).getId());
		assertEquals(TagType.CLOSING, codes.get(3).getTagType());
		assertEquals(1, codes.get(3).getId());
	}


//Reader + Writer:

	@Test
	public void placeholdersArePreservedAfterSegmentation() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\">Sentence <sph ID=\"1\" type=\"style\"/> one. <sph type=\"ph\" ID=\"2\"/>Two.</Seg>" +
							"			<Seg SegID=\"1\">Three <eph ID=\"2\"/></Seg>" +
							"           <Seg SegID=\"2\">Four <eph ID=\"1\" type=\"link\"/></Seg>" +
							"			<Seg SegID=\"3\">A line <br/> break</Seg>" +
							"			<Seg SegID=\"4\"><ph ID=\"3\"><ph ID=\"4\">a</ph>b</ph>cc</Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		List<Event> eventsSnippet = toEvents(snippet);
		Xini xini = toXini(eventsSnippet);
		List<Field> field = getFieldsByPageIdAndElementId(xini, 1, 10);

		String segContent = getSegContentBySegId(field.get(0), 0);
		xiniHelper.assertEquivalent("Sentence <sph ID=\"1\" type=\"style\"/> one. <sph type=\"ph\" ID=\"2\"/>Two.", segContent);

		segContent = getSegContentBySegId(field.get(0), 1);
		xiniHelper.assertEquivalent("Three <eph ID=\"2\"/>", segContent);

		segContent = getSegContentBySegId(field.get(0), 2);
		xiniHelper.assertEquivalent("Four <eph ID=\"1\" type=\"link\"/>", segContent);

		segContent = getSegContentBySegId(field.get(0), 3);
		xiniHelper.assertEquivalent("A line <br/> break", segContent);

		segContent = getSegContentBySegId(field.get(0), 4);
		assertEquals("<ph ID=\"3\"><ph ID=\"4\">a</ph>b</ph>cc", segContent);
	}

	@Test
	public void formattingsArePreservedAfterSegmentation() {
		String snippet = getStartSnippet() +
							"	<Fields>" +
							"		<Field FieldID=\"0\">" +
							"			<Seg SegID=\"0\"><b><i>a<sup>bb</sup>ccc</i>d</b></Seg>" +
							"			<Seg SegID=\"1\">e<sub>ff<br/>ggg</sub>hh<u>jj</u>k</Seg>" +
							"		</Field>" +
							"	</Fields>" +
						getEndSnippet();

		Xini xini = toXini(toEvents(snippet));
		List<Field> field = getFieldsByPageIdAndElementId(xini, 1, 10);

		String segContent = getSegContentBySegId(field.get(0), 0);
		assertEquals("<b><i>a<sup>bb</sup>ccc</i>d</b>", segContent);

		segContent = getSegContentBySegId(field.get(0), 1);
		assertEquals("e<sub>ff<br/>ggg</sub>hh<u>jj</u>k", segContent);
	}

	@Test
	public void xiniMetainformationIsPreserved() throws JAXBException {
		String snippet = "<?xml version=\"1.0\" ?>" +
		"<Xini SchemaVersion=\"1.0\" SourceLanguage=\"de\" xsi:noNamespaceSchemaLocation=\"http://www.ontram.com/xsd/xini.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
		"	<TargetLanguages>" +
		"		<Language>en</Language>" +
		"		<Language>en-US</Language>" +
		"	</TargetLanguages>" +
		"	<Main>" +
		"	</Main>" +
		"</Xini>";

		Xini xini = toXini(toEvents(snippet));

		assertEquals("1.0", xini.getSchemaVersion());
		assertEquals("de", xini.getSourceLanguage());

		TargetLanguages targetLangs = xini.getTargetLanguages();
		assertEquals(2,targetLangs.getLanguage().size());
		assertEquals("en", targetLangs.getLanguage().get(0));
		assertEquals("en-US", targetLangs.getLanguage().get(1));
	}

	@Test
	public void pageAndElementMetainformationIsPreserved() throws JAXBException {
		String snippet =
		 getStartSnippetXiniMain() +
		"		<Page PageID=\"1\">" +
		"			<PageName>Page Title</PageName>" +
		"			<Elements>" +
		"				<Element ElementID=\"10\" Size=\"50\" CustomerTextID=\"123Test\" ElementType=\"r/o text\" AlphaList=\"false\"" +
		"						RawSourceBeforeElement=\"&lt;before/&gt;\" RawSourceAfterElement=\"&lt;after/&gt;\">" +
		"					<Style>Headline</Style>" +
		"					<Label>h1</Label>" +
		"					<ElementContent>" +
		"						<Fields>" +
		"							<Field FieldID=\"0\">" +
		"								<Seg SegID=\"0\">Segment</Seg>" +
		"							</Field>" +
		"						</Fields>" +
		"					</ElementContent>" +
		"				</Element>" +
		"			</Elements>" +
		"		</Page>" +
		"	</Main>" +
		"</Xini>";

		Xini xini = toXini(toEvents(snippet));

		Page page = xini.getMain().getPage().get(0);
		assertEquals(1, page.getPageID());
		assertEquals("Page Title", page.getPageName());

		Element elem = page.getElements().getElement().get(0);
		assertEquals(10, elem.getElementID());
		assertEquals((Integer)50, elem.getSize());
		assertEquals("Headline", elem.getStyle());
		assertEquals("h1", elem.getLabel());

		assertEquals("123Test",elem.getCustomerTextID());
		assertEquals("<before/>",elem.getRawSourceBeforeElement());
		assertEquals("<after/>",elem.getRawSourceAfterElement());
		assertEquals(false, elem.isAlphaList());
		assertEquals("r/o text",elem.getElementType().value());
	}

	@Test
	public void fieldMetainformationIsPreserved() throws JAXBException {
		String snippet = getStartSnippet() +
				"	<Fields>" +
				"		<Field FieldID=\"0\" NoContent=\"true\"/>" +
				"		<Field FieldID=\"1\" 	Label=\"Footnote\" CustomerTextID=\"321Test\" " +
				"								ExternalID=\"54321\" EmptySegmentsFlags=\"0\" " +
				"								RawSourceBeforeField=\"&lt;before/&gt;\" RawSourceAfterField=\"&lt;after/&gt;\">" +
				"			<Seg SegID=\"0\">Segment</Seg>" +
				"		</Field>" +
				"	</Fields>" +
			getEndSnippet();

		Xini xini = toXini(toEvents(snippet));
		Page page = xini.getMain().getPage().get(0);
		Element elem = page.getElements().getElement().get(0);

		Field field = elem.getElementContent().getFields().getField().get(0);
		assertEquals(0, field.getFieldID());
		assertEquals(true, field.isNoContent());

		field = elem.getElementContent().getFields().getField().get(1);
		assertEquals("321Test",field.getCustomerTextID());
		assertEquals("Footnote", field.getLabel());
		assertEquals("54321", field.getExternalID());
		assertEquals("0", field.getEmptySegmentsFlags());
		assertEquals("<before/>",field.getRawSourceBeforeField());
		assertEquals("<after/>",field.getRawSourceAfterField());
	}

	@Test
	public void tableMetainformationIsPreserved() throws JAXBException {
		String snippet = getStartSnippet() +
							"	<Table>" +
							"		<TR>" +
							"			<TD ExternalID=\"EX-ID-123\" EmptySegmentsFlags=\"0\">" +
							"				<Seg SegID=\"0\">table cell 1</Seg>" +
							"			</TD>" +
							"			<TD Label=\"Test-Label\" CustomerTextID=\"CTiD-123\">" +
							"				<Seg SegID=\"0\">table cell 2</Seg>" +
							"			</TD>" +
							"		</TR>" +
							"		<TR>" +
							"			<TD NoContent=\"true\"/>" +
							"		</TR>" +
							"	</Table>" +
						getEndSnippet();

		Xini xini = toXini(toEvents(snippet));

		List<TR> trsTable = getTableRowsByPageIdAndElementId(xini, 1, 10);
		assertEquals(2, trsTable.size());

		TD tD = trsTable.get(0).getTD().get(0);
		assertEquals("EX-ID-123", tD.getExternalID());
		assertEquals("0", tD.getEmptySegmentsFlags());

		tD = trsTable.get(0).getTD().get(1);
		assertEquals("CTiD-123", tD.getCustomerTextID());
		assertEquals("Test-Label", tD.getLabel());

		tD = trsTable.get(1).getTD().get(0);
		assertTrue(tD.isNoContent());
	}

	@Test
	public void iniTableMetainformationIsPreserved() throws JAXBException {
		String snippet = getStartSnippet() +
							"	<INI_Table>" +
							"		<TR>" +
							"			<TD Label=\"Test-Label\" CustomerTextID=\"CTiD-123\">" +
							"				<Seg SegID=\"0\">Seg0</Seg>" +
							"			</TD>" +
							"			<TD ExternalID=\"EX-ID-123\" EmptySegmentsFlags=\"00\" >" +
							"				<Seg SegID=\"0\">Seg1</Seg>" +
							"				<Seg SegID=\"1\">Seg2</Seg>" +
							"			</TD>" +
							"		</TR>" +
							"		<TR>" +
							"			<TD NoContent=\"true\"/>" +
							"			<TD NoContent=\"true\"/>" +
							"		</TR>" +
							"	</INI_Table>" +
						getEndSnippet();

		Xini xini = toXini(toEvents(snippet));

		List<INITR> trsINITable = getINITableRowsByPageIdAndElementId(xini, 1, 10);
		assertEquals(2, trsINITable.size());

		String segContentINITable = getSegContentBySegId(trsINITable.get(0).getTD().get(0), 0);
		assertEquals("Seg0", segContentINITable);
		INITD iniTd = trsINITable.get(0).getTD().get(0);
		assertEquals("CTiD-123", iniTd.getCustomerTextID());
		assertEquals("Test-Label", iniTd.getLabel());
		
		segContentINITable = getSegContentBySegId(trsINITable.get(0).getTD().get(1), 0);
		assertEquals("Seg1", segContentINITable);
		segContentINITable = getSegContentBySegId(trsINITable.get(0).getTD().get(1), 1);
		assertEquals("Seg2", segContentINITable);

		iniTd = trsINITable.get(0).getTD().get(1);
		assertEquals("EX-ID-123", iniTd.getExternalID());
		assertEquals("00", iniTd.getEmptySegmentsFlags());
		
		iniTd = trsINITable.get(1).getTD().get(0);
		assertTrue(iniTd.isNoContent());
		iniTd = trsINITable.get(1).getTD().get(1);
		assertTrue(iniTd.isNoContent());
	}

	@Test
	public void segmentMetainformationIsPreserved() throws JAXBException {
		String snippet = getStartSnippet() +
							"	<Table>" +
							"		<TR>" +
							"			<TD>" +
							"				<Seg SegID=\"0\" EmptyTranslation=\"true\"/>" +
							"			</TD>" +
							"		</TR>" +
							" </Table>" +
						getEndSnippet();

		Xini xini = toXini(toEvents(snippet));

		List<TR> trsTable = getTableRowsByPageIdAndElementId(xini, 1, 10);
		assertEquals(1, trsTable.size());

		Seg seg = getSegBySegId(trsTable.get(0).getTD().get(0), 0);
		assertTrue(seg.isEmptyTranslation());
		String segContent = getSegContentBySegId(trsTable.get(0).getTD().get(0), 0);
		assertEquals("", segContent);
	}

	private String getStartSnippetXiniMain() {
		return 	"<?xml version=\"1.0\" ?>" +
				"<Xini SchemaVersion=\"1.0\" xsi:noNamespaceSchemaLocation=\"http://www.ontram.com/xsd/xini.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"	<Main>";
	}

	private String getStartSnippet() {
		return 	getStartSnippetXiniMain() +
				"		<Page PageID=\"1\">" +
				"			<Elements>" +
				"				<Element ElementID=\"10\" Size=\"50\">" +
				"					<ElementContent>";
	}

	private String getEndSnippet() {
		return 	"					</ElementContent>" +
				"				</Element>" +
				"			</Elements>" +
				"		</Page>" +
				"	</Main>" +
				"</Xini>";
	}

	private List<INITR> getINITableRowsByPageIdAndElementId(Xini xini, int pageId, int elementId) {
		return getElementContentByPageIdAndElementId(xini, pageId, elementId).getINITable().getTR();
	}

	private List<TR> getTableRowsByPageIdAndElementId(Xini xini, int pageId, int elementId) {
		return getElementContentByPageIdAndElementId(xini, pageId, elementId).getTable().getTR();
	}

	private ElementContent getElementContentByPageIdAndElementId(Xini xini, int pageId, int elementId) {
		for (Page eachPage : xini.getMain().getPage()) {
			if (eachPage.getPageID() == pageId) {
				for (Element eachElement : eachPage.getElements().getElement()) {
					if (eachElement.getElementID() == elementId) {
						return eachElement.getElementContent();
					}
				}
			}
		}
		return null;
	}

	private String getSegContentBySegId(TD td, int segId) {
		Seg seg = getSegBySegId(td, segId);
		return contentOf(seg);
	}

	private Seg getSegBySegId(TD td, int segId) {
		return td.getSeg().get(segId);
	}

	private String getSegContentBySegId(INITD td, int segId) {
		Seg seg = getSegBySegId(td, segId);
		return contentOf(seg);
	}

	private Seg getSegBySegId(INITD td, int segId) {
		return td.getSeg().get(segId);
	}

	private String contentOf(TextContent tc) {
		return xiniHelper.serializeTextContent(tc);
	}

	private List<Field> getFieldsByPageIdAndElementId(Xini xini, int pageId, int elementId) {
		for (Page eachPage : xini.getMain().getPage()) {
			if (eachPage.getPageID() == pageId) {
				for (Element eachElement : eachPage.getElements().getElement()) {
					if (eachElement.getElementID() == elementId) {
						return eachElement.getElementContent().getFields().getField();
					}
				}
			}
		}
		return new ArrayList<Field>();
	}

	private String getSegContentBySegId(Field field, int segId) {
		Seg seg = getSegBySegId(field, segId);
		return contentOf(seg);
	}

	private Seg getSegBySegId(Field field, int segId) {
		for (Seg each: field.getSeg()) {
			if (each.getSegID() == segId)
				return each;
		}
		return null;
	}
	
	private List<Event> toEvents(String snippet) {
		return xiniHelper.toEvents(snippet, filter, locEN, locDE);
	}

	private Xini toXini(List<Event> events) {
		return xiniHelper.toXini(events, filter);
	}


}
