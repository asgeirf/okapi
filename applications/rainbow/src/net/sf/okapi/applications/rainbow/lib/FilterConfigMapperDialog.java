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

package net.sf.okapi.applications.rainbow.lib;

import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.ui.ClosePanel;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.common.ui.filters.FilterConfigurationsPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

public class FilterConfigMapperDialog {

	private Shell shell;
	private IFilterConfigurationMapper mapper;
	private FilterConfigurationsPanel pnlConfigs;
	private String result = null;

	public FilterConfigMapperDialog (Shell parent,
		boolean selectionMode)
	{
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Filter Configurations");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout());
		
		pnlConfigs = new FilterConfigurationsPanel(shell, SWT.NONE);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		pnlConfigs.setLayoutData(gdTmp);
		
		// Dialog-level buttons
		SelectionAdapter Actions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = null;
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					//TODO: if ( help != null ) help.showTopic(this, "index", "inputDocProp.html"); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					result = pnlConfigs.getData(); 
				}
				shell.close();
			};
		};
		
		if ( selectionMode ) {
			OKCancelPanel pnlActions = new OKCancelPanel(shell, SWT.NONE, Actions, true, "Select");
			pnlActions.btCancel.setText("Close");
			gdTmp = new GridData(GridData.FILL_HORIZONTAL);
			gdTmp.horizontalSpan = 2;
			pnlActions.setLayoutData(gdTmp);
			shell.setDefaultButton(pnlActions.btOK);
		}
		else {
			ClosePanel pnlActions = new ClosePanel(shell, SWT.NONE, Actions, true);
			gdTmp = new GridData(GridData.FILL_HORIZONTAL);
			gdTmp.horizontalSpan = 2;
			pnlActions.setLayoutData(gdTmp);
			shell.setDefaultButton(pnlActions.btClose);
		}
		
		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Point startSize = shell.getMinimumSize();
		if ( startSize.x < 760 ) startSize.x = 760;
		if ( startSize.y < 550 ) startSize.y = 550;
		shell.setSize(startSize);
		Dialogs.centerWindow(shell, parent);
	}
	
	public String showDialog (IFilterConfigurationMapper mapper,
		String configId)
	{
		this.mapper = mapper;
		pnlConfigs.setData(mapper, configId);
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

}
