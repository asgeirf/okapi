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

import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.filter.FilterOptions;
import net.sf.okapi.lib.tmdb.filter.FilterOptions.TRISTATE;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class FilterForm {
	
	final private Shell shell;
	final private Button chkFlaggedEntriesBoth;
	final private Button chkFlaggedEntriesInclude;
	final private Button chkFlaggedEntriesExclude;
	final private List<String> availableFields;
	final private FilterOptions options;
	
	private boolean result = false;

	FilterForm (Shell parent,
		FilterOptions options,
		List<String> availableFields)
	{
		this.options = options;
		this.availableFields = availableFields;
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Create New TM");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout(1, false));

		Group group = new Group(shell, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		chkFlaggedEntriesBoth = new Button(group, SWT.RADIO);
		chkFlaggedEntriesBoth.setText("Include both &flagged and non-flagged entries");
		chkFlaggedEntriesBoth.setSelection(options.getFlaggedEntries() == TRISTATE.NO_VALUE);

		chkFlaggedEntriesInclude = new Button(group, SWT.RADIO);
		chkFlaggedEntriesInclude.setText("Include only &flagged entries");
		chkFlaggedEntriesInclude.setSelection(options.getFlaggedEntries() == TRISTATE.INCLUDE);

		chkFlaggedEntriesExclude = new Button(group, SWT.RADIO);
		chkFlaggedEntriesExclude.setText("Include only non-flagged entries");
		chkFlaggedEntriesExclude.setSelection(options.getFlaggedEntries() == TRISTATE.EXCLUDE);

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, false);
		GridData gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 3;
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
	}

	boolean showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

	private boolean saveData () {
		if ( chkFlaggedEntriesInclude.getSelection() ) {
			options.setFlaggedEntries(TRISTATE.INCLUDE);
		}
		else if ( chkFlaggedEntriesExclude.getSelection() ) {
			options.setFlaggedEntries(TRISTATE.EXCLUDE);
		}
		else {
			options.setFlaggedEntries(TRISTATE.NO_VALUE);
		}
		result = true;
		return true;
	}
}
