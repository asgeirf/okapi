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

package net.sf.okapi.steps.tokenization.ui;

import net.sf.okapi.common.ui.abstracteditor.IDialogPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Widget;

public class OptionsTab extends Composite implements IDialogPage {
	private Group grpTokenizeInThe;
	private Button btnTextUnitSource;
	private Button btnTextUnitTargets;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public OptionsTab(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));
		
		grpTokenizeInThe = new Group(this, SWT.NONE);
		grpTokenizeInThe.setText("Tokenize in the following resources:");
		grpTokenizeInThe.setToolTipText("");
		grpTokenizeInThe.setLayout(new GridLayout(1, false));
		grpTokenizeInThe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpTokenizeInThe.setData("name", "grpTokenizeInThe");
		
		btnTextUnitSource = new Button(grpTokenizeInThe, SWT.CHECK);
		btnTextUnitSource.setData("name", "btnTextUnitSource");
		btnTextUnitSource.setSelection(true);
		btnTextUnitSource.setText("Text Unit source");
		
		btnTextUnitTargets = new Button(grpTokenizeInThe, SWT.CHECK);
		btnTextUnitTargets.setData("name", "btnTextUnitTargets");
		btnTextUnitTargets.setSelection(true);
		btnTextUnitTargets.setText("Text Unit targets");

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public boolean canClose(boolean isOK) {

		return true;
	}

	public void interop(Widget speaker) {
		// TODO Auto-generated method stub
		
	}

	public boolean load(Object data) {

		return true;
	}

	public boolean save(Object data) {

		return true;
	}
}
