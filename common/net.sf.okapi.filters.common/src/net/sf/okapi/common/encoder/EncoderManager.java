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

import java.security.InvalidParameterException;
import java.util.Hashtable;

import net.sf.okapi.common.IParameters;

/**
 * Provides caching and lookup mechanism for the text encoders used when writing out text
 * processed by a filter.
 */
public class EncoderManager implements IEncoder {

	private String mimeType = "";
	private IEncoder encoder;
	private Hashtable<String, String> mimeMap;
	private String defEncoding;
	private String defLineBreak;
	private IParameters defParams;

	/**
	 * Creates a new encoder manager, with default pre-defined encoder loaded.
	 */
	public EncoderManager () {
		mimeMap = new Hashtable<String, String>();
		// Default mapping
		mimeMap.put("text/xml", "net.sf.okapi.common.encoder.XMLEncoder");
		mimeMap.put("text/x-odf", "net.sf.okapi.common.encoder.XMLEncoder");
		mimeMap.put("text/x-properties", "net.sf.okapi.common.encoder.PropertiesEncoder");
		mimeMap.put("text/x-mif", "net.sf.okapi.common.encoder.MIFEncoder");
		mimeMap.put("text/html", "net.sf.okapi.common.encoder.HtmlEncoder");
		mimeMap.put("text/x-po", "net.sf.okapi.common.encoder.POEncoder");
	}

	/**
	 * Clears all encoders from the manager.
	 */
	public void clearMap () {
		mimeMap.clear();
	}
	
	/**
	 * Adds a mapping to the manager. If a mapping for this MIME type exists already
	 * in the manager, it will be overridden by this new one.
	 * @param mimeType The MIME type identifier for this mapping.
	 * @param className The class name of the encoder to use.
	 */
	public void addMapping (String mimeType,
		String className)
	{
		mimeMap.put(mimeType, className);
	}
	
	/**
	 * Removes a given mapping from the manager.
	 * @param mimeType The MIME type identifier of the mapping to remove.
	 */
	public void removeMapping (String mimeType) {
		mimeMap.remove(mimeType);
	}
	
	/**
	 * Updates the current cached encoder for this manager.
	 * @param newMimeType The MIME type identifier for the encoder to use now. If there is no mapping for the
	 * given MIME type, the cache is cleared and no encoder is active.
	 */
	public void updateEncoder (String newMimeType) {
		try {
			if ( newMimeType == null ) return;
			// Check if the current encoder is for the same mime-type
			if ( mimeType.compareTo(newMimeType) == 0 ) return;
		
			// If not: lookup what encoder to use
			mimeType = newMimeType;
			String name = mimeMap.get(mimeType);
			if ( name == null ) { // Not in the map, nullify the encoder
				encoder = null;
				return;
			}
			// Else: Instantiate the encoder based on the class name
			encoder = (IEncoder)Class.forName(name).newInstance();
			// And set the options
			//TODO: Handle parameters at some point
			encoder.setOptions(defParams, defEncoding, defLineBreak);
		}
		catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		}
		catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Encodes a given text with the encoder currently cached. If no encoder is currently
	 * cached, the text is returned untouched.
	 * @param text The text to encode.
	 * @param context The context of the text: 0=text, 1=skeleton, 2=inline.
	 * @return The encoded text.
	 */
	public String encode (String text,
		int context)
	{
		if ( encoder != null ) return encoder.encode(text, context);
		else return text;
	}

	/**
	 * Encodes a given character with the encoder currently cached. If no encoder is currently
	 * cached, the character is returned as its string value.
	 * @param value The character to encode.
	 * @param context The context of the character: 0=text, 1=skeleton, 2=inline.
	 * @return The encoded character 9as a string since it can be now made up of
	 * more than one character).
	 */
	public String encode (char value,
		int context)
	{
		if ( encoder != null ) return encoder.encode(value, context);
		else return String.valueOf(value); 
	}

	/**
	 * Encodes a given code-point with the encoder currently cached. If no encoder is currently
	 * cached, the character is returned as its string value.
	 * @param codePoint The code-point to encode.
	 * @param context The context of the character: 0=text, 1=skeleton, 2=inline.
	 * @return The encoded character 9as a string since it can be now made up of
	 * more than one character).
	 */
	public String encode (int codePoint,
		int context)
	{
		if ( encoder != null ) return encoder.encode(codePoint, context);
		else {
			if ( Character.isSupplementaryCodePoint(codePoint) ) {
				return new String(Character.toChars(codePoint));
			}
			return String.valueOf((char)codePoint); 
		}
	}

	/**
	 * Gets the encoder currently cached by this manager.
	 * @return The encoder currently cached by this manager, or null if there is none.
	 */
	public IEncoder getEncoder () {
		return encoder;
	}

	/**
	 * Sets the options for the encoder currently cached. If no encoder is currently
	 * cached, the method does nothing.
	 * @param params The parameters object with all the configuration information 
	 * specific to this encoder.
	 * @param encoding The name of the charset encoding to use.
	 * @param lineBreak Type of line-break to use in the output.
	 */
	public void setOptions (IParameters params,
		String encoding,
		String lineBreak)
	{
		if ( encoder != null ) {
			encoder.setOptions(params, encoding, lineBreak);
		}
	}

	public String toNative (String propertyName,
		String value)
	{
		if ( encoder != null ) {
			return encoder.toNative(propertyName, value);
		}
		// No change if there is no encoder active
		return value;
	}

	/**
	 * Sets the default options for this encoder manager. The values
	 * passed here are set as the values to use for each encoder as they
	 * are invoked.
	 * @param params The default parameter object.
	 * @param encoding The default encoding.
	 */
	public void setDefaultOptions (IParameters params,
		String encoding,
		String lineBreak)
	{
		defParams = params;
		defEncoding = encoding;
		if ( lineBreak == null ) {
			throw new InvalidParameterException("lineBreak parameter is null");
		}
		defLineBreak = lineBreak;
	}

}