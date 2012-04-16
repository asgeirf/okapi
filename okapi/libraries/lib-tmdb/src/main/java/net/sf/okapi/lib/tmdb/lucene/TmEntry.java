/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.tmdb.lucene;

import net.sf.okapi.common.Util;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class TmEntry {

	public static final String ID_FIELDNAME = "entryId";
	public static final String SEGKEY_FIELDNAME = "segKey";
	public static final String TMID_FIELDNAME = "tmId";
	public static final String GTEXT_PREFIX = "gText";
	public static final String ETEXT_PREFIX = "eText";
	public static final String CODES_PREFIX = "codes";
	
	private Field idField;
	private Field segKeyField;
	private Field tmIdField;
	private Variants variants;
	private Fields attributes;

	public TmEntry (String segKey,
		String tmId)
	{
		idField = new Field(ID_FIELDNAME, tmId+segKey, Store.YES, Index.NOT_ANALYZED);
		tmIdField = new Field(TMID_FIELDNAME, tmId, Store.YES, Index.NOT_ANALYZED);
		segKeyField = new Field(SEGKEY_FIELDNAME, segKey, Store.YES, Index.NOT_ANALYZED);
		variants = new Variants();
	}
	
	public TmEntry (String segKey,
		String tmId,
		String locale,
		String genericText,
		String codesAsString)
	{
		this(segKey, tmId);
		variants.put(locale, new Variant(locale, genericText, codesAsString));
	}
	
	public String getId () {
		return idField.stringValue();
	}

	public Field getIdField () {
		return idField;
	}

	public String getTmId () {
		return tmIdField.stringValue();
	}
	
	public Field getTmIdField () {
		return tmIdField;
	}
	
	public String getSegKey () {
		return segKeyField.stringValue();
	}

	public Field getSegKeyField () {
		return segKeyField;
	}

	public Variants getVariants () {
		return variants;
	}

	public void addVariant (Variant variant) {
		variants.put(variant.getLocale(), variant);
	}
	
	public void setVariants (Variants variants) {
		this.variants = variants;
	}

	public Variant getVariant (String locale) {
		return variants.get(locale);
	}
	
	/**
	 * Sets an attribute
	 * @param name the name of the attribute.
	 * @param value the value of the attribute. If the value is null, no new attribute is set.
	 */
	public void setAttribute (String name,
		String value)
	{
		if ( value == null ) return;
		if ( attributes == null ) attributes = new Fields();
		Field field = new Field(name, value, Store.YES, Index.NOT_ANALYZED);
		attributes.put(field.name(), field);
	}

	public Fields getAttributes () {
		return attributes;
	}

	public boolean hasAttribute () {
		return !Util.isEmpty(attributes);
	}

}
