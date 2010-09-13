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

package net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipelinedriver.IBatchItemContext;

public class Batch extends BatchItem {

	private List<IBatchItemContext> items;
	
	public Batch() {
		super();
		this.items = new ArrayList<IBatchItemContext>();
	}
	
	public Batch(BatchItem... items) {		
		this();		
		addItems(items);
	}

	public void setItems(List<IBatchItemContext> items) {
		this.items = items;
	}

	public List<IBatchItemContext> getItems() {
		return items;
	}

	public Batch addItems(BatchItem... items) {
		for (BatchItem item : items)
			if (item instanceof Batch) 
				this.items.addAll(((Batch)item).getItems());
			else
				this.items.add(item.getContext());
		return this;
	}
			
	public Batch addItems(String dir, String[] fileList, 
			String defaultEncoding, LocaleId sourceLocale, LocaleId targetLocale) {		
		for (String file : fileList) {
			this.items.add(new BatchItem(Util.toURI(dir + file), defaultEncoding, sourceLocale, targetLocale).getContext());
		}
		return this;
	}
	
	public Batch addItems(String dir, String[] fileList, 
			String defaultEncoding, URI outputURI, String outputEncoding, LocaleId sourceLocale, LocaleId targetLocale) {		
		for (String file : fileList) {
			this.items.add(new BatchItem(Util.toURI(dir + file), defaultEncoding, outputURI, outputEncoding,
					sourceLocale, targetLocale).getContext());
		}
		return this;
	}
}
