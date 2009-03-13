/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.filters.tests;

import org.junit.Assert;
import org.junit.Test;

import net.sf.okapi.filters.tests.DummyBaseFilter;
import net.sf.okapi.filters.tests.FilterTestDriver;

public class BaseFilterTests {

	@Test
	public void testMultilingual () {
		DummyBaseFilter filter = null;		
		try {
			FilterTestDriver testDriver = new FilterTestDriver();
			testDriver.setDisplayLevel(0);
			testDriver.setShowSkeleton(true);
			filter = new DummyBaseFilter();

			filter.setOptions("en", "is", "UTF-8", true);
			filter.open("1");
			if ( !testDriver.process(filter) ) Assert.fail();
			filter.close();

			filter.open("2");
			if ( !testDriver.process(filter) ) Assert.fail();
			filter.close();
		}
		catch ( Throwable e ) {
			e.printStackTrace();
			Assert.fail("Exception occured");
		}
		finally {
			if ( filter != null ) filter.close();
		}
	}

}
