package net.sf.okapi.common.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.TextFragment;

import org.junit.Before;
import org.junit.Test;

public class AltTranslationTest {
	private TextFragment source1;
	private TextFragment source2;
	private TextFragment source3;

	private TextFragment target1;
	private TextFragment target2;
	private TextFragment target3;
	
	private AltTranslation at1;
	private AltTranslation at2;
	private AltTranslation at3;
	
	private AltTranslation at4;
	private AltTranslation at5;
	private AltTranslation at6;

	
	@Before
	public void setUp() throws Exception {
		source1 = new TextFragment("source one");
		source2 = new TextFragment("source two");
		source3 = new TextFragment("source three");
		
		target1 = new TextFragment("target one");
		target2 = new TextFragment("target two");
		target3 = new TextFragment("target three");
		
		at1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source1,
				target1, AltTranslationType.MT, 60, "");
		at2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		at3 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source3,
				target3, AltTranslationType.EXACT_PREVIOUS_VERSION, 100, "");
		
		at4 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source1,
				target1, AltTranslationType.FUZZY, 60, "");
		at5 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.FUZZY_EXACT_TEXT, 95, "");
		at6 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source3,
				target3, AltTranslationType.FUZZY_UNIQUE_ID, 99, "");
	}

	@Test
	public void altTranslationSortedList() {
		List<AltTranslation> ats = new LinkedList<AltTranslation>();
		ats.add(at1);
		ats.add(at2);
		ats.add(at3);
		Collections.sort(ats);
		Assert.assertEquals(at3, ats.get(0));
		Assert.assertEquals(at2, ats.get(1));
		Assert.assertEquals(at1, ats.get(2));
	}
	
	@Test
	public void altTranslationFuzzySortedList() {
		List<AltTranslation> ats = new LinkedList<AltTranslation>();
		ats.add(at4);
		ats.add(at5);
		ats.add(at6);
		Collections.sort(ats);
		Assert.assertEquals(at6, ats.get(0));
		Assert.assertEquals(at5, ats.get(1));
		Assert.assertEquals(at4, ats.get(2));
	}
	@Test
	public void instanceEquality() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		AltTranslation h2 = h1;
		assertTrue("instance equality", h1.equals(h2));
	}

	@Test
	public void equals() {		
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		assertTrue("equals", h1.equals(at2));
	}

	@Test
	public void notEquals() {
		assertFalse("not equals", at1.equals(at2));
	}
	
	@Test 
	public void compareToEquals() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		assertEquals(0, h1.compareTo(at2));
	}
	
	@Test 
	public void compareToGreaterThanScore() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.FUZZY, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.FUZZY, 50, "");
		assertTrue(h1.compareTo(h2) < 0);
	}
	
	@Test 
	public void compareToLessThanScore() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.FUZZY, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.FUZZY, 50, "");
		assertTrue(h2.compareTo(h1) > 0);
	}
	
	@Test 
	public void compareToLessThanMatchType() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.MT, 90, "");
		assertTrue(h1.compareTo(h2) < 0);
	}
	
	@Test 
	public void compareToGreaterThanMatchType() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.EXACT, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, source2,
				target2, AltTranslationType.MT, 90, "");
		assertTrue(h2.compareTo(h1) > 0);
	}
	
	@Test 
	public void compareToLessThanSource() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, new TextFragment("A"),
				target2, AltTranslationType.EXACT, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, new TextFragment("B"),
				target2, AltTranslationType.MT, 90, "");
		assertTrue(h1.compareTo(h2) < 0);
	}
	
	@Test 
	public void compareToGreaterThanSource() {
		AltTranslation h1 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, new TextFragment("A"),
				target2, AltTranslationType.EXACT, 90, "");
		AltTranslation h2 = new AltTranslation(LocaleId.ENGLISH, LocaleId.SPANISH, null, new TextFragment("B"),
				target2, AltTranslationType.MT, 90, "");
		assertTrue(h2.compareTo(h1) > 0);
	}
}