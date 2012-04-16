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

import java.util.HashMap;
import java.util.List;

import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.ITm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

class QueryAttributesForm {
	
	private Shell shell;
	private final Table table;
	private HashMap<String, String> newAttrs;

	QueryAttributesForm (Shell parent,
		ITm tm,
		HashMap<String, String> prevAttrs)
	{
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Attributes");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout());

		Label stLabel = new Label(shell, SWT.NONE);
		stLabel.setText("Available attributes:");
		
		table = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		(new TableColumn(table, SWT.NONE)).setText("Field");
		(new TableColumn(table, SWT.NONE)).setText("Value to Match");
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 150;
		table.setLayoutData(gdTmp);
		
		table.addControlListener(new ControlAdapter() {
		    public void controlResized(ControlEvent e) {
		    	try {
		    		table.setRedraw(false);
		    		Rectangle rect = table.getClientArea();
		    		int nameColWidth = table.getColumn(0).getWidth();
		    		table.getColumn(1).setWidth(rect.width-(nameColWidth));
		    	}
		    	finally {
		    		table.setRedraw(true);
		    	}
		    }
		});
		
		// Get the index info
		List<String> fields = DbUtil.indexInfoFromString(tm.getIndexInfo());

		// Fill the list
		if ( fields != null ) {
			for ( String fn : fields ) {
				if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) continue;
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(0, fn);
				if ( prevAttrs != null ) {
					String value = prevAttrs.get(fn);
					if ( value != null ) {
						ti.setChecked(true);
						ti.setText(1, value);
					}
				}
			}
		}
		
		for ( TableColumn col : table.getColumns() ) {
			col.pack();
		}
		table.select(0);
		
		final TableEditor editor = new TableEditor(table);
		// The editor must have the same size as the cell
		// And must not be any smaller than 50 pixels.
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		final int COLTOEDIT = 1;
		// Set the editor
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = editor.getEditor();
				if ( oldEditor != null ) oldEditor.dispose();
				// Identify the selected row
				TableItem item = (TableItem)e.item;
				if ( item == null ) return;
				Text newEditor = new Text(table, SWT.BORDER);
				newEditor.setText(item.getText(COLTOEDIT));
				newEditor.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent me) {
						Text text = (Text)editor.getEditor();
						editor.getItem().setText(COLTOEDIT, text.getText());
					}
				});
				newEditor.selectAll();
				newEditor.setFocus();
				editor.setEditor(newEditor, item, COLTOEDIT);
			}
		});		
		
		
		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newAttrs = null;
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					newAttrs = new HashMap<String, String>();
					for ( TableItem ti : table.getItems() ) {
						if ( ti.getChecked() ) {
							newAttrs.put(ti.getText(0), ti.getText(1));
						}
					}
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, false);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
	}
	
	/**
	 * Opens the dialog box.
	 * @return null if the user canceled, the list of selected attributes otherwise.
	 */
	HashMap<String, String> showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return newAttrs;
	}

}
