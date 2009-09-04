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

package net.sf.okapi.steps.tokenization.ui.common;

import net.sf.okapi.common.ui.abstracteditor.AbstractListTab;

import org.eclipse.swt.widgets.Composite;

public abstract class CompoundStepItemsTab extends AbstractListTab {

	public CompoundStepItemsTab(Composite parent, int style) {
		
		super(parent, style);
	}

	@Override
	protected String getAddButtonCaption() {

		return "Add...";
	}

	@Override
	protected String getListDescription() {

		return "Listed below are internal steps in the order of invocation.";
	}

	@Override
	protected String getModifyButtonCaption() {

		return "Modify...";
	}

	@Override
	protected String getRemoveButtonCaption() {

		return "Remove";
	}

	@Override
	protected boolean getUpDownVisible() {

		return true;
	}

	@Override
	protected String getMoveDownButtonCaption() {

		return "Move Down";
	}

	@Override
	protected String getMoveUpButtonCaption() {

		return "Move Up";
	}

}
