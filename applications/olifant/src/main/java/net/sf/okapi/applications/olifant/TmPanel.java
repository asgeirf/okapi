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

import java.sql.ResultSet;
import java.util.ArrayList;

import net.sf.okapi.common.observer.IObservable;
import net.sf.okapi.common.observer.IObserver;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.ITm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class TmPanel extends Composite implements IObserver {

	private final static int KEYCOLUMNWIDTH = 90;

	private CTabItem tabItem;
	private final SashForm sashMain;
	private final EditorPanel editPanel;
	private final Table table;
	private final LogPanel logPanel;
	private ITm tm;
	private int currentEntry;
	private ArrayList<String> visibleFields;
	private StatusBar statusBar;
	private Thread workerThread;
	private MainForm mainForm;
	private int srcCol; // Column in the table that holds the source text, use -1 for none, 0-based, 1=SegKey+Flag
	private int trgCol; // Column in the table that holds the target text, use -1 for none, 0-based, 1=SegKey+Flag

	public TmPanel (MainForm mainForm,
		Composite parent,
		int flags,
		ITm tm,
		StatusBar statusBar)
	{
		super(parent, flags);
		this.mainForm = mainForm;
		this.tm = tm;
		this.statusBar = statusBar;
		
		srcCol = -1;
		trgCol = -1;
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Create the two main parts of the UI
		sashMain = new SashForm(this, SWT.VERTICAL);
		sashMain.setLayout(new GridLayout(1, false));
		sashMain.setLayoutData(new GridData(GridData.FILL_BOTH));
		sashMain.setSashWidth(4);
		
		// Edit panels
		editPanel = new EditorPanel(sashMain, SWT.VERTICAL);
		editPanel.clear();

		// Table
		table = new Table(sashMain, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

//		table.addControlListener(new ControlAdapter() {
//		    public void controlResized(ControlEvent e) {
//		    	Table table = (Table)e.getSource();
		    	
//		    	Rectangle rect = table.getClientArea();
//				int itemHeight = table.getItemHeight();
//				int headerHeight = table.getHeaderHeight();
//				int visibleCount = (rect.height - headerHeight + itemHeight - 1) / itemHeight;
//				int u = visibleCount;
				
//		    	Rectangle rect = table.getClientArea();
//				int nPart = rect.width / 100;
//				int nRemain = rect.width % 100;
//				table.getColumn(0).setWidth(8*nPart);
//				table.getColumn(1).setWidth(12*nPart);
//				table.getColumn(2).setWidth(40*nPart);
//				table.getColumn(3).setWidth((40*nPart)+nRemain);
//		    }
//		});
		
		table.addControlListener(new ControlAdapter() {
		    public void controlResized(ControlEvent e) {
		    	int count = table.getColumnCount()-1; // Exclude Key column
		    	if ( count < 1 ) return;
		    	try {
		    		table.setRedraw(false);
		    		Rectangle rect = table.getClientArea();
		    		int keyColWidth = table.getColumn(0).getWidth();
		    		int part = (int)((rect.width-keyColWidth) / count);
		    		int remainder = (int)((rect.width-keyColWidth) % count);
		    		for ( int i=1; i<table.getColumnCount(); i++ ) {
		    			table.getColumn(i).setWidth(part);
		    		}
		    		table.getColumn(1).setWidth(table.getColumn(1).getWidth()+remainder);
		    	}
		    	finally {
		    		table.setRedraw(true);
		    	}
		    }
		});
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				saveEntry();
				updateCurrentEntry();
            }
		});

		// Create the first column (always present)
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText("Flag/SegKey");
		col.setWidth(KEYCOLUMNWIDTH);
		
		logPanel = new LogPanel(sashMain, 0);
		
		sashMain.setWeights(new int[]{3, 7, 2});
	}

	ITm getTm () {
		return tm;
	}
	
	void setTabItem (CTabItem tabItem) {
		this.tabItem = tabItem;
	}
	
	CTabItem getTabItem () {
		return tabItem;
	}
	
	LogPanel getLog () {
		return logPanel;
	}
	
	boolean canClose () {
		if ( hasRunningThread() ) {
			return false;
		}
		return true;
	}
	
	boolean hasRunningThread () {
		return (( workerThread != null ) && workerThread.isAlive() );
	}
	
	void editColumns () {
		try {
			ColumnsForm dlg = new ColumnsForm(getShell(), tm, visibleFields);
			ArrayList<String> res = dlg.showDialog();
			if ( res == null ) return;
			
			visibleFields = res;
			//TODO Set the columns with the source and target
			srcCol = -1;
			trgCol = -1;
			int n = 1;
			for ( String fn : visibleFields ) {
				if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
					if ( srcCol == -1 ) srcCol = n;
					else if ( trgCol == -1 ) trgCol = n;
				}
				n++;
			}
			updateVisibleFields();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing columns document.\n"+e.getMessage(), null);
		}
	}

	void resetTmDisplay () {
		srcCol = -1;
		trgCol = -1;
		// By default: all and only text fields are visible
		visibleFields = new ArrayList<String>();
		int n = 1; // SEGKEY and FLAG are there by default
		for ( String fn : tm.getAvailableFields() ) {
			if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
				visibleFields.add(fn);
				if ( srcCol == -1 ) srcCol = n;
				else if ( trgCol == -1 ) trgCol = n;
				n++;
			}
		}

		// Update the visible fields
		updateVisibleFields();
	}
	
	private void updateVisibleFields () {
		try {
			table.setRedraw(false);
			// Indicate to the TM back-end which fields the UI wants
			tm.setRecordFields(visibleFields);
			// Remove all variable columns
			int n;
			while ( (n = table.getColumnCount()) > 1 ) {
				table.getColumns()[n-1].dispose();
			}
			// Add the new ones
			for ( String fn : visibleFields ) {
				TableColumn col = new TableColumn(table, SWT.NONE);
				col.setText(fn);
				col.setWidth(150);
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error updating columns.\n"+e.getMessage(), null);
		}
		finally {
			table.setRedraw(true);
		}
		fillTable(0);
	}
	
	@Override
	protected void finalize () {
		dispose();
	}

	@Override
	public void dispose () {
		tm = null;
		super.dispose();
	}

	void updatePage () {
		int newEntry = table.getSelectionIndex();
		boolean moveDown = ((newEntry-currentEntry) > 0);
		if ( newEntry >= table.getItemCount() ) {
			fillTable(1);
			
		}
	}
	
	void updateCurrentEntry () {
		try {
			int n = table.getSelectionIndex();
			if ( n == -1 ) {
				editPanel.setFields(null, null);
			}
			else {
				TableItem ti = table.getItem(n);
				editPanel.setFields(
					srcCol==-1 ? null : ti.getText(srcCol),
					trgCol==-1 ? null : ti.getText(trgCol));
			}
			currentEntry = n;
			statusBar.setCounter(n, table.getItemCount());
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while updating entry.\n"+e.getMessage(), null);
		}
	}
	
	void saveEntry () {
		if ( currentEntry < 0 ) return;
		// Else: save the entry
		TableItem ti = table.getItem(currentEntry);
		if ( editPanel.isSourceModified() && ( srcCol != -1 )) {
			ti.setText(srcCol, editPanel.getSourceText());
		}
		if ( editPanel.isTargetModified() && ( trgCol != -1 )) {
			ti.setText(trgCol, editPanel.getTargetText());
		}
	}
	
	/**
	 * Fills the table with a new page
	 * @param direction 0=from the top, 1=next, 2=previous
	 */
	void fillTable (int direction) {
		try {
			table.removeAll();
			currentEntry = -1;
			
			ResultSet rs;
			switch ( direction ) {
			case 0:
				rs = tm.getFirstPage();
				break;
			case 1:
				rs = tm.getNextPage();
				break;
			case 2:
				rs = tm.getPreviousPage();
				break;
			default:
				rs = tm.getLastPage();
				break;
			}
			
			while ( rs.next() ) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, String.format("%d", rs.getLong(ITm.SEGKEY_FIELD)));
				item.setChecked(rs.getBoolean(ITm.FLAG_FIELD));
				for ( int i=0; i<visibleFields.size(); i++ ) {
					// +2 because the result set has always seg-key and flag (and 1-based index)
					item.setText(i+1, rs.getString(i+3)==null ? "" : rs.getString(i+3));
				}
			}
			if ( table.getItemCount() > 0 ) {
				table.setSelection(0);
				updateCurrentEntry();
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while filling the table.\n"+e.getMessage(), null);
		}
	}
	
	void toggleExtra () {
		editPanel.toggleExtra();
	}
	
	void toggleLog () {
		if ( sashMain.getWeights()[2] > 0 ) {
			sashMain.setWeights(new int[]{3, 7, 0});
		}
		else {
			sashMain.setWeights(new int[]{3, 7, 2});
		}
	}

	EditorPanel getEditorPanel () {
		return editPanel;
	}
	
	void startThread (Thread workerThread) {
		this.workerThread = workerThread;
		workerThread.start();
	}
	
	@Override
	public void update (IObservable source,
		Object arg)
	{
		if ( mainForm.getCurrentTmPanel() == this ) {
			mainForm.updateCommands();
		}
		resetTmDisplay();
	}

}