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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

class StatusBar extends Composite {

	private CLabel      counterLabel;
	private CLabel      pageLabel;
	private CLabel      infoLabel;
	
	StatusBar (Composite p_Parent,
		int p_nFlags) {
		super(p_Parent, p_nFlags);
		createContent();
	}
	
	private void createContent () {
		GridData gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 3;
		setLayoutData(gdTmp);
		
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 2;
		setLayout(layout);
		
		counterLabel = new CLabel(this, SWT.BORDER | SWT.CENTER);
		gdTmp = new GridData();
		gdTmp.widthHint = 120;
		counterLabel.setLayoutData(gdTmp);

		pageLabel = new CLabel(this, SWT.BORDER | SWT.CENTER);
		gdTmp = new GridData();
		gdTmp.widthHint = 200;
		pageLabel.setLayoutData(gdTmp);

		infoLabel = new CLabel(this, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		infoLabel.setLayoutData(gdTmp);
		
		pack();
	}

	void setInfo (String p_sText,
		boolean displayNow)
	{
		infoLabel.setText((p_sText == null) ? "" : p_sText); //$NON-NLS-1$
		if ( displayNow ) update(); // Force the text to be displayed now
	}
	
	void clearInfo () {
		infoLabel.setText(""); //$NON-NLS-1$
	}
	
	void setCounter (int current,
		int pageTotal)
	{
		if ( current < 0 ) counterLabel.setText(""); //$NON-NLS-1$
		else counterLabel.setText(String.format("%d / %d", current+1, pageTotal)); //$NON-NLS-1$
	}
	
	void setPage (long current,
		long numberOfPages)
	{
		if (( current < 0 ) || ( numberOfPages < 1 )) {
			pageLabel.setText(String.format("pages: %d", numberOfPages));
		}
		else {
			pageLabel.setText(String.format("page %d of %d", current+1, numberOfPages));
		}
	}
}
