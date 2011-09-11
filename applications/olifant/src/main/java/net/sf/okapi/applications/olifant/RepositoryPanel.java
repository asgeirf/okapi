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

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ResourceManager;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

class RepositoryPanel extends Composite {

	static final String NOREPOSELECTED_TEXT = "<No Repository Selected>";
	
	private MainForm mainForm;
	private List tmList;
	private Button btNewTM;
	private Label stListTitle;
	private IRepository repo;
	private MenuItem miContextDeleteTM;
	private MenuItem miContextRenameTM;

	public RepositoryPanel (MainForm mainForm,
		Composite parent,
		int flags,
		ResourceManager rm)
	{
		super(parent, flags);
		this.mainForm = mainForm;
		
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
		
		btNewTM = UIUtil.createGridButton(this, SWT.PUSH, "Create New TM...", minButtonWidth, 1);
		//btNewTM.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		btNewTM.setEnabled(false); // No repository is open yet
		btNewTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				createTM();
			}
		});
		
		UIUtil.setSameWidth(minButtonWidth, btSelectRepo, btNewTM);

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
				if ( e.character == 13 ) { // Return key
					openTmTab(null); // Use current selection
				}
			}
		});
		
		// Context menu for the list
		Menu contextMenu = new Menu(getShell(), SWT.POP_UP);
		
		miContextDeleteTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextDeleteTM, "repository.deleteTM"); //$NON-NLS-1$
		miContextDeleteTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				deleteTm(null); // selected TM
            }
		});
		miContextRenameTM = new MenuItem(contextMenu, SWT.PUSH);
		rm.setCommand(miContextRenameTM, "repository.renameTM"); //$NON-NLS-1$
		miContextRenameTM.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				renameTm(null); // selected TM
            }
		});

		tmList.setMenu(contextMenu);
		
		resetRepositoryUI(0);
	}

	void deleteTm (String tmName) {
		int n = 0;
		try {
			// If tmName is null use the current selection
			n = tmList.getSelectionIndex();
			if ( tmName == null ) {
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
			}
			
			// Ask confirmation
			MessageBox dlg = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			dlg.setMessage(String.format("This command will delete the selected TM ('%s').\nThis operation cannot be undone.\nDo you want to proceed?", tmName));
			//dlg.setText(APPNAME);
			if ( dlg.open() != SWT.YES ) {
				return; // Cancel or no.
			}
			
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
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
		finally {
			resetRepositoryUI(n);
		}
	}
	
	void renameTm (String tmName) {
		int n = 0;
		try {
			// If tmName is null use the current selection
			n = tmList.getSelectionIndex();
			if ( tmName == null ) {
				if ( n < 0 ) return;
				tmName = tmList.getItem(n);
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
				// No point to the TM yet, get it from the repository
				// It will get disposed with the garbage collector
				tm = repo.openTm(tmName);
			}
			tm.rename(newName);
 			
			// Update the UI
			resetRepositoryUI(-1); // New TM is listed at the end
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
			tp = mainForm.addTmTabEmpty(tm);
			if ( tp == null ) return;

			// Now the tab should exist
			mainForm.findTmTab(tmName, true);
			tp.resetTmDisplay();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
	}
	
	@Override
	public void dispose () {
		closeRepository();
		super.dispose();
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
			ITm tm = repo.createTm(name, description, locId);
			if ( tm == null ) return null;
			
			tmList.add(tm.getName());
			resetRepositoryUI(-1);
			TmPanel tp = mainForm.addTmTabEmpty(tm);
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
			RepositoryForm dlg = new RepositoryForm(getShell());
			String[] res = dlg.showDialog();
			if ( res == null ) return; // No repository selected
			openRepository(res[0], res[1]);
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error selecting repository.\n"+e.getMessage(), null);
		}
	}

	private void openRepository (String type,
		String name)
	{
		try {
			// Instantiate the new repository
			if ( type.equals("m") ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.h2.Repository(null);
			}
			else if ( type.equals("d") ) {
				closeRepository();
				repo = new net.sf.okapi.lib.tmdb.h2.Repository(name);
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), "Error opening repository.\n"+e.getMessage(), null);
		}
		finally {
			// Update the display
			resetRepositoryUI(0);
			updateRepositoryStatus();
		}
	}
	
	void closeRepository () {
		// Close all tabs
		mainForm.closeAllTmTabs();
		// Free the current repository
		if ( repo != null ) {
			repo.close();
			repo = null;
		}
		resetRepositoryUI(0);
		updateRepositoryStatus();
	}

	void updateRepositoryCommands () {
		boolean hasOneTm = tmList.getItemCount()>0;
		miContextDeleteTM.setEnabled(hasOneTm);
		miContextRenameTM.setEnabled(hasOneTm);
	}
	
	/**
	 * Resets the content of the panel.
	 * @param selection -1 to select the last TM, otherwise the index of the TM to select.
	 */
	void resetRepositoryUI (int selection) {
		tmList.removeAll();
		if ( repo == null ) {
			stListTitle.setText(NOREPOSELECTED_TEXT);
		}
		else {
			// Otherwise: populate the list of TMs
			for ( String name : repo.getTmNames() ) {
				tmList.add(name);
			}
			// Select one
			if ( tmList.getItemCount() > 0 ) {
				if (( selection < 0 ) || ( selection > tmList.getItemCount()-1 )) {
					selection = tmList.getItemCount()-1; 
				}
				tmList.setSelection(selection);
			}
			stListTitle.setText(String.format("TMs in this repository (%d):", tmList.getItemCount()));
		}
		updateRepositoryCommands();
	}

	void updateRepositoryStatus () {
		btNewTM.setEnabled(isRepositoryOpen());
		updateRepositoryCommands();
		mainForm.updateCommands();
		mainForm.updateTitle();
	}
	
	void getStatistics () {
		try {
			StringBuilder tmp = new StringBuilder();
			long totalSeg = 0;
			for ( String tmName : tmList.getItems() ) {
				long segCount = repo.getTotalSegmentCount(tmName);
				totalSeg += segCount;
			}
			tmp.append(String.format("Total number of segment-entries: %d", totalSeg));
			MessageBox dlg = new MessageBox(getShell());
			dlg.setMessage(tmp.toString());
			dlg.setText("Statistics");
			dlg.open();
		}
		catch ( Throwable e ) {
			Dialogs.showError(getShell(), e.getMessage(), null);
		}
	}

}
