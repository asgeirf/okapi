package net.sf.okapi.filters.mif;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Hashtable;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.FilterEvent;
import net.sf.okapi.common.filters.FilterEventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.Document;
import net.sf.okapi.common.resource.IResource;
import net.sf.okapi.common.resource.SkeletonUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;

public class MIFFilter implements IFilter {
	
	static final Hashtable<String, Character> charTable = initCharTable();

	private BufferedReader reader;
	private StringBuilder tagBuffer;
	private StringBuilder strBuffer;
	private StringBuilder sklBuffer;
	private StringBuilder ilcBuffer;
	private StringBuilder buffer;
	private Document docRes;
	private int parseState = 0;
	private int inPara;
	private int inString;
	private IResource currentRes;
	private int tuId;
	private int skId;
	private int level;
	private TextContainer cont;
	
	private static Hashtable<String, Character> initCharTable () {
		Hashtable<String, Character> table = new Hashtable<String, Character>();
		table.put("HardSpace",    '\u00a0');
		table.put("DiscHyphen",   '\u00ad');
		table.put("NoHyphen",     '\u200d');
		table.put("Tab",          '\t');
		table.put("Cent",         '\u00a2');
		table.put("Pound",        '\u00a3');
		table.put("Yen",          '\u00a5');
		table.put("EnDash",       '\u2013');
		table.put("Dagger",       '\u2020');
		table.put("EmDash",       '\u2014');
		table.put("DoubleDagger", '\u2021');
		table.put("Bullet",       '\u2022');
		table.put("NumberSpace",  '\u2007');
		table.put("ThinSpace",    '\u2009');
		table.put("EnSpace",      '\u2002');
		table.put("EmSpace",      '\u2003');
		table.put("HardReturn",   '\r');
		return table;
	}

	public void cancel () {
		// TODO
	}

	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public String getName () {
		return "MIFFilter";
	}

	public IParameters getParameters () {
		return null;
	}

	public IResource getResource () {
		return currentRes;
	}

	public boolean hasNext () {
		return (( parseState == 1 ) || ( parseState == 2 ));
	}
	
	public void open (InputStream input) {
		try {
			close(); //TODO: encoding for non-EN
			reader = new BufferedReader(
				new InputStreamReader(input, "UTF-8"));
			tagBuffer = new StringBuilder();
			sklBuffer = new StringBuilder();
			ilcBuffer = new StringBuilder();
			strBuffer = new StringBuilder();
			level = 0;
			parseState = 1; // Need to send start-document
			inPara = -1;
			inString = -1;
			tuId = -1;
			skId = -1;
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void open (URL inputPath) {
		try { //TODO: Make sure this is actually working (encoding?, etc.)
			open(inputPath.openStream());
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public void open (CharSequence inputText) {
		// Not supported with MIF filter
		throw new UnsupportedOperationException();
	}

	public void setOptions (String language,
		String defaultEncoding)
	{
	}

	public void setParameters (IParameters params) {
	}

	public FilterEvent next () {
		try {
			// Handle first call
			if ( parseState == 1 ) {
				docRes = new Document();
				parseState = 2; // Inside the document
				currentRes = docRes;
				return new FilterEvent(FilterEventType.START_DOCUMENT, docRes);
			}
			
			// Process other calls
			sklBuffer.setLength(0);
			ilcBuffer.setLength(0);
			buffer = sklBuffer;// Start buffer is the skeleton buffer
			int c;
			while ( (c = reader.read()) != -1 ) {
				switch ( c ) {
				case '#':
					buffer.append((char)c);
					readComment();
					break;
				case '<': // Start of statement
					level++;
					buffer.append((char)c);
					String tag = readTag();
					if ( "Para".equals(tag) ) {
						inPara = level;
						cont = new TextContainer();
						// Return skeleton before
						currentRes = new SkeletonUnit(getSkeletonId(), sklBuffer.toString());
						return new FilterEvent(FilterEventType.SKELETON_UNIT, currentRes);
					}
					else if ( "String".equals(tag) ) {
						inString = level;
					}
					break;
				case '>': // End of statement
					if ( inString == level ) {
						inString = -1;
					}
					else if ( inPara == level ) {
						inPara = -1;
						if ( !cont.isEmpty() ) {
							TextUnit tu = new TextUnit();
							tu.setID(getTextId());
							tu.setSourceContent(cont);
							currentRes = tu;
							return new FilterEvent(FilterEventType.TEXT_UNIT, currentRes);
							//TODO: Skeleton should be attached too
						}
					}
					buffer.append((char)c);
					level--;
					// Return skeleton
					currentRes = new SkeletonUnit(getSkeletonId(), sklBuffer.toString());
					return new FilterEvent(FilterEventType.SKELETON_UNIT, currentRes);
				case '`':
					if (( inPara > -1 ) && ( level == inString )) {
						cont.append(processString());
					}
					else {
						buffer.append((char)c); // Store '`'
						copyStringToStorage();
					}
					break;
				default:
					buffer.append((char)c);
					break;
				}
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		
		parseState = 0; // No more
		currentRes = docRes;
		return new FilterEvent(FilterEventType.END_DOCUMENT, null);
	}

	private void readComment () throws IOException {
		int c;
		while ( (c = reader.read()) != -1 ) {
			buffer.append((char)c);
			switch ( c ) {
			case '\r':
			case '\n':
			case -1:
				return;
			}
		}
	}
	
	private String readTag () throws IOException {
		tagBuffer.setLength(0);
		int c;
		boolean leadingWSDone = false;
		// Skip and whitespace between '<' and the name
		do {
			switch ( c = reader.read() ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				// Let go for now
				//buffer.append((char)c);
				break;
			case -1:
			default:
				leadingWSDone = true;
				break;
			}
		}
		while ( !leadingWSDone );
		
		// Now read the name
		while ( true ) {
			
			switch ( c ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
			case -1:
				buffer.append(tagBuffer);
				buffer.append((char)c);
				return tagBuffer.toString();
			default:
				tagBuffer.append((char)c);
				break;
			}
			c = reader.read();
		}
	}
	
	void copyStringToStorage () throws IOException {
		int c;
		boolean inEscape = false;
		while ( (c = reader.read()) != -1 ) {
			buffer.append((char)c);
			if ( inEscape ) {
				inEscape = false;
			}
			else {
				if ( c == '\'' ) return;
			}
		}
		// Else: Missing end of string error
		throw new RuntimeException("End of string is missing.");
	}
	
	String processString () throws IOException {
		strBuffer.setLength(0);
		int c;
		boolean inEscape = false;
		while ( (c = reader.read()) != -1 ) {
			if ( inEscape ) {
				switch ( c ) {
				case '\\':
				case '>':
					strBuffer.append((char)c);
					break;
				case 't':
					strBuffer.append('\t');
					break;
				case 'Q':
					strBuffer.append('`');
					break;
				case 'q':
					strBuffer.append('\'');
					break;
				case 'u':
				case 'x':
					//TODO
					break;
				}
				inEscape = false;
			}
			else {
				switch ( c ) {
				case '\'': // End of string
					return strBuffer.toString();
				case '\\':
					inEscape = true;
					break;
				default:
					strBuffer.append((char)c);
					break;
				}
			}
		}
		// Else: Missing end of string error
		throw new RuntimeException("End of string is missing.");
	}

	private String getTextId () {
		return String.valueOf(++tuId);
	}

	private String getSkeletonId () {
		return String.valueOf(++skId);
	}

}
