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

package net.sf.okapi.steps.wordcount.common;

import net.sf.okapi.common.ParametersString;
import net.sf.okapi.lib.extra.AbstractParameters;

/**
 * WordCount step parameters
 * 
 * @version 0.1 06.07.2009
 */

public class Parameters extends AbstractParameters {

	public boolean countInBatch;
	public boolean countInBatchItems;
	public boolean countInDocuments;
	public boolean countInSubDocuments;
	public boolean countInGroups;
		
	@Override
	protected void parameters_init() {
	}

	@Override
	protected void parameters_load(ParametersString buffer) {
		countInBatch = buffer.getBoolean("countInBatch", false);
		countInBatchItems = buffer.getBoolean("countInBatchItems", false);
		countInDocuments = buffer.getBoolean("countInDocuments", false);
		countInSubDocuments = buffer.getBoolean("countInSubDocuments", false);
		countInGroups = buffer.getBoolean("countInGroups", false);		
	}

	@Override
	protected void parameters_reset() {
		countInBatch = true; // Defaults for the scoping report step
		countInBatchItems = true; // Defaults for the scoping report step
		countInDocuments = false;
		countInSubDocuments = false;
		countInGroups = false;
	}

	@Override
	protected void parameters_save(ParametersString buffer) {
		buffer.setBoolean("countInBatch", countInBatch);
		buffer.setBoolean("countInBatchItems", countInBatchItems);
		buffer.setBoolean("countInDocuments", countInDocuments);
		buffer.setBoolean("countInSubDocuments", countInSubDocuments);
		buffer.setBoolean("countInGroups", countInGroups);
	}
}
