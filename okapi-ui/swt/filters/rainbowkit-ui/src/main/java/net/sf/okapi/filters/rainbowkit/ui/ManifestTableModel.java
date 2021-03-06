/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.rainbowkit.ui;

import net.sf.okapi.filters.rainbowkit.Manifest;
import net.sf.okapi.filters.rainbowkit.MergingInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class ManifestTableModel {
	
	private Table table;
	private Manifest manifest;

	public void linkTable (Table newTable) {
		table = newTable;
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText("Documents to Post-Process");
		col = new TableColumn(table, SWT.NONE);
		col.setText("Missing?");
		col = new TableColumn(table, SWT.NONE);
		col.setText("Output");
	}
	
	public void setManifest (Manifest newManifest) {
		manifest = newManifest;
		updateTable(null, 0);
	}

	void updateTable (int[] selection,
		int index)
	{
		table.removeAll();
		MergingInfo info;
		for ( int i : manifest.getItems().keySet() ) {
			info = manifest.getItem(i);
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, info.getRelativeInputPath());
//			if ( !info.exists() ) item.setText(1, "missing");
			item.setText(2, info.getRelativeTargetPath());
			item.setChecked(info.getSelected());
		}
		if ( selection == null ) {
			if ( table.getItemCount() > 0 ) {
				if ( index > -1 ) {
					if ( index > table.getItemCount()-1 ) {
						index = table.getItemCount()-1;
					}
				}
				else index = 0;
				table.setSelection(index);
			}
			// Else: nothing to select	
		}
		else table.setSelection(selection);
	}

	public void saveData () {
		MergingInfo info;
		for ( int i=0; i<table.getItemCount(); i++ ) {
			info = manifest.getItem(i+1); // docID are 1-based
			if ( info == null ) continue; // Could be a non-extractable file
			info.setSelected(table.getItem(i).getChecked());
		}
	}
}
