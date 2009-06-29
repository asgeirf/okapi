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

package net.sf.okapi.filters.table.base;

/**
 * Base Table Filter parameters
 * 
 * @version 0.1, 09.06.2009
 * @author Sergei Vasilyev   
 */

public class Parameters extends net.sf.okapi.filters.plaintext.base.Parameters {
	
	/**
	 * @see detectColumnsMode
	 */
	public static int DETECT_COLUMNS_NONE = 0;
	public static int DETECT_COLUMNS_COL_NAMES = 1;
	public static int DETECT_COLUMNS_FIXED_NUMBER = 2;
	
	/**
	 * @see sendHeaderMode
	 */
	public static int SEND_HEADER_NONE = 0;
	public static int SEND_HEADER_COLUMN_NAMES_ONLY = 1;
	public static int SEND_HEADER_ALL = 2;
	
	/**
	 * @see sendColumnsMode
	 */
	public static int SEND_COLUMNS_NONE = 0;
	public static int SEND_COLUMNS_LISTED = 1;
	public static int SEND_COLUMNS_ALL = 2;
	
	/**
	 * @see trimMode
	 */
	public static int TRIM_NONE = 0;
	public static int TRIM_NONQUALIFIED_ONLY = 1;
	public static int TRIM_ALL = 2;
		
	/**
	 * Number of the line (1-based) containing filed names (column captions).<p>
	 * Default: 1
	 */
	public int columnNamesLineNum = 1;
	
	/**
	 * Number of the line (1-based) where actual data start after the header.<p>
	 * Default: 2
	 */
	public int valuesStartLineNum = 2;
	
	/**
	 * The filter can detect number of columns in the input. This option specifies the way of columns number detection:
	 * <li>DETECT_COLUMNS_NONE = 0 - no detection is performed, if different rows contain different number of values, then different 
	 * number of TUs will be sent for different rows  
	 * <li>DETECT_COLUMNS_COL_NAMES = 1 - number of columns is determined by the number of field names listed in the row 
	 * specified by the fieldNamesRow parameter. 
	 * <li>DETECT_COLUMNS_FIXED_NUMBER = 2 - number of columns is explicitly specified by the numColumns parameter.<p>
	 * Default: DETECT_COLUMNS_NONE
	 */
	public int detectColumnsMode = DETECT_COLUMNS_NONE;
	
	/**
	 * Number of columns in the input. This option is active only if detectColumnsMode = DETECT_COLUMNS_FIXED_NUMBER.<p>
	 * Extra columns are dropped, empty TUs are created for missing columns.<p>
	 */
	public int numColumns = 1;
	
	/**
	 * If there are one or more lines containing description of the data, names of fields etc., 
	 * and actual data don't start in the first line, then such first lines are considered a header, and this option specifies how to handle them:
	 * <li>SEND_HEADER_NONE = 0 - none of the header lines are sent as text units
	 * <li>SEND_HEADER_FIELD_NAMES_ONLY = 1 - only the values in the line specified by fieldNamesLineNum are sent as text units
	 * <li>SEND_HEADER_ALL = 2 - values in all header lines are sent as text units
	 * @see valuesStartLineNum
	 * @see fieldNamesLineNum
	 */
	public int sendHeaderMode = SEND_HEADER_COLUMN_NAMES_ONLY;
	
	/**
	 * Specifies how field values are trimmed of spaces:
	 * <li>TRIM_NONE = 0 - field values are not trimmed
	 * <li>TRIM_NONQUALIFIED_ONLY = 1 - only non-qualified field values are trimmed, leading and trailing spaces remain in qualified fields
	 * <li>TRIM_ALL = 2 - both non-qualified and qualified field values are trimmed of leading and trailing spaces.
	 * Default: TRIM_NONQUALIFIED_ONLY
	 * @see textQualifier
	 */
	public int trimMode = TRIM_NONQUALIFIED_ONLY;
	
	/**
	 * Specifies values of which columns should be sent as text units 
	 * <li>SEND_COLUMNS_NONE = 0 - none of the columns are sent as text units
	 * <li>SEND_COLUMNS_LISTED = 1 - only values from the columns listed in idColumns, sourceColumns, targetColumns, commentColumns 
	 * will be sent
	 * <li>SEND_COLUMNS_ALL = 2 - values from all columns will be sent.<p>
	 * Default: SEND_COLUMNS_ALL (send all columns)
	 */
	public int sendColumnsMode = SEND_COLUMNS_ALL;
	
	/**
	 * Index (1-based) of the column containing an optional record ID (or a key) of the current row in the table. 
	 */
	public int recordIdColumn = 0;
	
	/** 
	 * Indicates which columns contain source text. If this list is empty, then all columns of the table will be considered
	 * containing source text to be extracted.
	 * Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing source text
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing source text
	 */
	public String sourceColumns = "";				

	/** 
	 * Indicates which columns contain source IDs. The source IDs become names of TU resources. 
	 * Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing a source ID
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing source IDs
	 */
	public String sourceIdColumns = "";

	/**
	 * If there is the recordIdColumn index, and a source ID for the given column is not specified in the sourceIdColumns,
	 * then the source ID for that column is compiled of the value in recordIdColumn column, and the corresponding suffix in the
	 * sourceIdSuffixes. The source IDs become names of TU resources. 
	 */
	public String sourceIdSuffixes = "";
	
	/**
	 * Indicates which source columns the source ID columns on the sourceIdSourceRefs list correspond to.  
	 * Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing source text
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing source text
	 */	
	public String sourceIdSourceRefs = "";
	
	/** 
	 * Indicates which columns contain target text. Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing target text
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing target text
	 */
	public String targetColumns = "";		
	
	/**
	 * Indicates which source columns the target columns on the targetColumns list correspond to.  
	 * Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing source text
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing source text
	 */
	public String targetSourceRefs = "";
	
	/** 
	 * Indicates which languages should be used in target columns. Can be represented by one of the following strings:
	 *<li>"fr" - target text in all target columns is French
	 *<li>"fr, it, ge, fr" - target text in 1-st and 4-th columns is French, 2-nd - Italian, 3-rd - German
	 */
	public String targetLanguages = "";
	
	/** 
	 * Indicates which columns contain comments. Can be represented by one of the following string types:
	 *<li>"1" - index (1-based) of the column, containing a comment
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing comments
	 */
	public String commentColumns = "";
	
	/**
	 * Indicates which source columns the comment columns on the commentColumns list correspond to.
	 * The comment extracted from a comment column will become a read-only NOTE property of the corresponding source column referred.
	 * If you need a comment translated, list its column on the sourceColumns list.
	 * Can be represented by one of the following strings:
	 *<li>"1" - index (1-based) of the column, containing source text
	 *<li>"1,2,5" - comma-delimited list (1-based) of indexes of the columns, containing source text.
	 */	
	public String commentSourceRefs = "";
	
//----------------------------------------------------------------------------------------------------------------------------	
	
	public Parameters() {
		
		super();		
		
		reset();
		toString(); // fill the list
	}

	public void reset() {
		
		super.reset();
		
		// All parameters are set to defaults here
		columnNamesLineNum = 1;
		valuesStartLineNum = 2;
		detectColumnsMode = DETECT_COLUMNS_NONE;
		numColumns = 1;
		sendHeaderMode = SEND_HEADER_COLUMN_NAMES_ONLY;
		trimMode = TRIM_NONQUALIFIED_ONLY;
		sendColumnsMode = SEND_COLUMNS_ALL;
		sourceIdColumns = "";
		sourceColumns = "";
		targetColumns = "";
		commentColumns = "";
		commentSourceRefs = "";
		recordIdColumn = 0;
		sourceIdSourceRefs = "";
		sourceIdSuffixes = "";
		targetLanguages = "";
		targetSourceRefs = "";		
	}

	public void fromString(String data) {
		
		reset();
		
		super.fromString(data);
		
		buffer.fromString(data);
		
		// All parameters are retrieved here		
		columnNamesLineNum = buffer.getInteger("columnNamesLineNum", 1);
		valuesStartLineNum = buffer.getInteger("valuesStartLineNum", 2);
		detectColumnsMode = buffer.getInteger("detectColumnsMode", DETECT_COLUMNS_NONE);
		numColumns = buffer.getInteger("numColumns", 1);
		sendHeaderMode = buffer.getInteger("sendHeaderMode", SEND_HEADER_COLUMN_NAMES_ONLY);
		trimMode = buffer.getInteger("trimMode", TRIM_NONQUALIFIED_ONLY);		
		sendColumnsMode = buffer.getInteger("sendColumnsMode", SEND_COLUMNS_ALL);
		sourceIdColumns = buffer.getString("sourceIdColumns", "");
		sourceColumns = buffer.getString("sourceColumns", "");
		targetColumns = buffer.getString("targetColumns", "");
		commentColumns = buffer.getString("commentColumns", "");
		commentSourceRefs = buffer.getString("commentSourceRefs", "");
		recordIdColumn = buffer.getInteger("recordIdColumn", 0);
		sourceIdSourceRefs = buffer.getString("sourceIdSourceRefs", "");
		sourceIdSuffixes = buffer.getString("sourceIdSuffixes", "");
		targetLanguages = buffer.getString("targetLanguages", "");
		targetSourceRefs = buffer.getString("targetSourceRefs", "");
	}
	
	@Override
	public String toString () {
		
		buffer.reset();
		
		super.toString(); // Will write to the same buffer
		
		// All parameters are set here				
		buffer.setInteger("columnNamesLineNum", columnNamesLineNum);
		buffer.setInteger("valuesStartLineNum", valuesStartLineNum);
		buffer.setInteger("detectColumnsMode", detectColumnsMode);
		buffer.setInteger("numColumns", numColumns);
		buffer.setInteger("sendHeaderMode", sendHeaderMode);
		buffer.setInteger("trimMode", trimMode);		
		buffer.setInteger("sendColumnsMode", sendColumnsMode);
		buffer.setString("sourceIdColumns", sourceIdColumns);
		buffer.setString("sourceColumns", sourceColumns);
		buffer.setString("targetColumns", targetColumns);
		buffer.setString("commentColumns", commentColumns);
		buffer.setString("commentSourceRefs", commentSourceRefs);
		buffer.setInteger("recordIdColumn", recordIdColumn);
		buffer.setString("sourceIdSourceRefs", sourceIdSourceRefs);
		buffer.setString("sourceIdSuffixes", sourceIdSuffixes);
		buffer.setString("targetLanguages", targetLanguages);
		buffer.setString("targetSourceRefs", targetSourceRefs);
		
		return buffer.toString();
	}
	
}
