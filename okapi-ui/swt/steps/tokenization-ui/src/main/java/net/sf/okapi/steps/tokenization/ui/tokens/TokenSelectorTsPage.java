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

package net.sf.okapi.steps.tokenization.ui.tokens;

import java.util.List;

import net.sf.okapi.common.ui.abstracteditor.SWTUtil;
import net.sf.okapi.common.ui.abstracteditor.TableAdapter;
import net.sf.okapi.steps.tokenization.tokens.Parameters;
import net.sf.okapi.steps.tokenization.tokens.TokenItem;

import org.eclipse.swt.widgets.Composite;

public class TokenSelectorTsPage extends TokenSelectorPage {

	public TokenSelectorTsPage(Composite parent, int style) {
		
		super(parent, style);
	
		SWTUtil.setText(listDescr, "This program lets you configure the global set of tokens.");
	}

	@Override
	public boolean save(Object data) {
		
		if (super.save(data)) {
			
			Parameters params = new Parameters();
			if (params == null) return false;
			
			TableAdapter adapter = getAdapter();
			List<TokenItem> items = params.getItems();
			
			for (int i = 0; i < adapter.getNumRows(); i++)			
				items.add(new TokenItem(adapter.getValue(i + 1, 1), adapter.getValue(i + 1, 2)));
			
			params.saveItems();
		}			
		
		return true;
	}

}
