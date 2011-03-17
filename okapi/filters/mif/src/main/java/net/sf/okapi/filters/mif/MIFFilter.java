/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mif;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiIllegalFilterOperationException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiUnsupportedEncodingException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

@UsingParameters(Parameters.class)
public class MIFFilter implements IFilter {
	
	private final Logger logger = Logger.getLogger(getClass().getName());

	private static final int BLOCKTYPE_TEXTFLOW = 1;
	private static final int BLOCKTYPE_TABLE = 2;
	
	private static final Hashtable<String, String> charTable = initCharTable();
	private static final Hashtable<String, String> encodingTable = initEncodingTable();
	
	private static final String TOPSTATEMENTSTOSKIP = "ColorCatalog;ConditionCatalog;BoolCondCatalog;"
		+ "CombinedFontCatalog;PgfCatalog;ElementDefCatalog;FmtChangeListCatalog;DefAttrValuesCatalog;"
		+ "AttrCondExprCatalog;FontCatalog;RulingCatalog;TblCatalog;KumihanCatalog;Views;VariableFormats;"
		+ "MarkerTypeCatalog;XRefFormats;Document;BookComponent;InitialAutoNums;Dictionary;AFrames;Page;";
	// Must end with ';'
	
	private static final String IMPORTOBJECT = "ImportObject";
	private static final String FRAMEROMAN = "x-MacRoman"; //TODO: Set to x-FrameRoman when ready
	
	private Parameters params;
	private String lineBreak;
	private String docName;
	private BufferedReader reader;
	private StringBuilder tagBuffer;
	private StringBuilder strBuffer;
	private int tuId;
	private int otherId;
	private int grpId;
	private boolean canceled;
	private LinkedList<Event> queue;
	private LocaleId srcLang;
	private GenericSkeleton skel;
	private boolean hasNext;
	private EncoderManager encoderManager;
	private int inBlock;
	private int blockLevel;
	private String version;
	private String sdId;
	private int paraLevel;
	private StringBuilder paraBuf;
	private boolean paraBufNeeded;
	private int tableGroupLevel;
	private int rowGroupLevel;
	private int cellGroupLevel;
	private int fnoteGroupLevel;
	private Stack<String> parentIds;
	private ArrayList<String> textFlows; 
	private ArrayList<String> tables;
	private boolean secondPass;
	private ByteArrayOutputStream byteStream;
	private CharsetDecoder[] decoders;
	private CharsetEncoder[] encoders;
	private CharsetDecoder currentDecoder;
	private int currentCharsetIndex;
	private MIFEncoder encoder;
	private int decodingErrors;
	private String baseEncoding;
	private boolean useUTF;
	
	private static Hashtable<String, String> initCharTable () {
		Hashtable<String, String> table = new Hashtable<String, String>();
		table.put("Tab", "\t");
		table.put("HardSpace", "\u00a0"); // = Unicode non-breaking space
		table.put("SoftHyphen", ""); // "\u2010" = Unicode Hyphen (not Soft-Hyphen), but we remove those
		table.put("HardHyphen", "\u2011"); // = Unicode Non-Breaking Hyphen
		table.put("DiscHyphen", "\u00ad"); // = Unicode Soft-Hyphen
		table.put("NoHyphen", "\u200d"); // = Unicode Zero-Width Joiner
		table.put("Cent", "\u00a2");
		table.put("Pound", "\u00a3");
		table.put("Yen", "\u00a5");
		table.put("EnDash", "\u2013");
		table.put("EmDash", "\u2014");
		table.put("Dagger", "\u2020");
		table.put("DoubleDagger", "\u2021");
		table.put("Bullet", "\u2022");
		table.put("HardReturn", "\n");
		table.put("NumberSpace", "\u2007");
		table.put("ThinSpace", "\u2009");
		table.put("EnSpace", "\u2002");
		table.put("EmSpace", "\u2003");
		return table;
	}
	
	private static Hashtable<String, String> initEncodingTable () {
		Hashtable<String, String> table = new Hashtable<String, String>();
		// Map the Java canonical charset names
		table.put("FrameRoman", FRAMEROMAN);
		table.put("JISX0208.ShiftJIS", "Shift_JIS");
		table.put("BIG5", "Big5");
		table.put("GB2312-80.EUC", "GB2312");
		table.put("KSC5601-1992", "EUC-KR");
		return table;
	}

	public MIFFilter () {
		params = new Parameters();
	}

	@Override
	public void cancel () {
		canceled = true;
	}

	@Override
	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
			hasNext = false;
			docName = null;
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public String getName () {
		return "okf_mif";
	}
	
	@Override
	public String getDisplayName () {
		return "MIF Filter (ALPHA don't even try)";
	}

	@Override
	public String getMimeType () {
		return MimeTypeMapper.MIF_MIME_TYPE;
	}

	@Override
	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
		list.add(new FilterConfiguration(getName(),
			MimeTypeMapper.MIF_MIME_TYPE,
			getClass().getName(),
			"MIF (ALPHA don't even try)",
			"Adobe FrameMaker MIF documents"));
		return list;
	}
	
	@Override
	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(MimeTypeMapper.MIF_MIME_TYPE, "net.sf.okapi.filters.mif.MIFEncoder");
		}
		return encoderManager;
	}

	@Override
	public IParameters getParameters () {
		return params;
	}

	@Override
	public boolean hasNext () {
		return hasNext;
	}

	@Override
	public void open (RawDocument input) {
		open(input, true);
	}
	
	@Override
	public void open (RawDocument input,
		boolean generateSkeleton)
	{
		close();
		if (( input.getInputURI() == null ) && ( input.getInputCharSequence() == null )) {
			// Can do this currently because of the double pass
			throw new OkapiBadFilterInputException("Direct stream input not supported for MIF.");
		}
		
		srcLang = input.getSourceLocale();
		if ( input.getInputURI() != null ) {
			docName = input.getInputURI().getPath();
		}
		
		input.setEncoding("UTF-8");
		open(input.getStream(), input);
	}
	
	
	private void initialize () {
		tagBuffer = new StringBuilder();
		strBuffer = new StringBuilder();
		paraBuf = new StringBuilder();
		tuId = 0;
		otherId = 0;
		grpId = 0;
		canceled = false;
		hasNext = true;
		inBlock = 0;
		blockLevel = 0;
		lineBreak = "\n"; //TODO: Get from input file
		tableGroupLevel = -1;
		rowGroupLevel = -1;
		cellGroupLevel = -1;
		fnoteGroupLevel = -1;
		sdId = "sd1";
		parentIds = new Stack<String>();
		parentIds.push(sdId);
		encoder = new MIFEncoder();
		decodingErrors = 0;
	}
	
	private void open (InputStream input,
		RawDocument rd)
	{
		try {
			//--- First pass: gather information
			
			// Detect encoding
			version = "";
			Object[] res = guessEncoding(input);
			reader = new BufferedReader(new InputStreamReader((InputStream)res[0], (String)res[1]));
			initialize();
			secondPass = false;
			textFlows = new ArrayList<String>();
			tables = new ArrayList<String>();
			gatherExtractionInformation();
			reader.close();
			input.close();
			
//System.out.println("Text flow to extract:");			
//for ( String s : textFlows ) {
//	System.out.println(s);
//}

			//--- Second pass: extract
			
			secondPass = true;
			input = rd.getStream();
			res = guessEncoding(input);
			baseEncoding = (String)res[1];
			reader = new BufferedReader(new InputStreamReader((InputStream)res[0], baseEncoding));
			initialize();
			
			// Compile code finder rules
			if ( params.getUseCodeFinder() ) {
				params.getCodeFinder().compile();
			}
			
			// Initialize the decoders
			decoders = new CharsetDecoder[2];
			decoders[0] = Charset.forName(baseEncoding).newDecoder();
			decoders[1] = decoders[0]; // Use the same to start
			currentCharsetIndex = 0;
			currentDecoder = decoders[currentCharsetIndex];
			encoders = new CharsetEncoder[2];
			encoders[0] = Charset.forName(baseEncoding).newEncoder();
			encoders[1] = encoders[0];
			byteStream = new ByteArrayOutputStream(20);
			
			queue = new LinkedList<Event>();
			StartDocument startDoc = new StartDocument(sdId);
			startDoc.setName(docName);
			startDoc.setLineBreak(lineBreak);
			startDoc.setEncoding(baseEncoding, false);
			// We assume no BOM in all case for MIF
			startDoc.setLocale(srcLang);
			startDoc.setFilterParameters(getParameters());
			startDoc.setFilterWriter(createFilterWriter());
			startDoc.setType(getMimeType());
			startDoc.setMimeType(getMimeType());

			queue.add(new Event(EventType.START_DOCUMENT, startDoc));
		}
		catch ( UnsupportedEncodingException e ) {
			throw new OkapiUnsupportedEncodingException("Error reading MIF input.", e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error reading MIF input.", e);
		}
	}
	
	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper fcMapper) {
		// Not used
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public Event next () {
		// Treat cancel
		if ( canceled ) {
			queue.clear();
			queue.add(new Event(EventType.CANCELED));
			hasNext = false;
		}
		// Fill the queue if it's empty
		if ( queue.isEmpty() ) {
			read();
		}
		// Update hasNext flag on the FINISHED event
		if ( queue.peek().getEventType() == EventType.END_DOCUMENT ) {
			hasNext = false;
		}
		// Return the head of the queue
		return queue.poll();
	}

	@Override
	public ISkeletonWriter createSkeletonWriter () {
		return new GenericSkeletonWriter();
	}

	@Override
	public IFilterWriter createFilterWriter () {
		return new MIFFilterWriter(createSkeletonWriter(), getEncoderManager());
	}

	/**
	 * Top-level read
	 */
	private void read () {
		try {
			skel = new GenericSkeleton();
			int c;
			
			// Check if we are still processing a TextFlow 
			if ( inBlock > 0 ) {
				processBlock(inBlock, false);
				return;
			}
			
			while ( (c = reader.read()) != -1 ) {
				switch ( c ) {
				case '#':
					skel.append((char)c);
					readComment(true, null);
					break;
					
				case '<': // Start of statement
					skel.append((char)c);
					blockLevel++;
					String tag = readTag(true, true, null);
					if ( TOPSTATEMENTSTOSKIP.indexOf(tag+";") > -1 ) {
						skipOverContent(true, null);
						blockLevel--;
					}
					else if ( "TextFlow".equals(tag) ) {
						if ( startBlock(blockLevel, BLOCKTYPE_TEXTFLOW) ) return;
					}
					else if ( "Tbls".equals(tag) ) {
						// Do nothing, but do not skip.
						// The tables will be read in Tbl tags
						tag = tag.toString();
						continue;
					}
					else if ( "Tbl".equals(tag) ) {
						if ( startBlock(blockLevel, BLOCKTYPE_TABLE) ) return;
					}
					else if ( "MIFFile".equals(tag) ) {
						MIFToken token = getNextTokenInStatement(true, null);
						if ( token.isLast() ) blockLevel--;
						if ( token.getType() == MIFToken.TYPE_STRING ) {
							//version = token.getString();
						}
						else {
							throw new OkapiIOException("MIF version not found.");
						}
					}
					else {
						// Default: skip over
						skipOverContent(true, null);
						blockLevel--;
					}
					// Flush the skeleton from time to time to allow very large files
					queue.add(new Event(EventType.DOCUMENT_PART,
						new DocumentPart(String.valueOf(++otherId), false),
						skel));
					return;
					
				case '>': // End of statement
					skel.append((char)c);
					blockLevel--;
					// Return skeleton
					DocumentPart dp = new DocumentPart(String.valueOf(++otherId), false, skel); 
					queue.add(new Event(EventType.DOCUMENT_PART, dp));
					return;
					
				default:
					skel.append((char)c);
					break;
				}
			}

			// We are done
			Ending ending = new Ending(String.valueOf(++otherId)); 
			queue.add(new Event(EventType.END_DOCUMENT, ending, skel));
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	/**
	 * Gather the information about what to extract.
	 * This method create a list of all text flow and table to extract, based
	 * on the filter's options.
	 */
	private void gatherExtractionInformation () {
		try {
			int c;
			MIFToken token;
			boolean inEscape = false;
			boolean inString = false;
			ArrayList<String> trToExtract = new ArrayList<String>();
			ArrayList<String> tblIds = new ArrayList<String>();
			boolean hasPages = false; // Simple MIF may not have any page

			while ( true ) {
				
				int res = -1;
				while ( res == -1 ) {
					c = reader.read();
					// Parse a string content
					if ( inString ) {
						if ( c == '\'' ) inString = false;
						continue;
					}
					// Else: we are outside a string
					if ( inEscape ) {
						inEscape = false;
					}
					else {
						switch ( c ) {
						case -1:
							if ( !hasPages ) {
								textFlows = null;
								tables = null;
							}
							return; // No more data
						case '`':
							inString = true;
							break;
						case '\\':
							inEscape = true;
							break;
						case '<':
							blockLevel++;
							res = 1;
							break;
						case '>':
							blockLevel--;
							res = 0;
							break;
						}
					}
				}
				// Res can be 0 or 1 here
				
				if ( res == 1 ) {
					// We reached an opening <
					// Get the tag name
					String tag = readTag(false, false, null);
					if ( tag.equals("Page") ) {
						// If it's a Page: get the first TextRect id and the type
						String pageType = null;
						String textRectId = null;
						hasPages = true;
						
						while ( true ) {
							tag = readUntil("PageType;TextRect;", false, blockLevel);
							if ( tag == null ) {
								// One of PageType or TextRect was not in the page
								break;
							}
							// Else it's a PageType or a TextRect
							if ( tag.equals("PageType") ) {
								token = getNextTokenInStatement(false, null);
								if ( token.isLast() ) blockLevel--;
								if ( token.getType() == MIFToken.TYPE_STRING ) {
									pageType = token.getString();
								}
								else {
									// Error: Missing page type value.
									throw new OkapiIOException("Missing PageType value.");
								}
								if ( textRectId != null ) break;
							}
							else if ( tag.equals("TextRect") ) {
								while ( true ) {
									tag = readUntil("ID", false, blockLevel);
									if ( tag != null ) {
										// Found
										token = getNextTokenInStatement(false, null);
										if ( token.isLast() ) blockLevel--;
										if ( token.getType() == MIFToken.TYPE_STRING ) {
											textRectId = token.getString();
										}
										else {
											// Error: Missing ID value
											throw new OkapiIOException("Missing ID value.");
										}
									}
									else {
										// Error ID not found
										throw new OkapiIOException("ID statement not found.");
									}
									break;
								}
								if ( pageType != null ) break;
							}
						
						} // End of while
						
						// We have looked at the page data
						if ( !Util.isEmpty(pageType) && !Util.isEmpty(textRectId) ) {
							if ( pageType.equals("BodyPage") ) {
								if ( params.getExtractBodyPages() ) {
									trToExtract.add(textRectId);
								}
							}
							else if ( pageType.equals("ReferencePage") ) {
								if ( params.getExtractReferencePages() ) {
									trToExtract.add(textRectId);
								}
							}
							else if ( pageType.equals("HiddenPage") ) {
								if ( params.getExtractHiddenPages() ) {
									trToExtract.add(textRectId);
								}
							}
							else if ( pageType.endsWith("MasterPage") ) {
								if ( params.getExtractMasterPages() ) {
									trToExtract.add(textRectId);
								}
							}
							else {
								// Else: unexpected type of page: extract it just in case
								trToExtract.add(textRectId);
								logger.warning(String.format("Unknown page type '%s' (It will be extracted)", pageType));
							}
						}

					}
					else if ( tag.equals("TextFlow") ) {
						// Check which text flows have table reference,
						// So we know which one to extract during the second pass
						String textRectId = null;
						String unique = null;
						boolean textRectDone = false;
						
						// Next harvest the Para groups
						// to get the first TextRectID and all ATbl in the ParaLine entries
						int tfLevel = blockLevel;
						while ( true ) {
							
							if ( readUntil("Para", false, tfLevel) == null ) {
								break; // Done
							}
							tblIds.clear(); // Hold all table references for this paragraph

							// Get the unique id for the text flow: the unique id of the first Para
							tag = readUntil("Unique", false, blockLevel);
							if ( tag == null ) {
								// Error: Unique ID missing
								throw new OkapiIOException("Missing unique id for the text flow.");
							}
							token = getNextTokenInStatement(false, null);
							if ( token.isLast() ) blockLevel--;
							if ( token.getType() != MIFToken.TYPE_STRING ) {
								throw new OkapiIOException("Missing unique id value for the text flow.");
							}
							unique = token.getString();

							// Inside a Para:
							while ( true ) {
								if ( readUntil("ParaLine", false, blockLevel) == null ) {
									break; // Done for this Para
								}
								// Else: inside a ParaLine
								while ( true ) {
									tag = readUntil("TextRectID;ATbl", false, blockLevel);
									if ( tag == null ) {
										break; // Done
									}
									if ( !textRectDone && tag.equals("TextRectID") ) {
										token = getNextTokenInStatement(false, null);
										if ( token.isLast() ) blockLevel--;
										if ( token.getType() == MIFToken.TYPE_STRING ) {
											textRectId = token.getString();
											// A FNote may occur before the Para that holds the key TextRect id
											// so we don't count them if we are inside a FNote group
											if ( fnoteGroupLevel == -1 ) {
												textRectDone = true;
											}
										}
									}
									else if ( tag.equals("ATbl") ) {
										token = getNextTokenInStatement(false, null);
										if ( token.isLast() ) blockLevel--;
										if ( token.getType() == MIFToken.TYPE_STRING ) {
											tblIds.add(token.getString());
										}
									}
								}
							}
							
							// Check the TextRect id against the ones found for the pages
							if ( trToExtract.contains(textRectId) ) {
								// This text flow is to be extracted
								// and so are any table referenced in it
								textFlows.add(unique);
								tables.addAll(tblIds);
							}
						}
						
					}
					else if ( tag.equals(IMPORTOBJECT) ) {
						skipOverImportObject(false, null);
						blockLevel--;
					}
					else if ( "MIFFile".equals(tag) ) {
						token = getNextTokenInStatement(false, null);
						if ( token.isLast() ) blockLevel--;
						if ( token.getType() == MIFToken.TYPE_STRING ) {
							version = token.getString();
						}
						else {
							throw new OkapiIOException("MIF version not found.");
						}
					}

				}
				// Else: Ending of statement. Nothing to do
			}
			
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error while gathering extraction information.", e);
		}
		finally {
			
		}
	}
	
	/**
	 * Skips over the content of the current statement.
	 * Normally "<token" has been processed and level for after '<'
	 * @param store true to store in the skeleton
	 * @param buffer the StringBuilder object where to copy the content, or null to not copy.
	 * @throws IOException if an error occurs.
	 */
	private void skipOverContent (boolean store,
		StringBuilder buffer)
		throws IOException
	{
		int baseLevel = 1;
		int state = 0;
		int c;
		
		while ( (c = reader.read()) != -1 ) {
			// Store if needed
			if ( store ) {
				if ( buffer != null ) buffer.append((char)c);
				else skel.append((char)c);
			}
			
			// Parse according current state
			switch ( state ) {
			case 0:
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '\\':
					state = 2; // In escape
					continue;
				case '<':
					baseLevel++;
					tagBuffer.setLength(0);
					state = 3; // In tag buffer mode
					continue;
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						return;
					}
					continue;
				}
				// Else do nothing
				continue;
				
			case 1: // In string
				if ( c == '\'' ) state = 0;
				continue;
				
			case 2: // In escape
				state = 0; // Go back to normal 
				continue;
				
			case 3: // In tag buffer mode
				switch ( c ) {
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						return;
					}
					// Fall thru
				case ' ':
				case '\t':
					if ( tagBuffer.toString().equals(IMPORTOBJECT) ) {
						skipOverImportObject(store, buffer);
						baseLevel--;
					}
					state = 0;
					continue;
				default:
					tagBuffer.append((char)c);
					continue;
				}
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException(
			String.format("Unexpected end of input at state = %d", state));
	}
	
	private void readComment (boolean store,
		StringBuilder sb)
		throws IOException
	{
		int c;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				if ( sb != null ) sb.append((char)c);
				else skel.append((char)c);
			}
			switch ( c ) {
			case '\r':
			case '\n':
				return;
			}
		}
		// A comment can end the file
		// Actually that's the case most of the time
	}

	private boolean startBlock (int stopLevel,
		int type)
		throws IOException
	{
		if ( type == BLOCKTYPE_TABLE ) {
			// Get the table id
			String tag = readUntil("TblID", true, stopLevel);
			if ( tag == null ) {
				// Error: Unique ID missing
				throw new OkapiIOException("Missing id for the table.");
			}
			MIFToken token = getNextTokenInStatement(true, null);
			if ( token.getType() != MIFToken.TYPE_STRING ) {
				throw new OkapiIOException("Missing id value for the table.");
			}
			// If the table is not listed as to be extracted: we skip it
			if (( tables != null ) && !tables.contains(token.getString()) ) {
				skipOverContent(true, null);
				blockLevel--;
				return false;
			}
			// If tables==null it's because we didn't have any page, so we extract by default
			// Else: extract: use fall thru code
		}
		else if ( type == BLOCKTYPE_TEXTFLOW ) {
			// Get the unique id
			String tag = readUntil("Unique", true, stopLevel);
			if ( tag == null ) {
				// Error: Unique ID missing
				throw new OkapiIOException("Missing unique id for the table.");
			}
			MIFToken token = getNextTokenInStatement(true, null);
			if ( token.getType() != MIFToken.TYPE_STRING ) {
				throw new OkapiIOException("Missing unique id value for the text flow.");
			}
			// If the table is not listed as to be extracted: we skip it
			if (( textFlows != null ) && !textFlows.contains(token.getString()) ) {
				skipOverContent(true, null);
				blockLevel--;
				return false;
			}
			// If textFlows==null it's because we didn't have any page, so we extract by default
			// Else: extract: use fall thru code
		}
		
		// Extract
		processBlock(stopLevel, (type==BLOCKTYPE_TEXTFLOW));
		return true;
	}
	
	/**
	 * Process the first or next entry of a TextFlow statement.
	 * @throws IOException if a low-level error occurs.
	 */
	private void processBlock (int stopLevel,
		boolean inPara)
		throws IOException
	{
		// Process one Para statement at a time
		if ( inPara ) {
			inBlock = stopLevel; // We are not done yet with this TextFlow statement
			processPara();
			blockLevel--; // Closing the Para statement here
		}
		else {
			if ( readUntil("Para", true, stopLevel) != null ) {
				inBlock = stopLevel; // We are not done yet with this TextFlow statement
				processPara();
				blockLevel--; // Closing the Para statement here
			}
			else { // Done
				inBlock = 0; // We are done
			}
		}
		
		// If needed, create a document part and return
		if ( !skel.isEmpty() ) {
			queue.add(new Event(EventType.DOCUMENT_PART,
				new DocumentPart(String.valueOf(++otherId), false),
				skel));
		}
	}

	private void processPara ()
		throws IOException
	{
		TextFragment tf = new TextFragment();
		boolean first = true;
		paraLevel = 1;
		paraBuf.setLength(0);
		paraBufNeeded = false;
		String endString = null;
		resetToDefaultDecoder();

		// Go to the first ParaLine
		int res = readUntilText(paraBuf, false);
		while ( res > 0 ) {
			
			// Get the text to append
			paraLevel--; // We close String or Char outside of readUntilText() 
			String text = null;
			switch ( res ) {
			case 1: // String
				text = processString(false);
				paraBuf.append("`");
				break;
			case 2: // Char
				text = processChar(false).toString();
				break;
			}
			
			if ( Util.isEmpty(text) ) {
				// Nothing to do, keep on reading
				if ( paraBuf.length() > 0 ) {
					if ( res == 1 ) {
						// We have inline plus an empty string ("<Dummy 1><String`'>")
						// We remove the empty string "<String `"
						paraBuf.delete(paraBuf.length()-9, paraBuf.length());
					}
				}
			}
			else { // We have text
				if ( first ) { // First text in the fragment: put the codes in the skeleton
					first = false;
					skel.append(paraBuf.toString());
					if ( res == 2 ) skel.append("<String `");
					endString = "'>";
				}
				else { // Put the codes in an inline code 
					if ( paraBufNeeded && ( paraBuf.length() > 0 )) {
						if ( res != 1 ) {
							paraBuf.append("<String `");
						}
						tf.append(TagType.PLACEHOLDER, "x", endString + paraBuf.toString());
					}
				}
				// Add the text
				tf.append(text);
				// Reset the codes buffer for next sequence
				paraBuf.setLength(0);
				paraBufNeeded = false;
			}
			
			// Move to the next text
			res = readUntilText(paraBuf, true);
		}

		// Check for inline codes
		if ( params.getUseCodeFinder() ) {
			params.getCodeFinder().process(tf);
		}

		TextUnit tu = null;
		if ( !tf.isEmpty() ) {
			if ( tf.hasText() ) {
				// Add the text unit to the queue
				tu = new TextUnit(String.valueOf(++tuId));
				tu.setPreserveWhitespaces(true);
				tu.setSourceContent(tf);
				queue.add(new Event(EventType.TEXT_UNIT, tu, skel));
				skel.addContentPlaceholder(tu);
			}
			else { // No text (only codes and/or white spaces) Put back the content/codes in skeleton
				// We need to escape the text parts (white spaces like tabs)
				String ctext = tf.getCodedText();
				StringBuilder tmp = new StringBuilder();
				for ( int i=0; i<ctext.length(); i++ ) {
					char ch = ctext.charAt(i);
					if ( TextFragment.isMarker(ch) ) {
						tmp.append(tf.getCode(ctext.charAt(++i)));
					}
					else {
						tmp.append(encoder.encode(ch, 1));
					}
				}
				GenericSkeletonPart part = skel.getLastPart();
				if (( part == null ) || !part.getData().toString().endsWith("<String `") ) {
					skel.append("<String `");
					endString = "'>";
				}
				skel.append(tmp.toString());
			}
		}
		
		if ( endString == null ) {
			skel.append(paraBuf.toString());
		}
		else {
			skel.append(endString + paraBuf.toString());
		}

		if ( tu != null ) {
			// New skeleton object for the next parts of the parent statement
			skel = new GenericSkeleton();
		}
	}
	
	private MIFToken getNextTokenInStatement (boolean store,
		StringBuilder sb)
		throws IOException
	{
		int n;
		boolean leadingWSDone = false;
		do {
			n = reader.read();
			if ( store ) {
				if ( sb != null ) sb.append((char)n);
				else skel.add((char)n);
			}
			switch ( n ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				break;
			case -1:
				throw new OkapiIllegalFilterOperationException("Unexpected end of input.");
			default:
				leadingWSDone = true;
				break;
			}
		}
		while ( !leadingWSDone );
		
		StringBuilder tmp = new StringBuilder();
		tmp.append((char)n);
		do {
			n = reader.read();
			if ( store ) {
				if ( sb != null ) sb.append((char)n);
				else skel.add((char)n);
			}
			switch ( n ) {
//TODO: what if the token is a string? 			
			case ' ':
			case '\t':
			case '\r':
			case '\n':
			case '>': // End of statement
				MIFToken token = new MIFToken(tmp.toString());
				token.setLast(n == '>');
				return token;
			case -1:
				throw new OkapiIllegalFilterOperationException("Unexpected end of input.");
			default:
				tmp.append((char)n);
				break;
			}
		}
		while ( true );
	}
	
	private MIFToken processChar (boolean store)
		throws IOException
	{
		// Get the next token: the name of the character
		MIFToken token = getNextTokenInStatement(store, null);
		if ( !token.isLast() ) {
			skipOverContent(store, null);
		}
		
		// Default return is also a token
		MIFToken chToken = new MIFToken();
		// Map the character to its string, if possible
		if ( token.getType() == MIFToken.TYPE_STRING ) {
			String str = charTable.get(token.getString());
			if ( str == null ) {
				logger.warning(String.format("Unknow character name '%s'. This character will be ignored.", token));
			}
			else {
				chToken.setString(str); 
			}
		}
		else {
			// Invalid statement
			logger.warning("Unexpected token is Char statement. This character will be ignored.");
		}
		return chToken;
	}
	
	private int readUntilText (StringBuilder sb,
		boolean checkIfParaBufNeeded)
		throws IOException
	{
		int c;
		while ( (c = reader.read()) != -1 ) {
			switch ( c ) {
			case '#':
				sb.append((char)c);
				readComment(true, sb);
				break;
				
			case '<': // Start of statement
				paraLevel++;
				sb.append((char)c);
				String tag = readTag(true, false, sb);
				if ( "ParaLine".equals(tag) ) {
					return readUntilText(sb, checkIfParaBufNeeded);
				}
				// Cases for inside ParaLine
				else if ( "String".equals(tag) ) {
					return 1;
				}
				else if ( "Char".equals(tag) ) {
					return 2;
				}
				else if ( !useUTF && "Font".equals(tag) ) {
					// Do the font-driven encoding resolving only for non-UTF-8 files
					//TODO: Is it safe? need to be verified
					if ( checkIfParaBufNeeded ) paraBufNeeded = true;
					readFont(sb); //skipOverContent(true, sb);
					paraLevel--;
				}
				// Default: skip over
				else {
					if ( checkIfParaBufNeeded ) paraBufNeeded = true;
					skipOverContent(true, sb);
					paraLevel--;
				}
				break;
				
			case '>': // End of statement
				paraLevel--;
				sb.append((char)c);
				if ( paraLevel == 0 ) {
					return 0;
				}
				break;

			default:
				sb.append((char)c);
				break;
			}
		}
		return 0;
	}

	private void readFont (StringBuilder sb)
		throws IOException
	{
		int c;
		int baseLevel = 1;
		
		String encoding = null;
		String fontHint = null;
		String ftag = null;
		boolean inString = false;
		MIFToken token;
		
		while ( (c = reader.read()) != -1 ) {
			sb.append((char)c);
			// Handle string content
			if ( inString ) {
				if ( c == '\'' ) inString = false;
				continue;
			}
			// Otherwise:
			switch ( c ) {
			case '#':
				readComment(true, sb);
				break;
				
			case '`':
				inString = true;
				break;
				
			case '<':
				baseLevel++;
				String tag = readTag(true, true, sb);
				if ( "FTag".equals(tag) ) {
					token = getNextTokenInStatement(true, sb);
					if ( token.isLast() ) baseLevel--;
					ftag = token.toString().substring(1, token.toString().length()-1);
				}
				if ( "FEncoding".equals(tag) ) {
					token = getNextTokenInStatement(true, sb);
					if ( token.isLast() ) baseLevel--;
					encoding = token.toString().substring(1, token.toString().length()-1);
				}
				else if ( "FPlatformName".equals(tag) ) {
					token = getNextTokenInStatement(true, sb);
					if ( token.isLast() ) baseLevel--;
					fontHint = token.toString().substring(1, token.toString().length()-1);
				}
				// TODO: use Flanguage as well?
				// FLanguage
				break;
				
			case '>':
				baseLevel--;
				if ( baseLevel == 0 ) {
					updateCurrentDecoder(ftag, encoding, fontHint);
					return;
				}
				break;
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException(
			"Unexpected end of input when reading a font");
	}
	
	private void resetToDefaultDecoder () {
		currentCharsetIndex = 0;
		currentDecoder = decoders[0];
	}
	
	private void updateCurrentDecoder (String ftag,
		String newEncoding,
		String newHint)
	{
		if ( newEncoding == null ) {
			// No encoding definition
			if ( Util.isEmpty(ftag) ) {
				// No FTag and no encoding: likely a default for the encoding
				resetToDefaultDecoder();
			}
			else {
				//TODO: get the encoding info from the font of this paragraph style
			}
		}
		else {
			// Map the MIF encoding name to Java canonical charset name
			String mappedEncoding = encodingTable.get(newEncoding);
			if ( mappedEncoding == null ) {
				// Warn if the name is not found (and just move on)
				logger.warning(String.format("Unknown encoding name: '%s'.", mappedEncoding));
				return;
			}
			
			// Special case for FrameRoman: it may be anything depending on the font
			if ( mappedEncoding.equals(FRAMEROMAN) && !Util.isEmpty(newHint) ) {
				// Try to guess the real encoding from the platform font name
//TODO: the strings should be in parameters so they can be adapted
				newHint = newHint.toLowerCase();
				if ( newHint.contains("greek") ) {
					newEncoding = "x-MacGreek";
				}
				else if ( newHint.contains("cyrillic") ) {
					newEncoding = "x-MacCyrillic";
				}
				else if ( newHint.contains(" ce ") ) {
					newEncoding = "x-MacCentralEurope";
				}
			}
			
			// Check if the new encoding is the same as the current one
			if ( !mappedEncoding.equals(currentDecoder.charset().name()) ) {
				// Test the other one
				int n = ((currentCharsetIndex == 0) ? 1 : 0);
				if ( mappedEncoding.equals(decoders[n].charset().name()) ) {
					// Use this one
					currentCharsetIndex = n;
				}
				else { // Create a new one (keep the number 0 always the same)
					decoders[1] = Charset.forName(mappedEncoding).newDecoder();
					encoders[1] = Charset.forName(mappedEncoding).newEncoder();
					currentCharsetIndex = 1;
				}
				// Set the current one
				currentDecoder = decoders[currentCharsetIndex];
			}
			// Else: current decoder in fine
		}
	}
	
	/**
	 * Reads until the first occurrence of one of the given statements, or (if stopLevel
	 * is -1) at the end of the current level, or at the end of the given level.
	 * @param tagNames the list of tag names to stop at.
	 * @param store true if we store the parsed characters into the skeleton.
	 * @param stopLevel -1=return if the end of the current blockLevel is reached.
	 * other values=return if the blockLevel get lower than that value
	 * False to stop when it reaches 0.
	 * @return the name of the tag found, or null if none was found.
	 * @throws IOException if a low-level error occurs.
	 */
	private String readUntil (String tagNames,
		boolean store,
		int stopLevel)
		throws IOException
	{
		int endNow = stopLevel;
		if ( stopLevel == -1 ) {
			endNow = blockLevel;
		}
		
		int c;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				skel.append((char)c);
			}
			switch ( c ) {
			case '#':
				readComment(store, null);
				break;

			case '<': // Start of statement
				while ( true ) {
					blockLevel++;
					String tag = readTag(store, true, null);
					if ( tagNames.indexOf(tag) > -1 ) {
						return tag;
					}
					else if ( "Tbl".equals(tag) ) {
						tableGroupLevel = blockLevel;
						if ( secondPass ) {
							StartGroup sg = new StartGroup(parentIds.peek());
							sg.setId(parentIds.push(String.valueOf(++grpId)));
							sg.setType("table");
							queue.add(new Event(EventType.START_GROUP, sg));
						}
					}
					else if ( "Row".equals(tag) ) {
						rowGroupLevel = blockLevel;
						if ( secondPass ) {
							StartGroup sg = new StartGroup(parentIds.peek());
							sg.setId(parentIds.push(String.valueOf(++grpId)));
							sg.setType("row");
							queue.add(new Event(EventType.START_GROUP, sg));
						}
					}
					else if ( "Cell".equals(tag) ) {
						cellGroupLevel = blockLevel;
						if ( secondPass ) {
							StartGroup sg = new StartGroup(parentIds.peek(), String.valueOf(++grpId));
							sg.setType("cell");
							queue.add(new Event(EventType.START_GROUP, sg));
						}
					}
					else if ( "FNote".equals(tag) ) {
						fnoteGroupLevel = blockLevel;
						if ( secondPass ) {
							StartGroup sg = new StartGroup(parentIds.peek(), String.valueOf(++grpId));
							sg.setType("fn");
							queue.add(new Event(EventType.START_GROUP, sg));
						}
					}
					else if ( IMPORTOBJECT.equals(tag) ) {
						skipOverImportObject(store, null);
						blockLevel--;
					}
					else { // Default: skip over
						if ( !readUntilOpenOrClose(store) ) {
							blockLevel--;
							break;
						}
						// Else: re-process the next tag
					}
				}
				break;
				
			case '>': // End of statement
				if ( tableGroupLevel == blockLevel ) {
					tableGroupLevel = -1;
					if ( secondPass ) {
						queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
						parentIds.pop();
					}
				}
				else if ( rowGroupLevel == blockLevel ) {
					rowGroupLevel = -1;
					if ( secondPass ) {
						queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
						parentIds.pop();
					}
				}
				else if ( cellGroupLevel == blockLevel ) {
					cellGroupLevel = -1;
					if ( secondPass ) {
						queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
					}
				}
				else if ( fnoteGroupLevel == blockLevel ) {
					fnoteGroupLevel = -1;
					if ( secondPass ) {
						queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
					}
				}
				blockLevel--;
				if ( blockLevel < endNow ) {
					return null;
				}
				break;
			}
		}
		//TODO: we shouldn't exit this way, except when starting at 0
		return null;
	}
	
	private void skipOverImportObject (boolean store,
		StringBuilder buffer)
		throws IOException
	{
		// At the point only the tag has been read
		// We should leave after the corresponding '>' is found
		// The content may have one or more inset data (start with line-break and '&' per line)
		int state = 0;
		int c;
		int baseLevel = 1;
		
		while ( (c = reader.read()) != -1 ) {
			// Store if needed
			if ( store ) {
				if ( buffer != null ) buffer.append((char)c);
				else skel.append((char)c);
			}
			
			// Parse according current state
			switch ( state ) {
			case 0: // In facet mode wait for line-break
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '<':
					baseLevel++;
					continue;
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						// We are done
						return;
					}
				case '\r':
				case '\n':
					state = 3;
					continue;
				}
				// Else: stay in this current state
				continue;
				
			case 1: // In string
				if ( c == '\'' ) {
					state = 0; // Back to normal
				}
				continue;
				
			case 2: // In escape
				state = 0; // Back to normal
				continue;
				
			case 3: // After \r or \r: wait for & or =
				switch ( c ) {
				case '&':
					state = 4; // In facet line
					continue;
				case '<':
					state = 0;
					baseLevel++;
					continue;
				case '>':
					state = 0;
					baseLevel--;
					if ( baseLevel == 0 ) {
						return; // Done
					}
					continue;
					
				case '\n':
				case '\r':
					// Stay in this current state
					continue;
				default:
					// Back to within an ImportObject (after a line-break)
					state = 0;
					continue;
				}
			
			case 4: // Inside a facet line, waiting for end-of-line
				if (( c == '\r' ) || ( c == '\n' )) {
					state = 3; // Back to after a line-break state
				}
				continue;
			
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException(
			String.format("Unexpected end of input at state = %d", state));
	}
	
	/**
	 * Reads until the next opening or closing statement. 
	 * @param store
	 * @return true if stops on opening, false if stops on closing.
	 * @throws IOException if the end of file occurs.
	 */
	private boolean readUntilOpenOrClose (boolean store)
		throws IOException
	{
		int c;
		boolean inEscape = false;
		boolean inString = false;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				skel.append((char)c);
			}
			// Parse a string content
			if ( inString ) {
				if ( c == '\'' ) inString = false;
				continue;
			}
			// Else: we are outside a string
			if ( inEscape ) {
				inEscape = false;
			}
			else {
				switch ( c ) {
				case '`':
					inString = true;
					break;
				case '\\':
					inEscape = true;
					break;
				case '<':
					return true;
				case '>':
					return false;
				}
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException("Unexpected end of input.");
	}
	
	/**
	 * Reads a tag name.
	 * @param store true to store the tag codes
	 * @param storeCharStatement true to store if it's a Char statement.
	 * @param sb Not null to store there, null to store in the skeleton.
	 * @return The name of the tag.
	 * @throws IOException
	 */
	private String readTag (boolean store,
		boolean storeCharStatement,
		StringBuilder sb)
		throws IOException
	{
		tagBuffer.setLength(0);
		int c;
		int wsStart = ((sb != null ) ? sb.length()-1 : -1);
		boolean leadingWSDone = false;
		// Skip and whitespace between '<' and the name
		do {
			switch ( c = reader.read() ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				if ( store ) {
					if ( sb != null ) sb.append((char)c);
					else skel.add((char)c);
				}
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
				if ( store ) {
					if ( !storeCharStatement && tagBuffer.toString().equals("Char") ) {
						// Special case for <Char...>: we don't store it
						if ( wsStart > 0 ) {
							sb.delete(wsStart, sb.length());
						}
					}
					else {
						if ( sb != null ) {
							sb.append(tagBuffer.toString());
							sb.append((char)c);
						}
						else {
							skel.append(tagBuffer.toString());
							skel.append((char)c);
						}
					}
				}
				return tagBuffer.toString();
				
			case -1:
				throw new OkapiIllegalFilterOperationException("Unexpected end of input.");
				
			default:
				tagBuffer.append((char)c);
				break;
			}
			c = reader.read();
		}
	}
	
	private String processString (boolean store)
		throws IOException
	{
		strBuffer.setLength(0);
		int c;
		int state = 0;
		boolean byteMode = false;
		
		while ( (c = reader.read()) != -1 ) {
			
			if ( store ) skel.append((char)c);
			
			switch ( state ) {
			case 0: // Outside the string
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '>':
					if ( byteMode ) {
						try {
							CharBuffer buf = currentDecoder.decode(ByteBuffer.wrap(byteStream.toByteArray()));
							strBuffer.append(buf.toString());
						}
						catch ( CharacterCodingException e ) {
							if ( ++decodingErrors < 25 ) {
								// Warning message, but only up to a point
								logger.warning(String.format("Error with decoding character \\x%2x (%d) with encoding '%s'.",
									c, c, currentDecoder.charset().name()));
							}
						}
					}
					return strBuffer.toString();
				}
				continue;
			
			case 1: // In string
				switch ( c ) {
				case '\'': // End of string
					state = 0;
					continue;
				case '\\':
					state = 2; // In escape mode
					continue;
				default:
					if ( byteMode ) {
						if ( c > 127 ) {
							// Extended characters are normally all escaped in a byte string
							logger.warning(String.format("A raw extended character (0x%04X) was found in a byte string.\n"
								+ "This may be a problem.", c));
						}
						byteStream.write(c);
					}
					else strBuffer.append((char)c);
					continue;
				}
				
			case 2: // In escape mode (after a backslash)
				state = 1; // Reset to in-string state
				switch ( c ) {
				case '\\':
				case '>':
					if ( byteMode ) byteStream.write(c);
					else strBuffer.append((char)c);
					continue;
				case 't':
					if ( byteMode ) byteStream.write(c);
					else strBuffer.append('\t');
					continue;
				case 'Q':
					if ( byteMode ) byteStream.write(c);
					else strBuffer.append('`');
					continue;
				case 'q':
					if ( byteMode ) byteStream.write(c);
					else strBuffer.append('\'');
					continue;
				case 'u':
					c = readHexa(4, false);
					if ( c == Integer.MAX_VALUE ) {
						continue; // warning already logged
					}
					if ( byteMode ) {
						// We should not see this in byte mode
						logger.warning("A Uniocde escape sequence was found in a byte string.\n"
							+ "Mixed notations are not supported, this character will be skipped.");
					}
					else {
						strBuffer.append((char)c);
					}
					continue;
				case 'x':
					c = readHexa(2, true);
					if ( c == Integer.MAX_VALUE ) {
						continue; // warning already logged
					}
					if ( !byteMode ) {
						byteStream.reset();
						byteMode = true;
					}
					byteStream.write(c);
					continue;
				}				
			}
			
		}
		// Else: Missing end of string error
		throw new OkapiIllegalFilterOperationException("End of string is missing.");
	}

	private int readHexa (int length,
		boolean readExtraSpace)
		throws IOException
	{
		tagBuffer.setLength(0);

		// Fill the buffer
		for ( int i=0; i<length; i++ ) {
			int c = reader.read();
			if ( c == -1 ) {
				throw new OkapiIllegalFilterOperationException("Unexpected end of file.");
			}
			tagBuffer.append((char)c);
		}
		if ( readExtraSpace ) {
			reader.read();
		}
		
		// Try to convert
		try {
			int n = Integer.valueOf(tagBuffer.toString(), 16);
			return n;
		}
		catch ( NumberFormatException e ) {
			// Log warning
			logger.warning(String.format("Invalid escape sequence found: '%s'", tagBuffer.toString()));
		}
		
		// Error
		return Integer.MAX_VALUE;
	}
	
	private Object[] guessEncoding (InputStream input)
		throws IOException
	{
		Object[] res = new Object[2];
		String defEncoding = "UTF-8"; // Use v8/v9 default by default
		if ( !Util.isEmpty(version) ) {
			// If we have a version, it means we are in the second pass and need to get the correct encoding
			if ( version.compareTo("8.00") < 0 ) {
				// Before 8.00 the default was not UTF-8
				defEncoding = FRAMEROMAN;
			}
		}
		
		// Detect any BOM-type encoded (and set the stream to skip over it)
		BOMAwareInputStream bais = new BOMAwareInputStream(input, defEncoding);
		res[0] = bais;
		res[1] = bais.detectEncoding();
		useUTF = (((String)res[1]).startsWith("UTF-"));
		if ( bais.autoDtected() ) {
			return res;
		}
			
//		// Else: try detect based on MIF weird mechanism
//		//TODO
//
//		// Try to match the file signature with the pre-defined signatures
//		String signature = "";
//			
//		if ( signature.equals("\u201C\u00FA\u2013\u007B\u0152\u00EA") ) {
//			// "Japanese" in Shift-JIS seen in Windows-1252 (coded in Unicode)
//			res[1] = "Shift_JIS";
//			return res;
//		}
//		else if ( signature.equals("\u00C6\u00FC\u00CB\u00DC\u00B8\u00EC") ) {
//			res[1] = "EUC-JP";
//			// "Japanese" in EUC seen in Windows-1252 (coded in Unicode)
//			return res;
//		}
//
//		// Default
//		res[1] = DEFENCODING;
		return res;
	}

}
