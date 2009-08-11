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

package net.sf.okapi.ui.steps.tokenization.tokens;

import net.sf.okapi.common.ui.abstracteditor.InputQueryDialog;
import net.sf.okapi.steps.tokenization.tokens.Parameters;
import net.sf.okapi.steps.tokenization.tokens.Tokens;

public class TokenSelector {

	/**
	 * For creation of new tokens and storing them to the globally accessible net.sf.okapi.steps.tokenization.tokens/okf_tokens.fprm
	 */
	public static void main(String[] args) {
		
		select();
	}

	private static Tokens select() {
				
		InputQueryDialog dlg = new InputQueryDialog();
		Parameters params = new Parameters(); 
		
		dlg.run(null, TokensTab.class, "Tokens", "", params, null);
		
		return params.selectedTokens;
	}
}
