/*===========================================================================*/
/* Copyright (C) 2008 Yves Savourel (at ENLASO Corporation)                  */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Default panel for Help/OK/cancel buttons
 */
public class OKCancelPanel extends Composite {

	public Button       btOK;
	public Button       btCancel;
	public Button       btHelp;

	/**
	 * Creates a new panel for Help/OK/Cancel buttons.
	 * @param parent Parent control.
	 * @param flags Style flags.
	 * @param action Action to execute when any of the buttons is clicked.
	 * The receiving event, the widget's data is marked: 'c' for the Cancel
	 * button, 'o' for OK, and 'h' for help.
	 * @param showHelp True to display the Help button.
	 */
	public OKCancelPanel (Composite parent,
		int flags,
		SelectionAdapter action,
		boolean showHelp)
	{
		super(parent, SWT.NONE);
		createContent(action, showHelp);
	}
	
	private void createContent (SelectionAdapter action,
		boolean showHelp)
	{
		GridLayout layTmp = new GridLayout(2, true);
		layTmp.marginHeight = 0;
		layTmp.marginWidth = 0;
		setLayout(layTmp);
		int nWidth = 80;

		btHelp = new Button(this, SWT.PUSH);
		btHelp.setText("Help");
		btHelp.setData("h");
		btHelp.addSelectionListener(action);
		GridData gdTmp = new GridData();
		gdTmp.widthHint = nWidth;
		btHelp.setLayoutData(gdTmp);
		btHelp.setVisible(showHelp);
		
		Composite cmpTmp = new Composite(this, SWT.NONE);
		RowLayout layRow = new RowLayout(SWT.HORIZONTAL);
		layRow.marginWidth = 0;
		layRow.marginHeight = 0;
		cmpTmp.setLayout(layRow);
		gdTmp = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gdTmp.grabExcessHorizontalSpace = true;
		cmpTmp.setLayoutData(gdTmp);

		// Create the buttons in a platform-specific order
		if ( UIUtil.getPlatformType() == UIUtil.PFTYPE_WIN ) {
			btOK = new Button(cmpTmp, SWT.PUSH);
			btCancel = new Button(cmpTmp, SWT.PUSH);
		}
		else {
			btCancel = new Button(cmpTmp, SWT.PUSH);
			btOK = new Button(cmpTmp, SWT.PUSH);
		}

		btOK.setText("OK");
		btOK.setData("o");
		btOK.addSelectionListener(action);
		RowData rdTmp = new RowData();
		rdTmp.width = nWidth;
		btOK.setLayoutData(rdTmp);
		
		btCancel.setText("Cancel");
		btCancel.setData("c");
		btCancel.addSelectionListener(action);
		rdTmp = new RowData();
		rdTmp.width = nWidth;
		btCancel.setLayoutData(rdTmp);
	}
	
	public void setOKText (String text) {
		btOK.setText(text);
	}
}
