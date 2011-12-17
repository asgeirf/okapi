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

package net.sf.okapi.applications.olifant;

import java.io.File;

import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.TextAndBrowsePanel;
import net.sf.okapi.common.ui.UIUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class RepositoryForm {

	public static final String REPOTYPE_INMEMORY = "M";
	public static final String REPOTYPE_DEFAULTLOCAL = "L";
	public static final String REPOTYPE_OTHERLOCALORNETWORK = "O";
	public static final String REPOTYPE_MONGOSERVER = "SM";
	public static final String REPOTYPE_H2SERVER = "SH";
	
	private final Shell shell;
	private final IHelp help;
	private final Button rdDefaultLocal;
	private final Text edDefaultLocal;
	private final Button rdInMemory;
	private final Text edInMemory;
	private final Button rdOtherLocal;
	private final TextAndBrowsePanel pnlOtherLocal;
	private final Button rdMongoServerBased;
	private final Text edMongoServerBased;
	private final Button rdH2ServerBased;
	private final Text edH2ServerBased;
	private final String defaultLocalname;
	private final Button chkAutoOpen;

	private Object[] results = null;

	RepositoryForm (Shell parent,
		IHelp helpParam,
		String type,
		String param,
		boolean autoOpen)
	{
		help = helpParam;
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Repository Selection");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout());
		
		Group group = new Group(shell, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		rdDefaultLocal = new Button(group, SWT.RADIO);
		rdDefaultLocal.setText("Default local repository");
		rdDefaultLocal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDisplay();
            }
		});
		
		defaultLocalname = Util.ensureSeparator(System.getProperty("user.home"), false)
			+ "Olifant" + File.separator + "defaultOlifantTMRepository";
		edDefaultLocal = new Text(group, SWT.BORDER);
		edDefaultLocal.setEditable(false);
		edDefaultLocal.setText(defaultLocalname);
		GridData gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		final int indent = 16;
		gdTmp.horizontalIndent = indent;
		edDefaultLocal.setLayoutData(gdTmp);
		
		rdOtherLocal = new Button(group, SWT.RADIO);
		rdOtherLocal.setText("Other local or network repository");
        rdOtherLocal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDisplay();
            }
		});

		pnlOtherLocal = new TextAndBrowsePanel(group, SWT.NONE, false);
		pnlOtherLocal.setTitle("Select the repository file");
		pnlOtherLocal.setBrowseFilters("TM Repositories (*.h2.db)\tAll Files (*.*)", "*.h2.db\t*.*");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalIndent = indent;
		gdTmp.widthHint = 500;
		pnlOtherLocal.setLayoutData(gdTmp);

		rdH2ServerBased = new Button(group, SWT.RADIO);
		rdH2ServerBased.setText("H2 server-based repository");
		rdH2ServerBased.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDisplay();
            }
		});
		
		edH2ServerBased = new Text(group, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalIndent = indent;
		edH2ServerBased.setLayoutData(gdTmp);
		
		rdMongoServerBased = new Button(group, SWT.RADIO);
		rdMongoServerBased.setText("MongoDB server-based repository");
		rdMongoServerBased.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDisplay();
            }
		});
		
		edMongoServerBased = new Text(group, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalIndent = indent;
		edMongoServerBased.setLayoutData(gdTmp);
		
		rdInMemory = new Button(group, SWT.RADIO);
		rdInMemory.setText("Memory-based repository");
		rdInMemory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDisplay();
            }
		});
		
		edInMemory = new Text(group, SWT.NONE);
		edInMemory.setEditable(false);
		edInMemory.setText("(No physical storage: all data are in memory)");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalIndent = indent;
		edInMemory.setLayoutData(gdTmp);
		
		// Options group
		group = new Group(shell, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		chkAutoOpen = new Button(group, SWT.CHECK);
		chkAutoOpen.setText("When starting Olifant, open automatically the last repository used");
		chkAutoOpen.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Set the current selection
		if ( type == null ) type = REPOTYPE_DEFAULTLOCAL;
		if ( param == null ) param = "";
		if ( type.equals(REPOTYPE_OTHERLOCALORNETWORK) ) {
			rdOtherLocal.setSelection(true);
			pnlOtherLocal.setText(param);
		}
		else if ( type.equals(REPOTYPE_MONGOSERVER) ) {
			rdMongoServerBased.setSelection(true);
		}
		else if ( type.equals(REPOTYPE_H2SERVER) ) {
			rdH2ServerBased.setSelection(true);
			edH2ServerBased.setText(param);
		}
		else if ( type.equals(REPOTYPE_INMEMORY) ) {
			rdInMemory.setSelection(true);
		}
		else { // Default
			rdDefaultLocal.setSelection(true);
		}
		
		chkAutoOpen.setSelection(autoOpen);
		
		updateDisplay();

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					help.showWiki("Olifant - Repositories");
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, (help!=null));
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 2;
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
	}
	
	private void updateDisplay () {
		edInMemory.setEnabled(rdInMemory.getSelection());
		edDefaultLocal.setEnabled(rdDefaultLocal.getSelection());
		pnlOtherLocal.setEnabled(rdOtherLocal.getSelection());
		edMongoServerBased.setEnabled(rdMongoServerBased.getSelection());
		edH2ServerBased.setEnabled(rdH2ServerBased.getSelection());
	}
	
	/**
	 * Opens and show the dialog.
	 * @return an array of 3 objects (0=repository type, 1=repository parameter
	 * 2=auto-open option), or null if the user cancelled the operation.
	 */
	Object[] showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return results;
	}

	private boolean saveData () {
		try {
			Object[] res = new Object[3];
			if ( rdDefaultLocal.getSelection() ) {
				res[0] = REPOTYPE_DEFAULTLOCAL;
				res[1] = defaultLocalname;
			}
			else if ( rdInMemory.getSelection() ) {
				res[0] = REPOTYPE_INMEMORY;
			}
			else if ( rdMongoServerBased.getSelection() ) {
				res[0] = REPOTYPE_MONGOSERVER;
			}
			else if ( rdH2ServerBased.getSelection() ) {
				String path = edH2ServerBased.getText().trim();
				if ( path.isEmpty() ) {
					Dialogs.showError(shell, "You must specify a host and database name.", null);
					edH2ServerBased.setFocus();
					return false;
				}
				res[0] = REPOTYPE_H2SERVER;
				res[1] = path;
			}
			else if ( rdOtherLocal.getSelection() ) {
				String path = pnlOtherLocal.getText().trim();
				if ( path.isEmpty() ) {
					Dialogs.showError(shell, "You must specify a database file.", null);
					pnlOtherLocal.setFocus();
					return false;
				}
				res[0] = REPOTYPE_OTHERLOCALORNETWORK;
				res[1] = path;
			}
			res[2] = chkAutoOpen.getSelection();
			results = res;
		}
		catch ( Exception e ) {
			Dialogs.showError(shell, e.getMessage(), null);
			return false;
		}
		return true;
	}
}
