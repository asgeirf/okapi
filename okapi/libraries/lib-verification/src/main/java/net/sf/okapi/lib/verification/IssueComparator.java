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

import java.util.Comparator;

public class IssueComparator implements Comparator<Issue> {

	public static final int DIR_ASC = 1;
	public static final int DIR_DESC = -1;
	
	public static final int TYPE_ENABLED = 0;
	public static final int TYPE_TU = 1;
	public static final int TYPE_SEG = 2;
	public static final int TYPE_MESSAGE = 3;
	
	private int type = 0;
	private int direction = 1;
	
	public IssueComparator (int type,
		int direction)
	{
		this.type = type;
		this.direction = direction;
	}
	
	@Override
	public int compare (Issue issue1, Issue issue2) {
	
		switch ( type ) {
		case TYPE_ENABLED:
			if ( issue1.enabled == issue2.enabled ) return 0;
			if ( direction == DIR_ASC ) {
				return issue1.enabled ? 1 : -1;
			}
			return issue1.enabled ? -1 : 1;

		case TYPE_TU:
			String key1 = issue1.docId.toString()+issue1.tuId;
			String key2 = issue2.docId.toString()+issue2.tuId;
			if ( key1.equals(key2) ) return 0;
			if ( direction == DIR_ASC ) {
				return key1.compareTo(key2);
			}
			return key2.compareTo(key1);

		case TYPE_SEG:
			key1 = issue1.docId.toString()+issue1.tuId + (issue1.segId==null ? "" : issue1.segId);
			key2 = issue2.docId.toString()+issue2.tuId + (issue2.segId==null ? "" : issue2.segId);
			if ( key1.equals(key2) ) return 0;
			if ( direction == DIR_ASC ) {
				return key1.compareTo(key2);
			}
			return key2.compareTo(key1);

		case TYPE_MESSAGE:
			if ( issue1.message.equals(issue2.message) ) return 0;
			if ( direction == DIR_ASC ) {
				return issue1.message.compareTo(issue2.message);
			}
			return issue2.message.compareTo(issue1.message);
			
		}

		return 0;
	}

}
