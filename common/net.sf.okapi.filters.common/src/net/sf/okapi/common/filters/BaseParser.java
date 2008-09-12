/*===========================================================================*/
/* Copyright (C) 2008 Asgeir Frimannsson, Jim Hargrave, Yves Savourel        */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.filters;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.Group;
import net.sf.okapi.common.resource.IContainable;
import net.sf.okapi.common.resource.SkeletonUnit;
import net.sf.okapi.common.resource.TextUnit;

/**
 * Abstract helper class for filter writers.
 */
public abstract class BaseParser implements IParser {
	private int groupId = 0;
	private int textUnitId = 0;
	private int skeleltonUnitId = 0;
	private TextUnit textUnit;
	private SkeletonUnit skeletonUnit;
	private Group group;
	private IContainable finalizedToken;
	private ParserTokenType finalizedTokenType;
	private boolean finishedToken = false;
	private boolean finishedParsing = false;
	private boolean cancel = false;

	public BaseParser() {
	}

	protected TextUnit getTextUnit() {
		return textUnit;
	}

	protected boolean isTextUnitEmtpy() {
		return (textUnit == null || textUnit.isEmpty());
	}

	protected SkeletonUnit getSkeletonUnit() {
		return skeletonUnit;
	}

	protected boolean isSkeletonUnitEmtpy() {
		return (skeletonUnit == null || skeletonUnit.isEmpty());
	}

	protected Group getGroup() {
		return group;
	}

	protected boolean isGroupEmtpy() {
		return (group == null || group.isEmpty());
	}

	protected int getGroupId() {
		return groupId;
	}

	protected int getTextUnitId() {
		return textUnitId;
	}

	protected int getSkeleltonUnitId() {
		return skeleltonUnitId;
	}

	protected IContainable getFinalizedToken() {
		return finalizedToken;
	}

	protected ParserTokenType getFinalizedTokenType() {
		return finalizedTokenType;
	}

	protected void reset() {
		finishedToken = false;
		cancel = false;
	}

	private void finalizeToken(TextUnit textUnit) {
		finishedToken = true;
		finalizedToken = textUnit;
		finalizedTokenType = ParserTokenType.TRANSUNIT;
		this.textUnit = null;
	}

	private void finalizeToken(SkeletonUnit skeletonUnit) {
		finishedToken = true;
		finalizedToken = skeletonUnit;
		finalizedTokenType = ParserTokenType.SKELETON;
		this.skeletonUnit = null;
	}

	private void finalizeToken(Group group) {
		finishedToken = true;
		finalizedToken = group;
		finalizedTokenType = ParserTokenType.ENDGROUP;
		this.group = null;
	}

	protected void finalizeCurrentToken() {
		finishedToken = true;
		if (!isGroupEmtpy()) {			
			finalizedToken = getGroup();
			// since we are starting the next token we assume Group is finished
			// and set ENDGROUP as the token type, STARTGROUP should have been
			// returned earlier.
			finalizedTokenType = ParserTokenType.ENDGROUP;
			group = null;
		} else if (!isSkeletonUnitEmtpy()) {
			// should be only one instance of textUnit, skeletonUnit
			assert (isTextUnitEmtpy());
			assert (isGroupEmtpy());
			finalizedToken = getSkeletonUnit();
			finalizedTokenType = ParserTokenType.SKELETON;
			skeletonUnit = null;
		} else if (!isTextUnitEmtpy()) {
			// should be only one instance of textUnit, skeletonUnit
			assert (isSkeletonUnitEmtpy());
			assert (isGroupEmtpy());
			finalizedToken = getTextUnit();
			finalizedTokenType = ParserTokenType.TRANSUNIT;
			textUnit = null;
		} else {
			// TODO: throw exception
		}
	}

	protected boolean isFinishedToken() {
		return finishedToken;
	}

	protected boolean isFinishedParsing() {
		return finishedParsing;
	}

	protected void setFinishedParsing(boolean finishedParsing) {
		this.finishedParsing = finishedParsing;
	}

	/**
	 * Append to the current SkeletonUnit. If skeletonUnit already created
	 * ignore offset and increase length.
	 * 
	 * @param offset
	 * @param length
	 */
	protected void appendToSkeletonUnit(int offset, int length) {
		if (skeletonUnit == null) {
			createSkeletonUnit(offset, length);
		} else {
			skeletonUnit.addToLength(length);
		}
		if (textUnit != null && !textUnit.isEmpty()) {
			finalizeToken(textUnit);
		}
	}

	protected void appendToSkeletonUnit(String skeleton) {
		if (skeletonUnit == null) {
			createSkeletonUnit(skeleton);
		} else {
			skeletonUnit.appendData(skeleton);
		}
		if (textUnit != null && !textUnit.isEmpty()) {
			finalizeToken(textUnit);
		}
	}

	protected void appendToSkeletonUnit(String skeleton, int offset, int length) {
		if (skeletonUnit == null) {
			createSkeletonUnit(skeleton, offset, length);
		} else {
			skeletonUnit.appendData(skeleton);
			skeletonUnit.addToLength(length);
		}
		if (textUnit != null && !textUnit.isEmpty()) {
			finalizeToken(textUnit);
		}
	}

	protected void appendToTextUnit(Code code) {
		if (textUnit == null) {
			createTextUnit(code);
		} else {
			textUnit.getSourceContent().append(code.getTagType(), code.getType(), code.getData());
		}
		if (skeletonUnit != null && !skeletonUnit.isEmpty()) {
			finalizeToken(skeletonUnit);
		}
	}

	protected void appendToTextUnit(String text) {
		if (textUnit == null) {
			createTextUnit(text);
		} else {
			textUnit.getSourceContent().append(text);
		}
		if (skeletonUnit != null && !skeletonUnit.isEmpty()) {
			finalizeToken(skeletonUnit);
		}
	}

	protected void appendToTextUnit(TextUnit child) {
		if (textUnit == null) {
			createTextUnit(child);
		} else {
			textUnit.addChild(child);
		}
		if (skeletonUnit != null && !skeletonUnit.isEmpty()) {
			finalizeToken(skeletonUnit);
		}
	}
	
	protected void appendToGroup(TextUnit textUnit) {
		if (group == null) {
			createGroup(textUnit);
		} else {
			group.add(textUnit);
		}
		if (skeletonUnit != null && !skeletonUnit.isEmpty()) {
			finalizeToken(skeletonUnit);
		}
	}
	
	protected void appendToGroup(SkeletonUnit skeletonUnit) {
		if (group == null) {
			createGroup(skeletonUnit);
		} else {
			group.add(skeletonUnit);
		}
		if (textUnit != null && !textUnit.isEmpty()) {
			finalizeToken(textUnit);
		}
	}

	public void cancel() {
		finalizeCurrentToken();
		setFinishedParsing(true);
		cancel = true;
	}

	protected boolean isCanceled() {
		return cancel;
	}

	private void createSkeletonUnit(int offset, int length) {
		skeletonUnit = new SkeletonUnit(String.format("s%d", ++skeleltonUnitId), offset, length);
	}

	private void createSkeletonUnit(String skeleton) {
		skeletonUnit = new SkeletonUnit(String.format("s%d", ++skeleltonUnitId), skeleton);
	}

	private void createSkeletonUnit(String skeleton, int offset, int length) {
		skeletonUnit = new SkeletonUnit(String.format("s%d", ++skeleltonUnitId), offset, length);
		skeletonUnit.setData(skeleton);
	}

	private void createTextUnit(Code code) {
		textUnit = new TextUnit();
		textUnit.setID(String.format("s%d", ++textUnitId));
		textUnit.getSource().getContent().append(code.getTagType(), code.getType(), code.getData());
	}

	private void createTextUnit(String text) {
		textUnit = new TextUnit(String.format("s%d", ++textUnitId), text);
	}

	private void createTextUnit(TextUnit child) {
		textUnit = new TextUnit();
		textUnit.setID(String.format("s%d", ++textUnitId));
		child.setParent(textUnit);
		textUnit.addChild(child);
	}
	
	private void createGroup(TextUnit textUnit) {
		group = new Group();
		group.setID(String.format("s%d", ++groupId));
		group.add(textUnit);
	}
	
	private void createGroup(SkeletonUnit skeletonUnit) {
		group = new Group();
		group.setID(String.format("s%d", ++groupId));
		group.add(skeletonUnit);
	}
	
	private void createGroup(Group group) {
		group = new Group();
		group.setID(String.format("s%d", ++groupId));
		group.add(group);
	}
	
	protected void createGroup() {
		group = new Group();
		group.setID(String.format("s%d", ++groupId));		
	}
}
