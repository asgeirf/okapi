/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

public enum IssueType {

	MISSING_TARGETTU,
	MISSING_TARGETSEG,
	
	EMPTY_TARGETSEG,
	
	MISSING_LEADINGWS,
	MISSINGORDIFF_LEADINGWS,
	EXTRA_LEADINGWS,
	EXTRAORDIFF_LEADINGWS,
	MISSING_TRAILINGWS,
	MISSINGORDIFF_TRAILINGWS,
	EXTRA_TRAILINGWS,
	EXTRAORDIFF_TRAILINGWS,

	TARGET_SAME_AS_SOURCE,
	
	CODE_DIFFERENCE,
	
	UNEXPECTED_PATTERN,
	
	SUSPECT_PATTERN,
	
	TARGET_LENGTH,
	
	LANGUAGETOOL_ERROR
}
