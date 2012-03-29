/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xini;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.filters.xini.jaxb.Empty;
import net.sf.okapi.filters.xini.jaxb.EndPlaceHolder;
import net.sf.okapi.filters.xini.jaxb.ObjectFactory;
import net.sf.okapi.filters.xini.jaxb.PlaceHolder;
import net.sf.okapi.filters.xini.jaxb.PlaceHolderType;
import net.sf.okapi.filters.xini.jaxb.StartPlaceHolder;
import net.sf.okapi.filters.xini.jaxb.TextContent;

public class InlineCodeTransformer {
    private static final Logger LOGGER = Logger.getLogger(InlineCodeTransformer.class.getName());
	private static final boolean TARGET_IS_TKIT = true;
	private static final boolean TARGET_IS_FILTER = false;
	private static final Map<String, String> tagType;
    static {
        Map<String, String> tagTypes = new HashMap<String, String>();
        tagTypes.put("b", Code.TYPE_BOLD);
        tagTypes.put("i", Code.TYPE_ITALIC);
        tagTypes.put("u", Code.TYPE_UNDERLINED);
        tagTypes.put("sup", InlineCodeTransformer.CODE_TYPE_SUP);
        tagTypes.put("sub", InlineCodeTransformer.CODE_TYPE_SUB);
        tagTypes.put("br", Code.TYPE_LB);
        tagType = Collections.unmodifiableMap(tagTypes);
    }
	private static ObjectFactory objectFactory = new ObjectFactory();
	public static final String CODE_TYPE_SUP = "superscript";
	public static final String CODE_TYPE_SUB = "subscript";
	// Use negative IDs for formatting codes so they don't overlap with positive placeholder IDs 
	private int id;

	public InlineCodeTransformer(){
		id = -2;
	}

	public ArrayList<Serializable> codesToJAXBForFilter(String codedText, List<Code> codes) {
		return codesToJAXB(codedText, codes, TARGET_IS_FILTER);
	}

	public ArrayList<Serializable> codesToJAXBForTKit(String codedText, List<Code> codes) {
		return codesToJAXB(codedText, codes, TARGET_IS_TKIT);
	}

	public ArrayList<Serializable> codesToJAXB(String codedText, List<Code> codes, boolean targetIsRainbowkit) {

		ArrayList<Serializable> parts = new ArrayList<Serializable>();
		StringBuilder tempString = new StringBuilder();

		for (int charIndex = 0; charIndex < codedText.length(); charIndex++) {

			char chr = codedText.charAt(charIndex);

			if (!TextFragment.isMarker(chr)) {

				// This is a regular character
				tempString.append(chr);
			}

			else {

				// This is a code
				int codePoint = codedText.codePointAt(charIndex);
				Integer codeIndex = TextFragment.toIndex(codedText.charAt(++charIndex));
				Code code = codes.get(codeIndex);
				boolean codeIsIsolated = false;

				// Save last part of the text that had no codes
				if (tempString.length() > 0)
					parts.add(tempString.toString());
				tempString = new StringBuilder();

				switch (codePoint) {
				case TextFragment.MARKER_OPENING:

					Integer endMarkerIndex = findEndMark(codes, code, codedText, charIndex);
					String innerCodedText = null;

					if (endMarkerIndex != null) {

						innerCodedText = codedText.substring(charIndex + 1, endMarkerIndex - 1);
						charIndex = endMarkerIndex;
					} else {
						codeIsIsolated = true;
					}

					parts.add(codeToXMLObject(code, codes, innerCodedText, codeIsIsolated, targetIsRainbowkit));
					break;

				case TextFragment.MARKER_CLOSING:

					// This closing code does not have it's corresponding
					// opening code in the same segment
					parts.add(codeToXMLObject(code, codes, null, true, targetIsRainbowkit));
					break;

				case TextFragment.MARKER_ISOLATED:

					parts.add(codeToXMLObject(code, codes, null, true, targetIsRainbowkit));
					break;
				}
			}
		}

		if (tempString.length() > 0)
			parts.add(tempString.toString());
		return parts;
	}

	private Integer findEndMark(List<Code> codes, Code code, String codedText,
			int startCharIndex) {

		for (int charIndex = startCharIndex; charIndex < codedText.length(); charIndex++) {

			int codePoint = codedText.codePointAt(charIndex);

			if (codePoint == TextFragment.MARKER_CLOSING
					&& codedText.length() > charIndex + 1) {

				int endCodeIndex = TextFragment.toIndex(codedText
						.charAt(++charIndex));
				Code endCode = codes.get(endCodeIndex);

				try {
					if (endCode.getType().equals(code.getType()) && endCode.getId() == code.getId()) {
						if (codePoint == TextFragment.MARKER_CLOSING)
							return charIndex;
					}
				}
				catch (NullPointerException e) {
					// Codes without Type cannot be compared
				}
			}
		}

		// No closing marker found
		return null;
	}

	private Serializable codeToXMLObject(Code code, List<Code> codes,
			String innerCodedText, boolean codeIsIsolated, boolean targetIsRainbowkit) {
		if (!targetIsRainbowkit)
			return codeToXMLObjectForXINIFilter(code, codes, innerCodedText, codeIsIsolated);
		else
			return codeToXMLObjectForRainbowKit(code, codes, innerCodedText, codeIsIsolated);
	}

	private Serializable codeToXMLObjectForXINIFilter(Code code, List<Code> codes,
			String innerCodedText, boolean codeIsIsolated) {

		String codeType = code.getType();
		String phTypeAttribute = null;

		if ("null".equals(codeType))
			codeType = null;
		// See comment in processInlineTagForFilter()
		// Restore original placeholder type from this string
		else if (codeType.equals("x-sph") || codeType.equals("x-eph"))
			phTypeAttribute = null;
		else if (codeType.startsWith("x-sph-") || codeType.startsWith("x-eph-"))
			phTypeAttribute = codeType.substring(6);
		else if (codeType.startsWith("x-"))
			phTypeAttribute = codeType.substring(2);

		Serializable inlineElement;

		if (!codeIsIsolated) {

			ArrayList<Serializable> innerXML = null;
			if (innerCodedText != null && !innerCodedText.isEmpty()) {
				innerXML = codesToJAXB(innerCodedText, codes, TARGET_IS_FILTER);
			}

			TextContent tc = new TextContent();
			if (innerXML != null)
				tc.getContent().addAll(innerXML);

			if(Code.TYPE_BOLD.equals(codeType)) {
				inlineElement = objectFactory.createTextContentB(tc);
			}
			else if (Code.TYPE_ITALIC.equals(codeType)) {
				inlineElement = objectFactory.createTextContentI(tc);
			}
			else if (Code.TYPE_UNDERLINED.equals(codeType)) {
				inlineElement = objectFactory.createTextContentU(tc);
			}
			else if (CODE_TYPE_SUP.equals(codeType)) {
				inlineElement = objectFactory.createTextContentSup(tc);
			}
			else if (CODE_TYPE_SUB.equals(codeType)) {
				inlineElement = objectFactory.createTextContentSub(tc);
			}
			else {
				// use g/x-style placeholder
				PlaceHolder ph = new PlaceHolder();
				ph.setID(code.getId());
				if (phTypeAttribute != null)
					ph.setType(PlaceHolderType.fromValue(phTypeAttribute));

				ph.getContent().addAll(tc.getContent());

				inlineElement = objectFactory.createTextContentPh(ph);
			}
		}
		else if (codeIsIsolated
				&& codeType == Code.TYPE_LB) {
			inlineElement = objectFactory.createTextContentBr(new Empty());
		}

		else if (codeIsIsolated
				&& code.getTagType() == TagType.PLACEHOLDER) {
			// use g/x-style placeholder
			PlaceHolder ph = new PlaceHolder();
			ph.setID(code.getId());
			if (phTypeAttribute != null)
				ph.setType(PlaceHolderType.fromValue(phTypeAttribute));

			inlineElement = objectFactory.createTextContentPh(ph);
		}
		else if (code.getTagType() == TagType.OPENING) {
			// use bpt style placeholder
			StartPlaceHolder sph = new StartPlaceHolder();
			sph.setID(code.getId());
			if (phTypeAttribute != null){
				sph.setType(PlaceHolderType.fromValue(phTypeAttribute));
			}
			inlineElement = objectFactory.createTextContentSph(sph);
		}
		else {
			// use ept-style placeholder
			EndPlaceHolder eph = new EndPlaceHolder();
			eph.setID(code.getId());
			//eph.setType(PlaceHolderType.PH);
			if (phTypeAttribute != null){
				eph.setType(PlaceHolderType.fromValue(phTypeAttribute));
			}
			inlineElement = objectFactory.createTextContentEph(eph);
		}

		return inlineElement;
	}

	private Serializable codeToXMLObjectForRainbowKit(Code code, List<Code> codes,
			String innerCodedText, boolean codeIsIsolated) {

		Serializable phelement;

		if (!codeIsIsolated || codeIsIsolated
				&& code.getTagType() == TagType.PLACEHOLDER) {
			// use g/x-style placeholder
			PlaceHolder ph = new PlaceHolder();
			ph.setID(code.getId());
			ph.setType(PlaceHolderType.PH);

			if (innerCodedText != null && !innerCodedText.isEmpty()) {
				ph.getContent().addAll(
						codesToJAXB(innerCodedText, codes, TARGET_IS_TKIT));
			}

			phelement = objectFactory.createTextContentPh(ph);
		} else if (code.getTagType() == TagType.OPENING) {
			// use bpt style placeholder
			StartPlaceHolder sph = new StartPlaceHolder();
			sph.setID(code.getId());
			sph.setType(PlaceHolderType.PH);
			phelement = objectFactory.createTextContentSph(sph);
		} else {
			// use ept-style placeholder
			EndPlaceHolder eph = new EndPlaceHolder();
			eph.setID(code.getId());
			eph.setType(PlaceHolderType.PH);
			phelement = objectFactory.createTextContentEph(eph);
		}

		return phelement;
	}

	public TextFragment serializeTextPartsForFilter(List<Serializable> parts) {
		return serializeTextParts(parts, TARGET_IS_FILTER);
	}

	public TextFragment serializeTextPartsForTKit(List<Serializable> parts) {
		return serializeTextParts(parts, TARGET_IS_TKIT);
	}

	public TextFragment serializeTextParts(List<Serializable> parts, boolean targetIsTKit) {
		TextFragment fragment = new TextFragment();
		for (Serializable part : parts) {

			if (part instanceof String) {
				fragment.append((String) part);
			}
			else if (part instanceof JAXBElement<?>) {
				fragment.insert(-1, processInlineTag(part, targetIsTKit), true);
			}

		}
		return fragment;
	}

	/**
	 * @param part
	 * @return
	 */
	private TextFragment processInlineTag(Serializable part, boolean targetIsTKit) {
		if(targetIsTKit)
			return processInlineTagForTKit(part);
		else
			return processInlineTagForFilter(part);
	}

	@SuppressWarnings("unchecked")
	private TextFragment processInlineTagForFilter(Serializable part) {
		TextFragment fragment = new TextFragment();

		JAXBElement<?> jaxbEl = (JAXBElement<?>) part;

		Code code;
		List<Serializable> content = null;
		if (jaxbEl.getValue() instanceof PlaceHolder) {
			JAXBElement<PlaceHolder> ph = (JAXBElement<PlaceHolder>) part;
			content = ph.getValue().getContent();

			if (content == null || content.isEmpty()) {
				code = new Code(TagType.PLACEHOLDER, null);
				code.setId(ph.getValue().getID());
				// preserve type attribute from XINI
				PlaceHolderType phTypeValue = ph.getValue().getType();
				if (phTypeValue != null)
					code.setType("x-" + phTypeValue.value());
				fragment.append(code);
			}
			else {
				code = new Code(TagType.OPENING, null);
				code.setId(ph.getValue().getID());
				// preserve type attribute from XINI
				PlaceHolderType phTypeValue = ph.getValue().getType();
				if (phTypeValue != null)
					code.setType("x-" + phTypeValue.value());

				fragment.append(code);

				TextFragment innerText = serializeTextParts(content, TARGET_IS_FILTER);
				fragment.insert(-1, innerText, true);

				code = new Code(TagType.CLOSING, null);
				code.setId(ph.getValue().getID());
				if (phTypeValue != null)
					code.setType("x-" + phTypeValue.value());
				fragment.append(code);
			}
		}
		else if (jaxbEl.getValue() instanceof StartPlaceHolder) {
			JAXBElement<StartPlaceHolder> sph = (JAXBElement<StartPlaceHolder>) part;
			code = new Code(TagType.OPENING, null);
			code.setId(sph.getValue().getID());
			// preserve type attribute from XINI
			PlaceHolderType sphTypeValue = sph.getValue().getType();
			// prevent textFragment.append() from assigning new IDs to codes in the same Segment, which have the same type, but different IDs
			String sphCodeType = "x-sph";
			if (sphTypeValue != null)
				sphCodeType +=  "-" + sphTypeValue.value();
			code.setType(sphCodeType);

			fragment.append(code);
		}
		else if (jaxbEl.getValue() instanceof EndPlaceHolder) {
			JAXBElement<EndPlaceHolder> eph = (JAXBElement<EndPlaceHolder>) part;
			code = new Code(TagType.CLOSING, null);
			code.setId(eph.getValue().getID());
			// preserve type attribute from XINI
			PlaceHolderType ephTypeValue = eph.getValue().getType();
			// prevent textFragment.append() from assigning new IDs to codes in the same Segment, which have the same type, but different IDs
			String ephCodeType = "x-eph";
			if (ephTypeValue != null)
				ephCodeType +=  "-" + ephTypeValue.value();
			code.setType(ephCodeType);

			fragment.append(code);
		}
		else if (jaxbEl.getValue() instanceof Empty) {
			// <br/> from XINI
			code = new Code(TagType.PLACEHOLDER, null);
			code.setType(Code.TYPE_LB);
			code.setId(getNextDecrementedID());
			fragment.append(code);
		}
		else if (jaxbEl.getValue() instanceof TextContent) {
			JAXBElement<TextContent> inlineElem = (JAXBElement<TextContent>) part;
			content = inlineElem.getValue().getContent();

			if (content == null || content.isEmpty()) {
				code = new Code(TagType.PLACEHOLDER, null);
				code.setId(getNextDecrementedID());
				changeCodeType(code, inlineElem);
				fragment.append(code);
			}
			else {
				code = new Code(TagType.OPENING, null);
				int codeId = getNextDecrementedID();
				code.setId(codeId);
				changeCodeType(code, inlineElem);
				fragment.append(code);

				TextFragment innerText = serializeTextParts(content, TARGET_IS_FILTER);
				fragment.insert(-1, innerText, true);

				code = new Code(TagType.CLOSING, null);
				code.setId(codeId);
				changeCodeType(code, inlineElem);
				fragment.append(code);
			}
		}
		else {
			throw new OkapiBadFilterInputException("Unknown inline element: " + part);
		}

		return fragment;
	}

	private void changeCodeType(Code code, JAXBElement<TextContent> inlineElem) {
		String elementName = inlineElem.getName().getLocalPart();
		code.setType(tagType.get(elementName));
	}

	@SuppressWarnings("unchecked")
	private TextFragment processInlineTagForTKit(Serializable part) {
		TextFragment fragment = new TextFragment();

		JAXBElement<?> jaxbEl = (JAXBElement<?>) part;

		Code code;
		List<Serializable> content = null;
		if (jaxbEl.getValue() instanceof PlaceHolder) {
			JAXBElement<PlaceHolder> ph = (JAXBElement<PlaceHolder>) part;
			content = ph.getValue().getContent();

			if (content == null || content.isEmpty()) {
				code = new Code(TagType.PLACEHOLDER, null);
				code.setId(ph.getValue().getID());
				fragment.append(code);
			}
			else {
				code = new Code(TagType.OPENING, null);
				code.setId(ph.getValue().getID());
				fragment.append(code);

				TextFragment innerText = serializeTextParts(content, TARGET_IS_TKIT);
				fragment.append(innerText);

				code = new Code(TagType.CLOSING, null);
				code.setId(ph.getValue().getID());
				fragment.append(code);
			}
		}
		else if (jaxbEl.getValue() instanceof StartPlaceHolder) {
			JAXBElement<StartPlaceHolder> sph = (JAXBElement<StartPlaceHolder>) part;
			code = new Code(TagType.OPENING, null);
			code.setId(sph.getValue().getID());
			fragment.append(code);
		}
		else if (jaxbEl.getValue() instanceof EndPlaceHolder) {
			JAXBElement<EndPlaceHolder> eph = (JAXBElement<EndPlaceHolder>) part;
			code = new Code(TagType.CLOSING, null);
			code.setId(eph.getValue().getID());
			fragment.append(code);
		}
		else if (jaxbEl.getValue() instanceof Empty) {
			LOGGER.warning("Inline element " + jaxbEl.getName() + " will be ignored");

		}
		else if (jaxbEl.getValue() instanceof TextContent) {
			LOGGER.warning("Inline element " + jaxbEl.getName() + " will be ignored");
			JAXBElement<TextContent> txtC = (JAXBElement<TextContent>) part;
			TextFragment innerText = serializeTextParts(txtC.getValue().getContent(), TARGET_IS_TKIT);
			fragment.append(innerText);
		}
		else {
			throw new OkapiBadFilterInputException("Unknown inline element: " + part);
		}

		return fragment;
	}

	private int getNextDecrementedID(){
		return id--;
	}

}
