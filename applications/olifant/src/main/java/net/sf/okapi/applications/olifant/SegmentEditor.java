/*===========================================================================
  Copyright (C) 2011-2012 by the Okapi Framework contributors
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

package net.sf.okapi.applications.olifant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.ui.editor.TextOptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

class SegmentEditor {

	private static final Pattern SHORTCODES = Pattern.compile("\\<[/be]?(\\d+?)/?\\>");
	private static final Pattern REALCODES = Pattern.compile("<(bpt|ept|ph|it|ut).*?>(.*?)</\\1>");

	private final ISegmentEditorUser caller;
	private final TextStyle codeStyle;

	private int column = -1;
	private StyledText edit;
	private boolean modified;
	private boolean fullCodesMode;
	private Pattern currentCodes;
	private String codesAsString;
	private TextOptions textOptions;
	private Color bgColor;

	public SegmentEditor (Composite parent,
		int flags,
		ISegmentEditorUser p_caller)
	{
		this(parent, flags, p_caller, null);
	}
	
	public SegmentEditor (Composite parent,
		int flags,
		ISegmentEditorUser p_caller,
		GridData gridData)
	{
		if ( flags < 0 ) { // Use the default styles if requested
			flags = SWT.WRAP | SWT.V_SCROLL | SWT.BORDER;
		}
		caller = p_caller;
		
		bgColor = new Color(parent.getDisplay(), 255, 204, 153); //204, 255, 204);
		codeStyle = new TextStyle();
		codeStyle.background = bgColor;
		//codeStyle.borderStyle = SWT.BORDER_DASH;
		
		edit = new StyledText(parent, flags);
		if ( gridData == null ) {
			gridData = new GridData(GridData.FILL_BOTH);
		}
		edit.setLayoutData(gridData);
		
		edit.setMargins(2, 2, 2, 2);

		// Set some keys
		edit.setKeyBinding(SWT.CTRL|'a', ST.SELECT_ALL);
//		// Disable Cut/Copy/Paste commands to override them
//		edit.setKeyBinding(SWT.CTRL|'c', SWT.NULL);
//		edit.setKeyBinding(SWT.CTRL|'v', SWT.NULL);
//		edit.setKeyBinding(SWT.SHIFT|SWT.DEL, SWT.NULL);
//		edit.setKeyBinding(SWT.SHIFT|SWT.INSERT, SWT.NULL);
		
		// Create a copy of the default text field options for the source
		textOptions = new TextOptions(parent.getDisplay(), edit, 3);
		textOptions.applyTo(edit);
		
		fullCodesMode = false;
		currentCodes = SHORTCODES;
		
		edit.addExtendedModifyListener(new ExtendedModifyListener() {
			@Override
			public void modifyText(ExtendedModifyEvent event) {
				String text = edit.getText();
		    	  java.util.List<StyleRange> ranges = new java.util.ArrayList<StyleRange>();
		    	  Matcher m = currentCodes.matcher(text);
		    	  while ( m.find() ) {
					StyleRange sr = new StyleRange(codeStyle);
					sr.start = m.start();
					sr.length = m.end()-m.start();
					ranges.add(sr);
		    	  }		    	  
		    	  if ( !ranges.isEmpty() ) {
		    		  edit.replaceStyleRanges(0, text.length(),
		    			  (StyleRange[])ranges.toArray(new StyleRange[0]));
		    	  }
		    	  modified = true;
			}
		});

		edit.addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey (VerifyEvent e) {
				if ( e.keyCode == 13 ) {
					e.doit = false;
					if ( !validateContent() ) {
						return; // No call to the user: Fix the error first
					}
					if ( caller.returnFromEdit(true) ) {
						setModified(false);
					}
				}
				else if ( e.keyCode == SWT.ESC ) {
					caller.returnFromEdit(false);
					e.doit = false;
				}
			}
		});
		
		edit.addFocusListener(new FocusListener() {
			@Override
			public void focusLost (FocusEvent e) {
			}
			@Override
			public void focusGained (FocusEvent e) {
				caller.notifyOfFocus(column);
			}
		});
		
//		edit.addVerifyKeyListener (new VerifyKeyListener() {
//			@Override
//			public void verifyKey(VerifyEvent e) {
//				if ( e.stateMask == SWT.CTRL ) {
//					switch ( e.keyCode ) {
//					}
//				}
//			}
//		});

//		edit.addVerifyKeyListener(new VerifyKeyListener() {
//		@Override
//		public void verifyKey(VerifyEvent e) {
//			if ( e.stateMask == SWT.ALT ) {
//				switch ( e.keyCode ) {
//				case SWT.ARROW_RIGHT:
//					selectNextCode(edit.getCaretOffset(), true);
//					e.doit = false;
//					break;
//				case SWT.ARROW_LEFT:
//					selectPreviousCode(edit.getCaretOffset(), true);
//					e.doit = false;
//					break;
////				case SWT.ARROW_DOWN: // Target-mode command
////					setNextSourceCode();
////					e.doit = false;
////					break;
////				case SWT.ARROW_UP: // Target-mode command
////					setPreviousSourceCode();
////					e.doit = false;
////					break;
//				}
//			}
//			else if ( e.stateMask == SWT.CTRL ) {
//				switch ( e.keyCode ) {
////				case 'd':
////					cycleDisplayMode();
////					e.doit = false;
////					break;
////				case 'c':
////					copyToClipboard(edit.getSelection());
////					e.doit = false;
////					break;
////				case 'v':
////					pasteFromClipboard();
////					e.doit = false;
////					break;
//				case ' ':
//					placeText("\u00a0");
//					e.doit = false;
//					break;
//				}
//			}
////			else if ( e.stateMask == SWT.SHIFT ) {
////				switch ( e.keyCode ) {
////				case SWT.DEL:
////					cutToClipboard(edit.getSelection());
////					e.doit = false;
////					break;
////				case SWT.INSERT:
////					pasteFromClipboard();
////					e.doit = false;
////					break;
////				}
////			}
//			else if ( e.keyCode == SWT.DEL ){
//				int i = 0;
//			}
//		}
//	});
		
	}
	
	@Override
	protected void finalize () {
		dispose();
	}

	public void dispose () {
		if ( textOptions != null ) {
			textOptions.dispose();
			textOptions = null;
		}
		if ( bgColor != null ) {
			bgColor.dispose();
			bgColor = null;
		}
		UIUtil.disposeTextStyle(codeStyle);
	}
	
	public void setFullCodesMode (boolean fullCodesMode) {
		// Save and validate text in current mode
		if ( !validateContent() ) return; // Do not change if there is an error

		// Set the new mode and the associated pattern
		this.fullCodesMode = fullCodesMode;
		if ( fullCodesMode ) {
			currentCodes = REALCODES;
		}
		else {
			currentCodes = SHORTCODES;
		}
		edit.append(""); // Make a modification to trigger the re-parsing
	}

	private boolean validateContent () {
		if ( !modified ) return true;
		
		// Save and validate text in current mode
		if ( fullCodesMode ) {
			saveTextWithFullCodes();
		}
		else {
			saveTextWithShortCodes();
		}
		
		return true;
	}
	
	private void saveTextWithFullCodes () {
		
	}
	
	private void saveTextWithShortCodes () {
//		String text = edit.getText();
//		Matcher m = SHORTCODES.matcher(text);

		if ( Util.isEmpty(codesAsString) ) return; // Nothing to check
		
	}

	public boolean getFullCodesMode () {
		return fullCodesMode;
	}
	
	public boolean setFocus () {
		return edit.setFocus();
	}
	
	public void setSelection (int start,
		int end)
	{
		if ( end < 0 ) end = edit.getText().length();
		edit.setSelection(start, end);
	}
	
	public void setEnabled (boolean enabled) {
		edit.setEnabled(enabled);
	}
	
	public void setEditable (boolean editable) {
		edit.setEditable(editable);
	}

	public void clear () {
		edit.setText("");
		codesAsString = null;
		modified = false;
	}
	
	public boolean isModified () {
		return modified;
	}
	
	public void setModified (boolean modified) {
		this.modified = modified;
	}

	/**
	 * Sets the text of the field, and its originating column.
	 * @param text the text
	 * @param codesAsString the codes as a string (can be null if there are no codes)
	 * @param column the column. Use -1 for no column, -2 for not changing the current one.
	 */
	public void setText (String text,
		String codesAsString,
		int column)
	{
		if ( column != -2 ) this.column = column;
		edit.setEnabled(text != null);
		this.codesAsString = codesAsString;
		if ( text == null ) {
			edit.setText("");
		}
		else {
			edit.setText(text);
		}
		modified = false;
	}

	public String getText () {
		validateContent(); 
		return edit.getText();
	}

//	private void placeText (String text) {
//		Point pt = edit.getSelection();
//		edit.replaceTextRange(pt.x, pt.y-pt.x, text);
//		edit.setCaretOffset(pt.x+text.length());
//	}
//	
//	private void selectNextCode (int position,
//		boolean cycle)
//	{
//		StyleRange[] ranges = edit.getStyleRanges();
//		if ( ranges.length == 0 ) return;
//		while ( true ) {
//			for ( StyleRange range : ranges ) {
//				if ( position <= range.start ) {
//					edit.setSelection(range.start, range.start+range.length);
//					return;
//				}
//			}
//			// Not found yet: Stop here if we don't cycle to the first
//			if ( !cycle ) return;
//			position = 0; // Otherwise: re-start from front
//		}
//	}
	
//	private void selectPreviousCode (int position,
//		boolean cycle)
//	{
//		StyleRange[] ranges = edit.getStyleRanges();
//		if ( ranges.length == 0 ) return;
//		StyleRange sr;
//		while ( true ) {
//			for ( int i=ranges.length-1; i>=0; i-- ) {
//				sr = ranges[i];
//				if ( position >= sr.start+sr.length ) {
//					Point pt = edit.getSelection();
//					if (( pt.x == sr.start ) && ( pt.x != pt.y )) continue;
//					edit.setSelection(sr.start, sr.start+sr.length);
//					return;
//				}
//			}
//			// Not found yet: Stop here if we don't cycle to the first
//			if ( !cycle ) return;
//			position = edit.getCharCount()-1; // Otherwise: re-start from the end
//		}
//	}

}
