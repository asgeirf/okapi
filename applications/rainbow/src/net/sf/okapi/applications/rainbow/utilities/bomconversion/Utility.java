/*===========================================================================
  Copyright (C) 2008 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.applications.rainbow.utilities.bomconversion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.okapi.applications.rainbow.utilities.BaseUtility;
import net.sf.okapi.applications.rainbow.utilities.ISimpleUtility;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;

public class Utility extends BaseUtility implements ISimpleUtility {

	private final byte[]     BOM_UTF8       = {(byte)0xEF,(byte)0xBB,(byte)0xBF};
	private final byte[]     BOM_UTF16BE    = {(byte)0xFE,(byte)0xFF};
	private final byte[]     BOM_UTF16LE    = {(byte)0xFF,(byte)0xFE};

	private Parameters params;
	private String commonFolder;
	private byte[] buffer;

	public Utility () {
		params = new Parameters();
	}
	
	public String getName () {
		return "oku_bomconversion";
	}
	
	/**
	 * Checks for BOM presence
	 * @param buffer The buffer to check.
	 * @param length The number of usable bytes in the buffer.
	 * @return 0 if there is no BOM, or the number of bytes used by
	 * the BOM if it is present.
	 */
	private int hasBOM (byte[] buffer,
		int length)
	{
		if ( length > 1 ) {
			// Check for UTF-16
			if (( buffer[0] == (byte)0xFE )
				&& ( buffer[1] == (byte)0xFF )) {
				// UTF-16BE
				logger.info("UTF-16BE detected");
				return 2;
			}
			else if (( buffer[0] == (byte)0xFF )
				&& ( buffer[1] == (byte)0xFE )) {
				// UTF-16LE
				logger.info("UTF-16LE detected");
				return 2;
			}
			// Check for UTF-8
			if ( length > 2 ) {
				if (( buffer[0] == (byte)0xEF )
					&& ( buffer[1] == (byte)0xBB )
					&& ( buffer[2] == (byte)0xBF )) {
					// UTF-8
					logger.info("UTF-8 detected");
					return 3;
				}
				// Check for UTF-32
				if ( length > 3) {
					if (( buffer[0] == (byte)0xFF )
						&& ( buffer[1] == (byte)0xFE )
						&& ( buffer[2] == (byte)0x00 )
						&& ( buffer[3] == (byte)0x00 )) {
						// UTF-32LE
						logger.info("UTF-32LE detected");
						return 4;
					}
					else if (( buffer[0] == (byte)0x00 )
						&& ( buffer[1] == (byte)0x00 )
						&& ( buffer[2] == (byte)0xFE )
						&& ( buffer[3] == (byte)0xFF )) {
						// UTF-32BE
						logger.info("UTF-32BE detected");
						return 4;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Tries to guess the type of endian from the byte patterns.
	 * @param buffer The buffer to check.
	 * @param length The number of usable bytes in the buffer.
	 * @return 0=no detection, 1=UTF-16BE, 2=UTF-16LE
	 */
	private int guessByteOrder (byte[] buffer,
		int length)
	{
		if ( length < 4 ) return 0;
		if (( buffer[0] != (byte)0x00 )
			&& ( buffer[1] == (byte)0x00 )
			&& ( buffer[2] != (byte)0x00 )
			&& ( buffer[3] == (byte)0x00 )) {
			// Probably UTF-16BE 
			return 1;
		}
		if (( buffer[0] == (byte)0x00 )
			&& ( buffer[1] != (byte)0x00 )
			&& ( buffer[2] == (byte)0x00 )
			&& ( buffer[3] != (byte)0x00 )) {
			// Probably UTF-16LE
			return 2;
		}
		return 0;
	}
	
	public void processInput () {
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			// Open the input
			input = new FileInputStream(getInputPath(0));
			// Open the output
			Util.createDirectories(getOutputPath(0));
			output = new FileOutputStream(getOutputPath(0));
			
			// Reset the start of the buffer
			for ( int i=0; i<5; i++ ) buffer[i] = 0;
			// And read the 4 initial bytes
			int len = input.read(buffer, 0, 4);
			
			// Process the initial buffer
			if ( len == -1 ) {
				// Do nothing yet
			}
			else if ( len == 0 ) { // Empty file
				if ( !params.removeBOM ) { // Add the BOM
					// Let's make that empty file a UTF-8 file
					output.write(BOM_UTF8);
				}
			}
			else { // Non-empty file
				int n = hasBOM(buffer, len);
				if ( n > 0 ) { // A BOM is present
					if ( params.removeBOM ) {
						if (( n == 3 ) || ( params.alsoNonUTF8 )) {
							// Skip it, output the remaining bytes
							output.write(buffer, n, len-n);
						}
						else {
							// Keep the BOM
							output.write(buffer, 0, len);
						}
					}
					else { // Add the BOM: It's there, just write the buffer 
						output.write(buffer, 0, len);
					}
				}
				else { // No BOM present
					if ( !params.removeBOM ) { // If we add, do it 
						switch ( guessByteOrder(buffer, len) ) {
						case 1: // UTF-16BE
							output.write(BOM_UTF16BE);
							break;
						case 2: // UTF-16LE
							output.write(BOM_UTF16LE);
							break;
						default: // Assume the file is UTF-8 
							output.write(BOM_UTF8);
							break;
						}
					}
					// Then write the buffer we checked
					output.write(buffer, 0, len);
				}
			}
			
			// Now copy the remaining of the file
			while ( (len = input.read(buffer)) > 0 ) {
				output.write(buffer, 0, len);
			}
		}
		catch ( FileNotFoundException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try { // Close the files
				if ( output != null ) {
					output.close();
					output = null;
				}
				if ( input != null ) {
					input.close();
					input = null;
				}
			}
			catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	public void setOptions (String sourceLanguage,
		String targetLanguage)
	{
	}

	public void postprocess () {
		// Release the buffer
		buffer = null;
	}

	public void preprocess () {
		commonFolder = null; // Reset
		buffer = new byte[1024*2];
	}

	public IParameters getParameters () {
		return params;
	}

	public boolean hasParameters () {
		return true;
	}

	public boolean isFilterDriven () {
		return false;
	}

	public boolean needsRoots () {
		return false;
	}

	public void addOutputData (String path,
		String encoding)
	{
		super.addOutputData(path, encoding);
		// Compute the longest common folder
		commonFolder = Util.longestCommonDir(commonFolder,
			Util.getDirectoryName(path), !Util.isOSCaseSensitive());
	}

	public void setParameters (IParameters paramsObject) {
		params = (Parameters)paramsObject;
	}

	public String getFolderAfterProcess () {
		return commonFolder;
	}

	public int requestInputCount() {
		return 1;
	}

}
