/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.wordcount.common;

import net.sf.okapi.common.annotation.AltTranslation;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.steps.wordcount.WordCounter;

public abstract class AltAnnotationBasedCountStep extends BaseCountStep {

	abstract protected boolean accept(MatchType type); 
	
	private long countInATA(AltTranslationsAnnotation ata) {
		if (ata == null) return 0;
		
		for (AltTranslation at : ata) {
			if (at == null) continue;
			
			if (accept(at.getType())) {
				return WordCounter.count(getSource(), getSourceLocale()); // Word Count metrics are based on counting in source
			}				
		}
		return 0;		
	}
	
	@Override
	protected long count(TextContainer textContainer) {
		return countInATA(textContainer.getAnnotation(AltTranslationsAnnotation.class));
	}

	@Override
	protected long count(Segment segment) {
		return countInATA(segment.getAnnotation(AltTranslationsAnnotation.class));
	}
	
	@Override
	protected boolean countOnlyTranslatable() {
		return true;
	}

	@Override
	protected CountContext getCountContext() {
		return CountContext.CC_TARGET;
	}
}
