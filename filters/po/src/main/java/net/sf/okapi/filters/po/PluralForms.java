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

package net.sf.okapi.filters.po;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PluralForms {
	
	private static final String BUNDLE_NAME = "net.sf.okapi.filters.po.PluralForms";
	private static final String DEFAULT_EXP = "nplurals=2; plural=(n != 1);";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
		.getBundle(BUNDLE_NAME);

	private PluralForms() {
	}

	public static String getExpression (String langCode) {
		try {
			return RESOURCE_BUNDLE.getString(langCode);
		}
		catch ( MissingResourceException e ) {
			return DEFAULT_EXP; // Default fall-back
		}
	}

	public static int getNumber (String langCode) {
		String tmp;
		try {
			tmp = RESOURCE_BUNDLE.getString(langCode);
		}
		catch ( MissingResourceException e ) {
			tmp = DEFAULT_EXP; // Default fall-back
		}
		int n1 = tmp.indexOf('=');
		int n2 = tmp.indexOf(';', n1);
		return Integer.valueOf(tmp.substring(n1+1, n2));
	}
}
