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

package net.sf.okapi.steps.ui.textmodification;

import net.sf.okapi.common.IContext;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.ui.segmentation.SegmentationPanel;
import net.sf.okapi.steps.textmodification.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class ParametersEditor implements IParametersEditor {

	private Shell shell;
	private boolean result = false;
	private OKCancelPanel pnlActions;
	private Parameters params;
	private List lbTypes;
	private Button chkAddPrefix;
	private Text edPrefix;
	private Button chkAddSuffix;
	private Text edSuffix;
	private boolean inInit = true;
	private Button chkApplyToExistingTarget;
	private Button chkAddID;
	private Button chkAddName;
	private Button chkMarkSegments;
	private SegmentationPanel pnlSegmentation;
	private IHelp help;
	private String projectDir;
	
	public boolean edit (IParameters params,
		boolean readOnly,
		IContext context)
	{
		boolean bRes = false;
		try {
			shell = null;
			this.projectDir = context.getString("projDir");
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
	
	private void create (Shell parent,
		boolean readOnly)
	{
		shell.setText("Text Rewriting");
		if ( parent != null ) UIUtil.inheritIcon(shell, parent);
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		tfTmp.setLayoutData(new GridData(GridData.FILL_BOTH));

		//--- Options tab

		Composite cmpTmp = new Composite(tfTmp, SWT.NONE);
		cmpTmp.setLayout(new GridLayout(2, true));
		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Options");
		tiTmp.setControl(cmpTmp);

		Label stTmp = new Label(cmpTmp, SWT.NONE);
		stTmp.setText("Type of change to perform:");
		GridData gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		stTmp.setLayoutData(gdTmp);

		lbTypes = new List(cmpTmp, SWT.BORDER | SWT.V_SCROLL);
		lbTypes.add("Keep the original text");
		lbTypes.add("Replace letters by Xs and digits by Ns");
		lbTypes.add("Remove text but keep inline codes");
		lbTypes.add("Replace selected ASCII letters by extended characters");
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 70;
		gdTmp.horizontalSpan = 2;
		lbTypes.setLayoutData(gdTmp);

		chkAddPrefix = new Button(cmpTmp, SWT.CHECK);
		chkAddPrefix.setText("Add the following prefix:");
		chkAddPrefix.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				edPrefix.setEnabled(chkAddPrefix.getSelection());
			}
		});
		
		chkAddSuffix = new Button(cmpTmp, SWT.CHECK);
		chkAddSuffix.setText("Add the following suffix:");
		chkAddSuffix.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				edSuffix.setEnabled(chkAddSuffix.getSelection());
			}
		});
		
		edPrefix = new Text(cmpTmp, SWT.BORDER);
		edPrefix.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		edSuffix = new Text(cmpTmp, SWT.BORDER);
		edSuffix.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		chkAddName = new Button(cmpTmp, SWT.CHECK);
		chkAddName.setText("Append the name of the item.");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkAddName.setLayoutData(gdTmp);

		chkAddID = new Button(cmpTmp, SWT.CHECK);
		chkAddID.setText("Append the extraction ID of the item.");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkAddID.setLayoutData(gdTmp);

		chkApplyToExistingTarget = new Button(cmpTmp, SWT.CHECK);
		chkApplyToExistingTarget.setText("Modify also the items with an exiting translation.");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkApplyToExistingTarget.setLayoutData(gdTmp);
		
		chkMarkSegments = new Button(cmpTmp, SWT.CHECK);
		chkMarkSegments.setText("Mark segments with '[' and ']' delimiters");
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		chkMarkSegments.setLayoutData(gdTmp);

		Group grpTmp = new Group(cmpTmp, SWT.NONE);
		grpTmp.setText("Segmentation");
		grpTmp.setLayout(new GridLayout(4, false));
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 2;
		grpTmp.setLayoutData(gdTmp);
		
		pnlSegmentation = new SegmentationPanel(grpTmp, SWT.NONE,
			"Apply the following segmentation rules:", null, projectDir);
		pnlSegmentation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
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
		inInit = false;
		Dialogs.centerWindow(shell, parent);
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
		lbTypes.setSelection(params.type);
		chkAddPrefix.setSelection(params.addPrefix);
		edPrefix.setText(params.prefix);
		chkAddSuffix.setSelection(params.addSuffix);
		edSuffix.setText(params.suffix);
		chkApplyToExistingTarget.setSelection(params.applyToExistingTarget);
		chkAddName.setSelection(params.addName);
		chkAddID.setSelection(params.addID);
		chkMarkSegments.setSelection(params.markSegments);

		edPrefix.setEnabled(chkAddPrefix.getSelection());
		edSuffix.setEnabled(chkAddSuffix.getSelection());
		pnlSegmentation.setData(params.segment, params.sourceSrxPath, params.targetSrxPath);
	}

	private boolean saveData () {
		if ( inInit ) return true;
		params.type = lbTypes.getSelectionIndex();
		params.addPrefix = chkAddPrefix.getSelection();
		params.prefix = edPrefix.getText();
		params.addSuffix = chkAddSuffix.getSelection();
		params.suffix = edSuffix.getText();
		params.applyToExistingTarget = chkApplyToExistingTarget.getSelection();
		params.addName = chkAddName.getSelection();
		params.addID = chkAddID.getSelection();
		params.markSegments = chkMarkSegments.getSelection();
		params.segment = pnlSegmentation.getSegment();
		params.sourceSrxPath = pnlSegmentation.getSourceSRX();
		params.targetSrxPath = pnlSegmentation.getTargetSRX();
		result = true;
		return true;
	}
	
}
