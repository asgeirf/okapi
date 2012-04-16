/*===========================================================================
  Copyright (C) 2010-2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.extra.steps;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
//import net.sf.okapi.steps.xliffkit.common.persistence.sessions.OkapiJsonSession;

public class EventLogger extends BasePipelineStep {

	private int indent = 0;
	private boolean increasing = true;
	private final Logger logger = Logger.getLogger(getClass().getName());
	private StringBuilder sb;
	
	@Override
	public String getDescription() {
		return "Logs events going through the pipeline.";
	}

	@Override
	public String getName() {
		return "Event logger";
	}

	private String getEventDescr(Event event) {
		String res = ""; 
		if (event.getResource() != null)
			res = String.format("  [%s]", event.getResource().getId());
		
		switch ( event.getEventType() ) {	
			case TEXT_UNIT:
				//res = "  " + event.getResource().getId();
				res = String.format("  [%s]", event.getResource().getId());
				break;
			case START_DOCUMENT:
				res +=  "  " + ((StartDocument) event.getResource()).getName();
				break;
			case START_SUBDOCUMENT:
				res +=  "  " + ((StartSubDocument) event.getResource()).getName();
				break;
//			case TEXT_UNIT:
//				if ("30".equals(event.getResource().getId()))
//						res += "\n" + session.writeObject(event); 
		}
		return res;
	}
		
	private void printEvent(Event event) {
		String indentStr = "";
		for (int i = 0; i < indent; i++) 
			indentStr += "  ";
				
		sb.append(indentStr);
		sb.append(event.getEventType() + getEventDescr(event));
		sb.append("\n");
	}
	
	@Override
	public Event handleEvent(Event event) {
		switch ( event.getEventType() ) {
		case START_BATCH:
			sb = new StringBuilder("\n\n");
		case START_DOCUMENT:
		case START_SUBDOCUMENT:
		case START_GROUP:		
		case START_BATCH_ITEM:
			if (!increasing) sb.append("\n");
			printEvent(event);
			indent++; 
			increasing = true;
			break;

		case END_DOCUMENT:
		case END_SUBDOCUMENT:
		case END_GROUP:
		case END_BATCH:
		case END_BATCH_ITEM:
			if (indent > 0) indent--;
			increasing = false;
			printEvent(event);
			if (event.getEventType() == EventType.END_BATCH) {
				logger.setLevel(Level.INFO);
				logger.info(sb.toString());
			}
			break;		
			
		default:
			if (!increasing) sb.append("\n");
			printEvent(event);
		}
		
		
		return super.handleEvent(event);
	}
}
