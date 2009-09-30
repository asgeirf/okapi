/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.ui.abstracteditor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.lib.extra.INotifiable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractBaseDialog implements INotifiable {

	static final public String REGISTER_DIALOG_PAGE = "REGISTER_DIALOG_PAGE"; 
	static final public String UNREGISTER_DIALOG_PAGE = "UNREGISTER_DIALOG_PAGE";
	static final public String NOTIFICATION_OK = "NOTIFICATION_OK";
	static final public String NOTIFICATION_CANCEL = "NOTIFICATION_CANCEL";
	
	protected boolean result = true;
	protected Shell shell;
	protected Shell parent;
	private String caption;
	private Object data = null;
	private IHelp help;
	protected IDialogPage page;
	protected Composite pageC;
	private Class<? extends Composite> pageClass;

	protected int getStyle() {
		
		return SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL;
//		return 
//		//SWT.CLOSE |
//		//SWT.BORDER |
//		SWT.TITLE | 
//		SWT.RESIZE | 
//		SWT.APPLICATION_MODAL;
	}
	
	protected abstract void setActionButtonsPanel(Shell shell, SelectionAdapter listener);
	protected abstract void init();
	protected abstract void done();
	
	protected void create (Shell p_Parent) {
				
		try {	
			result = false;
			
			shell = new Shell(p_Parent, getStyle());
			if (shell == null) return;
			
			shell.setText(caption);
			shell.setData("owner", this);			
			shell.setData("parent", p_Parent);
			
			//if ( p_Parent != null ) shell.setImage(p_Parent.getImage());
			
			GridLayout layTmp = new GridLayout();		
			layTmp.marginBottom = 0;
			layTmp.verticalSpacing = 0;
			shell.setLayout(layTmp);
			
							
			if (pageClass == null) return;
				
			Constructor<? extends Composite> cc = (Constructor<? extends Composite>) pageClass.getConstructor(new Class[] {Composite.class, int.class});
			
			if (cc == null) return;
			
			pageC = cc.newInstance(new Object[] {shell, SWT.BORDER});
			if (pageC instanceof IDialogPage) {
				page = (IDialogPage) pageC;
				pageC.setData("dialog", this);
			}
			else
				page = null;
			
//			if (page == null) return;
			
			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					
					if (parent != null) {
						
						Object owner = parent.getData("owner");
						
						if (owner instanceof INotifiable)
							((INotifiable) owner).exec(this, UNREGISTER_DIALOG_PAGE, page);
					}					
				}
			});
			
		} catch (InstantiationException e) {
			
			result = false;
			return;
			
		} catch (IllegalAccessException e) {
			
			result = false;
			return;
			
		} catch (SecurityException e) {
			
			result = false;
			return;
			
		} catch (NoSuchMethodException e) {
			
			result = false;
			return;
			
		} catch (IllegalArgumentException e) {

			result = false;
			return;
			
		} catch (InvocationTargetException e) {
			
			result = false;
			return;
		}		

		result = true;
		
		pageC.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		if (!result) return;
		
		init();
		if (page != null) page.load(data);
		if (page != null) page.interop(null);
		
		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				if ( e.widget.getData().equals("h") ) {  // Help
					if ( help != null ) help.showTopic(this, "index");
					return;
				} 
				else if ( e.widget.getData().equals("o") ) { // OK
					
					if (page != null) result = page.save(data);
				}
				else {  // Cancel
					result = false;
				}
				
				shell.close();
			};
		};
		
		setActionButtonsPanel(shell, OKCancelActions);
		
		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, p_Parent);
	}
	
	protected boolean run(Shell parent, Class<? extends Composite> pageClass, String caption, Object initialData, IHelp help) {
		
		try {
			//if (parent.getClass().isAssignableFrom(this.getClass()));
//			if (parent instanceof INotifiable);
//			if (parent instanceof IParametersEditor);
			// if (parent instanceof AbstractParametersEditor);
			
			this.parent = parent;
			this.pageClass = pageClass;
			this.caption = caption;
			this.data = initialData;
			this.help = help;
			
			create(parent);			
			if (!result) return  false;
						
			showDialog();			
			if (!result) return  false;
		}
		finally {
			
			if (shell != null) {
						
				done();
				
				shell.dispose();
			}
		}
		
		return result;
		
	}
	
	protected void showDialog () {
		
		if (!result) return;
		
		
		if (parent != null) {
			
			Object owner = parent.getData("owner");
			
			if (owner instanceof INotifiable && page != null)
				((INotifiable) owner).exec(this, REGISTER_DIALOG_PAGE, page);
		}
		
		result = false; // To react to OK only
		shell.open();
		
		shell.update();
		if (page != null) page.interop(shell);
		
		while ( !shell.isDisposed() ) {
			
			try {
				if ( !shell.getDisplay().readAndDispatch() )
					shell.getDisplay().sleep();
			}
			catch ( Exception E ) {
				Dialogs.showError(shell, E.getLocalizedMessage(), null);
			}
		}		
	}
	
	public Object getResult() {
		
		return data;
	}
	
	public void setData(Object data) {
	
		this.data = data;
	}

	public Shell getShell() {
		
		return shell;
	}

	public boolean exec(Object sender, String command, Object info) {
		
		if (command.equalsIgnoreCase(NOTIFICATION_OK)) {
		
			if (page != null) 
				result = page.save(data);
			
			shell.close();
			
			return true;
		}
		
		return false;		
	}
	
}
