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

package net.sf.okapi.common.encoder;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import net.sf.okapi.common.IParameters;

/**
 * Implements IEncoder for XML format.
 */
public class XMLEncoder implements IEncoder {

	private CharsetEncoder chsEnc;
	private String lineBreak;
	private IParameters params;
	
	public void setOptions (IParameters params,
		String encoding,
		String lineBreak)
	{
		this.params = params;
		this.lineBreak = lineBreak;
		// Use an encoder only if the output is not UTF-8/16
		// since those support all characters
		if ( "utf-8".equals(encoding) || "utf-16".equals(encoding) ) {
			chsEnc = null;
		}
		else {
			chsEnc = Charset.forName(encoding).newEncoder();
		}
	}

	public String encode (String text, 
		int context)
	{
		if ( text == null ) return "";
		boolean escapeGT = false;
		int quoteMode = 1;
		
		StringBuffer sbTmp = new StringBuffer(text.length());
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case '<':
				sbTmp.append("&lt;");
				continue;
			case '>':
				if ( escapeGT ) sbTmp.append("&gt;");
				else {
					if (( i > 0 ) && ( text.charAt(i-1) == ']' )) sbTmp.append("&gt;");
					else sbTmp.append('>');
				}
				continue;
			case '&':
				sbTmp.append("&amp;");
				continue;
			case '"':
				if ( quoteMode > 0 ) sbTmp.append("&quot;");
				else sbTmp.append('"');
				continue;
			case '\'':
				switch ( quoteMode ) {
				case 1:
					sbTmp.append("&apos;");
					break;
				case 2:
					sbTmp.append("&#39;");
					break;
				default:
					sbTmp.append(text.charAt(i));
					break;
				}
				continue;
			case '\n':
				sbTmp.append(lineBreak);
				break;
			default:
				if ( text.charAt(i) > 127 ) { // Extended chars
					if (( chsEnc != null ) && ( !chsEnc.canEncode(text.charAt(i)) )) {
						sbTmp.append(String.format("&#x%04x;", text.codePointAt(i)));
					}
					else { // No encoder or char is supported
						sbTmp.append(text.charAt(i));
					}
				}
				else { // ASCII chars
					sbTmp.append(text.charAt(i));
				}
				continue;
			}
		}
		return sbTmp.toString();
	}

	public String encode (char value, int context) {
		switch ( value ) {
		case '<':
			return "&lt;";
		case '\"':
			return "&quot;";
		case '\'':
			return "&apos;";
		case '&':
			return "&amp;";
		case '\n':
			return lineBreak;
		default:
			if ( value > 127 ) { // Extended chars
				if (( chsEnc != null ) && ( !chsEnc.canEncode(value) )) {
					return String.format("&#x%04x;", (int)value);
				}
				else { // No encoder or char is supported
					return String.valueOf(value);
				}
			}
			else { // ASCII chars
				return String.valueOf(value);
			}
		}
	}

	public String toNative (String propertyName,
		String value)
	{
		// PROP_ENCODING: Same value in native
		// PROP_LANGUGE: Same value in native
		return value;
	}

}
