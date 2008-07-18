package net.sf.okapi.filters.regex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.filters.IParser;
import net.sf.okapi.common.resource.CodeFragment;
import net.sf.okapi.common.resource.Container;
import net.sf.okapi.common.resource.ExtractionItem;
import net.sf.okapi.common.resource.GroupResource;
import net.sf.okapi.common.resource.IBaseResource;
import net.sf.okapi.common.resource.IContainer;
import net.sf.okapi.common.resource.IExtractionItem;
import net.sf.okapi.common.resource.IPart;
import net.sf.okapi.common.resource.ISkeletonResource;
import net.sf.okapi.common.resource.Part;
import net.sf.okapi.common.resource.SkeletonResource;

public class Parser implements IParser {

	protected Resource                 resource;
	
	private Stack<GroupResource>       groupStack;
	private int                        sklID;
	private int                        groupID;
	private int                        itemID;
	private LinkedList<IBaseResource>  resultQueue;
	private BufferedReader             reader;
	private String                     inputText;
	private int                        startSearch;
	private int                        startSkl;
	private int                        nextAction;

	
	public Parser () {
		resource = new Resource();
	}

	public void close () {
		try {
			inputText = null;
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
		}
		catch ( IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void open (InputStream input)
		throws IOException
	{
		try {
			// Close any previously non-closed file
			close();
			
			// Open the input reader from the provided stream
			BOMAwareInputStream bis = new BOMAwareInputStream(input, resource.getSourceEncoding());
			reader = new BufferedReader(
				new InputStreamReader(bis, bis.detectEncoding()));

			// Read the whole file into one string
			//TODO: detect the original line-break type
			//TODO: Optimize this with a better 'readToEnd()'
			StringBuilder tmp = new StringBuilder();
			String buffer;
			while ( (buffer = reader.readLine()) != null ) {
				if ( tmp.length() > 0 ) tmp.append("\n");
				tmp.append(buffer);
			}
			
			close(); // We can free the file handle now
			// Make sure to close before inputText is set
			// so it does not get reset to null
			inputText = tmp.toString();
			resultQueue = new LinkedList<IBaseResource>();
			groupStack = new Stack<GroupResource>();
			startSearch = 0;
			startSkl = 0;
			itemID = 0;
			sklID = 0;
			groupID = 0;
			nextAction = -1;
			
//start test
			resource.params.rules.clear();
/*			Rule r = new Rule();
			r.name = "r3";
			r.start = "^(.*?)\\t\"";
			r.end = "\"\\t(.*?)\\t(.*?)\\t(.*?)\\t(.*?)\\t(.*?)\\t(.*?)\\t(\\d*?)\\t(.*?)\\t1$";
			r.splitters = "(\\\\[ntr])";
			r.startName = "^";
			r.endName = "\\t";
			r.ruleType = Rule.RULETYPE_CONTENT;
			r.preserveWS = true;
			resource.params.rules.add(r);*/

/*			Rule r = new Rule();
			r.name = "r3";
			r.start = "^(.*?)\\t";
			r.end = "1$";
			r.splitters = "(\\\\[ntr])";
			r.startName = "^";
			r.endName = "\\t";
			r.ruleType = Rule.RULETYPE_STRING;
			r.preserveWS = true;
			resource.params.rules.add(r);*/
			
			Rule r = new Rule();
			r.ruleName = "Start string";
			r.start = "^(.+?)\\t";
			r.end = "";
			r.nameStart = "^";
			r.nameEnd = "\\t";
			r.ruleType = Rule.RULETYPE_OPENGROUP;
			resource.params.rules.add(r);

			r = new Rule();
			r.ruleName = "Segment";
			r.start = "\\#\\!\\[BeginSeg (\\d+?)\\]";
			r.end = "\\#\\!\\[EndSeg\\]";
			r.splitters = null; 
			r.nameStart = "BeginSeg ";
			r.nameEnd = "\\]";
			r.nameFormat = "<parentName>_<self>";
			r.ruleType = Rule.RULETYPE_CONTENT;
			r.preserveWS = true;
			resource.params.rules.add(r);
			
			r = new Rule();
			r.ruleName = "Close String";
			r.start = "1$";
			r.end = "";
			r.ruleType = Rule.RULETYPE_CLOSEGROUP;
			resource.params.rules.add(r);
			//end test			
			
			// Compile the rules
			resource.params.compileRules();
		}
		catch ( UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		//catch ( IOException e) {
		//	throw new RuntimeException(e);
		//}
	}
	
	public void open (CharSequence input) {
		// TODO open(CharSequence input)
	}

	public void open (URL input)
		throws IOException
	{
		// TODO open(URL input)
	}
	
	public IBaseResource getResource() {
		return resource.currentRes;
	}
	
	public int parseNext () {
		//TODO: process outerString if needed for each skeleton part
		switch ( nextAction ) {
		case TRANSUNIT:
			nextAction = -1;
			return TRANSUNIT;
		case ENDINPUT:
			nextAction = -1;
			return ENDINPUT;
		case -2: // Pop group
			nextAction = -1;
			break;
		}
		
		// Return from the queue if needed
		if ( resultQueue.size() > 0 ) return nextEvent();
		
		/* No support for named group means we cannot use a big 'ORed' expression
		 * and just get the first rule that was matched.
		 * We have to calculate which rule matches the closer from the
		 * start position, one by one.
		 */
		//TODO: optimize by flagging and not re-using rules that never match
		//TODO: optimize by having the rules with pre-compiled pattern
		Rule bestRule = null;
		int bestPosition = inputText.length()+9;
		MatchResult startResult = null;
		MatchResult endResult = null;
		int i = 0;
		for ( Rule rule : resource.params.rules ) {
			Pattern p = Pattern.compile(rule.start, Pattern.MULTILINE);
			Matcher m = p.matcher(inputText);
			if ( m.find(startSearch) ) {
				if ( m.start() < bestPosition ) {
					// Try to find the corresponding end
					p = Pattern.compile(rule.end, Pattern.MULTILINE);
					Matcher me = p.matcher(inputText);
					if ( me.find(m.end()) ) {
						bestPosition = m.start();
						bestRule = rule;
						startResult = m.toMatchResult();
						endResult = me.toMatchResult();
					}
				}
			}
			i++;
		}
		
		if ( bestRule != null ) {
			// Process the match we just found
			int n = processMatch(bestRule, startResult, endResult);
			if ( n == -1 ) return nextEvent();
			else return n;
		}
		
		// Else: Send end of the skeleton if needed
		if ( startSearch < inputText.length() ) {
			SkeletonResource skl = new SkeletonResource();
			skl.setData(inputText.substring(startSkl, inputText.length()));
			skl.setID(String.format("%d", ++sklID));
			resource.currentRes = skl;
			nextAction = ENDINPUT;
			return SKELETON;
		}

		// Else: we have reach the end
		return ENDINPUT;
	}

	private int processMatch (Rule rule,
		MatchResult startResult,
		MatchResult endResult)
	{
		SkeletonResource skl;
		switch ( rule.ruleType ) {
		case Rule.RULETYPE_NOTRANS:
		case Rule.RULETYPE_COMMENT:
			// Skeleton data include the content
			skl = new SkeletonResource();
			skl.setData(inputText.substring(startSkl, endResult.start()));
			skl.setID(String.format("%d", ++sklID));
			resource.currentRes = skl;
			// Update starts for next read
			startSearch = endResult.end();
			startSkl = endResult.start();
			// If comment: process the comment for directives
			//TODO: process the comment for directives
			// Then just return one skeleton event
			return SKELETON;
			
		case Rule.RULETYPE_OPENGROUP:
		case Rule.RULETYPE_CLOSEGROUP:
			// Skeleton data include the content
			skl = new SkeletonResource();
			skl.setData(inputText.substring(startSkl, endResult.start()));
			skl.setID(String.format("%d", ++sklID));
			resource.currentRes = skl;
			// Update starts for next read
			startSearch = endResult.end();
			startSkl = endResult.start();
			//TODO: return group event, and deal with skeleton
			if ( rule.ruleType == Rule.RULETYPE_OPENGROUP ) {
				GroupResource groupRes = new GroupResource();
				groupRes.setID(String.format("%d", ++groupID));
				if ( rule.nameStart != null ) {
					String name = getMatch(startResult.group(), rule.nameStart, rule.nameEnd);
					if ( name != null ) {
						if ( rule.nameFormat != null ) {
							String tmp = rule.nameFormat.replace("<parentName>",
								(groupStack.size()>0 ? groupStack.peek().getName() : "" ));
							groupRes.setName(tmp.replace("<self>", name));
						}
						else groupRes.setName(name);
					}
				}
				groupStack.push(groupRes);
			}
			else { // Rule.RULETYPE_CLOSEGROUP
				groupStack.pop();
			}
			return SKELETON;
		}
		
		// Otherwise: process the item content

		// Set skeleton data if needed
		if ( startResult.end() > startSkl ) {
			addSkeletonToQueue(inputText.substring(startSkl, startResult.end()), false);
		}

		// Any resname we can use?
		String name = getMatch(startResult.group(), rule.nameStart,
			rule.nameEnd);

		// Set start positions for next read
		startSearch = endResult.end();
		startSkl = endResult.start();

		// Process the data, this will create a queue of event if needed
		if ( rule.ruleType == Rule.RULETYPE_CONTENT ) {
			processContent(rule, name, inputText.substring(
				startResult.end(), endResult.start()));
		}
		else if ( rule.ruleType == Rule.RULETYPE_STRING ) {
			processStrings(rule, name, inputText.substring(
				startResult.end(), endResult.start()));
		}
		return -1;
	}

	private String getMatch (String text,
		String start,
		String end)
	{
		if (( start == null ) || ( start.length() == 0 )) return null;
		if (( end == null ) || ( end.length() == 0 )) return null;
		
		Pattern p = Pattern.compile(start, Pattern.MULTILINE);
		Matcher m1 = p.matcher(text);
		if ( m1.find() ) {
			p = Pattern.compile(end, Pattern.MULTILINE);
			Matcher m2 = p.matcher(text);
			if ( m2.find(m1.end()) ) {
				return text.substring(m1.end(), m2.start());
			}
		}
		return null;
	}
	
	private void processContent (Rule rule,
		String name,
		String data)
	{
		ExtractionItem item = new ExtractionItem();
		item.setSource(new Container(data));
		item.setID(String.format("%d", ++itemID));
		item.setPreserveSpace(rule.preserveWS);
		splitItem(item, rule.splitters);
		if ( name != null ) {
			if ( rule.nameFormat != null ) {
				String tmp = rule.nameFormat.replace("<parentName>",
					(groupStack.size()>0 ? groupStack.peek().getName() : "" ));
				item.setName(tmp.replace("<self>", name));
			}
			else item.setName(name);
		}
		resultQueue.add(item);
	}
	
	private void splitItem (IExtractionItem item,
		String pattern)
	{
		if (( pattern == null ) || ( pattern.length() == 0 )) return;
		
		//TODO: optimize by compiling once
		Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
		IContainer src = item.getSource();
		String codedText = src.getCodedText();
		Matcher m = p.matcher(codedText);
		ArrayList<IPart> tmpList = new ArrayList<IPart>();
		int start = 0;
		int id = 0;
		CodeFragment cf = new CodeFragment(IContainer.CODE_ISOLATED, ++id, null);
		
		while ( true ) {
			if ( m.find(start) ) {
				if ( start < m.start() ) {
					// If there is a segment, add the code buffer
					if ( cf.toString() != null ) tmpList.add(new Part(cf, false));
					else --id; // Adjust ID value (we didn't use it)
					// And add the segment
					tmpList.add(src.copy(start, m.start()));
					// New fragment buffer for next time
					cf = new CodeFragment(IContainer.CODE_ISOLATED, ++id, m.group());
				}
				else {
					// Else: the code is simply compiled with any previous
					// in the code buffer. this allows to group matches and to have only
					// one code fragment between segments.
					cf.append(m.group());
				}
				// Set the new start position
				start = m.end();
			}
			else { // End of the container
				if ( tmpList.size() > 0 ) {
					if ( start < codedText.length()-1 ) {
						if ( cf.toString() != null ) tmpList.add(new Part(cf, false));
						tmpList.add(src.copy(start));
					}
					else if ( cf.toString() != null ) {
						tmpList.add(new Part(cf, false));
					}
				}
				break; // Exit loop
			}
		}
		
		// Now update the item with the segments created (if there are any)
		if ( tmpList.size() > 0 ) {
			src.reset();
			for ( IPart part : tmpList ) {
				src.append(part);
			}
		}
	}
	
	private void processStrings (Rule rule,
		String name,
		String data)
	{
		int state = 0;
		int i = -1;
		int j = 0;
		int start = -1;
		int end = -1;
		String mark = resource.params.startString;
		
		while (( ++i < data.length() ) && ( end == -1 )) {
			
			// Deal with \\, \" and \' escapes
			if ( state > 0 ) {
				if ( resource.params.useBSlashEscape ) {
					while ( data.codePointAt(i) == '\\' ) {
						if ( i+2 < data.length() ) i += 2; // Now point to next
						else throw new RuntimeException("Escape syntax error in ["+data+"]");
					}
				}
			}
		
			// Check characters
			switch ( state ) {
			case 0:
				if ( data.codePointAt(i) == mark.codePointAt(j) ) {
					if ( ++j == mark.length() ) {
						// Start of string match found, set search info for end
						start = i+1; // Start of the string content
						state = 2;
						mark = resource.params.endString;
						j = 0;
					}
					else state = 1;
				}
				break;
				
			case 1: // Look if we can finish a start match
				if ( data.codePointAt(i) == mark.codePointAt(j) ) {
					if ( ++j == mark.length() ) {
						// Start of string match found, set search info for end
						start = i+1; // Start of the string content
						state = 2;
						mark = resource.params.endString;
						j = 0;
					}
					// Else: keep moving
				}
				else { // Was not a match
					state = 0;
					i -= (j-1); // Go back just after the trigger
					j = 0; // And reset the mark index
				}
				break;
				
			case 2: // Look for an end mark
				if ( data.codePointAt(i) == mark.codePointAt(j) ) {
					if ( ++j == mark.length() ) {
						// End of string match found
						// Set the end of the string position (will stop the loop too)
						end = i-j+1;
					}
					else state = 3;
				}
				break;
				
			case 3: // Look if we can finish an end match
				if ( data.codePointAt(i) == mark.codePointAt(j) ) {
					if ( ++j == mark.length() ) {
						// End of string match found
						// Set the end of the string position (will stop the loop too)
						end = i-j+1;
					}
					// Else: Keep moving
				}
				else { // Was not a match
					state = 2;
					i -= (j-1); // Go back just after the trigger
					j = 0; // And reset the mark index
				}
				break;
			}
		}

		// If we have found a string: process it
		if ( end != -1 ) {
			// Skeleton part before
			if ( start > 0 ) {
				addSkeletonToQueue(data.substring(0, start), false);
			}
			
			// Item to extract
			ExtractionItem item = new ExtractionItem();
			item.setSource(new Container(data.substring(start, end)));
			item.setID(String.format("%d", ++itemID));
			item.setPreserveSpace(rule.preserveWS);
			//splitItem(item, rule.splitters);
			if ( name != null ) {
				if ( rule.nameFormat != null ) {
					String tmp = rule.nameFormat.replace("<parentName>",
						(groupStack.size()>0 ? groupStack.peek().getName() : "" ));
					item.setName(tmp.replace("<self>", name));
				}
				else item.setName(name);
			}
			resultQueue.add(item);
			
			// Skeleton part after
			if ( end < data.length() ) {
				addSkeletonToQueue(data.substring(end), false);
			}
		}
		else { // No string in this entry, all is skeleton
			addSkeletonToQueue(data, true);
		}
	}
	
	private void addSkeletonToQueue (String data,
		boolean forceNewEntry)
	{
		ISkeletonResource skl;
		if ( !forceNewEntry && ( resultQueue.size() > 0 )) {
			if ( resultQueue.getLast().getKind() == IBaseResource.KIND_SKELETON ) {
				// Append to the last queue entry if possible
				skl = (ISkeletonResource)resultQueue.getLast();
				skl.appendData(data);
				return;
			}
		}
		// Else: create a new skeleton entry
		skl = new SkeletonResource();
		skl.setData(data);
		skl.setID(String.format("%d", ++sklID));
		resultQueue.add(skl);
	}
	
	private int nextEvent () {
		if ( resultQueue.size() == 0 ) return -1;
		if ( resultQueue.peek().getKind() == IBaseResource.KIND_SKELETON ) {
			resource.currentRes = (ISkeletonResource)resultQueue.poll();
			return SKELETON;
		}
		// Else: it's an item
		resource.currentRes = (IExtractionItem)resultQueue.poll();
		return TRANSUNIT;
	}

}
