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

package net.sf.okapi.common;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Manages XML namespaces.
 * The reserved 'xml' and 'xmlns' prefixes are hard-coded.
 * The following namespaces are pre-added:
 * xsl : http://www.w3.org/1999/XSL/Transform
 */
public class NSContextManager implements NamespaceContext {

	private Hashtable<String, String>  table;
	
	public NSContextManager () {
		table = new Hashtable<String, String>();
		// Defaults in addition to the hard-coded
		add("xsl", "http://www.w3.org/1999/XSL/Transform");
	}
	
	public String getNamespaceURI (String prefix) {
		if ( table.containsKey(prefix) )
			return table.get(prefix);
		if ( prefix.equals(XMLConstants.XML_NS_PREFIX) )
			return XMLConstants.XML_NS_URI;
		if ( prefix.equals(XMLConstants.XMLNS_ATTRIBUTE) )
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		// Default
		return XMLConstants.NULL_NS_URI;
	}

	public String getPrefix (String uri) {
		Enumeration<String> E = table.keys();
		String key;
		while ( E.hasMoreElements() ) {
			key = E.nextElement();
			if ( table.get(key).equals(uri) )
				return key;
		}
		if ( uri.equals(XMLConstants.XML_NS_URI))
			return XMLConstants.XML_NS_PREFIX;
		if ( uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI) )
			return XMLConstants.XMLNS_ATTRIBUTE;
		else
			return null;
	}

	public Iterator<String> getPrefixes (String uri) {
		// Not implemented
		return null;
	} 

	/**
	 * Adds a prefix/uri pair to the manager. No checking is done for existing
	 * prefix: If the same is already defined, it will be overwritten.
	 * @param prefix The prefix of the namespace
	 * @param uri The uri of the namespace
	 */
	public void add (String prefix,
		String uri) {
		table.put(prefix, uri);
	}
}
