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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipelinedriver.PipelineContext;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

public class PseudoTranslateStep extends BasePipelineStep {

	private static final String OLDCHARS = "AaEeIiOoUuYyCcDdNn";
	private static final String NEWCHARS = "\u00c2\u00e5\u00c9\u00e8\u00cf\u00ec\u00d8\u00f5\u00db\u00fc\u00dd\u00ff\u00c7\u00e7\u00d0\u00f0\u00d1\u00f1";

	public String getName () {
		return "Pseudo-Translation";
	}

	public String getDescription () {
		return "Pseudo-translates text units content.";
	}
	
	@Override
	protected void handleTextUnit (Event event) {
		TextUnit tu = (TextUnit)event.getResource();
		if ( !tu.isTranslatable() ) return;

		TextFragment tf = tu.createTarget(((PipelineContext)getContext()).getTargetLanguage(0),
			false, IResource.COPY_CONTENT);
		StringBuilder text = new StringBuilder(tf.getCodedText());
		int n;
		for ( int i=0; i<text.length(); i++ ) {
			if ( TextFragment.isMarker(text.charAt(i)) ) {
				i++; // Skip the pair
			}
			else {
				if ( (n = OLDCHARS.indexOf(text.charAt(i))) > -1 ) {
					text.setCharAt(i, NEWCHARS.charAt(n));
				}
			}
		}
		tf.setCodedText(text.toString());
	}

}
