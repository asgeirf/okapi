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

package net.sf.okapi.applications.rainbow;

import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.common.ui.UserConfiguration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

class PreferencesForm {
	
	private Shell shell;
	private IHelp help;
	private Button rdStartPrjDoNotLoad;
	private Button rdStartPrjAsk;
	private Button rdStartPrjLoad;
	private Button chkAllowDuplicateInputs;
	private Combo cbLogLevel;
	private UserConfiguration config;

	PreferencesForm (Shell p_Parent,
		IHelp helpParam)
	{
		help = helpParam;
		shell = new Shell(p_Parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText(Res.getString("PreferencesForm.caption")); //$NON-NLS-1$
		UIUtil.inheritIcon(shell, p_Parent);
		shell.setLayout(new GridLayout());
		
		Group grpTmp = new Group(shell, SWT.NONE);
		grpTmp.setText(Res.getString("PreferencesForm.initialProjectGroup")); //$NON-NLS-1$
		grpTmp.setLayoutData(new GridData(GridData.FILL_BOTH));
		grpTmp.setLayout(new GridLayout());

		rdStartPrjDoNotLoad = new Button(grpTmp, SWT.RADIO);
		rdStartPrjDoNotLoad.setText(Res.getString("PreferencesForm.neverLoad")); //$NON-NLS-1$
		
		rdStartPrjAsk = new Button(grpTmp, SWT.RADIO);
		rdStartPrjAsk.setText(Res.getString("PreferencesForm.askUser")); //$NON-NLS-1$
		
		rdStartPrjLoad = new Button(grpTmp, SWT.RADIO);
		rdStartPrjLoad.setText(Res.getString("PreferencesForm.autoLoad")); //$NON-NLS-1$
		
		grpTmp = new Group(shell, SWT.NONE);
		grpTmp.setText(Res.getString("PreferencesForm.miscGroup")); //$NON-NLS-1$
		grpTmp.setLayoutData(new GridData(GridData.FILL_BOTH));
		grpTmp.setLayout(new GridLayout(2, false));
		
		chkAllowDuplicateInputs = new Button(grpTmp, SWT.CHECK);
		chkAllowDuplicateInputs.setText(Res.getString("PreferencesForm.allowDuplicated")); //$NON-NLS-1$
		GridData gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkAllowDuplicateInputs.setLayoutData(gdTmp);
		
		Label label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("PreferencesForm.logLevel")); //$NON-NLS-1$
		
		cbLogLevel = new Combo(grpTmp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cbLogLevel.add(Res.getString("PreferencesForm.logNormal")); //$NON-NLS-1$
		cbLogLevel.add(Res.getString("PreferencesForm.logFine")); //$NON-NLS-1$
		cbLogLevel.add(Res.getString("PreferencesForm.logFiner")); //$NON-NLS-1$
		cbLogLevel.add(Res.getString("PreferencesForm.logFinest")); //$NON-NLS-1$
		
		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					if ( help != null ) help.showTopic(this, "index", "preferences.html"); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 2;
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, p_Parent);
	}
	
	void showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
	}

	void setData (UserConfiguration config) {
		this.config = config;
		chkAllowDuplicateInputs.setSelection(config.getBoolean(MainForm.OPT_ALLOWDUPINPUT));
		int n = config.getInteger(MainForm.OPT_LOADMRU);
		if ( n == 1 ) rdStartPrjAsk.setSelection(true);
		else if ( n == 2 ) rdStartPrjLoad.setSelection(true);
		else rdStartPrjDoNotLoad.setSelection(true);
		n = config.getInteger(MainForm.OPT_LOGLEVEL);
		if (( n < 0 ) || ( n > 3)) n = 0;
		cbLogLevel.select(n);
	}

	private boolean saveData () {
		try {
			config.setProperty(MainForm.OPT_ALLOWDUPINPUT, chkAllowDuplicateInputs.getSelection());
			if ( rdStartPrjAsk.getSelection() ) config.setProperty(MainForm.OPT_LOADMRU, 1);
			else if ( rdStartPrjLoad.getSelection() ) config.setProperty(MainForm.OPT_LOADMRU, 2);
			else config.setProperty(MainForm.OPT_LOADMRU, 0);
			config.setProperty(MainForm.OPT_LOGLEVEL, cbLogLevel.getSelectionIndex());
		}
		catch ( Exception E ) {
			return false;
		}
		return true;
	}
}
