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

package net.sf.okapi.steps.leveraging.ui;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.IContext;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ISWTEmbeddableParametersEditor;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.TextAndBrowsePanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.ui.translation.ConnectorSelectionPanel;
import net.sf.okapi.lib.ui.translation.DefaultConnectors;
import net.sf.okapi.lib.ui.translation.IConnectorList;
import net.sf.okapi.steps.leveraging.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

@EditorFor(Parameters.class)
public class ParametersEditor implements IParametersEditor, ISWTEmbeddableParametersEditor {

	private Shell shell;
	private boolean result = false;
	private OKCancelPanel pnlActions;
	private Parameters params;
	private ConnectorSelectionPanel pnlConnector;
	private IConnectorList connectors;
	private IHelp help;
	private Composite mainComposite;
	private IContext context;
	private Spinner spnThreshold;
	private Button chkFillTarget;
	private Button chkMakeTMX;
	private TextAndBrowsePanel pnlTMXPath;
	
	public ParametersEditor () {
		connectors = new DefaultConnectors();
	}
	
	public boolean edit (IParameters params,
		boolean readOnly,
		IContext context)
	{
		boolean bRes = false;
		try {
			this.context = context;
			shell = null;
			help = (IHelp)context.getObject("help");
			this.params = (Parameters)params;
			shell = new Shell((Shell)context.getObject("shell"), SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)context.getObject("shell"), readOnly);
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
	
	@Override
	public Composite getComposite () {
		return mainComposite;
	}

	@Override
	public void initializeEmbeddableEditor (Composite parent,
		IParameters paramsObject,
		IContext context)
	{
		params = (Parameters)paramsObject;
		this.context = context;
		shell = (Shell)context.getObject("shell");
		createComposite(parent);
		setData();
	}

	@Override
	public String validateAndSaveParameters () {
		if ( !saveData() ) return null;
		return params.toString();
	}
	
	private void create (Shell parent,
		boolean readOnly)
	{
		shell.setText("Leveraging");
		if ( parent != null ) UIUtil.inheritIcon(shell, parent);
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		createComposite(shell);
		
		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) {
					if ( help != null ) help.showTopic(this, "index");
					return;
				}
				if ( e.widget.getData().equals("o") ) saveData();
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		pnlActions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pnlActions.btOK.setEnabled(!readOnly);
		if ( !readOnly ) {
			shell.setDefaultButton(pnlActions.btOK);
		}

		shell.pack();
		shell.setMinimumSize(shell.getSize());
		Point startSize = shell.getMinimumSize();
		if ( startSize.x < 600 ) startSize.x = 600;
		shell.setSize(startSize);
		
		setData();
		Dialogs.centerWindow(shell, parent);
	}

	private void createComposite (Composite parent) {
		mainComposite = new Composite(parent, SWT.BORDER);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(2, false));

		pnlConnector = new ConnectorSelectionPanel(mainComposite, SWT.NONE, connectors, context, null);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalSpan = 2;
		pnlConnector.setLayoutData(gdTmp);
		
		Label stTmp = new Label(mainComposite, SWT.NONE);
		stTmp.setText("Leverage only if the match is equal or above this score:");
		
		spnThreshold = new Spinner(mainComposite, SWT.BORDER);
		spnThreshold.setMinimum(0);
		spnThreshold.setMaximum(100);
		spnThreshold.setIncrement(1);
		spnThreshold.setPageIncrement(10);

		chkFillTarget = new Button(mainComposite, SWT.CHECK);
		chkFillTarget.setText("Fill the target if a leverageable match is found");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkFillTarget.setLayoutData(gdTmp);

		chkMakeTMX = new Button(mainComposite, SWT.CHECK);
		chkMakeTMX.setText("Generate a TMX document with the leverageable matches");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkMakeTMX.setLayoutData(gdTmp);
		chkMakeTMX.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pnlTMXPath.setEnabled(chkMakeTMX.getSelection());
			}
		});
		
		pnlTMXPath = new TextAndBrowsePanel(mainComposite, SWT.NONE, false);
		pnlTMXPath.setSaveAs(true);
		pnlTMXPath.setBrowseFilters("TMX Documents (*.tmx)\tAll Files (*.*)", "*.tmx\t*.*");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 2;
		pnlTMXPath.setLayoutData(gdTmp);
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
		pnlConnector.setData(params.getResourceClassName(), params.getResourceParameters());
		spnThreshold.setSelection(params.getThreshold());
		chkFillTarget.setSelection(params.getFillTarget());
		chkMakeTMX.setSelection(params.getMakeTMX());
		pnlTMXPath.setText(params.getTMXPath());
		pnlTMXPath.setEnabled(chkMakeTMX.getSelection());
	}

	private boolean saveData () {
		result = false;
		if ( chkMakeTMX.getSelection() ) {
			if ( !Util.isEmpty(pnlTMXPath.getText()) ) {
				Dialogs.showError(shell,
					"You must provide a path for the TMX output.", null);
				return false;
			}
		}
		params.setResourceClassName(pnlConnector.getConnectorClass());
		params.setResourceParameters(pnlConnector.getConnectorParameters());
		params.setThreshold(spnThreshold.getSelection());
		params.setFillTarget(chkFillTarget.getSelection());
		params.setMakeTMX(chkMakeTMX.getSelection());
		params.setTMXPath(pnlTMXPath.getText());
		result = true;
		return true;
	}
	
}
