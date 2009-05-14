/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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
package net.sf.okapi.filters.openxml;

//import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;
//import java.util.Iterator;
import java.util.List;
import java.util.Set;
//import java.util.TreeMap; // DWH 10-10-08
import java.util.logging.Level;
import java.util.logging.Logger;

import net.htmlparser.jericho.EndTag;
//import net.htmlparser.jericho.EndTagType;
import net.htmlparser.jericho.Segment;
//import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;

//import net.sf.okapi.common.encoder.IEncoder;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiIllegalFilterOperationException;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder;
import net.sf.okapi.filters.markupfilter.AbstractBaseMarkupFilter;
import net.sf.okapi.filters.markupfilter.Parameters;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

/**
 * <p>Filters Microsoft Office Word, Excel, and Powerpoint Documents.
 * OpenXML is the format of these documents.
 * 
 * <p>Since OpenXML files are Zip files that contain XML documents,
 * <b>OpenXMLFilter</b> handles opening and processing the zip file, and
 * instantiates this filter to process the XML documents.
 * 
 * <p>This filter extends AbstractBaseMarkupFilter, which extends
 * AbstractBaseFilter.  It uses the Jericho parser to analyze the
 * XML files.
 * 
 * <p>The filter exhibits slightly differnt behavior depending on whether
 * the XML file is Word, Excel, Powerpoint, or a chart in Word.  The
 * tags in these files are configured in yaml configuration files that
 * specify the behavior of the tags.  These configuration files are 
 * <p><li>wordConfiguration.yml
 * <li>excelConfiguration.yml
 * <li>powerpointConfiguration.yml
 * <li>wordChartConfiguration.yml
 * 
 * In Word and Powerpoint, text is always surrounded by paragraph tags
 * <w:p> or <a:p>, which signal the beginning and end of the text unit
 * for this filter, and are marked as TEXT_UNIT_ELEMENTs in the configuration
 * files.  Inside these are one or more text runs surrounded by <w:r> or <a:r>
 * tags and marked as TEXT_RUN_ELEMENTS by the configuration files.  The text
 * itself occurs between text marker tags <w:t> or <a:t> tags, which are 
 * designated TEXT_MARKER_ELEMENTS by the configuration files.  Tags between 
 * and including <w:r> and <w:t> (which usually include a <w:rPr> tag sequence 
 * for character style) are consolidated into a single MARKER_OPENING code.  Tags
 * between and including </w:t> and </w:r>, which sometimes include graphics
 * tags, are consolidated into a single MARKER_CLOSING code.  If there is no
 * text between <w:r> and </w:r>, a single MARKER_PLACEHOLDER code is created
 * for the text run.  If there is no character style information, 
 * <w:r><w:t>text</w:t></w:r> is not surrounded by MARKER_OPENING or 
 * MARKER_CLOSING codes, to simplify things for translators; these are supplied
 * by OpenXMLContentSkeletonWriter during output.  The same is true for text
 * runs marked by <a:r> and <a:t> in Powerpoint files.
 * 
 * Excel files are simpler, and only mark text by <v>, <t>, and <text> tags
 * in worksheet, sharedString, and comment files respectively.  These tags
 * work like TEXT_UNIT, TEXT_RUN, and TEXT_MARKER elements combined.
 */

public class OpenXMLContentFilter extends AbstractBaseMarkupFilter {
	
	private Logger LOGGER=null;
	
	public final static int MSWORD=1;
	public final static int MSEXCEL=2;
	public final static int MSPOWERPOINT=3;
	public final static int MSWORDCHART=4; // DWH 4-16-09
	public final static int MSEXCELCOMMENT=5; // DWH 5-13-09

	private int configurationType;
	private Package p=null;
	private int filetype=MSWORD; // DWH 4-13-09
	private String sConfigFileName; // DWH 10-15-08
	private URL urlConfig; // DWH 3-9-09
	private Hashtable<String,String> htXMLFileType=null;
	private boolean bInTextRun = false; // DWH 4-10-09
	private boolean bInSubTextRun = false; // DWH 4-10-09
	private boolean bInDeletion = false; // DWH 5-8-09 <w:del> deletion in tracking mode in Word
	private boolean bInInsertion = false; // DWH 5-8-09 <w:ins> insertion in tracking mode in Word
	private boolean bBetweenTextMarkers=false; // DWH 4-14-09
	private boolean bAfterText = false; // DWH 4-10-09
	private TextRun trTextRun = null; // DWH 4-10-09
	private TextRun trNonTextRun = null; // DWH 5-5-09
	private boolean bIgnoredPreRun = false; // DWH 4-10-09
	private boolean bBeforeFirstTextRun = true; // DWH 4-15-09
	private boolean bInMainFile = false; // DWH 4-15-09

	public OpenXMLContentFilter() {
		super(); // 1-6-09
		setMimeType("text/xml");
	}

	/**
	 * Logs information about the event fir the log level is FINEST. 
	 * @param event event to log information about 
	 */
	public void displayOneEvent(Event event) // DWH 4-22-09 LOGGER
	{
		Set<String> setter;
		if (LOGGER.isLoggable(Level.FINEST))
		{
			String etyp=event.getEventType().toString();
			if (event.getEventType() == EventType.TEXT_UNIT) {
	//			assertTrue(event.getResource() instanceof TextUnit);
			} else if (event.getEventType() == EventType.DOCUMENT_PART) {
	//			assertTrue(event.getResource() instanceof DocumentPart);
			} else if (event.getEventType() == EventType.START_GROUP
					|| event.getEventType() == EventType.END_GROUP) {
	//			assertTrue(event.getResource() instanceof StartGroup || event.getResource() instanceof Ending);
			}
			if (etyp.equals("START"))
				LOGGER.log(Level.FINEST,"\n");
			LOGGER.log(Level.FINEST,etyp + ": ");
			if (event.getResource() != null) {
				LOGGER.log(Level.FINEST,"(" + event.getResource().getId()+")");
				if (event.getResource() instanceof DocumentPart) {
					setter = ((DocumentPart) event.getResource()).getSourcePropertyNames();
					for(String seti : setter)
						LOGGER.log(Level.FINEST,seti);
				} else {
					LOGGER.log(Level.FINEST,event.getResource().toString());
				}
				if (event.getResource().getSkeleton() != null) {
					LOGGER.log(Level.FINEST,"*Skeleton: \n" + event.getResource().getSkeleton().toString());
				}
			}
		}		
	}
	/**
	 * Sets the name of the Yaml configuration file for the current file type, reads the file, and sets the parameters.
	 * @param filetype type of XML in the current file
	 */
	public void setUpConfig(int filetype)
	{
		this.filetype = filetype; // DWH 5-13-09
		switch(filetype)
		{
			case MSWORDCHART:
				sConfigFileName = "/net/sf/okapi/filters/openxml/wordChartConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = MSWORDCHART;
				break;
			case MSEXCEL:
				sConfigFileName = "/net/sf/okapi/filters/openxml/excelConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = MSEXCEL;
				break;
			case MSPOWERPOINT:
				sConfigFileName = "/net/sf/okapi/filters/openxml/powerpointConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = MSPOWERPOINT;
				break;
			case MSEXCELCOMMENT: // DWH 5-13-09
				sConfigFileName = "/net/sf/okapi/filters/openxml/excelCommentConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = MSEXCEL;
				break;
			case MSWORD:
			default:
				sConfigFileName = "/net/sf/okapi/filters/openxml/wordConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = MSWORD;
				break;
		}
		urlConfig = OpenXMLContentFilter.class.getResource(sConfigFileName); // DWH 3-9-09
		setDefaultConfig(urlConfig); // DWH 3-9-09
		try
		{
			setParameters(new Parameters(urlConfig)); // DWH 3-9-09 it doesn't update automatically from setDefaultConfig
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE,"Can't read MS Office Filter Configuration File.");
			throw new OkapiIOException("Can't read MS Office Filter Configuration File.");
		}
	}
	
	/**
	 * Combines contiguous compatible text runs, in order to simplify the inline tags presented 
               * to a user.  Note that MSWord can have embedded <w:r> elements for ruby text.  Note
               * that Piped streams are used which use a separate thread for this processing.
	 * @param in input stream of the XML file
	 * @param in piped output stream for the "squished" output
	 * @return a PipedInputStream used for further processing of the file
	 */
	public InputStream combineRepeatedFormat(final InputStream in, final PipedOutputStream pios)
	{
		PipedInputStream piis=null;
//		final PipedOutputStream pios = new PipedOutputStream();
		try {
			piis = new PipedInputStream(pios);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"Can't read piped input stream.");
			throw new OkapiIOException("Can't read piped input stream.");
		}		
		final OutputStreamWriter osw = new OutputStreamWriter(pios);
		final BufferedWriter bw = new BufferedWriter(osw);
		final InputStreamReader isr = new InputStreamReader(in);
		final BufferedReader br = new BufferedReader(isr);
	    Thread readThread = new Thread(new Runnable()
	    {
	      char cbuf[] = new char[512];
	      String curtag="",curtext="",curtagname="",onp="",offp="";
	      String r1b4text="",r1aftext="",t1="";
	      String r2b4text="",r2aftext="",t2="";
	      int i,n;
	      boolean bIntag=false,bGotname=false,bInap=false,bHavr1=false,bInr=false,bB4text=true,bInInnerR=false;
	      public void run()
	      {
	        try
	        {
	          while((n=br.read(cbuf,0,512))!=-1)
	          {
		    	for(i=0;i<n;i++)
		    	{
		    		handleOneChar(cbuf[i]);
		    	}
	          }
	          if (curtext.length()>0)
	        	  havtext(curtext);
	        }
	        catch(IOException e)
	        {
				LOGGER.log(Level.SEVERE,"Can't read input pipe.");
				throw new OkapiIOException("Can't read input pipe.");	        	
	        }
		    try {
		    	br.close();
		    	isr.close();
		    	bw.flush();
		    	bw.close();
//				osw.flush();
				osw.close();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE,"Can't read piped input.");
				throw new OkapiIOException("Can't read piped input.");
			}
	      }
	      private void handleOneChar(char c)
	      {
	    	  if (c=='>')
	    	  {
	    		  curtag = curtag + ">";
	    		  havatag(curtag,curtagname);
	    		  curtag = "";
	    		  curtagname = "";
	    		  bIntag = false;
	    	  }
	    	  else if (c=='<')
	    	  {
	    		  if (!bIntag)
	    		  {
		    		  if (curtext.length()>0)
		    		  {
		    			  havtext(curtext);
		    			  curtext = "";
		    		  }
	    			  curtag = curtag + "<";
	    			  bIntag = true;
	    			  bGotname = false;
	    		  }
	    		  else
	    		  {
	    			  curtag = curtag + "&lt;";
	    		  }
	    	  }
	    	  else
	    	  {
	    		  if (bIntag)
	    		  {
	    			  curtag = curtag + c;
	    			  if (!bGotname)
	    				  if (c==' ')
	    					  bGotname = true;
	    				  else
	    					  curtagname = curtagname + c;
	    		  }
	    		  else
	    			  curtext = curtext + c;
	    	  }
	      }
	      private void havatag(String tug,String tugname)
	      {
	    	  if (tugname.equals("w:p") || tugname.equals("a:p"))
	    	  {
	    		  onp = tug;
	    		  bInap = true;
	    		  bInr = false;
	    		  bInInnerR = false; // DWH 3-9-09
	    		  bHavr1 = false;
	    		  bB4text = false;
	    	  }
	    	  else if (tugname.equals("/w:p") || tugname.equals("/a:p"))
	    	  {
	    		  offp = tug;
	    		  bInap = false;
	    		  streamTheCurrentStuff();
	    	  }
	    	  else if (bInap)
	    	  {
		    	  if (tugname.equals("w:r") || tugname.equals("a:r"))
		    	  {
		    		  if (bInr)
		    		  {
		    			  bInInnerR = true; // DWH 3-2-09 ruby text has embedded <w:r> codes
		    			  innanar(tug);
		    		  }
		    		  else
		    		  {
		    			  if (bHavr1)
		    				  r2b4text = tug;
		    			  else
		    				  r1b4text = tug;
		    			  bInr = true;
		    			  bB4text = true;
		    		  }
		    	  }
		    	  else if (tugname.equals("/w:r") || tugname.equals("/a:r"))
		    	  {
		    		  if (bInInnerR)
		    		  {
		    			  bInInnerR = false; // DWH 3-2-09
		    			  innanar(tug);
		    		  }
		    		  else
		    		  {
			    		  bInr = false;
			    		  if (bHavr1)
			    		  {
			    			  r2aftext = r2aftext + tug;
			    			  if (r1b4text.equals(r2b4text) && r2aftext.equals(r2aftext))
			    			  {
			    				  t1 = t1 + t2;
			    				  r2b4text = "";
			    				  r2aftext = "";
			    				  t2 = "";
			    			  }
			    			  else
			    				  streamTheCurrentStuff();
			    		  }
			    		  else
			    		  {
			    			  r1aftext = r1aftext + tug;
			    			  bHavr1 = true;
			    		  }
		    		  }
		    	  }
		    	  else if (bInr)
		    		  innanar(tug);
		    	  else
		    	  {
		    		  streamTheCurrentStuff();
		    		  onp = tug; // this puts out <w:p> and any previous unoutput <w:r> blocks,
		    		  		     // then puts current tag in onp to be output next 
		    	  }
	    	  }
	    	  else
				rat(tug);
	      }
	      private void innanar(String tug)
	      {
    		  if (bHavr1)
    		  {
    			  if (bB4text)
    				  r2b4text = r2b4text + tug;
    			  else
    				  r2aftext = r2aftext + tug;
    		  }
    		  else
    		  {
    			  if (bB4text)
    				  r1b4text = r1b4text + tug;
    			  else
    				  r1aftext = r1aftext + tug;
    		  }	    	  
	      }
	      private void havtext(String curtext)
	      {
	    	  if (bInap)
	    	  {
		    	  if (bInInnerR) // DWH 3-2-09 (just the condition) ruby text has embedded <w:r> codes
		    		  innanar(curtext);
		    	  else
		    	  {
		    		  bB4text = false;
		    		  if (bHavr1)
			    	  {
			    		  t2 = curtext;
			    	  }
			    	  else
			    		  t1 = curtext;
		    	  }
	    	  }
	    	  else
				rat(curtext);
	      }
	      private void streamTheCurrentStuff()
	      {
	    	  if (bInap)
	    	  {
	    		    rat(onp+r1b4text+t1+r1aftext);
	    		    onp = "";
					r1b4text = r2b4text;
					t1 = t2;
					r1aftext = r2aftext;
					r2b4text = "";
					t2 = "";
					r2aftext = "";
					offp = "";
	    	  }
	    	  else
	    	  {
			  	    rat(onp+r1b4text+t1+r1aftext+r2b4text+t2+r2aftext+offp);
					onp = "";
					r1b4text = "";
					t1 = "";
					r1aftext = "";
					r2b4text = "";
					t2 = "";
					r2aftext = "";
					offp = "";
					bHavr1 = false;
	    	  }
	      }
	      private void rat(String s) // the Texan form of "write"
	      {
	    	try
	    	{
				bw.write(s);
				LOGGER.log(Level.FINEST,s); //
			} catch (IOException e) {
				LOGGER.log(Level.WARNING,"Problem writing piped stream.");
//				throw new OkapiIOException("Can't read piped input.");
				s = s + " ";
			}
	      }
	    });
	    readThread.start();
		return piis;
	}
	
	/**
	 * Adds CDATA as a DocumentPart
	 * @param a tag containing the CDATA
	 */
	protected void handleCdataSection(Tag tag) { // 1-5-09
		if (bInDeletion) // DWH 5-8-09
			addToNonTextRun(tag.toString());
		else
			addToDocumentPart(tag.toString());
	}

	/**
	 * Handles text.  If in a text run, it ends the text run and 
               * adds the tags that were in it as a single MARKER_OPENING code.
               * This would correspond to <w:r>...<w:t> in MSWord.  It will
               * then start a new text run anticipating </w:t>...</w:r>.  If
               * text is found that was not in a text run, i.e. it was not between
               * text markers, it is not text to be processed by a user, so it
               * becomes part of a new text run which will become part of a
               * code.  If the text is not in a text unit, then it is added to a
               * document part.
	 * @param text the text to be handled
	 */
	@Override
	protected void handleText(Segment text) {
		if (text==null) // DWH 4-14-09
			return;
		String txt=text.toString();
		handleSomeText(txt,text.isWhiteSpace()); // DWH 5-14-09
	}
	
	private void handleSomeText(String txt, boolean bWhiteSpace)
	{
		if (bInDeletion) // DWH 5-8-09
		{
			addToNonTextRun(txt);
			return;
		}
		// if in excluded state everything is skeleton including text
		if (getRuleState().isExludedState()) {
			addToDocumentPart(txt);
			return;
		}

		// check for ignorable whitespace and add it to the skeleton
		// The Jericho html parser always pulls out the largest stretch of text
		// so standalone whitespace should always be ignorable if we are not
		// already processing inline text
//		if (text.isWhiteSpace() && !isInsideTextRun()) {
		if (bWhiteSpace && !isInsideTextRun()) {
			addToDocumentPart(txt);
			return;
		}
		if (canStartNewTextUnit())
		{
//			if (bBetweenTextMarkers)
//				startTextUnit(txt);
//			else
				addToDocumentPart(txt);
		}
		else
		{
			if (bInTextRun) // DWH 4-20-09 whole if revised
			{
				if (bBetweenTextMarkers)
				{
					if (filetype==MSEXCEL && txt!=null && txt.length()>0 && txt.charAt(0)=='=')
						addToTextRun(txt); // DWH 5-13-09 don't treat Excel formula as text to be translated
					else
					{
						addTextRunToCurrentTextUnit(false); // adds a code for the preceding text run
						bAfterText = true;
						addToTextUnit(txt); // adds the text
						trTextRun = new TextRun(); // then starts a new text run for a code after the text
						bInTextRun = true;
					}
				}
				else
					addToTextRun(txt); // for <w:delText>text</w:delText> don't translate deleted text (will be inside code)
			}
			else
			{
				trTextRun = new TextRun();
				bInTextRun = true;
				addToTextRun(txt); // not inside text markers, so this text will become part of a code
			}
		}
	}

	/**
	 * Handles a tag that is anticipated to be a DocumentPart.  Since everything
               * between TEXTUNIT markers is treated as an inline code, if there is a
               * current TextUnit, this is added as a code in the text unit.
	 * @param tag a tag
	 */
	@Override
	protected void handleDocumentPart(Tag tag) {
		if (canStartNewTextUnit()) // DWH ifline and whole else: is an inline code if inside a text unit
			addToDocumentPart(tag.toString()); // 1-5-09
		else if (bInDeletion) // DWH 5-8-09
			addToNonTextRun(tag.toString());
		else
			addCodeToCurrentTextUnit(tag);				
	}

	/**
	 * Handle all numeric entities. Default implementation converts entity to
	 * Unicode character.
	 * 
	 * @param entity
	 *            - the numeric entity
	 */

	protected void handleCharacterEntity(Segment entity) { // DWH 5-14-09
		String decodedText = CharacterReference.decode(entity.toString(), false);
/*
 		if (!isCurrentTextUnit()) {

			startTextUnit();
		}
		addToTextUnit(decodedText);
*/		if (decodedText!=null && !decodedText.equals("")) // DWH 5-14-09 treat CharacterEntities like other text
		  handleSomeText(decodedText,false);
	}
	/**
	 * Handles a start tag.  TEXT_UNIT_ELEMENTs start a new TextUnit.  TEXT_RUN_ELEMENTs
               * start a new text run.  TEXT_MARKER_ELEMENTS set a flag that any following
               * text will be between text markers.  ATTRIBUTES_ONLY tags have translatable text
               * in the attributes, so within a text unit, it is added within a text run; otherwise it
               * becomes a DocumentPart.
	 * @param startTagt the start tag to process
	 */
	@Override
	protected void handleStartTag(StartTag startTag) {
		String sTagName;
		String sTagString; // DWH 2-26-09
		String sPartName; // DWH 2-26-09 for PartName attribute in [Content_Types].xml
		String sContentType; // DWH 2-26-09 for ContentType attribute in [Content_Types].xml
		// if in excluded state everything is skeleton including text
		String tempTagType; // DWH 5-7-09
		if (startTag==null) // DWH 4-14-09
			return;
		if (bInDeletion)
		{
			addToNonTextRun(startTag);
			return;
		}
		sTagName = startTag.getName(); // DWH 2-26-09
		sTagString = startTag.toString(); // DWH 2-26-09
		if (getRuleState().isExludedState()) {
			addToDocumentPart(sTagString);
			// process these tag types to update parser state
			switch (getConfig().getMainRuleType(sTagName)) {
			  // DWH 1-23-09
			case EXCLUDED_ELEMENT:
				getRuleState().pushExcludedRule(sTagName);
				break;
			case INCLUDED_ELEMENT:
				getRuleState().pushIncludedRule(sTagName);
				break;
			case PRESERVE_WHITESPACE:
				getRuleState().pushPreserverWhitespaceRule(sTagName);
				break;
			}
			return;
		}
		switch (getConfig().getMainRuleType(sTagName)) {
		  // DWH 1-23-09
		case INLINE_ELEMENT:
			if (canStartNewTextUnit()) {
				startTextUnit();
			}
			if (bInTextRun) // DWH 4-9-09
				addToTextRun(startTag);
			else // DWH 5-7-09
			{
				if (getConfig().getElementType(sTagName).equals("delete"))
					bInDeletion = true;
				addToNonTextRun(startTag); // DWH 5-5-09
			}
			break;

		case ATTRIBUTES_ONLY:
			// we assume we have already ended any (non-complex) TextUnit in
			// the main while loop above
			List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;

			if (canStartNewTextUnit()) // DWH 2-14-09 document part just created is part of inline codes
			{
				propertyTextUnitPlaceholders = createPropertyTextUnitPlaceholders(startTag); // 1-29-09
				if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) { // 1-29-09
					startDocumentPart(sTagString, sTagName, propertyTextUnitPlaceholders);
				 // DWH 1-29-09
					endDocumentPart();
				} else {
				// no attributes that need processing - just treat as skeleton
					addToDocumentPart(sTagString);
				}
			}
			else
			{
				propertyTextUnitPlaceholders = createPropertyTextUnitPlaceholders(startTag); // 1-29-09
				if (bInTextRun) // DWH 4-10-09
					addToTextRun(startTag,propertyTextUnitPlaceholders);
				else
					addToNonTextRun(startTag,propertyTextUnitPlaceholders);
			}
			break;
		case GROUP_ELEMENT:
			getRuleState().pushGroupRule(sTagName);
			startGroup(new GenericSkeleton(sTagString));
			break;
		case EXCLUDED_ELEMENT:
			getRuleState().pushExcludedRule(sTagName);
			addToDocumentPart(sTagString);
			break;
		case INCLUDED_ELEMENT:
			getRuleState().pushIncludedRule(sTagName);
			addToDocumentPart(sTagString);
			break;
		case TEXT_UNIT_ELEMENT:
			addNonTextRunToCurrentTextUnit(); // DWH 5-5-09 trNonTextRun should be null at this point
			bBeforeFirstTextRun = true; // DWH 5-5-09 addNonTextRunToCurrentTextUnit sets it false
//			if (startTag.isSyntacticalEmptyElementTag()) // means the tag ended with />
			if (sTagString.endsWith("/>")) // DWH 3-18-09 in case text unit element is a standalone tag (weird, but Microsoft does it)
				addToDocumentPart(sTagString); // 1-5-09
			else
			{
				getRuleState().pushTextUnitRule(sTagName);
				startTextUnit(new GenericSkeleton(sTagString)); // DWH 1-29-09
				if (configurationType==MSEXCEL || configurationType==MSWORDCHART)
				// DWH 4-16-09 Excel and Word Charts don't have text runs or text markers
				{
					bInTextRun = true;
					bBetweenTextMarkers = true;
				}
				else
				{
					bInTextRun = false;
					bBetweenTextMarkers = false;					
				}
			}
			break;
		case TEXT_RUN_ELEMENT: // DWH 4-10-09 smoosh text runs into single <x>text</x>
			if (canStartNewTextUnit()) // DWH 5-5-09 shouldn't happen
				addToDocumentPart(sTagString);
			else
			{
				addNonTextRunToCurrentTextUnit(); // DWH 5-5-09
				bBeforeFirstTextRun = false; // DWH 5-5-09
				if (getConfig().getElementType(sTagName).equals("insert")) // DWH 5-8-09 w:ins
					bInInsertion = true;
				else if (bInTextRun)
					bInSubTextRun = true;
				else
				{
					bInTextRun = true;
					bAfterText = false;
					bIgnoredPreRun = false;
					bBetweenTextMarkers = false; // DWH 4-16-09
				}
				addToTextRun(startTag);
			}
			break;
		case TEXT_MARKER_ELEMENT: // DWH 4-14-09 whole case
			if (canStartNewTextUnit()) // DWH 5-5-09 shouldn't happen
				addToDocumentPart(sTagString);
			else
			{
				addNonTextRunToCurrentTextUnit(); // DWH 5-5-09
				if (bInTextRun)
				{
					bBetweenTextMarkers = true;
					addToTextRun(startTag);
				}
				else
					addToNonTextRun(sTagString);
			}
			break;
		case PRESERVE_WHITESPACE:
			getRuleState().pushPreserverWhitespaceRule(sTagName);
			addToDocumentPart(sTagString);
			break;
		default:
			if (sTagName.equals("override")) // DWH 2-26-09 in [Content_Types].xml
			{ // it could be slow to do this test every time; I wonder if there is a better way
				sPartName = startTag.getAttributeValue("PartName");
				sContentType = startTag.getAttributeValue("ContentType");
				if (htXMLFileType!=null)
					htXMLFileType.put(sPartName, sContentType);
			}
			if (canStartNewTextUnit()) // DWH 1-14-09 then not currently in text unit; added else
				addToDocumentPart(sTagString); // 1-5-09
			else if (bInTextRun) // DWH 4-10-09
				addToTextRun(startTag);
			else
				addToNonTextRun(startTag); // DWH 5-5-09
		}
	}

	/**
	 * Handles end tags.  These either add to current text runs
               * or end text runs or text units as appropriate.
	 * @param endTag the end tag to process
	 */
	@Override
	protected void handleEndTag(EndTag endTag) {
		// if in excluded state everything is skeleton including text
		String sTagName; // DWH 2-26-09
		String sTagString; // DWH 4-14-09
		String tempTagType; // DWH 5-5-09
		if (endTag==null) // DWH 4-14-09
			return;
		sTagName = endTag.getName(); // DWH 2-26-09
		if (bInDeletion)
		{
			addToNonTextRun(endTag);
			if (getConfig().getElementType(sTagName).equals("delete"))
			{
				bInDeletion = false;
			}
			return;
		}
		sTagString = endTag.toString(); // DWH 2-26-09
		if (getRuleState().isExludedState()) {
			addToDocumentPart(endTag.toString());
			// process these tag types to update parser state
			switch (getConfig().getMainRuleType(sTagName)) {
			  // DWH 1-23-09
			case EXCLUDED_ELEMENT:
				getRuleState().popExcludedIncludedRule();
				break;
			case INCLUDED_ELEMENT:
				getRuleState().popExcludedIncludedRule();
				break;
			case PRESERVE_WHITESPACE:
				getRuleState().popPreserverWhitespaceRule();
				break;
			}

			return;
		}

		switch (getConfig().getMainRuleType(sTagName)) {
		  // DWH 1-23-09
		case INLINE_ELEMENT:
			if (canStartNewTextUnit()) {
				startTextUnit();
			}
			if (bInTextRun) // DWH 4-9-09
				addToTextRun(endTag);
			else if (getConfig().getElementType(sTagName).equals("delete")) // DWH 5-7-09
			{
				if (trNonTextRun!=null)
					addNonTextRunToCurrentTextUnit();
				tempTagType = getTagType(); // DWH 5-7-09 saves what Abstract Base Filter thinks current tag type is
				setTagType("d"); // DWH 5-7-09 type for current manufactured tag in text run, so tags will balance
				addToTextUnit(TextFragment.TagType.CLOSING, sTagString, "d"); // DWH 5-7-09 adds as opening d
				setTagType(tempTagType); // DWH 5-7-09  restore tag type to what AbstractBaseFilter expects
			}
			else
				addToNonTextRun(endTag); // DWH 5-5-09
			break;
		case GROUP_ELEMENT:
			getRuleState().popGroupRule();
			endGroup(new GenericSkeleton(sTagString));
			break;
		case EXCLUDED_ELEMENT:
			getRuleState().popExcludedIncludedRule();
			addToDocumentPart(sTagString);
			break;
		case INCLUDED_ELEMENT:
			getRuleState().popExcludedIncludedRule();
			addToDocumentPart(sTagString);
			break;
		case TEXT_UNIT_ELEMENT:
			if (bInTextRun)
			{
				addTextRunToCurrentTextUnit(true);
				bInTextRun = false;
			} // otherwise this is an illegal element, so just ignore it
			addNonTextRunToCurrentTextUnit(); // DWH 5-5-09
			bBetweenTextMarkers = true; // DWH 4-16-09
			getRuleState().popTextUnitRule();
			endTextUnit(new GenericSkeleton(sTagString));
			break;
		case TEXT_RUN_ELEMENT: // DWH 4-10-09 smoosh text runs into single <x>text</x>
			if (canStartNewTextUnit()) // DWH 5-5-09
				addToDocumentPart(sTagString);
			else
			{
				addToTextRun(endTag);
				if (getConfig().getElementType(sTagName).equals("insert")) // end of insertion </w:ins>
				{
					bInInsertion = false;
					addTextRunToCurrentTextUnit(true);
					bInTextRun = false;
					addNonTextRunToCurrentTextUnit(); // DWH 5-5-09					
				}
				else if (bInSubTextRun)
					bInSubTextRun = false;
				else if (bInTextRun)
				{
					if (!bInInsertion) // DWH 5-5-09 if inserting, don't end TextRun till end of insertion
					{
						addTextRunToCurrentTextUnit(true);
						addNonTextRunToCurrentTextUnit(); // DWH 5-5-09
					}
					bInTextRun = false;
				} // otherwise this is an illegal element, so just ignore it
			}
			break;
		case TEXT_MARKER_ELEMENT: // DWH 4-14-09 whole case
			if (canStartNewTextUnit()) // DWH 5-5-09
				addToDocumentPart(sTagString);
			else if (bInTextRun) // DWH 5-5-09 lacked else
			{
				bBetweenTextMarkers = false;
				addToTextRun(endTag);
			}
			else
				addToNonTextRun(sTagString); // DWH 5-5-09
			break;
		case PRESERVE_WHITESPACE:
			getRuleState().popPreserverWhitespaceRule();
			addToDocumentPart(sTagString);
			break;
		default:
			if (canStartNewTextUnit()) // DWH 1-14-09 then not currently in text unit; added else
				addToDocumentPart(sTagString); // not in text unit, so add to skeleton
			else if (bInTextRun) // DWH 4-9-09
				addToTextRun(endTag);
			else
				addToNonTextRun(endTag); // DWH 5-5-09
			break;
		}
	}

	/**
	 * Treats XML comments as DocumentParts.
	 * @param tag comment tag
	 */
	@Override
	protected void handleComment(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats XML doc type declaratons as DocumentParts.
	 * @param tag doc type declaration tag
	 */
	@Override
	protected void handleDocTypeDeclaration(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats XML markup declaratons as DocumentParts.
	 * @param tag markup declaration tag
	 */
	@Override
	protected void handleMarkupDeclaration(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats XML processing instructions as DocumentParts.
	 * @param tag processing instruction tag
	 */
	@Override
	protected void handleProcessingInstruction(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats XML server common tags as DocumentParts.
	 * @param tag server common tag
	 */
	@Override
	protected void handleServerCommon(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats server common escaped tags as DocumentParts.
	 * @param tag server common escaped tag
	 */
	@Override
	protected void handleServerCommonEscaped(Tag tag) {
		handleDocumentPart(tag);		
	}

	/**
	 * Treats XML declaratons as DocumentParts.
	 * @param tag XML declaration tag
	 */
	@Override
	protected void handleXmlDeclaration(Tag tag) {
		handleDocumentPart(tag);		
	}
	
	/**
	 * Returns name of the filter.
	 * @return name of the filter
	 */
	public String getName() {
		return "OpenXMLContentFilter";
	}
	/**
	 * Normalizes naming of attributes whose values are the
               * encoding or a language name, so that they can be 
               * automatically changed to the output encoding and output.
               * Unfortunately, this hard codes the tags to look for.
	 * @param attrName name of the attribute
	 * @param attrValue, value of the attribute
	 * @param tag tag that contains the attribute
	 * @return a normalized name for the attribute
	 */
	@Override
	protected String normalizeAttributeName(String attrName, String attrValue, Tag tag) {
		// normalize values for HTML
		String normalizedName = attrName;
		String tagName; // DWH 2-19-09 */
// Any attribute that encodes language should be renamed here to "language"
// Any attribute that encodes locale or charset should be normalized too
/*
		// <meta http-equiv="Content-Type"
		// content="text/html; charset=ISO-2022-JP">
		if (isMetaCharset(attrName, attrValue, tag)) {
			normalizedName = HtmlEncoder.NORMALIZED_ENCODING;
			return normalizedName;
		}

		// <meta http-equiv="Content-Language" content="en"
		if (tag.getName().equals("meta") && attrName.equals(HtmlEncoder.CONTENT)) {
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("http-equiv") != null) {
				if (st.getAttributeValue("http-equiv").equals("Content-Language")) {
					normalizedName = HtmlEncoder.NORMALIZED_LANGUAGE;
					return normalizedName;
				}
			}
		}
*/
		// <w:lang w:val="en-US" ...>
		tagName = tag.getName();
		if (tagName.equals("w:lang") || tagName.equals("w:themefontlang")) // DWH 4-3-09 themeFontLang
		{
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("w:val") != null)
			{
				normalizedName = Property.LANGUAGE;
				return normalizedName;
			}
		}
		else if (tagName.equals("c:lang")) // DWH 4-3-09
		{
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("val") != null)
			{
				normalizedName = Property.LANGUAGE;
				return normalizedName;
			}
		}
		else if (tagName.equals("a:endpararpr") || tagName.equals("a:rpr"))
		{
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("lang") != null)
			{
				normalizedName = Property.LANGUAGE;
				return normalizedName;
			}
		}
		return normalizedName;
	}
	protected void initFileTypes() // DWH $$$ needed?
	{
		htXMLFileType = new Hashtable();
	}
	protected String getContentType(String sPartName) // DWH $$$ needed?
	{
		String rslt="",tmp;
		if (sPartName!=null)
		{
			tmp = (String)htXMLFileType.get(sPartName);
			if (tmp!=null)
				rslt = tmp;
		}
		return(rslt);
	}
	/**
	 * Adds a text string to a sequence of tags that are
	 * not in a text run, that will become a single code.
	 * @param s the text string to add
	 */
	private void addToNonTextRun(String s) // DWH 5-5-09
	{
		if (trNonTextRun==null)
			trNonTextRun = new TextRun();
		trNonTextRun.append(s);
	}
	/**
	 * Adds a tag to a sequence of tags that are
	 * not in a text run, that will become a single code.
	 * @param s the text string to add
	 */
	private void addToNonTextRun(Tag tag) // DWH 5-5-09
	{
		if (trNonTextRun==null)
			trNonTextRun = new TextRun();
		trNonTextRun.append(tag.toString());
	}
	/**
	 * Adds a tag and codes to a sequence of tags that are
	 * not in a text run, that will become a single code.
	 * @param tag the tag to add
	 * @param propertyTextUnitPlaceholders a list of codes of embedded text
     */
	private void addToNonTextRun(Tag tag, List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders)
	{
		String txt;
		int offset;
		if (trNonTextRun==null)
			trNonTextRun = new TextRun();
		txt=trNonTextRun.getText();
		offset=txt.length();
		trNonTextRun.appendWithPropertyTextUnitPlaceholders(tag.toString(),offset,propertyTextUnitPlaceholders);
	}
	/**
	 * Adds a text string to a text run that will become a single code.
	 * @param s the text string to add
	 */
	private void addToTextRun(String s)
	{
		if (trTextRun==null)
			trTextRun = new TextRun();
		trTextRun.append(s);		
	}
	/**
	 * Adds a tag to a text run that will become a single code.
	 * @param tag the tag to add
	 */
	private void addToTextRun(Tag tag) // DWH 4-10-09 adds tag text to string that will be part of larger code later
	{
		// add something here to check if it was bold, italics, etc. to set a property
		if (trTextRun==null)
			trTextRun = new TextRun();
		trTextRun.append(tag.toString());
	}
	/**
	 * Adds a tag and codes to a text run that will become a single code.
	 * @param tag the tag to add
	 * @param propertyTextUnitPlaceholders a list of codes of embedded text
     */
	private void addToTextRun(Tag tag, List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders)
	{
		String txt;
		int offset;
		if (trTextRun==null)
			trTextRun = new TextRun();
		txt=trTextRun.getText();
		offset=txt.length();
		trTextRun.appendWithPropertyTextUnitPlaceholders(tag.toString(),offset,propertyTextUnitPlaceholders);
	}
	
	/**
	 * Adds the text and codes in a text run as a single code in a text unit.
               * If it is after text, it is added as a MARKER_CLOSING.  If no text
               * was encountered and this is being called by an ending TEXT_RUN_ELEMENT
               * or ending TEXT_UNIT_ELEMENT, it is added as a MARKER_PLACEHOLDER.
               * Otherwise, it is added as a MARKER_OPENING.
	 * compatible contiguous text runs if desired, and creates a 
	 * START_SUBDOCUMENT event
	 * @param bEndRun true if called while processing an end TEXT_RUN_ELEMENT
               * or end TEXT_UNIT_ELEMENT
	 */
	private void addTextRunToCurrentTextUnit(boolean bEndRun) {
		List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;
		TextFragment.TagType codeType;
		String text,tempTagType;
		int len;
		if (trTextRun!=null && !(text=trTextRun.getText()).equals("")) // DWH 5-14-09 "" can occur with Character entities
		{
			if (bAfterText)
				codeType = TextFragment.TagType.CLOSING;
			else if (bEndRun) // if no text was encountered and this is the </w:r> or </w:p>, this is a standalone code
				codeType = TextFragment.TagType.PLACEHOLDER;
			else
				codeType = TextFragment.TagType.OPENING;
//			text = trTextRun.getText();
			if (codeType==TextFragment.TagType.OPENING &&
				!bBeforeFirstTextRun && // DWH 4-15-09 only do this if there wasn't stuff before <w:r>
				bInMainFile && // DWH 4-15-08 only do this in MSWORD document and MSPOWERPOINT slides
				((/*text.equals("<w:r><w:t>") || */text.equals("<w:r><w:t xml:space=\"preserve\">")) ||
				 (/*text.equals("<a:r><a:t>") || */text.equals("<a:r><a:t xml:space=\"preserve\">"))))
			{
				bIgnoredPreRun = true; // don't put codes around text that has not attributes
				trTextRun = null;
				return;
			}
			else if (codeType==TextFragment.TagType.CLOSING && bIgnoredPreRun)
			{
				bIgnoredPreRun = false;
				if (text.endsWith("</w:t></w:r>") || text.endsWith("</a:t></a:r>"))
				{
					len = text.length();
					if (len>12) // take of the end codes and leave the rest as a placeholder code, if any
					{
						text = text.substring(0,len-12);
						codeType = TextFragment.TagType.CLOSING;
					}	
					else
					{
						trTextRun = null;
						return;						
					}
				}
			}
			propertyTextUnitPlaceholders = trTextRun.getPropertyTextUnitPlaceholders();
			tempTagType = getTagType(); // DWH 4-24-09 saves what Abstract Base Filter thinks current tag type is
			setTagType("x"); // DWH 4-24-09 type for current manufactured tag in text run, so tags will balance
			if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
				// add code and process actionable attributes
				addToTextUnit(codeType, text, "x", propertyTextUnitPlaceholders);
			} else {
				// no actionable attributes, just add the code as-is
				addToTextUnit(codeType, text, "x");
			}
			setTagType(tempTagType); // DWH 4-24-09 restore tag type to what AbstractBaseFilter expects
			trTextRun = null;
			bBeforeFirstTextRun = false; // since the text run has now been added to the text unit
		}
	}
	private void addNonTextRunToCurrentTextUnit() { // DWW 5-5-09
		List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;
		TextFragment.TagType codeType;
		String text,tempTagType;
		if (trNonTextRun!=null)
		{
			text = trNonTextRun.getText();
			if (canStartNewTextUnit()) // DWH shouldn't happen
			{
				addToDocumentPart(text);
			}
			propertyTextUnitPlaceholders = trNonTextRun.getPropertyTextUnitPlaceholders();
			if (bBeforeFirstTextRun &&
			   (propertyTextUnitPlaceholders==null || propertyTextUnitPlaceholders.size()==0))
				// if a nonTextRun occurs before the first text run, and it doesn't have any
				// embedded text, just add the tags to the skeleton after <w:r> or <a:r>.
				// Since skeleton is not a TextFragment, it can't have embedded text, so if
				// there is embedded text, do the else and make a PLACEHOLDER code
			{
				appendToFirstSkeletonPart(text);
			}
			else
			{
				codeType = TextFragment.TagType.PLACEHOLDER;
				tempTagType = getTagType(); // DWH 4-24-09 saves what Abstract Base Filter thinks current tag type is
				setTagType("x"); // DWH 4-24-09 type for current manufactured tag in text run, so tags will balance
				if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
					// add code and process actionable attributes
					addToTextUnit(codeType, text, "x", propertyTextUnitPlaceholders);
				} else {
					// no actionable attributes, just add the code as-is
					addToTextUnit(codeType, text, "x");
				}
				setTagType(tempTagType); // DWH 4-24-09 restore tag type to what AbstractBaseFilter expects
			}
			trNonTextRun = null;
		}
	}
	public int getConfigurationType()
	{
		return configurationType;
	}
	protected void setBInMainFile(boolean bInMainFile) // DWH 4-15-09
	{
		this.bInMainFile = bInMainFile;
	}
	protected boolean getBInMainFile() // DWH 4-15-09
	{
		return bInMainFile;
	}
	public void setLogger(Logger lgr)
	{
		LOGGER = lgr;
	}
}
