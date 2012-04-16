/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

import java.util.HashMap;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.IIndexAccess;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.lucene.TmHit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class QueryTMForm implements ISegmentEditorUser {
	
	private final Shell shell;
	private final ITm tm;
	private final SegmentEditor seQuery;
	private final SegmentEditor seMatch;
	private final Button btQuery;
	private final Table table;
	private final Spinner spThreshold;
	private final Combo cbLocales;
	private final Spinner spMaxHits;
	private final Button btAttributes;
	private final Label stCount;
	private HashMap<String, String> attributes;

	public QueryTMForm (Shell parent,
		ITm tm)
	{
		shell = new Shell(parent, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Query Matches");
		UIUtil.inheritIcon(shell, parent);
		shell.setLayout(new GridLayout(8, false));

		this.tm = tm;
		attributes = new HashMap<String, String>();

		Label label = new Label(shell, SWT.NONE);
		label.setText("Text to match:");
		GridData gdTmp = new GridData();
		gdTmp.horizontalSpan = 8;
		label.setLayoutData(gdTmp);
		
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalSpan = 8;
		gdTmp.widthHint = 550;
		gdTmp.heightHint = 70;
		seQuery = new SegmentEditor(shell, -1, this, gdTmp);
		
		btQuery = UIUtil.createGridButton(shell, SWT.PUSH, "Search", UIUtil.BUTTON_DEFAULT_WIDTH, 1);
		btQuery.setText("Search");
		btQuery.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				search();
			}
		});
		
		label = new Label(shell,SWT.NONE);
		label.setText("Threshold:");
		
		spThreshold = new Spinner(shell, SWT.BORDER);
		spThreshold.setMaximum(100);
		spThreshold.setMinimum(1);
		spThreshold.setPageIncrement(10);
		spThreshold.setSelection(50);
		
		cbLocales = new Combo(shell, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		gdTmp = new GridData();
		gdTmp.widthHint = 80;
		cbLocales.setLayoutData(gdTmp);
		java.util.List<String> tmpList = DbUtil.indexInfoFromString(tm.getIndexInfo());
		if ( !Util.isEmpty(tmpList) ) {
			for ( String fn : tmpList ) {
				if ( fn.startsWith(DbUtil.TEXT_PREFIX) ) {
					cbLocales.add(DbUtil.getFieldLocale(fn));
				}
			}
		}
		cbLocales.select(0);
		
		label = new Label(shell,SWT.NONE);
		label.setText("Maxinum hits:");
		
		spMaxHits = new Spinner(shell, SWT.BORDER);
		spMaxHits.setMaximum(500);
		spMaxHits.setMinimum(1);
		spMaxHits.setPageIncrement(10);
		spMaxHits.setSelection(100);
		
		btAttributes = new Button(shell, SWT.PUSH);
		updateAttributesDisplay();
		btAttributes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				editAttributes();
			}
		});
		
		stCount = new Label(shell, SWT.RIGHT);
		gdTmp = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gdTmp.widthHint = 130;
		stCount.setLayoutData(gdTmp);

		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalSpan = 8;
		gdTmp.heightHint = 70;
		seMatch = new SegmentEditor(shell, -1, this, gdTmp);
		seMatch.setEditable(false);
		
		// Creates the table
		table = new Table(shell, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 200;
		gdTmp.horizontalSpan = 8;
		table.setLayoutData(gdTmp);

		table.addControlListener(new ControlAdapter() {
		    public void controlResized(ControlEvent e) {
		    	try {
		    		table.setRedraw(false);
		    		Rectangle rect = table.getClientArea();
		    		int keyColWidth = table.getColumn(0).getWidth();
		    		int scoreColWidth = table.getColumn(1).getWidth();
		    		table.getColumn(2).setWidth(rect.width-(keyColWidth+scoreColWidth));
		    	}
		    	finally {
		    		table.setRedraw(true);
		    	}
		    }
		});
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				displayCurrentMatch();
            }
		});
		
		// Create the table columns
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(DbUtil.SEGKEY_NAME);
		col.setWidth(90);
		col = new TableColumn(table, SWT.NONE);
		col.setText("Similarity");
		col.setWidth(80);
		col = new TableColumn(table, SWT.NONE);
		col.setText("Text");
		col.setWidth(200);

		shell.setDefaultButton(btQuery);
		
		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
		
	}

	void showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		
		seQuery.dispose();
		seMatch.dispose();
	}

	private void displayCurrentMatch () {
		int n = table.getSelectionIndex();
		if ( n == -1 ) {
			seMatch.setText(null, null, -1);
			stCount.setText("");
		}
		else {
			TableItem ti = table.getItem(n);
			seMatch.setText(ti.getText(2), null, -1);
			stCount.setText(String.format("Match %d of %d  ", n+1, table.getItemCount()));
		}
	}
	
	private void editAttributes () {
		try {
			QueryAttributesForm dlg = new QueryAttributesForm(shell, tm, attributes);
			HashMap<String, String> newAttrs = dlg.showDialog();
			if ( newAttrs == null ) return; // Cancel
			// Else: set the new list
			attributes = newAttrs;
			updateAttributesDisplay();
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error editing attributes:\n"+e.getMessage(), null);
		}
	}
	
	private void updateAttributesDisplay () {
		btAttributes.setText(String.format("Attributes (%d)...", attributes.size()));
	}
	
	private void search () {
		try {
			String text = seQuery.getText();
			if ( text.trim().isEmpty() ) {
				Dialogs.showError(shell, "You must enter a text to query.", null);
				seQuery.setFocus();
				return;
			}
			
			btQuery.setEnabled(false);
			table.removeAll();
			stCount.setText("Searching...  ");
			seMatch.clear();
			
			IRepository repo = tm.getRepository();
			IIndexAccess ia = repo.getIndexAccess();

			int count = ia.search(text, null, tm.getUUID(), cbLocales.getText(), spMaxHits.getSelection(),
				spThreshold.getSelection(), attributes);
			if ( count == 0 ) {
				stCount.setText("<No match found>");
				return;
			}
			
			// Else: fill the table
			java.util.List<TmHit> res = ia.getHits();
			for ( TmHit hit : res ) {
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(hit.getSegKey());
				ti.setText(1, String.format("%f", hit.getScore()));
				ti.setText(2, hit.getVariant().getGenericTextField().stringValue());
			}
			// Select the first match as the current one
			table.select(0);
			displayCurrentMatch();			
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, "Error when searching:\n"+e.getMessage(), null);
		}
		finally {
			btQuery.setEnabled(true);
		}
	}

	@Override
	public boolean returnFromEdit (boolean save) {
		if ( save ) search();
		return true;
	}

	@Override
	public void notifyOfFocus (int field) {
		// TODO Auto-generated method stub
	}

}
