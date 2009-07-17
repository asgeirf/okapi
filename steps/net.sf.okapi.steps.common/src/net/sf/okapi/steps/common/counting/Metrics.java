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

package net.sf.okapi.steps.common.counting;

import java.util.Hashtable;

import net.sf.okapi.common.Util;

/**
 * Metrics hash-table 
 * 
 * @version 0.1 07.07.2009
 */

public class Metrics {

	private Hashtable <String, Long> metrics;
	
	public Metrics() {
		
		super();
		
		metrics = new Hashtable <String, Long> ();				
	}

	public void resetMetrics() {
		
		if (metrics == null) return;

		metrics.clear(); // removes all metrics from the hash table, getMetric() returns 0 for not found 
	}
	
	public boolean resetMetric(String name) {
		
		return setMetric(name, 0L);
	}

	/**
	 * Get the value of the metric, specified by the given symbolic name.
	 * @param name symbolic name of the metric
	 * @return value of the metric specified by the given symbolic name 
	 */
	public long getMetric(String name) {
		
		if (Util.isEmpty(name)) return 0L;
		if (metrics == null) return 0L;
		
		Long res = metrics.get(name);
		
		if (res == null) res = 0L; 
		return res;
	}
	
	public boolean setMetric(String name, long value) {
		
		if (Util.isEmpty(name)) return false;
		if (metrics == null) return false;
		
		metrics.put(name, value);
		
		return true;
	}
	
	public boolean registerMetric(String name) {
	
		if (Util.isEmpty(name)) return false;
		if (metrics == null) return false;
		
		if (metrics.containsKey(name)) return false;
		
		metrics.put(name, 0L);
		
		return true;
	}
	
	public boolean unregisterMetric(String name) {
		
		if (Util.isEmpty(name)) return false;
		if (metrics == null) return false;
		
		if (!metrics.containsKey(name)) return false;
		
		metrics.remove(name);
		
		return true;
	}
	
}
