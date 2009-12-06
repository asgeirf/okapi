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

package net.sf.okapi.lib.translation;

import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.XLIFFContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;

/**
 * Collection of helper method for preparing and querying translation resources.
 */
public class QueryUtil {

	private static final String HTML_CLOSING_CODE = "</s>";
	private static final int HTML_CLOSING_CODE_LENGTH = HTML_CLOSING_CODE.length();
	
	private static final Pattern HTML_OPENING = Pattern.compile("\\<s(\\s+)id=['\"](.*?)['\"]>", Pattern.CASE_INSENSITIVE);
	private static final Pattern HTML_ISOLATED = Pattern.compile("\\<br(\\s+)id=['\"](.*?)['\"](\\s*?)/>", Pattern.CASE_INSENSITIVE);

	private static final Pattern HTML_SPAN = Pattern.compile("\\<span\\s(.*?)>|\\</span>", Pattern.CASE_INSENSITIVE);

	private StringBuilder codesMarkers;
	private List<Code> codes;
	private XLIFFContent fmt;
	
	public QueryUtil () {
		codesMarkers = new StringBuilder();
		fmt = new XLIFFContent();
	}
	
	/**
	 * Indicates if the last text fragment passed to {@link #separateCodesFromText(TextFragment)}
	 * has codes or not.
	 * @return true if the fragment has one or more code, false if it does not.
	 */
	public boolean hasCode () {
		if ( codes == null ) return false;
		return (codes.size() > 0);
	}
	
	/**
	 * Separate and store codes of a given text fragment.
	 * @param frag the fragment to process. Use {@link #createNewFragmentWithCodes(String)}
	 * to reconstruct the text back with its codes at the end.
	 * @return the fragment content stripped of its codes.
	 */
	public String separateCodesFromText (TextFragment frag) {
		// Reset
		codesMarkers.setLength(0);
		codes = frag.getCodes();
		// Get coded text
		String text = frag.getCodedText();
		if ( !frag.hasCode() ) {
			return text; // No codes
		}
		// If there are codes: store them apart
		StringBuilder tmp = new StringBuilder();
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
			case TextFragment.MARKER_SEGMENT:
				codesMarkers.append(text.charAt(i));
				codesMarkers.append(text.charAt(++i));
				break;
			default:
				tmp.append(text.charAt(i));
			}
		}
		// Return text without codes
		return tmp.toString();
	}

	/**
	 * Appends the codes stored apart using {@link #separateCodesFromText(TextFragment)}
	 * at the end of a given plain text. The text fragment provided must be the same and
	 * without code modifications, as the one used for the splitting.
	 * @param plainText new text to use (must be plain)
	 * @return the provided fragment, but with the new text and the original codes
	 * appended at the end.
	 */
	public TextFragment createNewFragmentWithCodes (String plainText) {
		return new TextFragment(plainText + codesMarkers, codes);
	}

	/**
	 * Converts from coded text to coded HTML.
	 * @param fragment the fragment to convert.
	 * @return The resulting HTML string.
	 */
	public String toCodedHTML (TextFragment fragment) {
		if ( fragment == null ) return "";
		Code code;
		StringBuilder sb = new StringBuilder();
		String text = fragment.getCodedText();
		for ( int i=0; i<text.length(); i++ ) {
			switch ( text.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
				code = fragment.getCode(text.charAt(++i));
				sb.append(String.format("<s id='%d'>", code.getId()));
				break;
			case TextFragment.MARKER_CLOSING:
				i++;
				sb.append("</s>");
				break;
			case TextFragment.MARKER_ISOLATED:
				code = fragment.getCode(text.charAt(++i));
				sb.append(String.format("<br id='%d'/>", code.getId()));
				break;
			case TextFragment.MARKER_SEGMENT:
				// Segment-holder text not supported
				throw new RuntimeException("Fragment with segment markers are not supported by the Google connector. Send the segments instead.");
			case '&':
				sb.append("&amp;");
				break;
			case '<':
				sb.append("&lt;");
				break;
			default:
				sb.append(text.charAt(i));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Converts back a coded HTML to a coded text.
	 * @param text the coded HTML to convert back.
	 * @param fragment the original text fragment.
	 * @return the coded text with its code markers.
	 */
	public String fromCodedHTML (String text,
		TextFragment fragment)
	{
		if ( Util.isEmpty(text) ) return "";
		text = text.toString().replace("&#39;", "'");
		text = text.replace("&lt;", "<");
		text = text.replace("&gt;", ">");
		text = text.replace("&quot;", "\"");
		StringBuilder sb = new StringBuilder();
		sb.append(text.replace("&amp;", "&"));

		Matcher m = HTML_OPENING.matcher(sb.toString());
        while ( m.find() ) {
        	// Replace the HTML fake code by the coded text markers
        	int id = Util.strToInt(m.group(2), -1);
        	String markers = String.format("%c%c", TextFragment.MARKER_OPENING,
        		TextFragment.toChar(fragment.getIndex(id)));
        	sb.replace(m.start(), m.end(), markers);
        	// Search corresponding closing part
        	int n = sb.toString().indexOf(HTML_CLOSING_CODE);
        	// Replace closing code by the coded text markers for closing
        	markers = String.format("%c%c", TextFragment.MARKER_CLOSING,
        		TextFragment.toChar(fragment.getIndexForClosing(id)));
        	sb.replace(n, n+HTML_CLOSING_CODE_LENGTH, markers);
        	m = HTML_OPENING.matcher(sb.toString());
        }
        
		m = HTML_ISOLATED.matcher(sb.toString());
        while ( m.find() ) {
        	// Replace the HTML fake code by the coded text markers
        	int id = Util.strToInt(m.group(2), -1);
        	String markers = String.format("%c%c", TextFragment.MARKER_ISOLATED,
        		TextFragment.toChar(fragment.getIndex(id)));
        	sb.replace(m.start(), m.end(), markers);
        	m = HTML_ISOLATED.matcher(sb.toString());
        }

        // Remove any span elements that may have been added
        // (some MT engines mark up their output with extra info)
        m = HTML_SPAN.matcher(sb.toString());
        while ( m.find() ) {
        	sb.replace(m.start(), m.end(), "");
        	m = HTML_SPAN.matcher(sb.toString());
        }
        
		return sb.toString();
	}

	/**
	 * Converts from coded text to XLIFF.
	 * @param fragment the fragment to convert.
	 * @return The resulting XLIFF string.
	 */
	public String toXLIFF (TextFragment fragment) {
		if ( fragment == null ) return "";
		fmt.setContent(fragment);
		return fmt.toString();
	}
	
	/**
	 * Converts back an XLIFF text to a coded text.
	 * @param text the XLIFF text to convert back.
	 * @param fragment the original text fragment.
	 * @return the coded text with its code markers.
	 */
	public String fromXLIFF (String text,
		TextFragment fragment)
	{
		if ( Util.isEmpty(text) ) return "";
		// Un-escape first layer
		text = text.replace("&apos;", "'");
		text = text.replace("&lt;", "<");
		text = text.replace("&gt;", ">");
		text = text.replace("&quot;", "\"");
		text = text.replace("&amp;", "&");
		// Now we have XLIFF valid content
		
		// Read it to XML parser
		// Un-escape XML

		//TODO: code conversion
		return text;
	}
	
	// The original parameter can be null
	public TextFragment fromXLIFF (Element elem,
		TextFragment original)
	{
		NodeList list = elem.getChildNodes();
		int lastId = -1;
		int id = -1;
		Node node;
		Stack<Integer> stack = new Stack<Integer>();
		StringBuilder buffer = new StringBuilder();
		
		// Note that this parsing assumes non-overlapping codes.
		for ( int i=0; i<list.getLength(); i++ ) {
			node = list.item(i);
			switch ( node.getNodeType() ) {
			case Node.TEXT_NODE:
				buffer.append(node.getNodeValue());
				break;
			case Node.ELEMENT_NODE:
				NamedNodeMap map = node.getAttributes();
				Node attr = map.getNamedItem("type");
				if ( node.getNodeName().equals("bpt") ) {
					id = getRawIndex(lastId, map.getNamedItem("id"));
					stack.push(id);
					buffer.append(String.format("%c%c", TextFragment.MARKER_OPENING,
	            		TextFragment.toChar(original.getIndex(id))));
				}
				else if ( node.getNodeName().equals("ept") ) {
					buffer.append(String.format("%c%c", TextFragment.MARKER_CLOSING,
			        	TextFragment.toChar(original.getIndexForClosing(stack.pop()))));
				}
				else if ( node.getNodeName().equals("ph") ) {
					id = getRawIndex(lastId, map.getNamedItem("id"));
					buffer.append(String.format("%c%c", TextFragment.MARKER_ISOLATED,
		            	TextFragment.toChar(original.getIndex(id))));
				}
				else if ( node.getNodeName().equals("it") ) {
					Node pos = map.getNamedItem("pos");
					if ( pos == null ) { // Error, but just treat it as a placeholder
						id = getRawIndex(lastId, map.getNamedItem("id"));
						buffer.append(String.format("%c%c", TextFragment.MARKER_ISOLATED,
			            	TextFragment.toChar(original.getIndex(id))));
					}
					else if ( pos.getNodeValue().equals("begin") ) {
						id = getRawIndex(lastId, map.getNamedItem("id"));
						buffer.append(String.format("%c%c", TextFragment.MARKER_OPENING,
		            		TextFragment.toChar(original.getIndex(id))));
					}
					else { // Assumes 'end'
						id = getRawIndex(lastId, map.getNamedItem("id"));
						buffer.append(String.format("%c%c", TextFragment.MARKER_CLOSING,
				        	TextFragment.toChar(original.getIndexForClosing(id))));
					}
				}
				break;
			}
		}

		return new TextFragment(buffer.toString(), original.getCodes());
	}

	private int getRawIndex (int lastIndex, Node attr) {
		if ( attr == null ) return ++lastIndex;
		return Integer.valueOf(attr.getNodeValue());
	}
	
}
