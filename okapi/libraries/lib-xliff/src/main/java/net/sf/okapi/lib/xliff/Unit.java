/*===========================================================================
  Copyright (C) 2011-2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.oasisopen.xliff.v2.ICandidate;
import org.oasisopen.xliff.v2.IFragment;
import org.oasisopen.xliff.v2.INote;
import org.oasisopen.xliff.v2.IPart;
import org.oasisopen.xliff.v2.ISegment;
import org.oasisopen.xliff.v2.IWithCandidates;
import org.oasisopen.xliff.v2.IWithNotes;

public class Unit extends EventData implements Iterable<IPart>, IWithCandidates, IWithNotes {
	
	private static final long serialVersionUID = 0100L;

	private ArrayList<IPart> list;
	private DataStore store;
	private ArrayList<ICandidate> candidates;
	private ArrayList<INote> notes;

	public Unit (String id) {
		setId(id);
		list = new ArrayList<IPart>();
		store = new DataStore();
	}
	
	@Override
    public Iterator<IPart> iterator() {
		return new Iterator<IPart>() {
			int current = 0;

			@Override
			public void remove () {
				throw new UnsupportedOperationException("The method remove() not supported.");
			}

			@Override
			public IPart next () {
				return list.get((++current)-1);
			}

			@Override
			public boolean hasNext () {
				return ( !list.isEmpty() && ( current < list.size() ));
			}
		};
	};

	public int getPartCount () {
		return list.size();
	}
	
	public ISegment appendNewSegment () {
		Segment seg = new Segment(store);
		list.add(seg);
		return seg;
	}
	
	public Part appendNewIgnorable () {
		Part part = new Part(store); 
		list.add(part);
		return part;
	}

	public IPart getPart (int partIndex) {
		return list.get(partIndex);
	}

	public DataStore getDataStore () {
		return store;
	}

	@Override
	public void addCandidate (ICandidate candidate) {
		if ( candidates == null ) candidates = new ArrayList<ICandidate>();
		candidates.add(candidate);
	}
	
	@Override
	public List<ICandidate> getCandidates () {
		if ( candidates == null ) return Collections.emptyList();
		else return candidates;
	}

	@Override
	public int getCandidateCount () {
		if ( candidates == null ) return 0;
		return candidates.size();
	}

	@Override
	public void addNote (INote note) {
		if ( notes == null ) notes = new ArrayList<INote>();
		notes.add(note);
	}

	@Override
	public List<INote> getNotes () {
		if ( notes == null ) return Collections.emptyList();
		else return notes;
	}
	
	@Override
	public int getNoteCount () {
		if ( notes == null ) return 0;
		return notes.size();
	}

	//TODO
	public void split (int partIndex,
		int srcStart,
		int srcEnd,
		int trgStart,
		int trgEnd)
	{
		IPart part = getPart(partIndex);
		IFragment src = part.getSource();
		String ctext = src.getCodedText();
		if ( srcEnd == -1 ) srcEnd = ctext.length()-1;
		if ( srcStart > srcEnd ) {
			throw new RuntimeException("Bad range.");
		}
		
		if ( ctext.length() < srcEnd ) {
			throw new RuntimeException("Range out of bounds.");
		}
		//TODO: check valid position (not at the middle of a marker
		if (( srcStart < ctext.length() ) && Fragment.isMarker(ctext.charAt(srcStart+1)) ) {
			
		}

		String left = "";
		left = ctext.substring(0, srcStart);
		String mid = "";
		mid = ctext.substring(srcStart, srcEnd);
		String right = "";
		right = ctext.substring(srcEnd); 
		
		// Add on the right first (no change on the indices
		if ( !right.isEmpty() ) {
			Segment seg = new Segment(store);
			seg.getSource().setCodedText(right);
			list.add(partIndex+1, seg);
		}
		// Add to the left last: this part moves to partIndex+1
		if ( !left.isEmpty() ) {
			Segment seg = new Segment(store);
			seg.getSource().setCodedText(left);
			list.add(partIndex, seg);
		}
		if ( mid.isEmpty() ) {
			list.remove(part);
		}
		else if ( mid.length() != ctext.length() ) {
			part.getSource().setCodedText(mid);
		}
		// Else: new segments were empty:  No change for this part
		
		
	}
	
	
}
