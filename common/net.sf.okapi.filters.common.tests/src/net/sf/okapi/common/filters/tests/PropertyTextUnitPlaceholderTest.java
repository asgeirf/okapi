package net.sf.okapi.common.filters.tests;

import static org.junit.Assert.*;

import net.sf.okapi.common.HashCodeUtil;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder.PlaceholderType;

import org.junit.Test;

public class PropertyTextUnitPlaceholderTest {

	@Test
	public void PlaceholderCompareTo() {
		PropertyTextUnitPlaceholder p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test",
				10, 15, 20, 25);
		PropertyTextUnitPlaceholder p2 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test",
				10, 15, 20, 25);
		assertEquals(0, p1.compareTo(p2));

		p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 5, 15, 20, 25);
		p2 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 10, 15, 20, 25);
		assertEquals(-1, p1.compareTo(p2));

		p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 12, 15, 20, 25);
		p2 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 10, 15, 20, 25);
		assertEquals(1, p1.compareTo(p2));

	}

	@Test
	public void PlaceholderEquals() {
		PropertyTextUnitPlaceholder p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test",
				10, 15, 20, 25);
		PropertyTextUnitPlaceholder p2 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test",
				10, 15, 20, 25);
		assertTrue(p1.equals(p2));

		p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 10, 15, 20, 25);
		p2 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test", 11, 15, 20, 25);
		assertFalse(p1.equals(p2));
	}

	@Test
	public void PlaceholderHashCode() {
		PropertyTextUnitPlaceholder p1 = new PropertyTextUnitPlaceholder(PlaceholderType.TRANSLATABLE, "test", "test",
				10, 15, 20, 25);
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash(result, p1.getMainStartPos());
		assertEquals(result, p1.hashCode());
	}
}
