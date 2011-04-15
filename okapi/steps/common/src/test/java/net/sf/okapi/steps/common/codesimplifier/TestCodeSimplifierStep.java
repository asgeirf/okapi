package net.sf.okapi.steps.common.codesimplifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextUnit;

import org.junit.Before;
import org.junit.Test;

public class TestCodeSimplifierStep {

	private GenericContent fmt;
	private CodeSimplifierStep css;
	
	@Before
	public void setup() {
		css = new CodeSimplifierStep(); 
		fmt = new GenericContent();
	}	
	
	@Test
	public void testDefaults () {
		TextFragment tf = new TextFragment();		
		tf.append(TagType.PLACEHOLDER, "x1", "<x1/>");
		tf.append("   ");
		tf.append(TagType.PLACEHOLDER, "x2", "<x2/>");
		tf.append("T1");		
		tf.append(TagType.PLACEHOLDER, "x3", "<x3/>");
		tf.append(TagType.PLACEHOLDER, "x4", "<x4/>");
		tf.append(TagType.OPENING, "a", "<a>");
		tf.append("T2");
		tf.append("   ");
		tf.append(TagType.CLOSING, "a", "</a>");
		tf.append("   ");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append(TagType.PLACEHOLDER, "x5", "<x5/>");
		tf.append("   ");
		tf.append(TagType.PLACEHOLDER, "x6", "<x6/>");
				
		assertEquals("<1/>   <2/>T1<3/><4/><5>T2   </5>   <e8/><6/>   <7/>", fmt.setContent(tf).toString());
		
		TextUnit tu1 = new TextUnit("tu1");
		tu1.setSourceContent(tf);

		ISkeleton skel = tu1.getSkeleton();
		assertNull(skel);
		
		Event tue1 = new Event(EventType.TEXT_UNIT, tu1);
		css.handleEvent(tue1);
		tf = tu1.getSource().getUnSegmentedContentCopy();
		assertEquals("T1<2>T2   </2>", fmt.setContent(tf).toString());
		
		skel = tu1.getSkeleton();
		assertEquals("<x1/>   <x2/>[#$$self$]   </b><x5/>   <x6/>", skel.toString());
	}
	
	@Test
	public void testNoRemoval () {
		Parameters params = (Parameters) css.getParameters();
		params.setRemoveLeadingTrailingCodes(false);
		
		TextFragment tf = new TextFragment();		
		tf.append(TagType.PLACEHOLDER, "x1", "<x1/>");
		tf.append("   ");
		tf.append(TagType.PLACEHOLDER, "x2", "<x2/>");
		tf.append("T1");		
		tf.append(TagType.PLACEHOLDER, "x3", "<x3/>");
		tf.append(TagType.PLACEHOLDER, "x4", "<x4/>");
		tf.append(TagType.OPENING, "a", "<a>");
		tf.append("T2");
		tf.append("   ");
		tf.append(TagType.CLOSING, "a", "</a>");
		tf.append("   ");
		tf.append(TagType.CLOSING, "b", "</b>");
		tf.append(TagType.PLACEHOLDER, "x5", "<x5/>");
		tf.append("   ");
		tf.append(TagType.PLACEHOLDER, "x6", "<x6/>");
				
		assertEquals("<1/>   <2/>T1<3/><4/><5>T2   </5>   <e8/><6/>   <7/>", fmt.setContent(tf).toString());
		
		TextUnit tu1 = new TextUnit("tu1");
		tu1.setSourceContent(tf);

		ISkeleton skel = tu1.getSkeleton();
		assertNull(skel);
		
		Event tue1 = new Event(EventType.TEXT_UNIT, tu1);
		css.handleEvent(tue1);
		tf = tu1.getSource().getUnSegmentedContentCopy();
		// 1/ + 2/ -> 1/
		// 3/ + 4/ + 5 -> 2
		// /5 -> /2
		// e8/ + 6/ + 7/ -> e8/
		assertEquals("<1/>T1<2>T2   </2>   <e8/>", fmt.setContent(tf).toString());
		
		skel = tu1.getSkeleton();
		assertNull(skel);
	}
}
