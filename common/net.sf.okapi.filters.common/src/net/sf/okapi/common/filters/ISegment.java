/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel (at ENLASO Corporation)                  */
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
/* Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA              */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.filters;

/**
 * Provides an abstract layer to segmented text.
 * Assumptions:
 * <ul>
 * <li>All line-breaks set in the segment must be of type '\n'.</li>
 * </ul>
 * <p>Use the different <code>append()</code> methods to build the segment.
 * <p>Use {@link #getCodes()} and {@link #getCodedText()} to retrieve the serialized strings
 * corresponding to the internal representation of the segment. You can use this
 * to store segments in a database for example.
 * <p>Use {@link #setCodes(String)} and then {@link #setTextFromCoded(String)} to reset a segment with the strings 
 * obtained by {@link #getCodes()} and {@link #getCodedText()}. If you do not modify the in-line
 * codes you can just use {@link #setTextFromCoded(String)}.
 * <p>The method {@link #toString(int)} allows you to get different string representation
 * of the segment. The method <code>toString(TEXTTYPE_ORIGINAL)</code> is the same as {@link #toString()}
 * and the method <code>toString(TEXTTYPE_CODED)</code> is the same as {@link #getCodedText()}.
 * <p>Use toString(TEXTTYPE_GENERIC) to get a string representation where in-line
 * codes are marked as simple numbered tags. for example the HTML <code>"<b>bold text</b><br/>"</code>
 * will be marked <code>"<1>bold text</1><2/>"</code>.
 */
public interface ISegment {

	public static final int CODE_OPENING    = 0xE101;
	public static final int CODE_CLOSING    = 0xE102;
	public static final int CODE_ISOLATED   = 0xE103;

	public static final int TEXTTYPE_ORIGINAL    = 0;
	public static final int TEXTTYPE_CODED       = 1;
	public static final int TEXTTYPE_GENERIC     = 2;
	public static final int TEXTTYPE_PLAINTEXT   = 3;
	public static final int TEXTTYPE_XLIFF12     = 4;
	public static final int TEXTTYPE_XLIFF12XG   = 5;
	public static final int TEXTTYPE_TMX14       = 6;

	/**
	 * Creates a new ISegment object that is a clone of this one.
	 * This method is equivalent to create a new object and
	 * call {@link #copyFrom(ISegment)}.
	 * @return A cloned ISegment object.
	 */
	ISegment clone();
	
	/**
	 * Resets the object. All text, codes, and other properties
	 * are reset to their default values.
	 */
	void reset ();
	
	/**
	 * Appends text to the object.
	 * @param text The text to append.
	 */
	void append (String text);
	
	/**
	 * Appends a character to the object.
	 * @param value The character to append.
	 */
	void append (char value);
	
	/**
	 * Appends an in-line code to the object.
	 * @param type The type of code (one of the CODE_* values)
	 * @param label The label for the code (can be null). The value MUST be the 
	 * same for an opening code and its corresponding closing code.
	 * @param codeData The underlying data for the code (can be null).
	 */
	void append (int type,
		String label,
		String codeData);

	/**
	 * Appends the text and codes of an existing segment to the object.
	 * Note that no other data (e.g. properties) of the given segment are copied
	 * over the object. 
	 * @param segment The segment to append.
	 */
	void append (ISegment segment);
	
	/**
	 * Copies all the data of the original object into this one. 
	 * @param original The object where to copy the data from.
	 */
	void copyFrom (ISegment original);
	
	/**
	 * Gets the string representation of the object in the original format.
	 * Note that line-breaks will be '\n'.
	 * This method is the same as toString(TEXTTYPE_ORIGINAL);
	 * @return The object in its original format.
	 */
	String toString ();
	
	/**
	 * Gets the string representation of the object in a given format.
	 * @param textType The type of format requested (one of the TEXTTYPE_* values)
	 * @return The object in the requested text representation.
	 */
	String toString (int textType);
	
	/**
	 * Gets the character length of a string representation of the object in a 
	 * given format.
	 * @param textType The type of format requested (one of the TEXTTYPE_* values)
	 * @return The number of characters for the requested text representation.
	 */
	int getLength (int textType);
	
	/**
	 * Gets a string serialization of the internal storage of codes.
	 * @return The storage representation of the codes of the object. 
	 */
	String getCodes ();

	/**
	 * Sets all the codes of the object based on the given string. 
	 * @param data The serialization of the codes to apply.
	 * You obtain this string using the {@link #getCodes()} method.
	 */
	void setCodes (String data);
	
	/**
	 * Gets the coded representation of the text of the object.
	 * This method is the same as toString(TEXTTYPE_CODED);
	 * @return The coded representation of the text of the object.
	 */
	String getCodedText ();
	
	/**
	 * Sets the text of the object. This method removes any existing text
	 * or codes from the object.
	 * @param text Plain text to apply.
	 */
	void setText (String text);
	
	/**
	 * Sets the text of the object based on the given coded string.
	 * If you also change the codes using {@link #setCodes(String)}, make
	 * sure to call {@link #setCodes(String)} before calling
	 * {@link #setTextFromCoded(String)}. 
	 * @param codedText The coded text to apply. You can obtain this string
	 * using the getCodedText() method.
	 */
	void setTextFromCoded (String codedText);
	
	/**
	 * Sets the text of the object based on the given generic string.
	 * If you also change the codes using {@link #setCodes(String)}, make 
	 * sure to call {@link #setCodes(String)} before calling
	 * {@link #setTextFromGeneric(String)}. 
	 * @param genericText The generic text to apply. You can obtain this
	 * string using the toString(TEXTTYPE_GENERIC) method.
	 */
	void setTextFromGeneric (String genericText);
	
	/**
	 * Indicates if the object has one or more in-line codes.
	 * @return True if the object has at least one in-line code, false otherwise.
	 */
	boolean hasCode ();
	
	/**
	 * Gets the number of codes in the object.
	 * @return The number of codes in the object.
	 */
	int getCodeCount ();

	/**
	 * Gets the zero-based index of an existing code in the object.
	 * @param id The ID value of the code to look for.
	 * @param type The type of code (one of the CODE_* values).
	 * @return The zero-based index of the code, or -1 if not found. 
	 */
	int getCodeIndex (int id,
		int type);

	/**
	 * Gets the ID of a given in-line code.
	 * @param index The zero-based index of the code to lookup.
	 * This value must be between 0 and {@link #getCodeCount()}-1.
	 * @return The ID value of the code at the given index. 
	 */
	int getCodeID (int index);
	
	/**
	 * Gets the underlying data text for a given in-line code.
	 * @param index The zero-based index of the code to lookup.
	 * This value must be between 0 and {@link #getCodeCount()}-1.
	 * @return The underlying data of the code at the given index (can be null).
	 */
	String getCodeData (int index);

	/**
	 * Gets the label assigned to a given in-line code.
	 * @param index The zero-based index of the code to lookup.
	 * @return The label of the code at the given index (can be null).
	 */
	String getCodeLabel (int index);
	
	/**
	 * Indicates whether the object is empty (no text and no codes). 
	 * @return True if the object is empty, false if there is any text
	 * (white space is seen as text here) or codes.
	 */
	boolean isEmpty ();
	
	/**
	 * Indicates whether the object 
	 * @param whiteSpaceIsText Indicate if the method should consider white
	 * spaces as text. Note that in-line codes are not considered part of
	 * the text.
	 * @return True if at least one text character exists, false otherwise.
	 */
	boolean hasText (boolean whiteSpaceIsText);

	/**
	 * Creates a new segment from a section of this object. The in-lines codes
	 * in the returned new segment are adjusted as needed.
	 * @param start Start position (in the coded text representation) of
	 * the part to extract.
	 * @param length Length (in the coded text representation) of the part
	 * to extract.
	 * @param addMissingCodes True if the missing codes creating when
	 * splitting the object are to be added as needed. False if the extra
	 * codes are simply to be changed to isolated codes.
	 * @return A new ISegment object containing a copy of the original one
	 * with only the extracted part of the text. Null if any of the size
	 * parameters are out of bounds.
	 */
	ISegment subSegment (int start,
		int length,
		boolean addMissingCodes);
}
