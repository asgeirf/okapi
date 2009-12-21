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

package net.sf.okapi.applications.rainbow.pipeline;

import java.util.ArrayList;
import java.util.Map;

import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class StepPicker {

	private Shell shell;
	private List lbUtilities;
	private Text edDescription;
	private String result;
	private ArrayList<StepInfo> availableSteps;
	
	public StepPicker (Shell parent,
		Map<String, StepInfo> steps,
		IHelp helpParam) 
	{
		result = null;
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Add Step");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout());
		
		Label label = new Label(shell, SWT.None);
		label.setText("Available steps:");
		
		lbUtilities = new List(shell, SWT.BORDER | SWT.V_SCROLL);
		lbUtilities.setLayoutData(new GridData(GridData.FILL_BOTH));
		lbUtilities.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				if ( !saveData() ) return;
				shell.close();
			}
			public void mouseDown(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}
		});

		StepInfo step;
		availableSteps = new ArrayList<StepInfo>(); 
		for ( String id : steps.keySet() ) {
			step = steps.get(id);
			lbUtilities.add(String.format("%s   - [%s]", step.name, step.id));
			availableSteps.add(step);
		}
		lbUtilities.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateStepDisplay();
			}
		});
		
		edDescription = new Text(shell, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 45;
		gdTmp.horizontalSpan = 2;
		edDescription.setLayoutData(gdTmp);
		edDescription.setEditable(false);
		
		if ( lbUtilities.getItemCount() > 0 ) {
			lbUtilities.select(0);
			updateStepDisplay();
		}

		// Dialog-level buttons
		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = null;
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					//if ( help != null ) help.showTopic(this, "index", "inputDocProp.html"); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE,
			OKCancelActions, false); //TODO: Add help
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		//gdTmp.horizontalSpan = 2;
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
	}

	private void updateStepDisplay () {
		int n = lbUtilities.getSelectionIndex();
		if ( n < 0 ) {
			edDescription.setText("");
			return; 
		}
		StepInfo step = availableSteps.get(n);
		edDescription.setText(step.description);
	}
	
	public String showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

	private boolean saveData () {
		int n = lbUtilities.getSelectionIndex();
		if ( n == -1 ) return false;
		result = getId(lbUtilities.getItem(n));
		return true;
	}

	private String getId (String listEntry) {
		int pos = listEntry.indexOf('[');
		if ( pos == -1 ) return listEntry;
		return listEntry.substring(pos+1, listEntry.length()-1);
	}

}
