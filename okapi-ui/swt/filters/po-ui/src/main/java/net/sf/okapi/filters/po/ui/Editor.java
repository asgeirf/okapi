/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.po.ui;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.IContext;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.filters.InlineCodeFinderPanel;
import net.sf.okapi.filters.po.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

@EditorFor(Parameters.class)
public class Editor implements IParametersEditor {
	
	private Shell shell;
	private boolean result = false;
	//private LDPanel pnlLD;
	private Button chkProtectApproved;
	private Button rdBilingualMode;
	private Text edBModeNote;
	private Button rdMonolingualMode;
	private Text edMModeNote;
	private Button chkMakeID;
	private OKCancelPanel pnlActions;
	private Parameters params;
	private Button chkUseCodeFinder;
	private InlineCodeFinderPanel pnlCodeFinder;
	private IHelp help;

	public boolean edit (IParameters p_Options,
		boolean readOnly,
		IContext context)
	{
		help = (IHelp)context.getObject("help");
		boolean bRes = false;
		shell = null;
		params = (Parameters)p_Options;
		try {
			shell = new Shell((Shell)context.getObject("shell"), SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)context.getObject("shell"), readOnly);
			return showDialog();
		}
		catch ( Exception E ) {
			Dialogs.showError(shell, E.getLocalizedMessage(), null);
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
	
	private void create (Shell p_Parent,
		boolean readOnly)
	{
		shell.setText(Res.getString("editorCaption"));
		if ( p_Parent != null ) shell.setImage(p_Parent.getImage());
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		tfTmp.setLayoutData(gdTmp);

		//--- Options tab
		
		Composite cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);
		
		chkProtectApproved = new Button(cmpTmp, SWT.CHECK);
		chkProtectApproved.setText(Res.getString("protectApproved"));

		Group grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		grpTmp.setText(Res.getString("modeTitle"));
		gdTmp = new GridData(GridData.FILL_BOTH);
		grpTmp.setLayoutData(gdTmp);
		
		rdBilingualMode = new Button(grpTmp, SWT.RADIO);
		rdBilingualMode.setText(Res.getString("bilingualMode"));
		rdBilingualMode.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateOptions();
			};
		});
		
		edBModeNote = new Text(grpTmp, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		edBModeNote.setText(Res.getString("bilingualNote"));
		edBModeNote.setEditable(false);
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalIndent = 16;
		gdTmp.widthHint = 200;
		edBModeNote.setLayoutData(gdTmp);

		chkMakeID = new Button(grpTmp, SWT.CHECK);
		chkMakeID.setText(Res.getString("makeID"));
		gdTmp = new GridData();
		gdTmp.horizontalIndent = 16;
		chkMakeID.setLayoutData(gdTmp);
		
		rdMonolingualMode = new Button(grpTmp, SWT.RADIO);
		rdMonolingualMode.setText(Res.getString("monolingualMode"));
		rdMonolingualMode.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateOptions();
			};
		});
		
		edMModeNote = new Text(grpTmp, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		edMModeNote.setText(Res.getString("monolingualNote"));
		edMModeNote.setEditable(false);
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalIndent = 16;
		gdTmp.widthHint = 200;
		edMModeNote.setLayoutData(gdTmp);
		
		/*for later: grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		grpTmp.setText(Res.getString("locDirTitle"));
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);
		pnlLD = new LDPanel(grpTmp, SWT.NONE);
		*/
		
		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText(Res.getString("tabOptions"));
		tiTmp.setControl(cmpTmp);
		
		//--- Inline tab
		
		cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);
		
		chkUseCodeFinder = new Button(cmpTmp, SWT.CHECK);
		chkUseCodeFinder.setText("Has inline codes as defined below:");
		chkUseCodeFinder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateInlineCodes();
			};
		});
		
		pnlCodeFinder = new InlineCodeFinderPanel(cmpTmp, SWT.NONE);
		pnlCodeFinder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Inline Codes");
		tiTmp.setControl(cmpTmp);
			

		//--- Output tab
		
		/*cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);
		
//options go there
		
		tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText(Res.getString("tabOutput"));
		tiTmp.setControl(cmpTmp);
		*/
		
		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) {
					if ( help != null ) help.showWiki("PO Filter");
					return;
				}
				if ( e.widget.getData().equals("o") ) {
					if ( !saveData() ) return;
					result = true;
				}
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		pnlActions.setLayoutData(gdTmp);
		pnlActions.btOK.setEnabled(!readOnly);
		if ( !readOnly ) {
			shell.setDefaultButton(pnlActions.btOK);
		}

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, p_Parent);
		setData();
	}
	
	private boolean showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}
	
	private void updateOptions () {
		edBModeNote.setEnabled(rdBilingualMode.getSelection());
		chkMakeID.setEnabled(rdBilingualMode.getSelection());
		edMModeNote.setEnabled(rdMonolingualMode.getSelection());
	}

	private void setData () {
		chkProtectApproved.setSelection(params.getProtectApproved());
		
		rdBilingualMode.setSelection(params.getBilingualMode());
		rdMonolingualMode.setSelection(!params.getBilingualMode());
		chkMakeID.setSelection(params.getMakeID());
		updateOptions();
		
		//For later: pnlLD.setOptions(params.locDir.useLD(), params.locDir.localizeOutside());
		chkUseCodeFinder.setSelection(params.getUseCodeFinder());
		pnlCodeFinder.setRules(params.getCodeFinder().toString());
		
		updateInlineCodes();
		pnlCodeFinder.updateDisplay();
		//For later: pnlLD.updateDisplay();
	}
	
	private boolean saveData () {
		String tmp = pnlCodeFinder.getRules();
		if ( tmp == null ) {
			return false;
		}
		else {
			params.getCodeFinder().fromString(tmp);
		}
		
		params.setProtectApproved(chkProtectApproved.getSelection());
		params.setMakeID(chkMakeID.getSelection());
		params.setBilingualMode(rdBilingualMode.getSelection());
		
		//For later: params.locDir.setOptions(pnlLD.getUseLD(), pnlLD.getLocalizeOutside());
		params.setUseCodeFinder(chkUseCodeFinder.getSelection());
		return true;
	}
	
	private void updateInlineCodes () {
		pnlCodeFinder.setEnabled(chkUseCodeFinder.getSelection());
	}

}
