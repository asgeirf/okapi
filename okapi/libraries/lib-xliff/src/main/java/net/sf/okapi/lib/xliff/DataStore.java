/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.oasisopen.xliff.v2.ICode;
import org.oasisopen.xliff.v2.IDataStore;

public class DataStore implements IDataStore {

	private static final long serialVersionUID = 0100L;

	private Codes srcCodes;
	private Codes trgCodes;

	private transient Map<String, String> map;
	
	@Override
	public boolean hasCodeWithOriginalData () {
		if ( srcCodes != null ) {
			if ( srcCodes.hasCodeWithOriginalData() ) return true;
		}
		if ( trgCodes != null ) {
			if ( trgCodes.hasCodeWithOriginalData() ) return true;
		}
		return false;
	}
	
	@Override
	public boolean hasSourceCode () {
		return (( srcCodes != null ) && srcCodes.hasCode() );
	}
	
	@Override
	public boolean hasTargetCode () {
		return (( trgCodes != null ) && trgCodes.hasCode() );
	}
	
	@Override
	public Codes getSourceCodes () {
		if ( srcCodes == null ) srcCodes = new Codes(this);
		return srcCodes;
	}
	
	@Override
	public Codes getTargetCodes () {
		if ( trgCodes == null ) trgCodes = new Codes(this);
		return trgCodes;
	}

	@Override
	public void calculateOriginalDataToIdsMap () {
		map = new LinkedHashMap<String, String>(); // LinkedHashMap to keep the order (not mandatory, but nicer)
		int mapId = 0;
		String tmp;

		if ( srcCodes != null ) {
			for ( int i=0; i<srcCodes.size(); i++ ) {
				ICode code = srcCodes.get(i);
				tmp = code.getOriginalData(); if ( tmp == null ) tmp = "";
				if ( !map.containsKey(tmp) ) {
					// No item like this yet: create one
					map.put(code.getOriginalData(), "d"+String.valueOf(++mapId));
				}
			}
		}
		if ( trgCodes != null ) {
			for ( int i=0; i<trgCodes.size(); i++ ) {
				ICode code = trgCodes.get(i);
				tmp = code.getOriginalData(); if ( tmp == null ) tmp = "";
				if ( !map.containsKey(tmp) ) {
					// No item like this yet: create one
					map.put(code.getOriginalData(), "d"+String.valueOf(++mapId));
				}
			}
		}
	}

	@Override
	public void setOutsideRepresentationMap (Map<String, String> map) {
		this.map = map;
	}

	@Override
	public Map<String, String> getOutsideRepresentationMap () {
		return map;
	}

	/**
	 * Gets the id for the outside storage of a given original data.
	 * You must have called {@link #calculateOriginalDataToIdsMap()} before calling this method the first time.
	 * @param originalData the original data
	 * @return the id for the outside storage of the given original data 
	 */
	@Override
	public String getIdForOriginalData (String originalData) {
		if ( map == null ) {
			throw new XLIFFReaderException("No original data map defined for this object.");
		}
		if ( !map.containsKey(originalData) ) {
			throw new XLIFFReaderException(String.format("No id found for the original data '%s'.", originalData));
		}
		// Else: all is fine
		return map.get(originalData); // Id for the given data
	}

	@Override
	public String getOriginalDataForId (String id) {
		if ( map == null ) {
			throw new XLIFFReaderException("No original data map defined for this object.");
		}
		if ( !map.containsKey(id) ) {
			throw new XLIFFReaderException(String.format("No original data found for the id '%s'.", id));
		}
		return map.get(id);
	}
	
}