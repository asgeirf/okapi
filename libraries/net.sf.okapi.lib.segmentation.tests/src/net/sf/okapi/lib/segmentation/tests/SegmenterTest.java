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

package net.sf.okapi.lib.segmentation.tests;

import org.junit.Before;

import net.sf.okapi.lib.segmentation.Segmenter;

import org.junit.Test;
import static org.junit.Assert.*;

public class SegmenterTest {

	@Before
	public void setUp() {
	}

	@Test
	public void testDefaultOptions () {
		Segmenter seg = new Segmenter();
		// Check default options
		assertFalse(seg.cascade());
		assertTrue(seg.segmentSubFlows());
		assertFalse(seg.includeStartCodes());
		assertTrue(seg.includeEndCodes());
		assertFalse(seg.includeIsolatedCodes());
		assertFalse(seg.oneSegmentIncludesAll());
		assertFalse(seg.trimLeadingWhitespaces());
		assertFalse(seg.trimTrailingWhitespaces());
	}		
	
	@Test
	public void testChangedOptions () {
		Segmenter seg = new Segmenter();
		// Check changing options
		seg.setOptions(false, true, false, true, true, true, true);
		assertFalse(seg.segmentSubFlows());
		assertTrue(seg.includeStartCodes());
		assertFalse(seg.includeEndCodes());
		assertTrue(seg.includeIsolatedCodes());
		assertTrue(seg.oneSegmentIncludesAll());
		assertTrue(seg.trimLeadingWhitespaces());
		assertTrue(seg.trimTrailingWhitespaces());
	}
	
}
