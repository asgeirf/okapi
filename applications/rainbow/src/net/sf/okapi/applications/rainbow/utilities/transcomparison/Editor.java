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

package net.sf.okapi.applications.rainbow.utilities.transcomparison;

import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class Editor implements IParametersEditor {
	
	private Shell shell;
	private boolean result = false;
	private OKCancelPanel pnlActions;
	private Parameters params;
	private Button chkGenerateTMX;
	private Text edTMXPath;
	private Button chkGenerateHTML;
	private Button btGetPath;
	private Button chkOpenOutput;
	private IHelp help;

	public boolean edit (IParameters params,
		Object object,
		IHelp helpParam)
	{
		boolean bRes = false;
		try {
			shell = null;
			help = helpParam;
			this.params = (Parameters)params;
			shell = new Shell((Shell)object, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)object);
			return showDialog();
		}
		catch ( Exception e ) {
			Dialogs.showError(shell, e.getLocalizedMessage(), null);
			bRes = false;
		}
		finally {
			// Dispose of the shell, but not of the display
			if ( shell != null ) shell.dispose();
		}
		return bRes;
	}
	
	public IParameters createParameters () {
		return new Parameters();
	}
	
	private void create (Shell parent) {
		shell.setText("Translation Comparison");
		UIUtil.inheritIcon(shell, parent);
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		tfTmp.setLayoutData(new GridData(GridData.FILL_BOTH));

		//--- Options tab

		Composite cmpTmp = new Composite(tfTmp, SWT.NONE);
		cmpTmp.setLayout(new GridLayout());
		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Options");
		tiTmp.setControl(cmpTmp);
		
		Group grpTmp = new Group(cmpTmp, SWT.NONE);
		grpTmp.setText("HTML Output");
		grpTmp.setLayout(new GridLayout());
		GridData gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);

		chkGenerateHTML = new Button(grpTmp, SWT.CHECK);
		chkGenerateHTML.setText("Generate output tables in HTML");
		gdTmp = new GridData();
		chkGenerateHTML.setLayoutData(gdTmp);
		chkGenerateHTML.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chkOpenOutput.setEnabled(chkGenerateHTML.getSelection());
			}
		});

		chkOpenOutput = new Button(grpTmp, SWT.CHECK);
		chkOpenOutput.setText("Open the first HTML output after completion");
		gdTmp = new GridData();
		chkOpenOutput.setLayoutData(gdTmp);
		
		grpTmp = new Group(cmpTmp, SWT.NONE);
		grpTmp.setText("TMX Output");
		grpTmp.setLayout(new GridLayout(2, false));
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);

		chkGenerateTMX = new Button(grpTmp, SWT.CHECK);
		chkGenerateTMX.setText("Generate a TMX output document");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkGenerateTMX.setLayoutData(gdTmp);
		chkGenerateTMX.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				displayTMXPath();
			}
		});

		edTMXPath = new Text(grpTmp, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		edTMXPath.setLayoutData(gdTmp);
		
		btGetPath = new Button(grpTmp, SWT.PUSH);
		btGetPath.setText("..."); //$NON-NLS-1$
		btGetPath.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		btGetPath.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String path = Dialogs.browseFilenamesForSave(shell, "TMX Document", null,
					"TMX Documents (*.tmx)\tAll Files (*.*)",
					"*.tmx\t*.*");
				if ( path != null ) {
					edTMXPath.setText(path);
				}
			}
		});

		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					if ( help != null ) help.showTopic(this, "index"); //$NON-NLS-1$
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		pnlActions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		shell.setMinimumSize(shell.getSize());
		Point startSize = shell.getMinimumSize();
		if ( startSize.x < 450 ) startSize.x = 450;
		shell.setSize(startSize);
		Dialogs.centerWindow(shell, parent);
		setData();
	}
	
	private void displayTMXPath () {
		edTMXPath.setEnabled(chkGenerateTMX.getSelection());
		btGetPath.setEnabled(chkGenerateTMX.getSelection());
	}
	
	private boolean showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

	private void setData () {
		chkGenerateHTML.setSelection(params.generateHTML);
		chkGenerateTMX.setSelection(params.generateTMX);
		edTMXPath.setText(params.tmxPath);
		chkOpenOutput.setSelection(params.openOutput);
		// Enabling
		chkOpenOutput.setEnabled(chkGenerateHTML.getSelection());
		displayTMXPath();
	}

	private boolean saveData () {
		boolean doTMX = chkGenerateTMX.getSelection();
		if ( doTMX ) {
			if ( edTMXPath.getText().length() == 0 ) {
				edTMXPath.setFocus();
				return result;
			}
		}
		params.generateTMX = doTMX;
		params.tmxPath = edTMXPath.getText();

		params.generateHTML = chkGenerateHTML.getSelection();
		params.openOutput = chkOpenOutput.getSelection();
		result = true;
		return result;
	}
	
}
