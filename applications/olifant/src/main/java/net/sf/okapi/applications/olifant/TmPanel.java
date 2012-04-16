/*===========================================================================
  Copyright (C) 2011-2012 by the Okapi Framework contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.observer.IObservable;
import net.sf.okapi.common.observer.IObserver;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ResourceManager;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.IRecordSet;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.Location;
import net.sf.okapi.lib.tmdb.SearchAndReplace;
import net.sf.okapi.lib.tmdb.SearchAndReplaceOptions;
import net.sf.okapi.lib.tmdb.SearchAndReplaceOptions.ACTION;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class TmPanel extends Composite implements IObserver, ISegmentEditorUser {

	private final static int KEYCOLUMNWIDTH = 90;
	private final static int SAVE_FLAG = 0x01;
	private final static int SAVE_SOURCE = 0x02;
	private final static int SAVE_TARGET = 0x04;
	//private final static int SAVE_THIRD = 0x08;
	
	private final static String FLAGSEGKEY_COLNAME = "Flag/SegKey";

	private CTabItem tabItem;
	private final SashForm sashMain;
	private final EditorPanel editPanel;
	private final Table table;
	private final TableCursor cursor;
	private final LogPanel logPanel;
	private final LogHandler logHandler;
	private final Listener columnHeaderListener;
	
	private Point cursorLoc;
	private ITm tm;
	private int currentRow;
	private int previousRow;
	private int lastRowTested;
	private boolean needSave = false;
	private boolean wasModified = false;
	private StatusBar statusBar;
	private Thread workerThread;
	private MainForm mainForm;
	private int srcCol; // Column in the table that holds the source text, use -1 for none, 0-based, 1=SegKey+Flag
	private int trgCol; // Column in the table that holds the target text, use -1 for none, 0-based, 1=SegKey+Flag
	private TMOptions opt;
	private SearchAndReplaceForm sarForm;
	private SearchAndReplaceOptions sarOptions;
	private QueryTMForm queryForm;
	private FilterOptions fltOptions;
	private LinkedHashMap<String, Boolean> sortOrder;

	private MenuItem miCtxAddEntry;
	private MenuItem miCtxDeleteEntries;
	private MenuItem miCtxSortOrder;

	public TmPanel (MainForm mainForm,
		Composite parent,
		int flags,
		ITm tm,
		TMOptions opt,
		StatusBar statusBar,
		ResourceManager rm)
	{
		super(parent, flags);
		this.mainForm = mainForm;
		this.tm = tm;
		this.opt = opt;
		this.statusBar = statusBar;
		
		cursorLoc = new Point(0, 0);
		srcCol = -1;
		trgCol = -1;
		
		sarOptions = new SearchAndReplaceOptions();
		fltOptions = new FilterOptions();
		
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
		editPanel = new EditorPanel(sashMain, SWT.VERTICAL, this);
		editPanel.clear();
		
		columnHeaderListener = new Listener() {
			@Override
			public void handleEvent (Event event) {
				try {
					saveEntryAndModifications();
					// Get the field name for the column
					TableColumn col = (TableColumn)event.widget;
					String fn = col.getText();
					if ( fn.equals(FLAGSEGKEY_COLNAME) ) fn = DbUtil.SEGKEY_NAME;
					// Check the current sort
					Boolean asc = true;
					if ( !Util.isEmpty(sortOrder) ) {
						if ( sortOrder.size() == 1 ) {
							// Reverse the order if we were sorting on that field
							asc = sortOrder.get(fn);
							if ( asc != null ) asc = !asc;
							else asc = true;
						}
					}
					// Clear or create the holder variable
					if ( sortOrder == null ) sortOrder = new LinkedHashMap<String, Boolean>();
					else sortOrder.clear();
					// Set the new sort directive
					sortOrder.put(fn, asc);
					getTm().setSortOrder(sortOrder);
					// Refresh the display
					fillTable(0, 0, 0, cursor.getColumn(), -1);
				}
				catch ( Throwable e ) {
					Dialogs.showError(getShell(), "Error setting sort order.\n"+e.getMessage(), null);
				}
			}
		};
		
		// Table
		table = new Table(sashMain, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

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
				if ( event.detail == SWT.CHECK ) {
					// Do not force the selection: table.setSelection((TableItem)event.item);
					TableItem ti = (TableItem)event.item;
					ti.setData((Integer)ti.getData() | SAVE_FLAG); // Entry has been changed
					needSave = true;
				}
//				else {
//					saveEntry();
//					updateCurrentEntry();
//				}
            }
		});

//		table.addKeyListener(new KeyAdapter() {
//			public void keyPressed(KeyEvent e) {
//				e.doit = false;
////				if ( e.character == ' ' ) { // Changes the flag with the space-bar
////					TableItem si = table.getItem(table.getSelectionIndex());
////					for ( TableItem ti : table.getSelection() ) {
////						if ( ti == si ) continue; // Skip focused item because it will get set by SelectionAdapter()
////						ti.setChecked(!ti.getChecked());
////						ti.setData((Integer)ti.getData() | SAVE_FLAG); // Entry has been changed
////						needSave = true;
////					}
////				}
//////				else { 
//////					checkPage(e.keyCode, e.stateMask);
//////				}
//			}
//		});
		
		cursor = new TableCursor(table, SWT.NONE);

		cursor.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
		cursor.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

		cursor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Update the selection
				saveEntry();
				table.setSelection(cursor.getRow());
				updateCurrentEntry();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event){
				// When Enter is pressed on a cell
				editCurrentCell();
			}
		});
		
		cursor.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp (MouseEvent event) {
			}
			@Override
			public void mouseDown (MouseEvent event) {
			}
			@Override
			public void mouseDoubleClick (MouseEvent event) {
				editCurrentCell();
			}
		});
		
		// Hide the TableCursor when the user hits the "MOD1" or "MOD2" key.
		// This allows the user to select multiple items in the table.
		cursor.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if ( e.keyCode == SWT.MOD1 || e.keyCode == SWT.MOD2 || 
					(e.stateMask & SWT.MOD1) != 0 || (e.stateMask & SWT.MOD2) != 0 )
				{
					cursor.setVisible(false);
				}
				switch ( e.keyCode ) {
				case 32: // Changes the flag with the space-bar
					for ( TableItem ti : table.getSelection() ) {
						ti.setChecked(!ti.getChecked());
						ti.setData((Integer)ti.getData() | SAVE_FLAG); // Entry has been changed
						needSave = true;
					}
					break;
				case SWT.F2:
					editCurrentCell();
					break;
				case SWT.ARROW_DOWN:
				case SWT.ARROW_UP:
				case SWT.PAGE_DOWN:
				case SWT.PAGE_UP:
				case SWT.HOME:
				case SWT.END:
					checkPage(e.keyCode, e.stateMask);
					break;
				}
			}
        });

		// Show the TableCursor when the user releases the "MOD2" or "MOD1" key.
		// This signals the end of the multiple selection task.
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.MOD1 && (e.stateMask & SWT.MOD2) != 0) return;
				if (e.keyCode == SWT.MOD2 && (e.stateMask & SWT.MOD1) != 0) return;
				if (e.keyCode != SWT.MOD1 && (e.stateMask & SWT.MOD1) != 0) return;
				if (e.keyCode != SWT.MOD2 && (e.stateMask & SWT.MOD2) != 0) return;
				// cursorLoc.y has the last selected table row
				// and the column selected before hiding the cursor
				getCursorSelection(0);
				cursor.setSelection(cursorLoc.y, cursorLoc.x);
				cursor.setVisible(true);
				cursor.setFocus();
			}
        });
		
		// Create the first column (always present)
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(FLAGSEGKEY_COLNAME);
		col.setWidth(KEYCOLUMNWIDTH);
		col.addListener(SWT.Selection, columnHeaderListener);
		
		logPanel = new LogPanel(sashMain, 0);
		logHandler = new LogHandler();
		Logger.getLogger("").addHandler(logHandler); //$NON-NLS-1$
		
		sashMain.setWeights(new int[]{3, 7, 2});
		
		createContextMenu(rm);
	}
	
	LogHandler getLogHandler () {
		return logHandler;
	}
	
	private void createContextMenu (ResourceManager rm) {
		// Context menu for the list
		Menu contextMenu = new Menu(getShell(), SWT.POP_UP);
		
		miCtxAddEntry = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miCtxAddEntry, "entries.new"); //$NON-NLS-1$
		miCtxAddEntry.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				addNewEntry();
            }
		});
		
		miCtxDeleteEntries = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miCtxDeleteEntries, "entries.remove"); //$NON-NLS-1$
		miCtxDeleteEntries.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				deleteEntries();
			}
		});
		
		new MenuItem(contextMenu, SWT.SEPARATOR);

		miCtxSortOrder = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miCtxSortOrder, "view.sortorder"); //$NON-NLS-1$
		miCtxSortOrder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				editSortOrder();
			}
		});
		
		contextMenu.addListener (SWT.Show, new Listener () {
			public void handleEvent (Event event) {
				boolean enabled = false;
				int n = table.getSelectionIndex();
				if ( n > -1 ) {
					if ( !hasRunningThread() ) {
						enabled = true;
					}
				}
				miCtxAddEntry.setEnabled(!hasRunningThread());
				miCtxDeleteEntries.setEnabled(enabled);
				miCtxSortOrder.setEnabled(enabled);
			}
		});
		table.setMenu(contextMenu);
		cursor.setMenu(contextMenu);
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
	
	FilterOptions getFilterOptions () {
		return fltOptions;
	}
	
	boolean canClose () {
		try {
			if ( hasRunningThread() ) {
				return false;
			}
			saveEntryAndModifications();
			return true;
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), e.getMessage(), null);
			return false;
		}
	}

	void saveEntryAndModifications () {
		saveEntry();
		saveModificationsIfNeeded();
	}

	boolean hasRunningThread () {
		return (( workerThread != null ) && workerThread.isAlive() );
	}

	TMOptions getTmOptions () {
		return opt;
	}
	
	void editColumns () {
		try {
			saveEntryAndModifications();
			ArrayList<String> prevList = opt.getVisibleFields();
			ColumnsForm dlg = new ColumnsForm(getShell(), tm, prevList);
			
			Object[] res = dlg.showDialog();
			if (( res[0] == null ) && ( !(Boolean)res[1] )) {
				return; // Nothing to do
			}
			if ( res[0] == null ) {
				opt.setVisibleFields(prevList);
			}
			else {
				@SuppressWarnings("unchecked")
				ArrayList<String> newList = (ArrayList<String>)res[0];
				opt.setVisibleFields(newList);
			}

			// Update commands (including current locales, extra field)
			updateCurrentLocales(true);
			mainForm.updateCommands();
			updateVisibleFields();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing columns document.\n"+e.getMessage(), null);
		}
	}

	void editLocales () {
		try {
			saveEntryAndModifications();
			LocalesForm dlg = new LocalesForm(getShell(), tm);
			if ( !dlg.showDialog() ) {
				// No change was made, we can skip the re-drawing
				return;
			}

			// Ensure columns of delete locales are removed from the display
			java.util.List<String> available = tm.getAvailableFields();
			// Update the list of visible fields
			// by removing any fields not available anymore
			ArrayList<String> visible = opt.getVisibleFields();
			visible.retainAll(available);
			opt.setVisibleFields(visible);
			
			// Update commands (including current locales, extra field)
			updateCurrentLocales(true);
			mainForm.updateCommands();
			updateVisibleFields();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing columns document.\n"+e.getMessage(), null);
		}
	}

	/**
	 * Updates the column variable of the source and target locales after they
	 * have been (possibly) changed in the toolbar.
	 */
	void updateCurrentLocales (boolean updateLocaleLists) {
		if ( updateLocaleLists ) {
			mainForm.getToolBar().fillLocales();
		}
		// Set the source and target locales
		mainForm.getToolBar().setSource(opt.getSourceLocale(), false);
		mainForm.getToolBar().setTarget(opt.getTargetLocale());
		verifySourceChange();
		verifyTargetChange();
	}
	
	void verifySourceChange () {
		final ToolBarWrapper toolBar = mainForm.getToolBar();
		int oldSrc = srcCol;
		int oldTrg = trgCol;
		int newSrc = toolBar.getSourceIndex();
		int newTrg = toolBar.getTargetIndex();
		
		if ( newSrc > -1 ) {
			// If the new source is the same at the target, change the target
			if ( newSrc == newTrg ) {
				toolBar.setTarget("");
				newTrg = toolBar.getTargetIndex();
			}
		}
		
		// Update the new source column and field
		opt.setSourceLocale(mainForm.getToolBar().getSource());
		newSrc = opt.getVisibleFields().indexOf(DbUtil.TEXT_PREFIX+opt.getSourceLocale());
		newSrc = (newSrc > -1 ? newSrc+1 : newSrc); // Columns are 1-based
		
		// Update the new target column and field
		opt.setTargetLocale(mainForm.getToolBar().getTarget());
		newTrg = opt.getVisibleFields().indexOf(DbUtil.TEXT_PREFIX+opt.getTargetLocale());
		newTrg = (newTrg > -1 ? newTrg+1 : newTrg); // Columns are 1-based

		// Make sure the current entry is saved 
		if (( oldSrc != newSrc ) || ( oldTrg != newTrg )) {
			saveEntry();
		}
		// Set the new columns and update the content
		srcCol = newSrc;
		trgCol = newTrg;
		updateCurrentEntry();
	}
	
	void verifyTargetChange () {
		final ToolBarWrapper toolBar = mainForm.getToolBar();
		int oldTrg = trgCol;
		int newTrg = toolBar.getTargetIndex();
		
		if ( newTrg > -1 ) {
			// If the new source is the same at the target, change the target
			if ( newTrg == toolBar.getSourceIndex() ) {
				toolBar.setTarget("");
				newTrg = toolBar.getTargetIndex();
			}
		}
		
		// Update the new target column and field
		opt.setTargetLocale(mainForm.getToolBar().getTarget());
		newTrg = opt.getVisibleFields().indexOf(DbUtil.TEXT_PREFIX+opt.getTargetLocale());
		newTrg = (newTrg > -1 ? newTrg+1 : newTrg); // Columns are 1-based

		// Make sure the current entry is saved 
		if ( oldTrg != newTrg ) {
			saveEntry();
		}
		// Set the new column and update the content
		trgCol = newTrg;
		updateCurrentEntry();
	}
	
	void resetTmDisplay () {
		srcCol = -1;
		trgCol = -1;
		
		// Set the visible fields
		ArrayList<String> visibleFields = opt.getVisibleFields();
		if ( visibleFields.size() > 0 ) {
			// If we have already options: adjust them to fit the latest real-time TM
			java.util.List<String> available = tm.getAvailableFields();
			Iterator<String> iter = visibleFields.iterator();
			while ( iter.hasNext() ) {
				if ( !available.contains(iter.next()) ) {
					iter.remove();
				}
			}
		}
		else {
			// If there are no options: by default make visible just all text fields
			for ( String fn : tm.getAvailableFields() ) {
				if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
					opt.getVisibleFields().add(fn);
				}
			}
		}
		
		// Set the source and target locales
		updateCurrentLocales(true);
		
		tm.setPageSize(opt.getPageSize());
		// Update the visible fields
		updateVisibleFields();
	}
	
	private void updateVisibleFields () {
		try {
			getCursorSelection(0);
			table.setRedraw(false);
			// Indicate to the TM back-end which fields the UI wants
			tm.setRecordFields(opt.getVisibleFields());
			// Remove all variable columns
			int n;
			while ( (n = table.getColumnCount()) > 1 ) {
				table.getColumns()[n-1].dispose();
			}
			// Add the new ones
			for ( String fn : opt.getVisibleFields() ) {
				TableColumn col = new TableColumn(table, SWT.NONE);
				col.setText(fn);
				col.setWidth(150);
				col.addListener(SWT.Selection, columnHeaderListener);
			}
			// Update the list of locales in the toolbar
			
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error updating columns.\n"+e.getMessage(), null);
		}
		finally {
			table.setRedraw(true);
		}
		fillTable(0, cursorLoc.y, 0, cursorLoc.x, -1);
	}
	
	@Override
	protected void finalize () {
		dispose();
	}

	@Override
	public void dispose () {
		if ( queryForm != null ) {
			queryForm = null;
		}
		tm = null;
		super.dispose();
	}

	// To call before currentEntry is updated to the upcoming value
	void checkPage (int keyCode,
		int stateMask)
	{
		int direction = -1;
		int selection = 0;
		getCursorSelection(0);

		int rowToTest = previousRow;
		if ( rowToTest == lastRowTested ) {
			// The row was tested last time, it means we have used the key but the cursor
			// has not moved, which means it is at an end and we need to test the current row
			rowToTest = currentRow;
		}

		switch ( keyCode ) {
		case SWT.ARROW_DOWN:
			if ( rowToTest == table.getItemCount()-1 ) {
				direction = 1;
			}
			break;
		case SWT.PAGE_DOWN:
			if ( (stateMask & SWT.CTRL) != 0 ) {
				// Ctrl+PageDown goes to the next page
				direction = 1;
			}
			else if ( rowToTest == table.getItemCount()-1 ) {
				// PageDown goes to the next page only if 
				// the current selection is the last row of the current page
				direction = 1;
			}
			break;
		case SWT.ARROW_UP:
			if ( rowToTest == 0 ) {
				direction = 2;
				selection = -1;
			}
			break;
		case SWT.PAGE_UP:
			if ( (stateMask & SWT.CTRL) != 0 ) {
				// Ctrl+PageUp goes to the previous page
				direction = 2;
				selection = -1;
			}
			else if ( rowToTest == 0 ) {
				// PageUp goes to the previous page only if 
				// the current selection is the first row of the current page 
				direction = 2;
				selection = -1;
			}
			break;
		case SWT.HOME:
			if (( rowToTest == 0 ) && ( tm.getCurrentPage() > 0 )) {
				direction = 0;
			}
			break;
		case SWT.END:
			if (( rowToTest == table.getItemCount()-1 ) && ( tm.getCurrentPage() < tm.getPageCount()-1 )) {
				direction = 3;
				selection = -1;
			}
			break;
		}

		lastRowTested = rowToTest;
		if ( direction > -1 ) {
			saveEntry();
			fillTable(direction, selection, selection, cursorLoc.x, -1);
		}
		
	}
	
	void updateCurrentEntry () {
		try {
			int n = table.getSelectionIndex();
			if ( n == -1 ) {
				editPanel.setFields(null, null, -1, null, null, -1);
			}
			else {
				TableItem ti = table.getItem(n);
				editPanel.setFields(
					srcCol==-1 ? null : ti.getText(srcCol), null, srcCol,
					trgCol==-1 ? null : ti.getText(trgCol), null, trgCol);
			}
			previousRow = currentRow;
			currentRow = n;
			statusBar.setCounter(n, table.getItemCount());
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while updating entry.\n"+e.getMessage(), null);
		}
	}
	
	void saveEntry () {
		cursor.setVisible(true);
		if ( currentRow < 0 ) return;
		// Else: save the entry if needed
		if ( editPanel.isSourceModified() && ( srcCol != -1 )) {
			TableItem ti = table.getItem(currentRow);
			ti.setText(srcCol, editPanel.getSourceText());
			ti.setData((Integer)ti.getData() | SAVE_SOURCE);
			needSave = true;
		}
		if ( editPanel.isTargetModified() && ( trgCol != -1 )) {
			TableItem ti = table.getItem(currentRow);
			ti.setText(trgCol, editPanel.getTargetText());
			ti.setData((Integer)ti.getData() | SAVE_TARGET);
			needSave = true;
		}
	}
	
	boolean wasModified () {
		return wasModified;
	}
	
	void addNewEntry () {
		try {
			saveEntry();
			Map<String, Object> emptyMap = Collections.emptyMap();
			tm.startImport();
			tm.addRecord(-1, emptyMap, emptyMap);
			wasModified = true;
			// Move to the last entry (the one we just created)
			//TODO: adjust to go to proper entry when sort is working 
			fillTable(3, -1, -1, srcCol, -1);
			editPanel.setFocus(0, 0, -1);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while adding new entry.\n"+e.getMessage(), null);
		}
		finally {
			tm.finishImport();
		}
	}
	
	void deleteEntries () {
		try {
			getCursorSelection(-1);
			if ( cursorLoc.y == -1 ) {
				return; // Nothing to do
			}
			statusBar.setInfo("Deleting entries...", true);
			saveEntryAndModifications();
			ArrayList<Long> segKeys = new ArrayList<Long>();
			for ( TableItem ti : table.getSelection() ) {
				segKeys.add(Long.valueOf(ti.getText(0)));
			}
			tm.deleteSegments(segKeys);
			fillTable(4, cursorLoc.y, -1, cursorLoc.x, -1);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while deleting an entry.\n"+e.getMessage(), null);
		}
		finally {
			statusBar.setInfo("", false);
		}
	}
	
	void saveAndRefresh () {
		try {
			getCursorSelection(-1);
			if ( cursorLoc.y == -1 ) {
				return; // Nothing to do
			}
			// Note that fillTable saves the modifications if needed
			// (but not the current entry)
			saveEntry();
			fillTable(4, cursorLoc.y, 0, cursorLoc.x, -1);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while refreshing the table.\n"+e.getMessage(), null);
		}
	}
	
	/**
	 * Move the cursor at the given segment or page.
	 * @param value the key of the segment where to move. Use -1 have
	 * the application prompt the user. In that case the move can be a page.
	 */
	void gotoEntry (long value) {
		try {
			saveEntry();
			char type = 's';
			
			// Prompt the user if needed
			if ( value < 0 ) {
				GoToForm dlg = new GoToForm(getShell(), tm.getPageCount());
				Object[] res = dlg.showDialog();
				if ( res == null ) return;
				type = (Character)res[0];
				value = (Long)res[1];
			}

			statusBar.setInfo("Looking up entry...", true);
			
			if ( type == 's' ) {
				// Get the page for this segment
				if ( tm.getPageCount() != 1 ) {
					long page = tm.findPageForSegment(value);
					if ( page < 0 ) {
						Dialogs.showError(getShell(), String.format("No page with the segment key '%d' was not found.", value), null);
						return;
					}
					// Fill the table with the proper page
					fillTable(5, 0, 0, cursor.getColumn(), page);
				}
				// Find the entry in the current table
				int row = -1;
				String tmp = String.valueOf(value);
				for ( int i=0; i<table.getItemCount(); i++ ) {
					TableItem ti = table.getItem(i);
					if ( ti.getText(0).equals(tmp) ) {
						row = i;
						break;
					}
				}
				if ( row < 0 ) {
					// Not found
					Dialogs.showError(getShell(), String.format("The segment key '%d' was not found.", value), null);
					return;
				}
				// Move to the new row
				table.setSelection(row);
				cursor.setSelection(row, cursor.getColumn());
				updateCurrentEntry();
			}
			else { // Go to a page
				fillTable(5, 0, 0, cursor.getColumn(), value);
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while moving position.\n"+e.getMessage(), null);
		}
		finally {
			statusBar.setInfo("", false);
		}
	}
	
	void searchAndReplace (boolean search) {
		try {
//			Location loc = getCurrentLocation(null);
			
			if ( sarForm == null ) {
				sarForm = new SearchAndReplaceForm(getShell(), sarOptions, opt.getVisibleFields());
			}
			ACTION res = sarForm.showDialog();
			sarForm = null;
			if ( res == ACTION.CLOSE ) return; // Close
			
			showLog(); // Make sure to display the log
			
			// Start the thread
			ProgressCallback callback = new ProgressCallback(this);
			SearchAndReplace sar = new SearchAndReplace(callback, tm.getRepository(), tm.getName(), sarOptions);
			startThread(new Thread(sar));
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while search or replacing.\n"+e.getMessage(), null);
		}
	}
	
	void query () {
		saveEntry();
		try {
			if ( queryForm == null ) {
				queryForm = new QueryTMForm(getShell(), tm);
			}
			queryForm.showDialog();
			queryForm = null; // temporary
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error with query.\n"+e.getMessage(), null);
		}
	}
	
	/**
	 * Saves the modifications in the current page into the back-end.
	 */
	private void saveModificationsIfNeeded () {
		if ( !needSave ) {
			return; // Nothing need to be saved
		}
		wasModified = true; // Indicates that the TM was changed in this session
		
		LinkedHashMap<String, Object> tuFields = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> segFields = new LinkedHashMap<String, Object>();

		for ( int i=0; i<table.getItemCount(); i++ ) {
			TableItem ti = table.getItem(i);
			int signal = (Integer)ti.getData();
			if ( signal != 0 ) {
				long segKey = Long.valueOf(ti.getText(0));
				tuFields.clear();
				segFields.clear();
				
				if ( (signal & SAVE_FLAG) == SAVE_FLAG ) {
					segFields.put(DbUtil.FLAG_NAME, ti.getChecked());
				}
				if ( (signal & SAVE_SOURCE) == SAVE_SOURCE ) {
					String fn = table.getColumns()[srcCol].getText();
					segFields.put(fn, ti.getText(srcCol));
				}
				if ( (signal & SAVE_TARGET) == SAVE_TARGET ) {
					String fn = table.getColumns()[trgCol].getText();
					segFields.put(fn, ti.getText(trgCol));
				}
				tm.updateRecord(segKey, tuFields, segFields);
			}
		}
		needSave = false;
	}
	
	/**
	 * Fills the table with a new page
	 * @param direction 0=first page, 1=next page, 2=previous page, 3=last page, 4=current page
	 * @param row 0=top, -1=last, n=another row
	 * @param fallbackRow 0=top, -1=end. Selection to use if the given selection is
	 * not possible (e.g. when the page has less entries)
	 * @param column index of the column to select.
	 * @param pageIndex index of the page to display (used for direction==5 only)
	 */
	void fillTable (int direction,
		int row,
		int fallbackRow,
		int column,
		long pageIndex)
	{
		try {
			statusBar.setInfo("Fetching data...", true);
			saveModificationsIfNeeded();
			IRecordSet rs = null;
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
			case 3:
				rs = tm.getLastPage();
				break;
			case 4:
				rs = tm.refreshCurrentPage();
				break;
			case 5:
				rs = tm.getPage(pageIndex);
				break;
			}
			if ( rs == null ) {
				// No move of the page, leave things as they are
				// (except if we refresh)
				if ( direction == 4 ) {
					table.removeAll();
					currentRow = previousRow = lastRowTested = -1;
					updateCurrentEntry();
					statusBar.setPage(tm.getCurrentPage(), tm.getPageCount());
				}
				return;
			}
			
			table.removeAll();
			currentRow = previousRow = lastRowTested = -1;
			
			while ( rs.next() ) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, String.format("%d", rs.getLong(ITm.SEGKEY_FIELD)));
				item.setChecked(rs.getBoolean(ITm.FLAG_FIELD));
				item.setData(0); // Modified flag
				for ( int i=0; i<opt.getVisibleFields().size(); i++ ) {
					// +2 because the result set has always seg-key and flag (and 1-based index)
					item.setText(i+1, rs.getString(i+3)==null ? "" : rs.getString(i+3));
				}
			}
			if ( table.getItemCount() > 0 ) {
				if ( row == -1 ) {
					row = table.getItemCount()-1;
				}
				else if ( table.getItemCount() > row ) {
					// Selection value is already OK
				}
				else {
					if ( fallbackRow == -1 ) row = table.getItemCount()-1;
					row = 0;
				}
				
				table.setSelection(row);
				cursor.setSelection(row, (column >= table.getColumnCount() ? 0 : column));
				cursor.setFocus();
				getCursorSelection(0);
				
				updateCurrentEntry();
				statusBar.setPage(tm.getCurrentPage(), tm.getPageCount());
			}
			else {
				updateCurrentEntry();
				statusBar.setPage(-1, tm.getPageCount());
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while filling the table.\n"+e.getMessage(), null);
		}
		finally {
			statusBar.setInfo("", false);
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
	
	void showLog () {
		// Ensure the Log panel is visible
		if ( sashMain.getWeights()[2] <= 0 ) {
			toggleLog();
		}
	}

	EditorPanel getEditorPanel () {
		return editPanel;
	}
	
	void startThread (Thread workerThread) {
		this.workerThread = workerThread;
		workerThread.start();
		mainForm.updateCommands();
	}
	
	@Override
	public void update (IObservable source,
		Object arg)
	{
		if ( mainForm.getCurrentTmPanel() == this ) {
			mainForm.updateCommands();
		}
		
		// Update the list of the repositories if needed
		if ( arg != null ) {
			if (( arg instanceof Boolean ) && (Boolean)arg ) {
				String[] sel = mainForm.getRepositoryPanel().getTmList().getSelection();
				String name = "";
				if ( sel.length > 0 ) name = sel[0]; 
				mainForm.getRepositoryPanel().resetRepositoryUI(name);
				mainForm.getRepositoryPanel().updateRepositoryStatus();
			}
		}
		// Update the TM
		resetTmDisplay();
	}

	/**
	 * Updates and return the cursorLoc variable that holds
	 * the selected row and the column of the table cursor.
	 * @param fallbackRow row value to use if none is defined (-1). Use -1 to keep the row undefined.
	 * @return the cursorLoc variable.
	 */
	Point getCursorSelection (int fallbackRow) {
		cursorLoc.y = table.getSelectionIndex();
		if ( cursorLoc.y == -1 ) cursorLoc.y = fallbackRow;
		cursorLoc.x = cursor.getColumn();
		return cursorLoc;
	}

	/**
	 * Build a new Location object with the current location of the cursor. 
	 * @param loc Location object to update, or null to create a new one.
	 * @return the updated (and possibly new) Location object.
	 */
	public Location getCurrentLocation (Location loc) {
		if ( loc == null ) {
			loc = new Location();
		}
		// Get the current field
		Control ctrl = getDisplay().getFocusControl();
		if ( ctrl instanceof StyledText ) {
			loc.setPosition(((StyledText)ctrl).getCaretOffset());
		}
		else if ( ctrl == table ) {
			String fn = table.getColumn(cursor.getColumn()).getText();
			loc.setFieldName(fn);
		}
		else {
			loc.setPosition(-1);
			loc.setFieldName(null);
		}
		return loc;
	}
	
	private void editCurrentCell () {
		int n = cursor.getColumn();
		if ( n == srcCol ) {
			editPanel.setFocus(0, 0, -1);
		}
		else if ( n == trgCol ) {
			editPanel.setFocus(1, 0, -1);
		}
		//TODO: other columns go to extra
	}

	@Override
	public boolean returnFromEdit (boolean save) {
		if ( save ) {
			saveEntry();
		}
		else {
			updateCurrentEntry();
		}
		cursor.setFocus();
		return true;
	}

	@Override
	public void notifyOfFocus (int column) {
		if ( column > -1 ) {
			cursor.setSelection(table.getSelectionIndex(), column);
		}
	}

	void setFilterForFlaggedEntries () {
		try {
			saveEntryAndModifications();
			fltOptions.setSimpleFilterFlaggedOnly(!fltOptions.getSimpleFilterFlaggedOnly());
			tm.setFilter(fltOptions.getCurrentFilter());
			fillTable(0, 0, 0, cursor.getColumn(), -1);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error when filtering.\n"+e.getMessage(), null);
		}
		
	}

	void toggleFilter () {
		//TODO
		Dialogs.showError(getShell(), "Not implemented yet.", null);
	}
	
	void editSortOrder () {
		try {
			saveEntryAndModifications();
			SortOrderForm dlg = new SortOrderForm(getShell(), getTm(),
				opt.getSourceLocale(), opt.getTargetLocale(), sortOrder);
			LinkedHashMap<String, Boolean> res = dlg.showDialog();
			if ( res == null ) return;
			sortOrder = res; // Set the new sort order
			getTm().setSortOrder(sortOrder);
			fillTable(0, 0, 0, cursor.getColumn(), -1);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing sort order.\n"+e.getMessage(), null);
		}
	}
	
	void editFilterSettings () {
		try {
			saveEntryAndModifications();
			
			FilterForm dlg = new FilterForm(getShell(), fltOptions, getTm().getAvailableFields());
			if ( !dlg.showDialog() ) return;
			
			Dialogs.showError(getShell(), "Not implemented yet.", null);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing filter settings.\n"+e.getMessage(), null);
		}
	}
	
}
