/*===========================================================================*/
/* Copyright (C) 2008 Fredrik Liden                                          */
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

package net.sf.okapi.applications.rainbow.utilities.searchandreplace;

import java.util.regex.Pattern;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class Editor implements IParametersEditor {

	private Shell                 dialog;	
	private OKCancelPanel         pnlActionsDialog;	
	private Shell                 shell;
	private boolean               result = false;
	private OKCancelPanel         pnlActions;
	private Parameters            params;
	private Table                 table;
	private Text                  searchText;
	private Text                  replacementText;
	private Button                btMoveUp;
	private Button                btMoveDown;
	private Button 				  chkPlainText;
	private Button 				  chkRegEx;
	private Button 				  chkDotAll;
	private Button 				  chkIgnoreCase;
	private Button 				  chkMultiLine;
	private int 				  updateType;
	
	
	public static final int ADD_ITEM    = 1;
	public static final int EDIT_ITEM   = 2;

	public boolean edit (IParameters params,
		Object object)
	{
		boolean bRes = false;
		try {
			shell = null;
			this.params = (Parameters)params;
			shell = new Shell((Shell)object, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)object);
			return showDialog();
		}
		catch ( Exception e ) {
			Dialogs.showError(shell, e.getLocalizedMessage(), null);
			bRes = false;
		}
		finally {
			// Dispose of the shell, but not of the display
			if ( shell != null ) shell.dispose();
		}
		return bRes;
	}
	
	public IParameters createParameters () {
		return new Parameters();
	}
	
	
	public void updateUpDownBtnState(){

		int index = table.getSelectionIndex();
		int items = table.getItemCount();
		
        if(items>1){
        	if (index==-1){
	        	btMoveDown.setEnabled(false);
	        	btMoveUp.setEnabled(false);
        	}else if (index==0){
	        	btMoveUp.setEnabled(false);
	        	btMoveDown.setEnabled(true);
	        }else if((index+1)==items){
	        	btMoveDown.setEnabled(false);
	        	btMoveUp.setEnabled(true);
	        }else{
	        	btMoveDown.setEnabled(true);
	        	btMoveUp.setEnabled(true);
	        }
        }else{
        	btMoveDown.setEnabled(false);
        	btMoveUp.setEnabled(false);
        }
	}
	
	
	private void create (Shell parent)
	{
		shell.setText("Search And Replace");
		if ( parent != null ) shell.setImage(parent.getImage());

		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);
		
		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		tfTmp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite cmpTmp0 = new Composite(tfTmp, SWT.NONE);
		cmpTmp0.setLayout(new GridLayout(4, false));
		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Options");
		tiTmp.setControl(cmpTmp0);		
		
		// search and replace grid items
		table = new Table (cmpTmp0, SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible (true);
		table.setLinesVisible (true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		//--click updates button states--
		table.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				if ( event.detail!=SWT.CHECK ) {
					updateUpDownBtnState();
				}
			}
		});		

		//--double-click opens editor--
		table.addListener (SWT.MouseDoubleClick, new Listener () {
			public void handleEvent (Event event) {
				if(table.getSelectionIndex()!=-1){
					updateType=EDIT_ITEM;
					showAddItemsDialog();
					updateUpDownBtnState();
				}				
			}
		});		
		
		//--resizing the columns--
		table.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {

				int tableWidth = table.getBounds().width;
				int remaining = tableWidth - table.getColumn(0).getWidth();
				table.getColumn(1).setWidth(remaining/2-2);
				table.getColumn(2).setWidth(remaining/2-2);
			}
		});
		
		// table headers
		String[] titles = {"Use", "Search For", "Replace By"};
		for (int i=0; i<titles.length; i++) {
			TableColumn column = new TableColumn (table, SWT.LEFT);
			column.setText (titles [i]);
			column.pack();
		}

		// up and down buttons
		Composite cmpTmp = new Composite(cmpTmp0, SWT.NONE);
		RowLayout rl = new RowLayout();
		rl.type = SWT.VERTICAL;
		rl.pack = false;
		cmpTmp.setLayout(rl);
		
		btMoveUp = new Button(cmpTmp, SWT.PUSH);
		btMoveUp.setText("Move Up");
		btMoveUp.setEnabled(false);
		btMoveUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( table.getSelectionIndex()!=-1 ) {

			        int index = table.getSelectionIndex();
			        boolean isChecked=false;
			        
			        TableItem ti = table.getItem(index);
			        isChecked = ti.getChecked();
			        String[] values = {ti.getText(0), ti.getText(1), ti.getText(2)};
			        ti.dispose();

					ti = new TableItem (table, SWT.NONE,index-1);
					ti.setChecked(isChecked);
					String [] strs =values;
					ti.setText(strs);
					table.select(index-1);
					
					updateUpDownBtnState();
				}
			}
		});	
		btMoveDown = new Button(cmpTmp, SWT.PUSH);
		btMoveDown.setText("Move Down");
		btMoveDown.setEnabled(false);
		btMoveDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				if ( table.getSelectionIndex()!=-1 ) {
					
			        int index = table.getSelectionIndex();
			        boolean isChecked=false;
			        
			        TableItem ti = table.getItem(index);
			        isChecked = ti.getChecked();
			        String[] values = {ti.getText(0), ti.getText(1), ti.getText(2)};
			        ti.dispose();

					ti = new TableItem (table, SWT.NONE,index+1);
					ti.setChecked(isChecked);
					String [] strs =values;
					ti.setText(strs);
					table.select(index+1);
					
					updateUpDownBtnState();
				}
			}
		});	

		// Add, edit, delete buttons
		Composite cmpTmp2 = new Composite(cmpTmp0, SWT.NONE);
		RowLayout rl2 = new RowLayout();
		rl2.pack = false;
		cmpTmp2.setLayout(rl2);
		cmpTmp2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		
		Button btAdd = new Button(cmpTmp2, SWT.PUSH);
		btAdd.setText("Add...");
		btAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateType = ADD_ITEM;
				showAddItemsDialog();
			}
		});		
		Button btEdit = new Button(cmpTmp2, SWT.PUSH);
		btEdit.setText("Edit...");
		btEdit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(table.getSelectionIndex()!=-1){
					updateType = EDIT_ITEM; 
					showAddItemsDialog();
				}				
			}
		});		
		Button btRemove = new Button(cmpTmp2, SWT.PUSH);
		btRemove.setText("Remove");
		btRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(table.getSelectionIndex()!=-1){
					int index = table.getSelectionIndex();
					table.remove(index);
					if(index == table.getItemCount())
						table.setSelection(index-1);
					else
						table.setSelection(index);
					updateUpDownBtnState();
				}
			}
		});	
		
		chkRegEx = new Button(cmpTmp0, SWT.CHECK);
		chkRegEx.setText("Use regular expression");
		chkRegEx.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		chkRegEx.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chkDotAll.setEnabled(chkRegEx.getSelection());
				chkMultiLine.setEnabled(chkRegEx.getSelection());
				chkIgnoreCase.setEnabled(chkRegEx.getSelection());
			}
		});
		
		chkPlainText = new Button(cmpTmp0, SWT.CHECK);
		chkPlainText.setText("Process the files as plain text (not using filters)");
		chkPlainText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Group group = new Group(cmpTmp0, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setText("Regular expression options");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

		chkDotAll = new Button(group, SWT.CHECK);
		chkDotAll.setText("Dot also matches line-feed");
		
		chkMultiLine = new Button(group, SWT.CHECK);
		chkMultiLine.setText("Multi-line");
		
		chkIgnoreCase = new Button(group, SWT.CHECK);
		chkIgnoreCase.setText("Ignore case differences");
		

		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) {
					//TODO: Call help
					return;
				}
				if ( e.widget.getData().equals("o") ){
					if(!saveData()){
						return;
					}
				}
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		pnlActions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		shell.setDefaultButton(pnlActions.btOK);

		setData();
		shell.pack();
		shell.setMinimumSize(shell.getSize());
		shell.setSize(shell.getSize().x,shell.getSize().y+50);
		Dialogs.centerWindow(shell, parent);
	}
	
	
	private boolean showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

	
	private boolean showAddItemsDialog () {
		dialog = new Shell (shell);
		dialog.setText ("Search And Replace Item");
		dialog.setSize (400, 200);

		dialog.setLayout(new GridLayout());

		// start - content
		Label label = new Label(dialog, SWT.NONE);
		label.setText("Search Expression:");
		
		searchText = new Text(dialog, SWT.BORDER | SWT.WRAP | SWT.SINGLE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		searchText.setLayoutData(gridData);		
		
		label = new Label(dialog, SWT.NONE);
		label.setText("Replacement Expression:");

		replacementText = new Text(dialog, SWT.BORDER | SWT.WRAP | SWT.SINGLE);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		replacementText.setLayoutData(gridData);		
		// end - content		

		// start - dialog level buttons
		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) {
					//TODO: Call help
					return;
				}
				if ( e.widget.getData().equals("o") ) {
					
					//--validating empty search string--
					if(searchText.getText().trim().length()<1){
						Dialogs.showError(shell, "You need to provide a search expression", null);
						return;
					}					
					
					//--validating regEx--
					if( chkRegEx.getSelection() ){
						try{
							Pattern.compile(searchText.getText());
							Pattern.compile(replacementText.getText());

						}catch(Exception ex){
							Dialogs.showError(shell, ex.getLocalizedMessage(), null);
							return;
						}
					}
					
					//saveData();
					if(updateType==EDIT_ITEM){
						int index = table.getSelectionIndex();
				        TableItem ti = table.getItem(index);
				        String [] s ={"",searchText.getText(),replacementText.getText()};
				        ti.setText(s);
					}else{
						TableItem item = new TableItem (table, SWT.NONE);
						String [] strs ={"",searchText.getText(),replacementText.getText()};
						item.setText(strs);
						item.setChecked(true);
						table.setSelection(table.getItemCount()-1);
						updateUpDownBtnState();
					}
				}
				
				dialog.close();
			};
		};

		pnlActionsDialog = new OKCancelPanel(dialog, SWT.NONE, OKCancelActions, true);
		pnlActionsDialog.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dialog.setDefaultButton(pnlActionsDialog.btOK);
		// end - dialog level buttons
		
		// begin - initialize edit fields
		if ( updateType==EDIT_ITEM ) {
			int index = table.getSelectionIndex();
	        TableItem ti = table.getItem(index);
	        searchText.setText(ti.getText(1));
	        replacementText.setText(ti.getText(2));
		}
		// end - initialize edit fields
		
		Dialogs.centerWindow(dialog, shell);
		dialog.open ();
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
		return result;
	}
	
	private void setData () {
		
		chkPlainText.setSelection(params.plainText);
		chkRegEx.setSelection(params.regEx);
		chkDotAll.setSelection(params.dotAll);
		chkIgnoreCase.setSelection(params.ignoreCase);
		chkMultiLine.setSelection(params.multiLine);

		chkDotAll.setEnabled(chkRegEx.getSelection());
		chkMultiLine.setEnabled(chkRegEx.getSelection());
		chkIgnoreCase.setEnabled(chkRegEx.getSelection());		
		
		table.clearAll();
        for ( String[] s : params.rules ) {
        	TableItem item = new TableItem (table, SWT.NONE);
			String [] strs ={"",s[1],s[2]};
			item.setText(strs);
			if ( s[0].equals("true") ) {
				item.setChecked(true);				
			}
        }
        table.setSelection(0);
        updateUpDownBtnState();
	}

	private boolean saveData () {
		
		// validate regular expressions
		if(chkRegEx.getSelection() && !validRegEx()) return false;
		
		// make sure the list is not empty
		if (table.getItemCount()==0){
			Dialogs.showError(shell, "You need to provide a search expression", null);
			return false;
		}
		
		params.reset();
		for ( int i=0; i<table.getItemCount(); i++ ) {
			TableItem ti = table.getItem(i);
			String s[]=new String[3];
			s[0]=Boolean.toString(ti.getChecked());
			s[1]=ti.getText(1);
			s[2]=ti.getText(2);
        	params.addRule(s);
		};
	
		params.plainText = chkPlainText.getSelection();		
		params.regEx = chkRegEx.getSelection();
		params.dotAll = chkDotAll.getSelection();		
		params.ignoreCase = chkIgnoreCase.getSelection();
		params.multiLine = chkMultiLine.getSelection();

		result = true;
		return result;
	}

	private boolean validRegEx(){

		for ( int i=0; i<table.getItemCount(); i++ ) {
			TableItem ti = table.getItem(i);
			try{
				Pattern.compile(ti.getText(1));
				Pattern.compile(ti.getText(1));

			}catch(Exception ex){
				Dialogs.showError(shell, ex.getLocalizedMessage(), null);
				return false;
			}			
		};
		return true;
	}
}
