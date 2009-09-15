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
package net.sf.okapi.tm.pensieve.tmx;

import net.sf.okapi.tm.pensieve.writer.TmWriter;

import java.net.URI;
import java.io.IOException;

public interface TmxImporter {

    /**
     * Imports TMX to Pensieve
     * @param tmxUri The location of the TMX
     * @param targetLang The target language to index
     * @param tmWriter The TMWriter to use when writing to the TM
     * @throws java.io.IOException if there was a problem with the TMX import
     */
    void importTmx(URI tmxUri, String targetLang, TmWriter tmWriter) throws IOException;
    
}
