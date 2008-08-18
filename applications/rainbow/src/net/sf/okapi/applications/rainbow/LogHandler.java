/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel                                          */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.applications.rainbow;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.okapi.applications.rainbow.lib.ILog;

class LogHandler extends Handler {
	
	private ILog       log;
	

	public LogHandler (ILog log) {
		this.log = log;
	}
	
	@Override
	public void close ()
		throws SecurityException
	{
		// Do nothing
	}

	@Override
	public void flush () {
		// Do nothing
	}

	@Override
	public void publish (LogRecord record) {
		if ( record.getLevel() == Level.SEVERE ) {
			log.error(record.getMessage());
			log.message(record.getThrown().getLocalizedMessage());
			log.message(" @ "+record.getThrown().toString());
		}
		else if ( record.getLevel() == Level.WARNING ) {
			log.warning(record.getMessage());
		}
		else if ( record.getLevel() == Level.INFO ) {
			log.message(record.getMessage());
		}
	}

}
