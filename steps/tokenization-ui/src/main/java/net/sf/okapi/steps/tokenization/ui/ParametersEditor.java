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

package net.sf.okapi.steps.tokenization.ui;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ui.abstracteditor.AbstractParametersEditor;
import net.sf.okapi.steps.tokenization.Parameters;
import net.sf.okapi.steps.tokenization.ui.common.CompoundStepItemsTab;
import net.sf.okapi.steps.tokenization.ui.locale.LanguagesTab;
import net.sf.okapi.steps.tokenization.ui.tokens.TokenTypesTab;

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Widget;

public class ParametersEditor extends AbstractParametersEditor {

	@Override
	protected void createPages(TabFolder pageContainer) {
		
		addPage("Tokenizers", CompoundStepItemsTab.class);
		addPage("Options", OptionsTab.class);
		addPage("Languages", LanguagesTab.class);
		addPage("Tokens", TokenTypesTab.class);
	}

	@Override
	public IParameters createParameters() {
		
		return new Parameters();
	}

	@Override
	protected String getCaption() {

		return "Tokenization";
	}

	@Override
	protected void interop(Widget speaker) {
		// TODO Auto-generated method stub

	}

}
