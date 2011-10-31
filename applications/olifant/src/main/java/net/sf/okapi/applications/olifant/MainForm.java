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
import java.net.URLDecoder;

import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.ui.AboutDialog;
import net.sf.okapi.common.ui.BaseHelp;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ResourceManager;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.common.ui.UserConfiguration;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.Importer;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class MainForm {
	
	public static final String APPNAME = "Olifant"; //$NON-NLS-1$

	public static final String OPT_BOUNDS = "bounds"; //$NON-NLS-1$
	public static final String OPT_MAXIMIZED = "maximized"; //$NON-NLS-1$
	public static final String OPT_REPOSITORYTYPE = "repositoryType"; //$NON-NLS-1$
	public static final String OPT_REPOSITORYARG = "repositoryArg"; //$NON-NLS-1$
	public static final String OPT_TMOPT = "tmOptions_"; //$NON-NLS-1$

	private static final String HELP_USAGE = "Olifant - Usage"; //$NON-NLS-1$
	
	private Shell shell;
	private UserConfiguration config;
	private ResourceManager rm;
	private IFilterConfigurationMapper fcMapper;
	private SashForm topSash;
	private CTabFolder tabs;
	private RepositoryPanel repoPanel;
	private TmPanel currentTP;
	private StatusBar statusBar;
	private ToolBarWrapper toolBar;
	private IHelp help;
	
	private MenuItem miFileOpen;
	private MenuItem miTMNew;
	private MenuItem miTMClose;
	private MenuItem miTMImport;
	private MenuItem miTMExport;
	private MenuItem miTMDelete;
	private MenuItem miTMRename;
	private MenuItem miTMEditColumns;
	private MenuItem miTMEditLocales;
	private MenuItem miTMEditProperties;
	private MenuItem miShowHideThirdField;
	private MenuItem miShowHideFieldList;
	private MenuItem miStatistics;
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
			
	    	// Get the location of the main class source
	    	File file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
	    	String appRootFolder = URLDecoder.decode(file.getAbsolutePath(),"utf-8"); //$NON-NLS-1$
	    	// Remove the JAR file if running an installed version
	    	boolean fromJar = appRootFolder.endsWith(".jar");
	    	if ( fromJar ) appRootFolder = Util.getDirectoryName(appRootFolder); //$NON-NLS-1$
			help = new BaseHelp(appRootFolder);
			
			// Temporary ALPHA version warning for end-user (not when running under eclipse)
			//TODO: Remove this warning when ready
			if ( fromJar ) {
				Dialogs.showWarning(shell, "This version of Olifant is only ALPHA\nIt should not be used for production.\n\n"
					+ "If you are on Windows you can still use the .NET version of Olifant\n(http://sourceforge.net/projects/okapi/files/)", null);
			}

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

	Shell getShell () {
		return shell;
	}
	
	IHelp getHelp () {
		return help;
	}

	boolean canCloseRepository () {
		for ( CTabItem ti : tabs.getItems() ) {
			if ( !((TmPanel)ti.getControl()).canClose() ) return false;
		}
		return true;
	}
	
	private void createContent ()
		throws Exception
	{
		shell.setLayout(new GridLayout(1, false));
		shell.setImage(rm.getImage("Olifant")); //$NON-NLS-1$
		
		// Handling of the closing event
		shell.addShellListener(new ShellListener() {
			public void shellActivated(ShellEvent event) {}
			public void shellClosed(ShellEvent event) {
				saveUserConfiguration();
				if ( !canCloseRepository() ) event.doit = false;
			}
			public void shellDeactivated(ShellEvent event) {}
			public void shellDeiconified(ShellEvent event) {}
			public void shellIconified(ShellEvent event) {}
		});

		toolBar = new ToolBarWrapper(this);
		
		createMenus();

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
				setCurrentTP((TmPanel)tabs.getSelection().getControl());
			}
		});

		tabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close (CTabFolderEvent event) {
				// Get the tab panel from the closing item
				CTabItem ti = (CTabItem)(CTabItem)event.item;
				TmPanel tp = (TmPanel)ti.getControl();;
				// Check if we can close it
				if ( !tp.canClose() ) {
					event.doit = false;
					return;
				}
				// When removing dynamically a tab we need to manually dispose 
				// of both the tabItem and its control.
				ti.dispose();
				tp.dispose();
				// If we are closing the last tab we need to manually set the current TP to null
				if ( tabs.getItemCount() < 1 ) {
					setCurrentTP(null);
				}
			}
		});
		
		repoPanel = new RepositoryPanel(this, topSash, SWT.NONE, rm);
		repoPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

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
	
	/**
	 * Locates the tab for a given TM name.
	 * @param tmName the name of the tM to look for.
	 * @param selectTab true to make the found tab active, false to not change the tab selection.
	 * @return the TmPanel for that TM or null if no panel is open.
	 */
	TmPanel findTmTab (String tmName,
		boolean selectTab)
	{
		TmPanel tp;
		for ( CTabItem ti : tabs.getItems() ) {
			tp = (TmPanel)ti.getControl();
			if ( tmName.equals(tp.getTm().getName()) ) {
				if ( selectTab ) {
					tabs.setSelection(ti);
					setCurrentTP(tp);
				}
				return tp;
			}
		}
		return null;
	}

	boolean closeTmTab (CTabItem tabItem) {
		try {
			// If tabItem is null: use the current tab
			if ( tabItem == null ) {
				tabItem = tabs.getSelection();
				if ( tabItem == null ) return true; // Nothing to close
			}
			
			// Get the TmPanel
			TmPanel tp = (TmPanel)tabItem.getControl();
			// Verify that we can close
			if ( !tp.canClose() ) return false;
			// When removing dynamically a tab we need to manually dispose 
			// of both the tabItem and its control.
			tabItem.dispose();
			tp.dispose();
			// If we are closing the last tab we need to manually set the current TP to null
			if ( tabs.getItemCount() < 1 ) {
				setCurrentTP(null);
			}
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error closing tab.\n"+e.getMessage(), null);
		}
		return true;
	}
	
	void closeAllTmTabs () {
		while ( tabs.getItemCount() > 0 ) {
			closeTmTab(tabs.getItem(0));
		}
	}
	
	void updateCurrentTmTab () {
		updateCommands();
		if ( currentTP == null ) {
			statusBar.setCounter(-1, 0);
			statusBar.setPage(-1, 0);
			statusBar.clearInfo();
		}
		else {
			currentTP.updateCurrentEntry();
			statusBar.setPage(currentTP.getTm().getCurrentPage(), currentTP.getTm().getPageCount());
		}
	}
	
	void setCurrentTP (TmPanel tp) {
		currentTP = tp;
		updateCurrentTmTab();
	}

	void updateTitle () {
		String name = repoPanel.getRepositoryName();
		if ( name == null ) name = RepositoryPanel.NOREPOSELECTED_TEXT;
		shell.setText(APPNAME + " (ALPHA) - " + name);
	}

	void updateCommands () {
		boolean active = (repoPanel.isRepositoryOpen() && ( currentTP != null ) && !currentTP.hasRunningThread());
		
		miTMNew.setEnabled(repoPanel.isRepositoryOpen());
		miTMClose.setEnabled(active);
		miTMImport.setEnabled(active);
		miTMExport.setEnabled(active);
		miTMDelete.setEnabled(active);
		miTMRename.setEnabled(active);
		miTMEditColumns.setEnabled(active);
		miTMEditLocales.setEnabled(active);
		miTMEditProperties.setEnabled(active);
		
		miShowHideLog.setEnabled(active);
		miShowHideThirdField.setEnabled(active);
		miShowHideFieldList.setEnabled((active) && currentTP.getEditorPanel().isExtraVisible());
		miStatistics.setEnabled(repoPanel.isRepositoryOpen());
		
		toolBar.update(currentTP);
	}
	
	TmPanel addTmTabEmpty (ITm tm) {
		if ( tm == null ) return null;
		TmPanel tp = new TmPanel(this, tabs, SWT.NONE, tm, statusBar);
		CTabItem ti = new CTabItem(tabs, SWT.NONE);
		ti.setText(tm.getName());
		ti.setControl(tp);
		tp.setTabItem(ti);
		return tp;
	}

	private void createMenus () {
		// Menus
	    Menu menuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menuBar);

		//--- File menu
		
		MenuItem topItem = new MenuItem(menuBar, SWT.CASCADE);
		topItem.setText(rm.getCommandLabel("file")); //$NON-NLS-1$
		Menu dropMenu = new Menu(shell, SWT.DROP_DOWN);
		topItem.setMenu(dropMenu);

		MenuItem menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "file.selectrepository"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.selectRepository();
            }
		});

		new MenuItem(dropMenu, SWT.SEPARATOR);

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
		
		//--- Translation memory menu
		
		topItem = new MenuItem(menuBar, SWT.CASCADE);
		topItem.setText(rm.getCommandLabel("tm")); //$NON-NLS-1$
		dropMenu = new Menu(shell, SWT.DROP_DOWN);
		topItem.setMenu(dropMenu);
		
		miTMNew = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMNew, "tm.new"); //$NON-NLS-1$
		miTMNew.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.createTM();
            }
		});
		
		miTMClose = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMClose, "tm.close"); //$NON-NLS-1$
		miTMClose.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				closeTmTab(null); // null=use current tab
            }
		});
		
		new MenuItem(dropMenu, SWT.SEPARATOR);

		miTMImport = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMImport, "tm.import"); //$NON-NLS-1$
		miTMImport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.importDocument(currentTP.getTm().getName());
            }
		});
		
		miTMExport = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMExport, "tm.export"); //$NON-NLS-1$
		miTMExport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.exportTM(currentTP.getTm().getName());
            }
		});
		
		new MenuItem(dropMenu, SWT.SEPARATOR);

		miTMEditColumns = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMEditColumns, "tm.editcolumns"); //$NON-NLS-1$
		miTMEditColumns.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.editColumns();
				}
            }
		});
		
		miTMEditLocales = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMEditLocales, "tm.editlocales"); //$NON-NLS-1$
		miTMEditLocales.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					currentTP.editLocales();
				}
            }
		});
		
		miTMEditProperties = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMEditProperties, "tm.properties"); //$NON-NLS-1$
		miTMEditProperties.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( currentTP != null ) {
					repoPanel.editTmOptions(currentTP.getTm().getName());
				}
            }
		});

		new MenuItem(dropMenu, SWT.SEPARATOR);

		miTMRename = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMRename, "tm.rename"); //$NON-NLS-1$
		miTMRename.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.renameTm(currentTP.getTm().getName());
            }
		});
		
		miTMDelete = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miTMDelete, "tm.delete"); //$NON-NLS-1$
		miTMDelete.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.deleteTm(currentTP.getTm().getName());
            }
		});
		
		//--- View menu
		
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

		miStatistics = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(miStatistics, "view.stats"); //$NON-NLS-1$
		miStatistics.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				repoPanel.getStatistics();
            }
		});
		
		//--- Help menu
		
		topItem = new MenuItem(menuBar, SWT.CASCADE);
		topItem.setText(rm.getCommandLabel("help")); //$NON-NLS-1$
		dropMenu = new Menu(shell, SWT.DROP_DOWN);
		topItem.setMenu(dropMenu);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.topics"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( help != null ) help.showWiki("Olifant"); //$NON-NLS-1$
			}
		});

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.howtouse"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( help != null ) help.showWiki(HELP_USAGE); //$NON-NLS-1$
			}
		});

		menuItem = new MenuItem(dropMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.feedback"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				UIUtil.start("mailto:okapitools@opentag.com&subject=Feedback (Olifant)"); //$NON-NLS-1$
			}
		});
		
		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.bugreport"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				UIUtil.start("http://code.google.com/p/okapi/issues/list"); //$NON-NLS-1$
			}
		});
		
		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.featurerequest"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				UIUtil.start("http://code.google.com/p/okapi/issues/list"); //$NON-NLS-1$
			}
		});
		
		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.users"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				UIUtil.start("http://groups.yahoo.com/group/okapitools/"); //$NON-NLS-1$
			}
		});
		
		menuItem = new MenuItem(dropMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.tmx14b"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				UIUtil.start("http://www.gala-global.org/oscarStandards/tmx/tmx14b.html"); //$NON-NLS-1$
            }
		});

		new MenuItem(dropMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(dropMenu, SWT.PUSH);
		rm.setCommand(menuItem, "help.about"); //$NON-NLS-1$
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				AboutDialog dlg = new AboutDialog(shell,
					"About Olifant",
					"Olifant (ALPHA) - Okapi Translation Memory Manager",
					getClass().getPackage().getImplementationVersion());
				dlg.showDialog();
			}
		});

	}

	private void loadResources ()
		throws Exception 
	{
		rm = new ResourceManager(MainForm.class, shell.getDisplay());
		rm.addImage("Olifant"); //$NON-NLS-1$
	
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
			if ( rm != null ) rm.dispose();
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
			
			// Create an empty TM (filling the display is done later)
			TmPanel tp = repoPanel.createTmAndTmTab(filename, null, (LocaleId)data[3], false);
			// Trigger the import 
			if ( tp != null ) {
				// Start the import thread
				ProgressCallback callback = new ProgressCallback(tp);
				Importer imp = new Importer(callback, tp.getTm(), rd, fcMapper);
				tp.startThread(new Thread(imp));
			}
			
			// If dialog return OK, we return value of accept all
			return (Boolean)data[5];
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error adding document.\n"+e.getMessage(), null);
			return null;
		}
	}

	IFilterConfigurationMapper getFCMapper () {
		return fcMapper;
	}
}
