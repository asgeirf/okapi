package net.sf.okapi.filters.ui.properties;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.LDPanel;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.filters.properties.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class Editor implements IParametersEditor {
	
	private Shell            shell;
	private boolean          result = false;
	private Button           chkUseKeyFilter;
	private Button           rdExtractOnlyMatchingKey;
	private Button           rdExcludeMatchingKey;
	private Text             edKeyCondition;
	private Button           chkExtraComments;
	private LDPanel          pnlLD;
	private OKCancelPanel    pnlActions;
	private Parameters       params;
	private Button           chkEscapeExtendedChars;
	//TODO private CodeFinderPanel  m_CFPanel;

	/**
	 * Invokes the editor for the Properties filter parameters.
	 * @param p_Options The option object of the action.
	 * @param p_Object The SWT Shell object of the parent shell in the UI.
	 */
	public boolean edit (IParameters p_Options,
		Object p_Object)
	{
		boolean bRes = false;
		shell = null;
		params = (Parameters)p_Options;
		try {
			shell = new Shell((Shell)p_Object, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell)p_Object);
			return showDialog();
		}
		catch ( Exception E ) {
			Dialogs.showError(shell, E.getLocalizedMessage(), null);
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
	
	private void create (Shell p_Parent) {
		shell.setText("Properties Filter Parameters");
		if ( p_Parent != null ) shell.setImage(p_Parent.getImage());
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		tfTmp.setLayoutData(gdTmp);

		//--- Options tab
		
		Composite cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);
		
		Group grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		grpTmp.setText("Localization directives");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);
		pnlLD = new LDPanel(grpTmp, SWT.NONE);
		
		grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		grpTmp.setText("Key filtering");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);
		
		chkUseKeyFilter = new Button(grpTmp, SWT.CHECK);
		chkUseKeyFilter.setText("Use the following key condition:");
		chkUseKeyFilter.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateKeyFilter();
			};
		});

		rdExtractOnlyMatchingKey = new Button(grpTmp, SWT.RADIO);
		rdExtractOnlyMatchingKey.setText("Extract only the items with a key matching the given expression");
		gdTmp = new GridData();
		gdTmp.horizontalIndent = 16;
		rdExtractOnlyMatchingKey.setLayoutData(gdTmp);

		rdExcludeMatchingKey = new Button(grpTmp, SWT.RADIO);
		rdExcludeMatchingKey.setText("Do not extract the items with a key matching the given expression");
		rdExcludeMatchingKey.setLayoutData(gdTmp);
//TODO: m_edKeyCondition enabling/disabling depending on choice
		edKeyCondition = new Text(grpTmp, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalIndent = 16;
		edKeyCondition.setLayoutData(gdTmp);
		
		chkExtraComments = new Button(cmpTmp, SWT.CHECK);
		chkExtraComments.setText("Recognize additional comment markers");

		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Options");
		tiTmp.setControl(cmpTmp);
		
		//--- Inline tab
		
		//cmpTmp = new Composite(tfTmp, SWT.NONE);
		//layTmp = new GridLayout();
		//cmpTmp.setLayout(layTmp);
		
/*TODO		m_CFPanel = new CodeFinderPanel(tfTmp, SWT.NONE);
		
		tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Inline Codes");
		tiTmp.setControl(m_CFPanel);
*/
		//--- Output tab
		
		cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);
		
		grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		grpTmp.setText("Extended characters");
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);

		chkEscapeExtendedChars = new Button(grpTmp, SWT.CHECK);
		chkEscapeExtendedChars.setText("Always escape all extended characters");
		
		tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText("Output");
		tiTmp.setControl(cmpTmp);
		
		
		//--- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( e.widget.getData().equals("h") ) {
					//TODO: Call help
					return;
				}
				if ( e.widget.getData().equals("o") ) saveData();
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		pnlActions.setLayoutData(gdTmp);
		shell.setDefaultButton(pnlActions.btOK);

		setData();
		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, p_Parent);
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
//TODO		m_pnlLD.m_chkUseLD.setSelection(m_Data.m_LD.useDirectives());
//TODO		m_pnlLD.m_chkLocalizeOutside.setSelection(m_Data.m_LD.localizeOutside());
		edKeyCondition.setText(params.keyCondition);
		rdExtractOnlyMatchingKey.setSelection(params.extractOnlyMatchingKey);
		rdExcludeMatchingKey.setSelection(!params.extractOnlyMatchingKey);
		chkUseKeyFilter.setSelection(params.useKeyCondition);
		chkExtraComments.setSelection(params.extraComments);
		chkEscapeExtendedChars.setSelection(params.escapeExtendedChars);
//TODO		m_CFPanel.setData(m_Data.m_bUseCodeFinder, m_Data.m_CodeFinder);
		pnlLD.updateDisplay();
		updateKeyFilter();
	}
	
	private void saveData () {
//TODO		m_Data.m_LD.setOptions(m_pnlLD.m_chkUseLD.getSelection(),
//TODO			m_pnlLD.m_chkLocalizeOutside.getSelection(), false);
		params.useKeyCondition = chkUseKeyFilter.getSelection();
		params.keyCondition = edKeyCondition.getText();
		params.extractOnlyMatchingKey = rdExtractOnlyMatchingKey.getSelection();
		params.extraComments = chkExtraComments.getSelection();
		params.escapeExtendedChars = chkEscapeExtendedChars.getSelection();
		result = true;
	}
	
	private void updateKeyFilter () {
		edKeyCondition.setEnabled(chkUseKeyFilter.getSelection());
		rdExtractOnlyMatchingKey.setEnabled(chkUseKeyFilter.getSelection());
		rdExcludeMatchingKey.setEnabled(chkUseKeyFilter.getSelection());
	}
}
