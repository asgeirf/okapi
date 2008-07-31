package net.sf.okapi.applications.rainbow.utilities.linebreakconversion;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;

import net.sf.okapi.applications.rainbow.utilities.ISimpleUtility;
import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;

public class Utility implements ISimpleUtility {

	private IParameters                params;
	private String                     commonFolder;
	private String                     inputPath;
	private String                     outputPath;
	private String                     encoding;
	private String                     lineBreak;
	

	public Utility () {
		params = new Parameters();
	}
	
	public void resetLists () {
		// Not used in this utility
	}
	
	public String getID () {
		return "oku_linebreakconversion";
	}
	
	public void processInput () {
		BufferedReader reader = null;
		OutputStreamWriter writer = null;
		try {
			// Open the input
			InputStream input = new FileInputStream(inputPath);
			BOMAwareInputStream bis = new BOMAwareInputStream(input, encoding);
			encoding = bis.detectEncoding(); // Update the encoding: it'll be use for the output
			reader = new BufferedReader(new InputStreamReader(bis, encoding));
			
			// Open the output
			OutputStream output = new FileOutputStream(outputPath);
			writer = new OutputStreamWriter(new BufferedOutputStream(output), encoding);
			Util.writeBOMIfNeeded(writer, true, encoding);
			
			// Set the variables
			CharBuffer buffer = CharBuffer.allocate(1024);
			int length = 0;
			int start = 0;
			int i;
			int done = 0;
			
			// Process the file
			while ( (length = reader.read(buffer)) > 0 ) {
				buffer.position(0);
				// Reset 'done' flag on second pass after it was set
				if ( done == 1 ) done++; else done = 0;
				// Replace line-breaks
				for ( i=0; i<length; i++ ) {
					if ( buffer.charAt(i) == '\n') {
						if (( i != 0 ) || ( done == 0 )) {
							writer.write(buffer.array(), start, i-start);
							writer.write(lineBreak);
						}
						start = i+1;
					}
					else if ( buffer.charAt(i) == '\r') {
						writer.write(buffer.array(), start, i-start);
						writer.write(lineBreak);
						// Check if it's a \r\n
						if ( i+1 < length ) {
							if ( buffer.charAt(i+1) == '\n' ) {
								i++; // Skip it
							}
						}
						start = i+1;
						// We could be splitting a \r\n, so let's remember
						done = 1;
					}
				}
				// Write out the remainder of the buffer
				if ( length-start > 0 ) {
					writer.write(buffer.array(), start, length-start);
				}
				// Reset positions
				start = 0;
			}
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( writer != null ) {
					writer.close();
					writer = null;
				}
				if ( reader != null ) {
					reader.close();
					reader = null;
				}
			}
			catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	public void doEpilog () {
		// Not used in this utility
	}

	public void doProlog (String sourceLanguage,
		String targetLanguage)
	{
		commonFolder = null; // Reset
		lineBreak = params.getParameter("lineBreak");
	}

	public String getInputRoot () {
		return null;
	}

	public String getOutputRoot () {
		return null;
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

	public void addInputData (String path,
		String encoding,
		String filterSettings)
	{
		this.inputPath = path;
		this.encoding = encoding;
	}

	public void addOutputData (String path,
		String encoding)
	{
		// Compute the longest common folder
		commonFolder = Util.longestCommonDir(commonFolder,
			Util.getDirectoryName(path), !Util.isOSCaseSensitive());
		outputPath = path;
		// Encoding stays the same as the input
	}

	public void setParameters (IParameters paramsObject) {
		params = paramsObject;
	}

	public void setRoots (String inputRoot,
		String outputRoot)
	{
		// Not used in this utility.
	}

	public String getFolderAfterProcess () {
		return commonFolder;
	}

	public int getInputCount() {
		return 1;
	}
}
