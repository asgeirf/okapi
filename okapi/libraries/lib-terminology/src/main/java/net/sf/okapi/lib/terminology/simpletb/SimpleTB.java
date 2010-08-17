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

package net.sf.okapi.lib.terminology.simpletb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.terminology.IGlossaryReader;
import net.sf.okapi.lib.terminology.TermEntry;
import net.sf.okapi.lib.terminology.TermHit;
import net.sf.okapi.lib.terminology.tbx.TBXReader;

/**
 * Very basic memory-only simple termbase.
 * This is used for prototyping the terminology interface.
 */
public class SimpleTB {
	
	LocaleId srcLoc;
	LocaleId trgLoc;
	private List<Entry> entries;
	
	public SimpleTB (LocaleId srcLoc,
		LocaleId trgLoc)
	{
		this.srcLoc = srcLoc;
		this.trgLoc = trgLoc;
		this.entries = new ArrayList<Entry>();
	}
	
	public void importTBX (File file) {
		importGlossary(new TBXReader(), file);
	}
	
	private void importGlossary (IGlossaryReader reader,
		File file)
	{
		try {
			reader.open(file);
			while ( reader.hasNext() ) {
//				ConceptEntry cent = reader.next();
			}
		}
		finally {
			if ( reader != null ) reader.close();
		}
	}
	
	public void removeAll () {
		entries.clear();
	}

	public Entry addEntry (String srcTerm,
		String trgTerm)
	{
		Entry ent = new Entry(srcTerm);
		ent.setTargetTerm(trgTerm);
		entries.add(ent);
		return ent;
	}

	/*
	 * Very crude implementation of the search terms function.
	 */
	public List<TermHit> getExistingTerms (TextFragment frag,
		LocaleId fragmentLoc,
		LocaleId otherLoc)
	{
		String text = frag.getCodedText();
		List<String> parts = Arrays.asList(text.split("\\s"));
		List<TermHit> res = new ArrayList<TermHit>();
	
		// Determine if the termbase has the searched locale
		boolean searchSource = fragmentLoc.equals(srcLoc);
		if ( !searchSource ) {
			if ( !fragmentLoc.equals(trgLoc) ) {
				return res; // Nothing
			}
		}

		String termToMatch;
		String otherTerm;
		for ( Entry ent : entries ) {
			if ( searchSource ) {
				termToMatch = ent.getSourceTerm();
				otherTerm = ent.getTargetTerm();
			}
			else {
				termToMatch = ent.getTargetTerm();
				otherTerm = ent.getSourceTerm();
			}
			if (( termToMatch == null ) || ( otherTerm == null )) continue;
			if ( parts.contains(termToMatch) ) {
				TermHit th = new TermHit();
				th.sourceTerm = new TermEntry(termToMatch);
				th.targetTerm = new TermEntry(otherTerm);
				res.add(th);
			}
		}
		
		return res;
	}
	
}
