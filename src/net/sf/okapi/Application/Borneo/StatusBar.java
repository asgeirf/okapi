/*===========================================================================*/
/* Copyright (C) 2008 ENLASO Corporation, Okapi Development Team             */
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

package net.sf.okapi.Application.Borneo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

class StatusBar extends Composite {

	private CLabel      m_stInfo;
	
	StatusBar (Composite p_Parent,
		int p_nFlags)
	{
		super(p_Parent, p_nFlags);
		createContent();
	}
	
	private void createContent () {
		GridLayout layTmp = new GridLayout();
		layTmp.numColumns = 1;
		layTmp.marginHeight = 1;
		layTmp.marginWidth = 1;
		setLayout(layTmp);
		
		m_stInfo = new CLabel(this, SWT.BORDER);
		GridData gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		m_stInfo.setLayoutData(gdTmp);
	}

	void setInfo (String p_sText) {
		m_stInfo.setText((p_sText == null) ? "" : p_sText);
	}
	
	void clearInfo () {
		m_stInfo.setText("");
	}
}
