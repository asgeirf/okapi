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

import java.io.File;
import java.net.URI;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ResourceManager;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.common.ui.UserConfiguration;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.ui.editor.InputDocumentDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class MainForm {
	
	public static final String APPNAME = "Olifant"; //$NON-NLS-1$

	public static final String OPT_BOUNDS = "bounds"; //$NON-NLS-1$
	public static final String OPT_MAXIMIZED = "maximized"; //$NON-NLS-1$

	private Shell shell;
	private UserConfiguration config;
	private ResourceManager rm;
	private IFilterConfigurationMapper fcMapper;
	private IRepository repo;
	private SashForm topSash;
	private CTabFolder tabs;
	private List tmList;
	private TmPanel currentTP;
	private StatusBar statusBar;
	
	private MenuItem miFileOpen;
	private MenuItem miShowHideThirdField;
	private MenuItem miShowHideFieldList;
	private MenuItem miEditColumns;
	private MenuItem miShowHideLog;

	public MainForm (Shell shell,
		String[] args)
	{
		try {
			this.shell = shell;
			shell.setLayout(new GridLayout());
			loadResources();
			
			config = new UserConfiguration();
			config.load(APPNAME); // Load the current user preferences
			
			createContent();
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, e.getMessage(), null);
		}
	}

	private void saveUserConfiguration () {
		// Set the window placement
		config.setProperty(OPT_MAXIMIZED, shell.getMaximized());
		Rectangle r = shell.getBounds();
		config.setProperty(OPT_BOUNDS, String.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height)); //$NON-NLS-1$
		// Save to the user home directory as ".appname" file
		config.save(APPNAME, getClass().getPackage().getImplementationVersion());
	}

	private boolean canClose () {
		// TODO: check here if the application can be closed
		return true;
	}
	
	private void createContent ()
		throws Exception
	{
		shell.setLayout(new GridLayout(1, false));
		
		// Handling of the closing event
		shell.addShellListener(new ShellListener() {
			public void shellActivated(ShellEvent event) {}
			public void shellClosed(ShellEvent event) {
				saveUserConfiguration();
				if ( !canClose() ) event.doit = false;
			}
			public void shellDeactivated(ShellEvent event) {}
			public void shellDeiconified(ShellEvent event) {}
			public void shellIconified(ShellEvent event) {}
		});

		createMenu();

		// Drag and drop handling for adding files
		DropTarget dropTarget = new DropTarget(shell, DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE);
		dropTarget.setTransfer(new FileTransfer[]{FileTransfer.getInstance()}); 
		dropTarget.addDropListener(new DropTargetAdapter() {
			public void drop (DropTargetEvent e) {
				FileTransfer ft = FileTransfer.getInstance();
				if ( ft.isSupportedType(e.currentDataType) ) {
					String[] paths = (String[])e.data;
					if ( paths != null ) {
						boolean acceptAll = false;
						for ( String path : paths ) {
							Boolean res;
							if ((res = addDocumentFromUI(path, paths.length>1, acceptAll)) == null ) {
								return; // Stop now
							}
							// Else use the result to set the next value of the accept-all button
							acceptAll = res;
						}
					}
				}
			}
		});

		// Create the two main parts of the UI
		topSash = new SashForm(shell, SWT.HORIZONTAL);
		topSash.setLayout(new GridLayout(1, false));
		topSash.setLayoutData(new GridData(GridData.FILL_BOTH));
		topSash.setSashWidth(4);
		
		tabs = new CTabFolder(topSash, SWT.TOP | SWT.CLOSE);
		tabs.setBorderVisible(true);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		tabs.setLayoutData(gdTmp);
		
		tabs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				currentTP = (TmPanel)tabs.getSelection().getControl();
				updateCommands();
				currentTP.updateCurrentEntry();
			}
		});

		tabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close (CTabFolderEvent event) {
				// Get the tab panel from the closing item
				TmPanel tp = (TmPanel)((CTabItem)event.item).getControl();
				// Check if we can close it
				if ( !tp.canClose() ) {
					event.doit = false;
					return;
				}
				// Remove the corresponding tm from the list (temporary)
				tmList.remove(tp.getTm().getName());
				// If the tab we are closing is the last one: update the commands
				// Otherwise currentTP will be set to null, but without update
				if ( tabs.getItemCount() == 1 ) {
					currentTP = null;
					updateCommands();
				}
			}
		});
		
		tmList = new List(topSash, SWT.BORDER);

		topSash.setWeights(new int[]{4, 1});
		
		statusBar = new StatusBar(shell, SWT.NONE);

		// Set the minimal size to the packed size
		// And then set the start size
		Point startSize = shell.getSize();
		shell.pack();
		shell.setMinimumSize(shell.getSize());
		shell.setSize(startSize);
		// Maximize if requested
		if ( config.getBoolean(OPT_MAXIMIZED) ) {
			shell.setMaximized(true);
		}
		else { // Or try to re-use the bounds of the previous session
			Rectangle ar = UIUtil.StringToRectangle(config.getProperty(OPT_BOUNDS));
			if ( ar != null ) {
				Rectangle dr = shell.getDisplay().getBounds();
				if ( dr.contains(ar.x+ar.width, ar.y+ar.height)
					&& dr.contains(ar.x, ar.y) ) {
					shell.setBounds(ar);
				}
			}
		}
		
		updateTitle();
		updateCommands();
	}

	TmPanel getCurrentTmPanel () {
		return currentTP;
	}

	private void updateTitle () {
		String tmp = APPNAME + " - ";
		if ( repo == null ) {
			shell.setText(tmp + "<No Repository>");
		}
		else {
			shell.setText(tmp + repo.getName());
		}
	}

	void updateCommands () {
		miFileOpen.setEnabled(repo!=null);
		boolean active = (( repo != null ) && ( currentTP != null ) && !currentTP.hasRunningThread() );
		miShowHideLog.setEnabled(active);
		miShowHideThirdField.setEnabled(active);
		miShowHideFieldList.setEnabled((active) && currentTP.getEditorPanel().isExtraVisible());
		miEditColumns.setEnabled(active);
	}
	
	private TmPanel addTmTab (ITm tm) {
		TmPanel tp = new TmPanel(this, tabs, SWT.NONE, tm, statusBar);
		CTabItem ti = new CTabItem(tabs, SWT.NONE);
		ti.setText(tm.getName());
		ti.setControl(tp);
		tmList.add(tm.getName());
		tmList.setData(tm.getUUID(), tm);
		return tp;
	}

	private void createMenu () {
		// Menus
	    Menu menuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menuBar);

		// File menu
		MenuItem topItem = new MenuItem(menuBar, SWT.CASCADE);
		topItem.setText(rm.getCommandLabel("file")); //$NON-NLS-1$
		Menu dropMenu = new Menu(shell, SWT.DROP_DOWN);
		topItem.setMenu(dropMenu);

		MenuItem menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "file.selectrepository"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				selectRepository();
            }
		});

		miFileOpen = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miFileOpen, "file.open"); //$NON-NLS-1$
		miFileOpen.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				openFile();
            }
		});
		
		new MenuItem(dropMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "file.exit"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				shell.close();
            }
		});
		
		// View menu
		topItem = new MenuItem(menuBar, SWT.CASCADE);
		topItem.setText(rm.getCommandLabel("view")); //$NON-NLS-1$
		dropMenu = new Menu(shell, SWT.DROP_DOWN);
		topItem.setMenu(dropMenu);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "view.showhidetmlist"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( topSash.getSashWidth() > 0 ) {
					topSash.setWeights(new int[]{1, 0});
					topSash.setSashWidth(0);
				}
				else {
					topSash.setWeights(new int[]{4, 1});
					topSash.setSashWidth(4);
				}
            }
		});
		
		miShowHideLog = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miShowHideLog, "view.showhidelog"); //$NON-NLS-1$
		miShowHideLog.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.toggleLog();
				}
			}
		});
	
		miShowHideThirdField = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miShowHideThirdField, "view.showhideextrafield"); //$NON-NLS-1$
		miShowHideThirdField.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.toggleExtra();
					updateCommands();
				}
            }
		});
		
		miShowHideFieldList = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miShowHideFieldList, "view.showhidefieldlist"); //$NON-NLS-1$
		miShowHideFieldList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.getEditorPanel().getExtraFieldPanel().toggleFieldList();
				}
			}
		});
	
		new MenuItem(dropMenu, SWT.SEPARATOR);

		miEditColumns = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miEditColumns, "view.editcolumns"); //$NON-NLS-1$
		miEditColumns.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.editColumns();
				}
            }
		});
		
	}

	private void loadResources ()
		throws Exception 
	{
		rm = new ResourceManager(MainForm.class, shell.getDisplay());
//		rm.addImage("Olifant"); //$NON-NLS-1$
	
		rm.loadCommands("net.sf.okapi.applications.olifant.Commands"); //$NON-NLS-1$

		// Create the filter configuration mapping
		fcMapper = new FilterConfigurationMapper();
		// Get pre-defined configurations
		fcMapper.addConfigurations("net.sf.okapi.filters.tmx.TmxFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.xliff.XLIFFFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.po.POFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.ttx.TTXFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.rtf.RTFFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.ts.TsFilter");
	}

	public void run () {
		try {
			Display Disp = shell.getDisplay();
			while ( !shell.isDisposed() ) {
				if (!Disp.readAndDispatch())
					Disp.sleep();
			}
		}
		finally {
			// Dispose of any global resources
			closeRepository();
			if ( rm != null ) rm.dispose();
		}
	}
	
	private void selectRepository () {
		try {
			RepositoryForm dlg = new RepositoryForm(shell);
			String[] res = dlg.showDialog();
			if ( res == null ) return; // No repository selected
			openRepository(res[0], res[1]);
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error selecting repository.\n"+e.getMessage(), null);
		}
	}
	
	private void closeRepository () {
		if ( repo != null ) {
			repo.close();
			repo = null;
		}
	}
	
	private void openRepository (String type,
		String name)
	{
		try {
			// Make sure we close the previous repository
			closeRepository();

			// Instantiate the new repository
			if ( type.equals("m") ) {
				//repo = new net.sf.okapi.lib.tmdb.memory.Repository();
			}
			else if ( type.equals("d") ) {
				repo = new net.sf.okapi.lib.tmdb.local.Repository();
				((net.sf.okapi.lib.tmdb.local.Repository)repo).open(name, true);
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error opening repository.\n"+e.getMessage(), null);
		}
		finally {
			// Update the display
			updateCommands();
			updateTitle();
		}
	}
	
	private void openFile () {
		try {
			String[] paths = Dialogs.browseFilenames(shell, "Open Files", true, null, null, null);
			if ( paths == null ) return;
			boolean acceptAll = false;
			for ( String path : paths ) {
				Boolean res;
				if ((res = addDocumentFromUI(path, paths.length>1, acceptAll)) == null ) {
					return; // Stop now
				}
				// Else use the result to set the next value of the accept-all button
				acceptAll = res;
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error opening document.\n"+e.getMessage(), null);
		}
	}

	/**
	 * Adds a document using the UI dialog.
	 * @param path the path of the document to add.
	 * @param batchMode true if the check box to accept all next documents should be displayed.
	 * @param acceptAll value of the check box to accept all.
	 * @return Null if the user cancel the operation, otherwise: true if the accept-all button was checked,
	 * or false if we are not in batch mode or if the accept-all button was not checked.
	 */
	private Boolean addDocumentFromUI (String path,
		boolean batchMode,
		boolean acceptAll)
	{
		try {
			InputDocumentDialog dlg = new InputDocumentDialog(shell, "Input Document",
				fcMapper, batchMode);
			// Lock the locales if we have already documents in the session
			boolean canChangeLocales = true; //session.getDocumentCount()==0;
			dlg.setLocalesEditable(canChangeLocales);
			// Set default data
			dlg.setData(path, null, "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH);
			
			if ( batchMode && ( path != null )) {
				dlg.setAcceptAll(acceptAll);
			}

			// Edit
			Object[] data = dlg.showDialog();
			if ( data == null ) return null;
			
			// Create the raw document to add to the session
			URI uri = (new File((String)data[0])).toURI();
			RawDocument rd = new RawDocument(uri, (String)data[2], (LocaleId)data[3], (LocaleId)data[4]);
			rd.setFilterConfigId((String)data[1]);
			
			// Create the TM in the repository
			String filename = Util.getFilename(rd.getInputURI().getPath(), false);
//TODO: check if it exists			
			ITm tm = repo.createTm(filename, null, (LocaleId)data[3]);
			// Create the tab for that TM
			TmPanel tp = addTmTab(tm);
			
			// Start the import thread
			Importer imp = new Importer(fcMapper, tm, rd, tp.getLog());
			imp.addObserver(tp);
			tp.startThread(new Thread(imp));
			updateCommands();
			
			// If dialog return OK, we return value of accept all
			return (Boolean)data[5];
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error adding document.\n"+e.getMessage(), null);
			return null;
		}
	}

//	//test for threading
//	private static Thread addRawDocument (IFilterConfigurationMapper p_fcMapper,
//		ITm p_tm,
//		RawDocument p_rd,
//		LogPanel p_logPanel)
//	{
//		final IFilterConfigurationMapper fcMapper = p_fcMapper;
//		final RawDocument rd = p_rd;
//		final ITm tm = p_tm;
//		final LogPanel logPanel = p_logPanel;
//
//		return new Thread () {
//			public void run () {
//				Importer imp = new Importer(fcMapper, tm, rd, logPanel);
//				imp.process();
//			}
//		};
//	}
//	
//	private void addRawDocument (RawDocument rd) {
//		TmPanel tp = null;
//		IFilter filter = null;
//		try {
//			filter = fcMapper.createFilter(rd.getFilterConfigId());
//			filter.open(rd);
//			LinkedHashMap<String, String> mapTUProp = new LinkedHashMap<String, String>();
//			LinkedHashMap<String, String> mapSrcProp = new LinkedHashMap<String, String>();
//			LinkedHashMap<String, String> mapTrgProp = new LinkedHashMap<String, String>();
//			LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
//			String[] trgFields;
//			String srcDbLang = DbUtil.toDbLang(rd.getSourceLocale());
//			
//			while ( filter.hasNext() ) {
//				Event event = filter.next();
//				if ( !event.isTextUnit() ) continue;
//				
//				ITextUnit tu = event.getTextUnit();
//				ISegments srcSegs = tu.getSourceSegments();
//				
//				// Get text-unit level properties
//				mapTUProp.clear();
//				for ( String name : tu.getPropertyNames() ) {
//					mapTUProp.put("@"+name, tu.getProperty(name).getValue());
//				}
//				
//				// Get source container properties
//				mapSrcProp.clear();
//				for ( String name : tu.getSourcePropertyNames() ) {
//					mapSrcProp.put("@"+name+"_"+srcDbLang, tu.getSourceProperty(name).getValue());
//				}
//	
//				// For each source segment
//				for ( net.sf.okapi.common.resource.Segment srcSeg : srcSegs ) {
//	
//					// Get the source fields
//					String[] srcFields = DbUtil.fragmentToTmFields(srcSeg.getContent());
//					map.clear();
//					map.put(DbUtil.TEXT_PREFIX+srcDbLang, srcFields[0]);
//	
//					// For each target
//					for ( LocaleId locId : tu.getTargetLocales() ) {
//						String trgDbLang = DbUtil.toDbLang(locId);
//						
//						mapTrgProp.clear();
//						for ( String name : tu.getTargetPropertyNames(locId) ) {
//							mapTrgProp.put("@"+name+"_"+trgDbLang, tu.getTargetProperty(locId, name).getValue());
//						}
//						
//						// Get the target segment
//						net.sf.okapi.common.resource.Segment trgSeg = tu.getTargetSegments(locId).get(srcSeg.getId());
//						if ( trgSeg != null ) {
//							trgFields = DbUtil.fragmentToTmFields(trgSeg.getContent());
//						}
//						else {
//							trgFields = new String[2];
//						}
//						map.put(DbUtil.TEXT_PREFIX+trgDbLang, trgFields[0]);
//					}
//					// Add the record to the database
//					if ( !mapTUProp.isEmpty() ) {
//						map.putAll(mapTUProp);
//					}
//					if ( !mapSrcProp.isEmpty() ) {
//						map.putAll(mapSrcProp);
//					}
//					if ( !mapTrgProp.isEmpty() ) {
//						map.putAll(mapTrgProp);
//					}
//					tm.addRecord(map);
//				}
//			}
//		}
//		finally {
//			if ( filter != null ) {
//				filter.close();
//			}
//			if ( tp != null ) {
//				tp.resetTmDisplay();
//			}
//		}
//	}

}
