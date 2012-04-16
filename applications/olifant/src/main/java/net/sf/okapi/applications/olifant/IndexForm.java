/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.ITm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

class IndexForm {
	
	private Shell shell;
	private final Table tblTextFields;
	private final Table tblAttrFields;
	private List<String> fieldsToIndex;

	IndexForm (Shell parent,
		ITm tm)
	{
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Index");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout());

		Group group = new Group(shell, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label stLabel = new Label(group, SWT.NONE);
		stLabel.setText("Text fields:");
		
		stLabel = new Label(group, SWT.NONE);
		stLabel.setText("Attribute fields:");

		tblTextFields = new Table(group, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 180;
		tblTextFields.setLayoutData(gdTmp);
		
		tblAttrFields = new Table(group, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		gdTmp = new GridData(GridData.FILL_BOTH);
		tblAttrFields.setLayoutData(gdTmp);
		
		// Get the index info
		List<String> fields = DbUtil.indexInfoFromString(tm.getIndexInfo());

		// Fill the lists
		for ( String fn : tm.getAvailableFields() ) {
			if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
				TableItem ti = new TableItem(tblTextFields, SWT.NONE);
				ti.setText(fn);
				if ( fields != null ) {
					ti.setChecked(fields.contains(fn));
				}
				continue;
			}
			if ( DbUtil.isPreDefinedField(fn) ) {
				continue; // Skip SegKey, TuRef, etc.
			}
			// Else: it could be used as attribute
			TableItem ti = new TableItem(tblAttrFields, SWT.NONE);
			ti.setText(fn);
			if ( fields != null ) {
				ti.setChecked(fields.contains(fn));
			}
		}
		
		tblTextFields.select(0);
		tblAttrFields.select(0);
		
		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fieldsToIndex = null;
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				if ( e.widget.getData().equals("x") ) { //$NON-NLS-1$
					//TODO: ask confirmation
					fieldsToIndex = Collections.emptyList();
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, false, "Index TM", "Remove Index");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
	}
	
	private boolean saveData () {
		fieldsToIndex = new ArrayList<String>();
		for ( TableItem ti : tblTextFields.getItems() ) {
			if ( ti.getChecked() ) fieldsToIndex.add(ti.getText());
		}
		if ( fieldsToIndex.isEmpty() ) {
			Dialogs.showError(shell, "You must select at least one text field to index.", null);
			tblTextFields.setFocus();
			return false;
		}
		for ( TableItem ti : tblAttrFields.getItems() ) {
			if ( ti.getChecked() ) fieldsToIndex.add(ti.getText());
		}
		return true;
	}
	
	/**
	 * Opens the dialog box.
	 * @return null if the user canceled, an empty list to remove the index,
	 * or a list with at least one text field to index.
	 */
	List<String> showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return fieldsToIndex;
	}

}
