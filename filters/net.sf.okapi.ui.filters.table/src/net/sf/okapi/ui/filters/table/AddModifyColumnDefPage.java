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

package net.sf.okapi.ui.filters.table;

import net.sf.okapi.ui.filters.plaintext.common.IInputQueryPage;
import net.sf.okapi.ui.filters.plaintext.common.SWTUtils;
import net.sf.okapi.ui.filters.plaintext.common.Util2;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * 
 * 
 * @version 0.1, 24.06.2009
 * @author Sergei Vasilyev
 */

public class AddModifyColumnDefPage extends Composite implements IInputQueryPage {
	private Composite composite;
	private Label lblColumnNumber;
	private Spinner colNum;
	private Group typeGroup;
	private Composite composite_1;
	private Button typeSource;
	private Button typeSourceId;
	private Button typeTarget;
	private Button typeComment;
	private Button typeRecordId;
	private Label lblSourceColumn;
	private Label lblIdSuffix;
	private Label label_2;
	private Label lblStart;
	private Label lblEnd;
	private Spinner srcIndex;
	private Spinner startPos;
	private Spinner endPos;
	private Text suffix;
	private Text language;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public AddModifyColumnDefPage(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));
		
		composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true, 1, 1));
		
		lblColumnNumber = new Label(composite, SWT.NONE);
		lblColumnNumber.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblColumnNumber.setText("Column #:");
		
		colNum = new Spinner(composite, SWT.BORDER);
		colNum.setMinimum(1);
		
		typeGroup = new Group(composite, SWT.NONE);
		typeGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		typeGroup.setLayout(new GridLayout(1, false));
		typeGroup.setText("Type");
		
		composite_1 = new Composite(typeGroup, SWT.NONE);
		composite_1.setLayout(new GridLayout(1, false));
		
		typeSource = new Button(composite_1, SWT.RADIO);
		typeSource.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				interop();
			}
		});
		typeSource.setText("Source");
		
		typeSourceId = new Button(composite_1, SWT.RADIO);
		typeSourceId.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				interop();
			}
		});
		typeSourceId.setText("Source ID");
		
		typeTarget = new Button(composite_1, SWT.RADIO);
		typeTarget.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				interop();
			}
		});
		typeTarget.setText("Target");
		
		typeComment = new Button(composite_1, SWT.RADIO);
		typeComment.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				interop();
			}
		});
		typeComment.setText("Comment");
		
		typeRecordId = new Button(composite_1, SWT.RADIO);
		typeRecordId.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				interop();
			}
		});
		typeRecordId.setText("Record ID");
		composite_1.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 1, 1));
		
		lblSourceColumn = new Label(composite, SWT.NONE);
		lblSourceColumn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSourceColumn.setText("Source column:");
		
		srcIndex = new Spinner(composite, SWT.BORDER);
		
		label_2 = new Label(composite, SWT.NONE);
		label_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_2.setText("Language:");
		
		language = new Text(composite, SWT.BORDER);
		language.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		lblIdSuffix = new Label(composite, SWT.NONE);
		lblIdSuffix.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblIdSuffix.setAlignment(SWT.RIGHT);
		lblIdSuffix.setText("ID suffix:");
		
		suffix = new Text(composite, SWT.BORDER);
		suffix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		lblStart = new Label(composite, SWT.NONE);
		lblStart.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblStart.setText("Start:");
		
		startPos = new Spinner(composite, SWT.BORDER);
		startPos.setMinimum(1);
		
		lblEnd = new Label(composite, SWT.NONE);
		lblEnd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblEnd.setText("End:");
		
		endPos = new Spinner(composite, SWT.BORDER);
		endPos.setMinimum(1);

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public boolean load(Object data) {

		if (!(data instanceof String[])) return false;
		
		String[] colDef = (String[]) data;
		if (colDef.length != 7) return false; 
		
		colNum.setSelection(Util2.strToInt(colDef[0], 1));
		SWTUtils.setRadioGroupSelection(typeGroup, colDef[1]);
		srcIndex.setSelection(Util2.strToInt(colDef[2], 0));		
		language.setText(colDef[3]);
		suffix.setText(colDef[4]);
		startPos.setSelection(Util2.strToInt(colDef[5], 0));
		endPos.setSelection(Util2.strToInt(colDef[6], 0));
		
		return true;
	}

	public boolean save(Object data) {

		if (!(data instanceof String[])) return false;
		
		String[] colDef = (String[]) data;
		if (colDef.length != 7) return false;
		
		colDef[0] = Util2.intToStr(colNum.getSelection());
		
		Button btn = SWTUtils.getRadioGroupSelection(typeGroup);
		if (btn == null) 
			colDef[1] = "";		
		else
			colDef[1] = btn.getText();
		
		colDef[2] = Util2.intToStr(srcIndex.getSelection());				
		colDef[3] = language.getText();
		colDef[4] = suffix.getText();
		colDef[5] = Util2.intToStr(startPos.getSelection());
		colDef[6] = Util2.intToStr(endPos.getSelection());
		
		return true;
	}

	public void setPrompt(String prompt) {
		
	}

	public void interop() {
		
		if (typeSource.getSelection()) {
		
			srcIndex.setMinimum(0);
			srcIndex.setSelection(0);
			srcIndex.setEnabled(false);
			
			language.setText("");
			language.setEnabled(false);
			
			suffix.setText("");
			suffix.setEnabled(true);
		} 
		else if (typeSourceId.getSelection()) {
			
			srcIndex.setMinimum(1);
			srcIndex.setSelection(1);
			srcIndex.setEnabled(true);
			
			language.setText("");
			language.setEnabled(false);
			
			suffix.setText("");
			suffix.setEnabled(false);
		}
		else if (typeTarget.getSelection()) {
		
			srcIndex.setMinimum(1);
			srcIndex.setSelection(1);
			srcIndex.setEnabled(true);
			
			language.setText("");
			language.setEnabled(true);
			
			suffix.setText("");
			suffix.setEnabled(false);
		}
		else if (typeComment.getSelection()) {
		
			srcIndex.setMinimum(1);
			srcIndex.setSelection(1);
			srcIndex.setEnabled(true);
			
			language.setText("");
			language.setEnabled(false);
			
			suffix.setText("");
			suffix.setEnabled(false);
		}
		else if (typeRecordId.getSelection()) {
		
			srcIndex.setMinimum(0);
			srcIndex.setSelection(0);
			srcIndex.setEnabled(false);
			
			language.setText("");
			language.setEnabled(false);
			
			suffix.setText("");
			suffix.setEnabled(false);
		}
			
	}
}

