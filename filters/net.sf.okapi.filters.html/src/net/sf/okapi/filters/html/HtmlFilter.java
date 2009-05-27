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

package net.sf.okapi.filters.html;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.EndTagType;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.HtmlEncoder;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder.PlaceholderType;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.filters.markupfilter.AbstractBaseMarkupFilter;
import net.sf.okapi.filters.yaml.TaggedFilterConfiguration.RULE_TYPE;

public class HtmlFilter extends AbstractBaseMarkupFilter {

	private static final Logger LOGGER = Logger.getLogger(HtmlFilter.class.getName());

	private StringBuilder bufferedWhitespace;

	/*
	 * HTML whitespace space (U+0020) tab (U+0009) form feed (U+000C) line feed
	 * (U+000A) carriage return (U+000D) zero-width space (U+200B) (IE6 does not
	 * recognize these, they are treated as unprintable characters)
	 */
	private static final String HTML_WHITESPACE_REGEX = "[ \t\r\n\f\u200B]+";
	private static final Pattern HTML_WHITESPACE_PATTERN = Pattern.compile(HTML_WHITESPACE_REGEX);

	public HtmlFilter() {
		super();
		bufferedWhitespace = new StringBuilder();
		setMimeType(MimeTypeMapper.HTML_MIME_TYPE);
		setFilterWriter(createFilterWriter());
		setDefaultConfig(HtmlFilter.class.getResource("defaultConfiguration.yml"));
	}

	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
		list.add(new FilterConfiguration(getName(),
			MimeTypeMapper.HTML_MIME_TYPE,
			getClass().getName(),
			"HTML/XHTML",
			"HTML and XHTML documents",
			"defaultConfiguration.yml"));
		return list;
	}

	/**
	 * Initialize parameters, rule state and parser. Called before processing of
	 * each input.
	 */
	@Override
	protected void startFilter() {
		super.startFilter();
		setPreserveWhitespace(false);
		if (getConfig().collapseWhitespace()) {
			LOGGER.log(Level.FINE,
					"By default the HTML filter will collapse whitespace unless overridden in the configuration");
		}
	}

	@Override
	protected void handleCdataSection(Tag tag) {
		addToDocumentPart(tag.toString());
	}

	@Override
	protected void preProcess(Segment segment) {
		boolean isInsideTextRun = false;
		if (segment instanceof Tag) {
			isInsideTextRun = getConfig().getMainRuleType(((Tag) segment).getName()) == RULE_TYPE.INLINE_ELEMENT;
		}

		// add buffered whitespace to the current translatable text
		if (bufferedWhitespace.length() > 0 && isInsideTextRun) {
			if (canStartNewTextUnit()) {
				startTextUnit(bufferedWhitespace.toString());
			} else {
				addToTextUnit(bufferedWhitespace.toString());
			}
		} else if (bufferedWhitespace.length() > 0) {
			// otherwise add it as non-translatable
			addToDocumentPart(bufferedWhitespace.toString());
		}
		// reset buffer for next pass
		bufferedWhitespace.setLength(0);
		bufferedWhitespace.trimToSize();
	}

	@Override
	protected void handleText(Segment text) {
		// if in excluded state everything is skeleton including text
		if (getRuleState().isExludedState()) {
			addToDocumentPart(text.toString());
			return;
		}

		// check for ignorable whitespace and add it to the skeleton
		if (text.isWhiteSpace() && !isInsideTextRun()) {
			if (bufferedWhitespace.length() <= 0) {
				// buffer the whitespace until we know that we are not inside
				// translatable text.
				bufferedWhitespace.append(text.toString());
			}
			return;
		}

		if (canStartNewTextUnit()) {
			startTextUnit(text.toString());
		} else {
			addToTextUnit(text.toString());
		}
	}

	private String collapseWhitespace(String text) {
		return HTML_WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
	}

	@Override
	protected void handleDocumentPart(Tag tag) {
		addToDocumentPart(tag.toString());
	}

	@Override
	protected void handleStartTag(StartTag startTag) {
		// if in excluded state everything is skeleton including text
		if (getRuleState().isExludedState()) {
			addToDocumentPart(startTag.toString());
			// process these tag types to update parser state
			switch (getConfig().getMainRuleType(startTag.getName())) {
			case EXCLUDED_ELEMENT:
				getRuleState().pushExcludedRule(startTag.getName());
				break;
			case INCLUDED_ELEMENT:
				getRuleState().pushIncludedRule(startTag.getName());
				break;
			case PRESERVE_WHITESPACE:
				getRuleState().pushPreserverWhitespaceRule(startTag.getName());
				break;
			}
			return;
		}

		List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;
		propertyTextUnitPlaceholders = createPropertyTextUnitPlaceholders(startTag);

		switch (getConfig().getMainRuleType(startTag.getName())) {
		case INLINE_ELEMENT:
			if (canStartNewTextUnit()) {
				startTextUnit();
			}
			addCodeToCurrentTextUnit(startTag);
			break;
		case ATTRIBUTES_ONLY:
			// we assume we have already ended any (non-complex) TextUnit in
			// the main while loop in BaseMarkupFilter
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		case GROUP_ELEMENT:
			getRuleState().pushGroupRule(startTag.getName());
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		case EXCLUDED_ELEMENT:
			getRuleState().pushExcludedRule(startTag.getName());
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		case INCLUDED_ELEMENT:
			getRuleState().pushIncludedRule(startTag.getName());
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		case TEXT_UNIT_ELEMENT:
			getRuleState().pushTextUnitRule(startTag.getName());
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		case PRESERVE_WHITESPACE:
			getRuleState().pushPreserverWhitespaceRule(startTag.getName());
			setPreserveWhitespace(getRuleState().isPreserveWhitespaceState());
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
			break;
		default:
			handleAttributesThatAppearAnywhere(propertyTextUnitPlaceholders, startTag);
		}
	}

	/*
	 * catch tags which are not listed in the config but have attributes that
	 * require processing
	 */
	private void handleAttributesThatAppearAnywhere(List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders,
			StartTag tag) {
		switch (getConfig().getMainRuleType(tag.getName())) {
		case TEXT_UNIT_ELEMENT:
			if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
				startTextUnit(new GenericSkeleton(tag.toString()), propertyTextUnitPlaceholders);
			} else {
				startTextUnit(new GenericSkeleton(tag.toString()));
			}
			break;
		case GROUP_ELEMENT:
			if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
				startGroup(new GenericSkeleton(tag.toString()), tag.getName(), propertyTextUnitPlaceholders);
			} else {
				// no attributes that need processing - just treat as skeleton
				startGroup(new GenericSkeleton(tag.toString()));
			}
			break;
		default:
			if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
				startDocumentPart(tag.toString(), tag.getName(), propertyTextUnitPlaceholders);
				endDocumentPart();
			} else {
				// no attributes that needs processing - just treat as skeleton
				addToDocumentPart(tag.toString());
			}

			break;
		}
	}

	@Override
	protected void handleEndTag(EndTag endTag) {
		// if in excluded state everything is skeleton including text
		if (getRuleState().isExludedState()) {
			addToDocumentPart(endTag.toString());
			// process these tag types to update parser state
			switch (getConfig().getMainRuleType(endTag.getName())) {
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

		switch (getConfig().getMainRuleType(endTag.getName())) {
		case INLINE_ELEMENT:
			if (canStartNewTextUnit()) {
				startTextUnit();
			}
			addCodeToCurrentTextUnit(endTag);
			break;
		case GROUP_ELEMENT:
			getRuleState().popGroupRule();
			endGroup(new GenericSkeleton(endTag.toString()));
			break;
		case EXCLUDED_ELEMENT:
			getRuleState().popExcludedIncludedRule();
			addToDocumentPart(endTag.toString());
			break;
		case INCLUDED_ELEMENT:
			getRuleState().popExcludedIncludedRule();
			addToDocumentPart(endTag.toString());
			break;
		case TEXT_UNIT_ELEMENT:
			getRuleState().popTextUnitRule();
			endTextUnit(new GenericSkeleton(endTag.toString()));
			break;
		case PRESERVE_WHITESPACE:
			getRuleState().popPreserverWhitespaceRule();
			setPreserveWhitespace(getRuleState().isPreserveWhitespaceState());

			addToDocumentPart(endTag.toString());
			break;
		default:
			addToDocumentPart(endTag.toString());
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.sf.okapi.common.markupfilter.BaseMarkupFilter#handleComment(net.
	 * htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleComment(Tag tag) {
		if (!isInsideTextRun()) {
			handleDocumentPart(tag);
		} else {
			addCodeToCurrentTextUnit(tag);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleDocTypeDeclaration
	 * (net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleDocTypeDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleMarkupDeclaration
	 * (net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleMarkupDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleProcessingInstruction
	 * (net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleProcessingInstruction(Tag tag) {
		if (!isInsideTextRun()) {
			handleDocumentPart(tag);
		} else {
			addCodeToCurrentTextUnit(tag);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleServerCommon(
	 * net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleServerCommon(Tag tag) {
		handleDocumentPart(tag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleServerCommonEscaped
	 * (net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleServerCommonEscaped(Tag tag) {
		handleDocumentPart(tag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#handleXmlDeclaration
	 * (net.htmlparser.jericho.Tag)
	 */
	@Override
	protected void handleXmlDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.filters.IFilter#getName()
	 */
	public String getName() {
		return "okf_html"; //$NON-NLS-1$
	}

	protected PropertyTextUnitPlaceholder createPropertyTextUnitPlaceholder(PlaceholderType type, String name,
			String value, Tag tag, Attribute attribute) {

		// Test for charset in meta tag - we need to isolate the position of
		// charset within the attribute value
		// i.e., content="text/html; charset=ISO-2022-JP"
		if (isMetaCharset(name, value, tag) && value.indexOf("charset=") != -1) {
			// offset of attribute
			int mainStartPos = attribute.getBegin() - tag.getBegin();
			int mainEndPos = attribute.getEnd() - tag.getBegin();

			// adjust offset of value of the attribute
			int charsetValueOffset = value.lastIndexOf("charset=") + "charset=".length();

			int valueStartPos = (attribute.getValueSegment().getBegin() + charsetValueOffset) - tag.getBegin();
			int valueEndPos = attribute.getValueSegment().getEnd() - tag.getBegin();
			// get the charset value (encoding)
			value = tag.toString().substring(valueStartPos, valueEndPos);
			return new PropertyTextUnitPlaceholder(type, normalizeAttributeName(name, value, tag), value, mainStartPos,
					mainEndPos, valueStartPos, valueEndPos);
		}

		return super.createPropertyTextUnitPlaceholder(type, name, normalizeHtmlText(value, true), tag, attribute);
	}

	@Override
	protected Event postProcessText(Event textUnit) {
		TextFragment text = ((TextUnit) textUnit.getResource()).getSourceContent();
		text.setCodedText(normalizeHtmlText(text.getCodedText(), false));
		return textUnit;
	}

	private String normalizeHtmlText(String text, boolean insideAttribute) {
		// convert all entities to Unicode
		String decodedValue = CharacterReference.decode(text, insideAttribute);

		if (!getRuleState().isPreserveWhitespaceState() && getConfig().collapseWhitespace()) {
			decodedValue = collapseWhitespace(decodedValue);
			decodedValue = decodedValue.trim();
		}
		decodedValue = Util.normalizeNewlines(decodedValue);

		return decodedValue;
	}

	@Override
	protected void addCodeToCurrentTextUnit(Tag tag) {
		List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;
		String literalTag = tag.toString();
		TextFragment.TagType codeType;

		// start tag or empty tag
		if (tag.getTagType() == StartTagType.NORMAL || tag.getTagType() == StartTagType.UNREGISTERED) {
			StartTag startTag = ((StartTag) tag);

			// is this an empty tag?
			if (startTag.isSyntacticalEmptyElementTag()) {
				codeType = TextFragment.TagType.PLACEHOLDER;
			} else if (startTag.isEndTagRequired()) {
				codeType = TextFragment.TagType.OPENING;				
			} else {
				codeType = TextFragment.TagType.PLACEHOLDER;
			}

			// create a list of Property or Text placeholders for this tag
			// If this list is empty we know that there are no attributes that
			// need special processing
			propertyTextUnitPlaceholders = null;

			propertyTextUnitPlaceholders = createPropertyTextUnitPlaceholders(startTag);
			if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) {
				// add code and process actionable attributes
				addToTextUnit(codeType, literalTag, startTag.getName(), propertyTextUnitPlaceholders);
			} else {
				// no actionable attributes, just add the code as-is
				addToTextUnit(codeType, literalTag, startTag.getName());
			}
		} else { // end or unknown tag
			if (tag.getTagType() == EndTagType.NORMAL || tag.getTagType() == EndTagType.UNREGISTERED) {
				codeType = TextFragment.TagType.CLOSING;
			} else {
				codeType = TextFragment.TagType.PLACEHOLDER;
			}
			addToTextUnit(codeType, literalTag, tag.getName());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.markupfilter.BaseMarkupFilter#normalizeName(java.
	 * lang.String)
	 */
	@Override
	protected String normalizeAttributeName(String attrName, String attrValue, Tag tag) {
		// normalize values for HTML
		String normalizedName = attrName;

		// <meta http-equiv="Content-Type"
		// content="text/html; charset=ISO-2022-JP">
		if (isMetaCharset(attrName, attrValue, tag)) {
			normalizedName = Property.ENCODING;
			return normalizedName;
		}

		// <meta http-equiv="Content-Language" content="en"
		if (tag.getName().equals("meta") && attrName.equals(HtmlEncoder.CONTENT)) {
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("http-equiv") != null) {
				if (st.getAttributeValue("http-equiv").equals("Content-Language")) {
					normalizedName = Property.LANGUAGE;
					return normalizedName;
				}
			}
		}

		// <x lang="en"> or <x xml:lang="en">
		if (attrName.equals("lang") || attrName.equals("xml:lang")) {
			normalizedName = Property.LANGUAGE;
		}

		return normalizedName;
	}

	private boolean isMetaCharset(String attrName, String attrValue, Tag tag) {
		if (tag.getName().equals("meta") && attrName.equals(HtmlEncoder.CONTENT)) {
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("http-equiv") != null && st.getAttributeValue("content") != null) {
				if (st.getAttributeValue("http-equiv").equals("Content-Type")
						&& st.getAttributeValue("content").contains("charset=")) {
					return true;
				}
			}
		}
		return false;
	}

}
