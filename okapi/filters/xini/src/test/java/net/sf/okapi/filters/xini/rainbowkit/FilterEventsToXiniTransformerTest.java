package net.sf.okapi.filters.xini.rainbowkit;

import static org.junit.Assert.assertTrue;

import java.util.List;

import junit.framework.Assert;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.xini.jaxb.Element;
import net.sf.okapi.filters.xini.jaxb.Field;
import net.sf.okapi.filters.xini.jaxb.Seg;
import net.sf.okapi.filters.xini.jaxb.TextContent;
import net.sf.okapi.filters.xini.jaxb.Xini;

import org.junit.Before;
import org.junit.Test;

public class FilterEventsToXiniTransformerTest {

	private static final String TARGET_TEST_STRING = "Übersetzter test String";
	private static final String SOURCE_TEST_STRING = "test String";
	private static final String NBSP_STRING = "\u00A0";
	
	private FilterEventToXiniTransformer transformer;
	private TextUnit txtUnit1;
	private Xini xini;

	@Before
	public void setUp() {
		transformer = new FilterEventToXiniTransformer();
		transformer.init();
		transformer.startPage("TestPage1");

		txtUnit1 = new TextUnit("TU1");

		TextContainer textContainer = createTextContainer("1",
				SOURCE_TEST_STRING);
		txtUnit1.setId("1");
		txtUnit1.setSource(textContainer);
		textContainer = createTextContainer("1", TARGET_TEST_STRING);
		txtUnit1.setTarget(new LocaleId("de"), textContainer);
		textContainer = createTextContainer("1", TARGET_TEST_STRING + "_EN");
		txtUnit1.setTarget(new LocaleId("en"), textContainer);

		// root = TestUtil.getParentDir(this.getClass(), "/test01.xml");
	}

	private TextContainer createTextContainer(String segId, String fragmentText) {
		TextFragment textFragment = new TextFragment(fragmentText);
		Segment segment = new Segment(segId, textFragment);
		return new TextContainer(segment);
	}

	@Test
	public void exportsPreTranslations() {
		transformer.transformTextUnit(txtUnit1);
		Xini xini = getXiniFrom(transformer);

		List<Seg> segs = getSegListFromFirstElement(xini);
		Assert.assertEquals(1, segs.size());
		List<TextContent> segAndTrans = getSegAndTransListFromFirstElement(xini);
		TextContent sourceCont = segAndTrans.get(0);
		TextContent targetCont = segAndTrans.get(1);
		Assert.assertEquals(3, segAndTrans.size());
		Assert.assertTrue(sourceCont.getContent().contains(SOURCE_TEST_STRING));
		Assert.assertTrue(targetCont.getContent().contains(
				TARGET_TEST_STRING + "_EN"));

		targetCont = segAndTrans.get(2);
		Assert.assertTrue(targetCont.getContent().contains(TARGET_TEST_STRING));

		//writeOutForTest();
	}
	
	@Test
	public void exportsNonBreakingSpaceAsEmptyTranslation() {
		this.setUpTextUnitWithNbspContent();
		
		transformer.transformTextUnit(txtUnit1);
		
		assertEmptyTranslationFlagSet();
	}
	
	private void setUpTextUnitWithNbspContent() {
		txtUnit1 = new TextUnit("TU1");

		TextContainer textContainer = createTextContainer("1", NBSP_STRING);
		txtUnit1.setId("1");
		txtUnit1.setSource(textContainer);
	}
	
	private void assertEmptyTranslationFlagSet() {
		final Xini xini = getXiniFrom(transformer);
		final Element firstElement = this.getFirstElementFromXini(xini);
		Field field = firstElement.getElementContent().getFields().getField().get(0);
		Seg seg = field.getSeg().get(0);
		assertTrue(seg.isEmptyTranslation());
	}

	private List<Seg> getSegListFromFirstElement(Xini xini2) {
		Element element = getFirstElementFromXini(xini);

		return element.getElementContent().getFields().getField().get(0)
				.getSeg();
	}

	private List<TextContent> getSegAndTransListFromFirstElement(Xini xini) {
		Element element = getFirstElementFromXini(xini);
		return element.getElementContent().getFields().getField().get(0)
				.getSegAndTrans();
	}

	private Element getFirstElementFromXini(Xini xini) {
		Element element = xini.getMain().getPage().get(0).getElements()
				.getElement().get(0);
		return element;
	}

	private Xini getXiniFrom(FilterEventToXiniTransformer transformer2) {
		try {
			java.lang.reflect.Field xiniField = transformer.getClass().getDeclaredField("xini");
			xiniField.setAccessible(true);

			return xini = (Xini) xiniField.get(transformer);

		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
