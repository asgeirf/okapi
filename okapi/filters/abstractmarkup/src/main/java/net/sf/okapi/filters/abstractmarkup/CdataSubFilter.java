package net.sf.okapi.filters.abstractmarkup;

import net.sf.okapi.common.filters.AbstractSubFilterAdapter;
import net.sf.okapi.common.filters.FilterState;
import net.sf.okapi.common.filters.IFilter;

public class CdataSubFilter extends AbstractSubFilterAdapter {

	public CdataSubFilter(IFilter filter, FilterState state) {
		super(filter, state);
	}
}
