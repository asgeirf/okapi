/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

package net.sf.okapi.steps.xsltransform.ui;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import net.sf.okapi.common.ConfigurationString;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.IContext;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.NSContextManager;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.ISWTEmbeddableParametersEditor;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.UIUtil;
import net.sf.okapi.steps.xsltransform.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@EditorFor(Parameters.class)
public class ParametersEditor implements IParametersEditor, ISWTEmbeddableParametersEditor {
	
	private Shell shell;
	private boolean result = false;
	private OKCancelPanel pnlActions;
	private Parameters params;
	private Button chkUseCustomTransformer;
	private Text edFactoryClass;
	private Text edXpathFactoryClass;
	private Text edXsltPath;
	private Text edParameters;
	private IHelp help;
	private String projectDir;
	private Composite mainComposite;

	public boolean edit (IParameters params,
		boolean readOnly,
		IContext context)
	{
		boolean bRes = false;
		try {
			shell = null;
			help = (IHelp)context.getObject("help");
			this.projectDir = context.getString("projDir");
			this.params = (Parameters)params;
			shell = new Shell((Shell)context.getObject("shell"), SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)context.getObject("shell"), readOnly);
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
	
	@Override
	public Composite getComposite () {
		return mainComposite;
	}

	@Override
	public void initializeEmbeddableEditor (Composite parent,
		IParameters paramsObject,
		IContext context)
	{
		params = (Parameters)paramsObject; 
		shell = (Shell)context.getObject("shell");
		createComposite(parent);
		setData();
	}

	@Override
	public String validateAndSaveParameters () {
		if ( !saveData() ) return null;
		return params.toString();
	}
	
	private void create (Shell parent,
		boolean readOnly)
	{
		shell.setText(Res.getString("editor.caption")); //$NON-NLS-1$
		if ( parent != null ) UIUtil.inheritIcon(shell, parent);
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		createComposite(shell);

		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if ( e.widget.getData().equals("h") ) { //$NON-NLS-1$
					if ( help != null ) help.showWiki("XSL Transformation Step");
					return;
				}
				if ( e.widget.getData().equals("o") ) { //$NON-NLS-1$
					if ( !saveData() ) return;
				}
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		pnlActions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pnlActions.btOK.setEnabled(!readOnly);
		if ( !readOnly ) {
			shell.setDefaultButton(pnlActions.btOK);
		}

		shell.pack();
		shell.setMinimumSize(shell.getSize());
		Dialogs.centerWindow(shell, parent);
		setData();
	}
	
	private void createComposite (Composite parent) {
		mainComposite = new Composite(parent, SWT.BORDER);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(4, false));
		
		Label label = new Label(mainComposite, SWT.NONE);
		label.setText(Res.getString("editor.stXsltPath")); //$NON-NLS-1$
		GridData gdTmp = new GridData();
		gdTmp.horizontalSpan = 4;
		label.setLayoutData(gdTmp);

		edXsltPath = new Text(mainComposite, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 3;
		edXsltPath.setLayoutData(gdTmp);
		
		Button btGetPath = new Button(mainComposite, SWT.PUSH);
		btGetPath.setText("..."); //$NON-NLS-1$
		btGetPath.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		btGetPath.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String[] paths = Dialogs.browseFilenames(shell,
					Res.getString("editor.captionSelectTemplate"), //$NON-NLS-1$
					false, null,
					Res.getString("editor.filterSelectTemplate"), //$NON-NLS-1$
					"*.xsl;*.xslt\t*.*"); //$NON-NLS-1$
				if ( paths == null ) return;
				UIUtil.checkProjectFolderAfterPick(paths[0], edXsltPath, projectDir);
			}
		});

		label = new Label(mainComposite, SWT.NONE);
		label.setText(Res.getString("editor.stParameters")); //$NON-NLS-1$
		
		int wideButtonWidth = Res.getInt("editor.wideButtonWidth"); //$NON-NLS-1$
		Button btGetDefaults = new Button(mainComposite, SWT.PUSH);
		btGetDefaults.setText(Res.getString("editor.btGetDefaults")); //$NON-NLS-1$
		gdTmp = new GridData();
		btGetDefaults.setLayoutData(gdTmp);
		UIUtil.ensureWidth(btGetDefaults, wideButtonWidth);
		btGetDefaults.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getParametersFromTemplate();
			}
		});
		
		Button btOpenFile = new Button(mainComposite, SWT.PUSH);
		btOpenFile.setText(Res.getString("editor.btOpenTemplate")); //$NON-NLS-1$
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 2;
		btOpenFile.setLayoutData(gdTmp);
		UIUtil.ensureWidth(btOpenFile, wideButtonWidth);
		btOpenFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Program.launch(edXsltPath.getText()); 
			}
		});
		
		edParameters = new Text(mainComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.heightHint = 70;
		gdTmp.horizontalSpan = 4;
		edParameters.setLayoutData(gdTmp);
		
		chkUseCustomTransformer = new Button(mainComposite, SWT.CHECK);
		chkUseCustomTransformer.setText(Res.getString("editor.useCustomTransformer")); //$NON-NLS-1$
		gdTmp = new GridData();
		gdTmp.horizontalSpan = 4;
		chkUseCustomTransformer.setLayoutData(gdTmp);
		chkUseCustomTransformer.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				edFactoryClass.setEnabled(chkUseCustomTransformer.getSelection());
				edFactoryClass.setText("net.sf.saxon.TransformerFactoryImpl"); // default for XSLT 2.0
				edXpathFactoryClass.setEnabled(chkUseCustomTransformer.getSelection());
				edXpathFactoryClass.setText("net.sf.saxon.xpath.XPathFactoryImpl"); // default for XSLT 2.0
			}
		});
		
		edFactoryClass = new Text(mainComposite, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 4;
		edFactoryClass.setLayoutData(gdTmp);

		edXpathFactoryClass = new Text(mainComposite, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 4;
		edXpathFactoryClass.setLayoutData(gdTmp);
}
	
	private boolean showDialog () {
		shell.open();
		while ( !shell.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		return result;
	}

	private void setData () {
		chkUseCustomTransformer.setSelection(params.useCustomTransformer);
		edFactoryClass.setText(params.factoryClass);
		edXpathFactoryClass.setText(params.xpathClass);
		edXsltPath.setText(params.getXsltPath());
		ConfigurationString tmp = new ConfigurationString(params.paramList);
		edParameters.setText(tmp.toString());
		edFactoryClass.setEnabled(chkUseCustomTransformer.getSelection());
		edXpathFactoryClass.setEnabled(chkUseCustomTransformer.getSelection());
	}

	private boolean saveData () {
		if ( edXsltPath.getText().length() == 0 ) {
			Dialogs.showError(shell, "You must specify a path for the XSLT file.", null);
			edXsltPath.setFocus();
			return false;
		}
		if ( chkUseCustomTransformer.getSelection() ) {
			if ( edFactoryClass.getText().length() == 0 ) {
				Dialogs.showError(shell, "You must specify a factory class.", null);
				edFactoryClass.setFocus();
				return false;
			}
			// If no Xpath factory is chosen, the default one will be used
		}

		params.useCustomTransformer = chkUseCustomTransformer.getSelection();
		if ( params.useCustomTransformer ) {
			params.factoryClass = edFactoryClass.getText();
			params.xpathClass = edXpathFactoryClass.getText();
		}

		params.setXsltPath(edXsltPath.getText());
		ConfigurationString tmp = new ConfigurationString(edParameters.getText());
		params.paramList = tmp.toString();
		result = true;
		return result;
	}
	
	private void getParametersFromTemplate () {
		try {
			String path = edXsltPath.getText();
			if ( path.length() == 0 ) return;

			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document doc = builder.parse(path);

			// Macintosh work-around
			// When you use -XstartOnFirstThread as a java -Xarg on Leopard, your ContextClassloader gets set to null.
			// That is not the case on 10.4 or with Windows or Linux flavors
			// This allows XPathFactory.newInstance() to have a non-null context
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			// end work-around
		    XPathFactory factory = XPathFactory.newInstance();

		    XPath xpath = factory.newXPath();
		    xpath.setNamespaceContext(new NSContextManager());
		    XPathExpression expr = xpath.compile("//xsl:param"); //$NON-NLS-1$

		    Object result = expr.evaluate(doc, XPathConstants.NODESET);
		    NodeList nodes = (NodeList) result;
		    ConfigurationString paramList = new ConfigurationString();
		    Element elem;
		    for (int i = 0; i < nodes.getLength(); i++) {
		    	elem = (Element)nodes.item(i);
		    	paramList.add(elem.getAttribute("name"), Util.getTextContent(elem)); //$NON-NLS-1$
		    }
		    edParameters.setText(paramList.toString());
		}
		catch ( Throwable e ) {
			Dialogs.showError(shell, e.getMessage(), null);
		}
	}
	
}
