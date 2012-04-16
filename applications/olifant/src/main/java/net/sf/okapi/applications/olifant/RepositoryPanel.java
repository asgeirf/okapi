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

import java.io.File;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ResourceManager;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.Exporter;
import net.sf.okapi.lib.tmdb.IIndexAccess;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.Importer;
import net.sf.okapi.lib.tmdb.Indexer;
import net.sf.okapi.lib.tmdb.Splitter;
import net.sf.okapi.lib.tmdb.SplitterOptions;
import net.sf.okapi.lib.ui.editor.InputDocumentDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

class RepositoryPanel extends Composite {

	static final String NOREPOSELECTED_TEXT = "<No Repository Selected>";
	static final String USEROPTIONSFILE = MainForm.APPNAME + "_Options";
	
	private MainForm mainForm;
	private List tmList;
	private TMOptionsList options;
	private Label stListTitle;
	private IRepository repo;
	private String repoType;
	private String repoParam;
	private MenuItem miContextNewTM;
	private MenuItem miContextOpen;
	private MenuItem miContextEditTMOptions;
	private MenuItem miContextIndexTM;
	private MenuItem miContextImportFile;
	private MenuItem miContextExportTM;
	private MenuItem miContextDeleteTM;
	private MenuItem miContextRenameTM;

	public RepositoryPanel (MainForm mainForm,
		Composite parent,
		int flags,
		ResourceManager rm)
	{
		super(parent, flags);
		this.mainForm = mainForm;
		
		options = new TMOptionsList();
		options.load(USEROPTIONSFILE);
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
	
		int minButtonWidth = 130;
		Button btSelectRepo = UIUtil.createGridButton(this, SWT.PUSH, "Select Repository...", minButtonWidth, 1);
		//btSelectRepo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		btSelectRepo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectRepository();
			}
		});
		
		stListTitle = new Label(this, SWT.NONE);
		stListTitle.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tmList = new List(this, SWT.BORDER | SWT.V_SCROLL);
		tmList.setLayoutData(new GridData(GridData.FILL_BOTH));
		tmList.addMouseListener(new MouseListener() {
			public void mouseDoubleClick (MouseEvent e) {
				openTmTab(null); // Use current selection
			}
			public void mouseDown (MouseEvent e) {}
			public void mouseUp (MouseEvent e) {}
		});
		tmList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Return and space-bar open the TM
				if (( e.character == 13 ) || ( e.character == 32 )) {
					openTmTab(null); // Use current selection
				}
				else if ( e.keyCode == SWT.F2 ) {
					renameTm(null); // Use current selection
				}
			}
		});
		
		// Context menu for the list
		Menu contextMenu = new Menu(getShell(), SWT.POP_UP);
		
		miContextNewTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextNewTM, "repository.createtm"); //$NON-NLS-1$
		miContextNewTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				createTM();
            }
		});
		
		new MenuItem(contextMenu, SWT.SEPARATOR);
		
		miContextOpen = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextOpen, "repository.opentm"); //$NON-NLS-1$
		miContextOpen.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				openTmTab(null); // Use current selection
            }
		});
		
		miContextEditTMOptions = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextEditTMOptions, "repository.edittmoptions"); //$NON-NLS-1$
		miContextEditTMOptions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				editTmOptions(null); // selected TM
            }
		});
		
		miContextIndexTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextIndexTM, "repository.indextm"); //$NON-NLS-1$
		miContextIndexTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				indexTM(null); // The selected TM
            }
		});

		
		new MenuItem(contextMenu, SWT.SEPARATOR);
		
		miContextImportFile = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextImportFile, "repository.import"); //$NON-NLS-1$
		miContextImportFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				importDocument(null); // In the selected TM
            }
		});

		miContextExportTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextExportTM, "repository.export"); //$NON-NLS-1$
		miContextExportTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				exportTM(null); // In the selected TM
            }
		});

		new MenuItem(contextMenu, SWT.SEPARATOR);
		
		miContextDeleteTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextDeleteTM, "repository.deletetm"); //$NON-NLS-1$
		miContextDeleteTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				deleteTm(null); // selected TM
            }
		});
		miContextRenameTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextRenameTM, "repository.renametm"); //$NON-NLS-1$
		miContextRenameTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				renameTm(null); // selected TM
            }
		});

		contextMenu.addListener (SWT.Show, new Listener () {
			public void handleEvent (Event event) {
				boolean enabled = false;
				int n = tmList.getSelectionIndex();
				if ( n > -1 ) {
					TmPanel tp = getTmPanel(-1);
					// If there is no tab or a tab but no process running: it's enabled
					if (( tp == null ) || !tp.hasRunningThread() ) {
						enabled = true;
					}
				}
				miContextNewTM.setEnabled(isRepositoryOpen());
				miContextOpen.setEnabled(n>-1);
				miContextEditTMOptions.setEnabled(enabled);
				miContextIndexTM.setEnabled(enabled && !repo.isServerMode()); // For now: Index is local only
				miContextImportFile.setEnabled(enabled);
				miContextExportTM.setEnabled(enabled);
				miContextDeleteTM.setEnabled(enabled);
				miContextRenameTM.setEnabled(enabled);
			}
		});
		tmList.setMenu(contextMenu);
		
		resetRepositoryUI("");
	}

	private TmPanel getTmPanel (int index) {
		if ( index < 0 ) index = tmList.getSelectionIndex();
		if ( index < 0 ) return null;
		return mainForm.findTmTab(tmList.getItem(index), false);
	}

	void deleteTm (String tmName) {
		int n = 0;
		try {
			if ( tmName == null ) {
				// If tmName is null use the current selection
				n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}
			else {
				n = tmList.indexOf(tmName);
				if ( n < 0 ) return;
			}
			
			// Ask confirmation
			MessageBox dlg = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			dlg.setMessage(String.format("This command will delete the selected TM ('%s').\nThis operation cannot be undone.\nDo you want to proceed?", tmName));
			//dlg.setText(APPNAME);
			if ( dlg.open() != SWT.YES ) {
				return; // Cancel or no.
			}
			
			mainForm.getStatusBar().setInfo("Deleting translation memory...", true);
			
			// Check if the TM is open
			TmPanel tp = mainForm.findTmTab(tmName, false);
			if ( tp != null ) {
				// Close if we can (canClose() is called from within)
				if ( !mainForm.closeTmTab(tp.getTabItem()) ) {
					return; // Can't close
				}
			}
			// Delete the TM
			repo.deleteTm(tmName);
//TODO: Delete the associated options			
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
		finally {
			mainForm.getStatusBar().setInfo("", false);
			resetRepositoryUI("");
		}
	}
	
	void renameTm (String tmName) {
		int n = 0;
		try {
			if ( tmName == null ) {
				// If tmName is null use the current selection
				n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}
			else {
				n = tmList.indexOf(tmName);
				if ( n < 0 ) return;
			}
			
			// Check if the TM is open
			ITm tm = null;
			TmPanel tp = mainForm.findTmTab(tmName, false);
			if ( tp != null ) {
				// Check if we can do something on that TM
				if ( !tp.canClose() ) {
					return;
				}
				tm = tp.getTm();
			}
			
			RenameTMForm dlg = new RenameTMForm(getShell(), tmName, repo.getTmNames());
			String newName = dlg.showDialog();
			if ( newName == null ) return; // Cancel
			
			// Rename the TM
			if ( tm == null ) {
				// No pointer to the TM yet, get it from the repository
				// It will get disposed with the garbage collector
				tm = repo.openTm(tmName);
			}
			tm.rename(newName);
 			
			// Update the UI
			resetRepositoryUI(newName);
			if ( tp != null ) {
				// Update the tab name
				tp.getTabItem().setText(tp.getTm().getName());
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
	}

	private void openTmTab (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}

			// Check if we have already a tab for that TM
			TmPanel tp = mainForm.findTmTab(tmName, true);
			// We are done if the tab exists
			if ( tp != null ) return;
			
			// Else: create a new TmPanel
			ITm tm = repo.openTm(tmName);
			TMOptions opt = options.getItem(tm.getUUID(), true);
			
			tp = mainForm.addTmTabEmpty(tm, opt);
			if ( tp == null ) return; // Just in case

			// Now the tab should exist
			mainForm.findTmTab(tmName, true);
			tp.resetTmDisplay();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
	}
	
	List getTmList () {
		return tmList;
	}

	IRepository getRepository () {
		return repo;
	}

	String getRepositoryName () {
		if ( repo == null ) return null;
		else return repo.getName();
	}
	
	String getRepositoryType () {
		return repoType;
	}
	
	String getRepositoryParameter () {
		return repoParam;
	}

	boolean isRepositoryOpen () {
		return (repo != null);
	}
	
	void createTM () {
		try {
			NewTMForm dlg = new NewTMForm(getShell(), repo.getTmNames());
			Object[] res = dlg.showDialog();
			if ( res == null ) return;
			// Otherwise create the new TM:
			createTmAndTmTab((String)res[0], null, (LocaleId)res[1], true);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error creating a new TM.\n"+e.getMessage(), null);
		}
	}
	
	TmPanel createTmAndTmTab (String name,
		String description,
		LocaleId locId,
		boolean fillTm)
	{
		try {
			// Make sure we have a repository open
			if ( !isRepositoryOpen() ) {
				selectRepository();
				// Check that we do have a repository open now
				if ( !isRepositoryOpen() ) return null;
			}
		
			// Check if it exists already
			if ( repo.getTmNames().contains(name) ) {
				Dialogs.showError(getShell(), "The TM "+name+" exists already.", null);
				return null;
			}
			
			// Create the empty TM
			ITm tm = repo.createTm(name, description, DbUtil.toOlifantLocaleCode(locId));
			if ( tm == null ) return null;
			TMOptions opt = options.getItem(tm.getUUID(), true);
			
			tmList.add(tm.getName());
			resetRepositoryUI(tm.getName());
			TmPanel tp = mainForm.addTmTabEmpty(tm, opt);
			if ( fillTm && ( tp != null )) {
				tp.resetTmDisplay();
			}
			return tp;
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error creating a new TM.\n"+e.getMessage(), null);
			return null;
		}
	}
	
	void selectRepository () {
		try {
			if ( !mainForm.canCloseRepository() ) return;
			RepositoryForm dlg = new RepositoryForm(getShell(), mainForm, repoType, repoParam);
			Object[] res = dlg.showDialog();
			if ( res == null ) return; // No repository selected
			openRepository((String)res[0], (String)res[1]);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
	}

	void openRepository (String type,
		String param)
	{
		try {
			// Instantiate the new repository
			if ( type.equals(RepositoryForm.REPOTYPE_INMEMORY) ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.h2.Repository(null, false);
				repoType = type;
			}
			else if ( type.equals(RepositoryForm.REPOTYPE_DEFAULTLOCAL) 
				|| type.equals(RepositoryForm.REPOTYPE_OTHERLOCALORNETWORK) ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.h2.Repository(param, false);
				repoType = type;
			}
			else if ( type.equals(RepositoryForm.REPOTYPE_H2SERVER) ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.h2.Repository(param, true);
				repoType = type;
			}
			else if ( type.equals(RepositoryForm.REPOTYPE_MONGOSERVER) ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.mongodb.Repository(param);
				repoType = type;
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error opening repository.\n"+e.getMessage(), null);
		}
		finally {
			repoType = type;
			repoParam = param;
			// Update the display
			resetRepositoryUI("");
			updateRepositoryStatus();
		}
	}
	
	void closeRepository () {
		if ( !mainForm.canCloseRepository() ) return;
		// Close all tabs
		mainForm.closeAllTmTabs();
		// Save the user options
		saveUserOptions();
		// Free the current repository
		if ( repo != null ) {
			repo.close();
			repo = null;
			repoType = "";
		}
		resetRepositoryUI("");
		updateRepositoryStatus();
	}

	/**
	 * Resets the content of the panel.
	 * @param selectedTmName use null to select the last entry, an empty string for the first entry.
	 */
	void resetRepositoryUI (String selectedTmName) {
		tmList.removeAll();
		if ( repo == null ) {
			stListTitle.setText(NOREPOSELECTED_TEXT);
		}
		else {
			// Otherwise: populate the list of TMs
			java.util.List<String> tmpList = repo.getTmNames();
			Collections.sort(tmpList, String.CASE_INSENSITIVE_ORDER);
			tmList.setItems(tmpList.toArray(new String[0]));
			
			// Select one item
			if ( tmList.getItemCount() > 0 ) {
				int selection = 0; // First by default
				if ( selectedTmName != null ) {
					if ( !selectedTmName.isEmpty() ) {
						selection = tmpList.indexOf(selectedTmName);
					}
				}
				if ( selection < 0 ) {
					selection = tmList.getItemCount()-1; 
				}
				tmList.setSelection(selection);
			}
			stListTitle.setText(String.format("TMs in this repository (%d):", tmList.getItemCount()));
		}
	}

	void updateRepositoryStatus () {
		mainForm.updateCommands();
		mainForm.updateTitle();
	}
	
	// Temporary output
	void getStatistics () {
		try {
			StringBuilder tmp = new StringBuilder();
			StringBuilder tmp2 = new StringBuilder();
			long totalSeg = 0;
			for ( String tmName : tmList.getItems() ) {
				// Counts
				long segCount = repo.getTotalSegmentCount(tmName);
				totalSeg += segCount;
				// Locales
				java.util.List<String> locales = repo.getTmLocales(tmName);
				for ( String loc: locales ) {
					String add = loc+", ";
					if ( tmp2.indexOf(add) == -1 ) {
						tmp2.append(add);
					}
				}
			}
			if ( tmp2.length() > 0 ) {
				tmp2.delete(tmp2.length()-2, tmp2.length());
			}
			tmp.append("Information for this repository:\n\n");
			tmp.append(String.format("Total number of segment-entries: %s",
				NumberFormat.getInstance().format(totalSeg)));
			tmp.append("\nLocales: "+tmp2.toString());
			
			MessageBox dlg = new MessageBox(getShell());
			dlg.setMessage(tmp.toString());
			dlg.setText("Repository Statistics");
			dlg.open();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), e.getMessage(), null);
		}
	}

	void editTmOptions (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}

			ITm tm = null;
			TMOptions opt;
			TmPanel tp = mainForm.findTmTab(tmName, false);
			if ( tp != null ) {
				tm = tp.getTm();
				opt = tp.getTmOptions();
			}
			else {
				tm = repo.openTm(tmName);
				opt = options.getItem(tm.getUUID(), true);
			}
//			long oldPageSize = opt.getPageSize();
			
			ArrayList<Object> data = new ArrayList<Object>();
			data.add(opt.getPageSize());
			
			TMOptionsForm dlg = new TMOptionsForm(getShell(), data);
			Object[]res = dlg.showDialog();
			if ( res == null ) return;
			
			long newPageSize = (Long)res[0];
			opt.setPageSize(newPageSize);
			
			if ( tp != null ) {
				tm.setPageSize(newPageSize);
				tp.saveAndRefresh();
			}
			else {
				//TODO: save the modified opt to the list
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error editing TM options.\n"+e.getMessage(), null);
		}
		finally {
			// Update the display
			resetRepositoryUI("");
			updateRepositoryStatus();
		}
	}
	
	void importDocument (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}

			// Get the file to import
			String[] paths = Dialogs.browseFilenames(getShell(), "Import File into "+tmName, false, null,
				MainForm.FILEFILTER_TEXT, MainForm.FILEFILTER_EXTS);
			if ( paths == null ) return;
			
			InputDocumentDialog dlg = new InputDocumentDialog(getShell(), "Document to import into "+tmName,
				mainForm.getFCMapper(), false);
			// Lock the locales if we have already documents in the session
			dlg.setLocalesEditable(true);
			// Set default data
			dlg.setData(paths[0], null, "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH);
			// Edit configuration
			Object[] data = dlg.showDialog();
			if ( data == null ) return; // Cancel

			// Get the Tab and TM data
			TmPanel tp = mainForm.findTmTab(tmName, true);
			if ( tp == null ) {
				ITm tm = repo.openTm(tmName);
				TMOptions opt = options.getItem(tm.getUUID(), true);
				tp = mainForm.addTmTabEmpty(tm, opt);
				if ( tp == null ) return;
				// Now the tab should exist
				mainForm.findTmTab(tmName, true);
				tp.resetTmDisplay();
			}
			tp.showLog(); // Make sure to display the log
			
			// Create the raw document to add to the session
			URI uri = (new File((String)data[0])).toURI();
			RawDocument rd = new RawDocument(uri, (String)data[2], (LocaleId)data[3], (LocaleId)data[4]);
			rd.setFilterConfigId((String)data[1]);
			
			// Start the import thread
			ProgressCallback callback = new ProgressCallback(tp);
			Importer imp = new Importer(callback, tp.getTm(), rd, mainForm.getFCMapper());
			tp.startThread(new Thread(imp));
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error adding document.\n"+e.getMessage(), null);
		}
	}

	void exportTM (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}

			// Get the output filename
			
			String path = Dialogs.browseFilenamesForSave(getShell(), "Export "+tmName, null,
				tmName + ".tmx",
				"TMX Documents (*.tmx)\tAll Files (*.*)", "*.tmx\t*.*");
			if ( path == null ) return;
			
			// Get the Tab and TM data
			TmPanel tp = mainForm.findTmTab(tmName, true);
			ITm tm;
			if ( tp == null ) {
				tm = repo.openTm(tmName);
			}
			else {
				tm = tp.getTm();
			}

			ExportForm dlg = new ExportForm(getShell(), tm);
			Object[] res = dlg.showDialog();
			if ( res == null ) return;
			
			if ( tp == null ) {
				TMOptions opt = options.getItem(tm.getUUID(), true);
				tp = mainForm.addTmTabEmpty(tm, opt);
				if ( tp == null ) return;
				// Now the tab should exist
				mainForm.findTmTab(tmName, true);
				tp.resetTmDisplay();
			}
			tp.showLog(); // Make sure to display the log
			
			// Start the import thread
			ProgressCallback callback = new ProgressCallback(tp);
			@SuppressWarnings("unchecked")
			Exporter exp = new Exporter(callback, repo, tmName, path,
				(java.util.List<String>)res[0],
				(java.util.List<String>)res[1]);
			tp.startThread(new Thread(exp));
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while exporting.\n"+e.getMessage(), null);
		}
	}

	void splitTM (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}
			
			// Get the Tab and TM data
			TmPanel tp = mainForm.findTmTab(tmName, true);
			if ( tp == null ) {
				ITm tm = repo.openTm(tmName);
				TMOptions opt = options.getItem(tm.getUUID(), true);
				tp = mainForm.addTmTabEmpty(tm, opt);
				if ( tp == null ) return;
				// Now the tab should exist
				mainForm.findTmTab(tmName, true);
				tp.resetTmDisplay();
			}
			tp.showLog(); // Make sure to display the log
			
			SplitTMForm dlg = new SplitTMForm(getShell(), repo, tmName, tp.getTmOptions().getSourceLocale());
			SplitterOptions options = dlg.showDialog();
			if ( options == null ) return;

			// Start the import thread
			ProgressCallback callback = new ProgressCallback(tp);
			Splitter exp = new Splitter(callback, repo, tmName, options);
			tp.startThread(new Thread(exp));
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while splitting.\n"+e.getMessage(), null);
		}
	}


	void indexTM (String tmName) {
		try {
			// If tmName is null use the current selection
			if ( tmName == null ) {
				int n = tmList.getSelectionIndex();
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}
			
			// Get the Tab and TM data
			ITm tm;
			TmPanel tp = mainForm.findTmTab(tmName, true);
			if ( tp == null ) {
				tm = repo.openTm(tmName);
				TMOptions opt = options.getItem(tm.getUUID(), true);
				tp = mainForm.addTmTabEmpty(tm, opt);
				if ( tp == null ) return;
				// Now the tab should exist
				mainForm.findTmTab(tmName, true);
				tp.resetTmDisplay();
			}
			else {
				tm = tp.getTm();
			}
			tp.showLog(); // Make sure to display the log

			// prompt the user for information
			IndexForm dlg = new IndexForm(getShell(), tm);
			java.util.List<String> fields = dlg.showDialog();
			if ( fields == null ) return;

			if ( fields.isEmpty() ) {
				IIndexAccess ia = repo.getIndexAccess();
				ia.deleteTMIndex(tm.getUUID());
				return;
			}
			
			// Start the import thread
			ProgressCallback callback = new ProgressCallback(tp);
			Indexer exp = new Indexer(callback, repo, tmName, fields);
			tp.startThread(new Thread(exp));
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error while indexing.\n"+e.getMessage(), null);
		}
	}

	/**
	 * Saves the live user options of the open TM into the
	 * list of options in the repository. 
	 * @param tp the tab of the TM associated with the options to save.
	 */
	void updateOptions (TmPanel tp) {
		options.setItem(tp);
	}

	/**
	 * Saves all the user options.
	 * Each user options in the repository list is updated when the tab of the TM is closed.
	 */
	void saveUserOptions () {
		// Make sure we keep the list relatively small
		options.purgeOldItems();
		// Save the file
		options.save(USEROPTIONSFILE, getClass().getPackage().getImplementationVersion());
	}

}
