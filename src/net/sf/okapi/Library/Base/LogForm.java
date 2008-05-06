package net.sf.okapi.Library.Base;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * General-purpose default log as a window.
 * This log is not thread-safe.
 */
public class LogForm implements ILog {

	private Shell            shell;
	//private String           m_sHelp;
	private Text             edLog;
	private Button           btStop;
	private int              errorCount;
	private int              warningCount;
	private long             data = 0;
	private ProgressBar      pbPrimary;
	private ProgressBar      pbSecondary;
	private boolean          inProgress = false;
	
	public LogForm (Shell p_Parent) {
		shell = new Shell(p_Parent, SWT.BORDER | SWT.RESIZE | SWT.TITLE
			| SWT.MODELESS | SWT.CLOSE | SWT.MAX | SWT.MIN);
		shell.setImage(p_Parent.getImage());
		createContent();
	}
	
	private void createContent () {
		shell.setLayout(new GridLayout(4, false));
		
		// On close: Hide instead of closing
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = false;
				hide();
			}
		});
		
		int nWidth = 80;
		Button button = new Button(shell, SWT.PUSH);
		button.setText("&Help");
		GridData gdTmp = new GridData();
		gdTmp.widthHint = nWidth;
		button.setLayoutData(gdTmp);
		
		button = new Button(shell, SWT.PUSH);
		button.setText("&Clear");
		gdTmp = new GridData();
		gdTmp.widthHint = nWidth;
		button.setLayoutData(gdTmp);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				clear();
			}
		});
		
		btStop = new Button(shell, SWT.PUSH);
		btStop.setText("&Stop");
		gdTmp = new GridData();
		gdTmp.widthHint = nWidth;
		btStop.setLayoutData(gdTmp);
		
		button = new Button(shell, SWT.PUSH);
		button.setText("&Close");
		gdTmp = new GridData();
		gdTmp.widthHint = nWidth;
		button.setLayoutData(gdTmp);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hide();
			}
		});
		
		//=== Progress
		
		pbPrimary = new ProgressBar(shell, SWT.HORIZONTAL);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 4;
		pbPrimary.setLayoutData(gdTmp);

		pbSecondary = new ProgressBar(shell, SWT.HORIZONTAL);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		gdTmp.horizontalSpan = 4;
		pbSecondary.setLayoutData(gdTmp);
		
		//=== Log itself

		edLog = new Text(shell, SWT.MULTI | SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_BOTH);
		gdTmp.horizontalSpan = 4;
		edLog.setLayoutData(gdTmp);
		
		updateDisplay();
		shell.pack();
		shell.setMinimumSize(shell.getSize());
		shell.setSize(600, 300);
	}

	private void updateDisplay () {
		btStop.setEnabled(inProgress);
		pbPrimary.setEnabled(inProgress);
		pbSecondary.setEnabled(inProgress);
	}
	
	public boolean beginProcess (String p_sText) {
		if ( inProgress() ) return false;
		if (( p_sText != null ) && ( p_sText.length() > 0 ))
			setLog(LogType.MESSAGE, 0, p_sText);
		errorCount = warningCount = 0;
		inProgress = true;
		updateDisplay();
		return inProgress;
	}

	public boolean beginTask (String p_sText) {
		if (( p_sText != null ) && ( p_sText.length() > 0 ))
			setLog(LogType.MESSAGE, 0, p_sText);
		return inProgress;
	}

	public boolean canContinue () {
		//TODO: Implement user-cancel with escape key
		return inProgress;
	}

	public void cancel (boolean p_bAskConfirmation) {
		if ( inProgress() )
		{
			if ( p_bAskConfirmation )
			{
//				System.out.print(Res.getString("CONFIRM_CANCEL")); //$NON-NLS-1$
//TODO				char chRes = char.ToLower((char)Console.Read());
//				string sYN = m_RM.GetString("CONFIRM_YESNOLETTERS");
//				if ( chRes != sYN[0] ) return; // No cancellation
			}
			// Cancel the process
			endTask(null);
			endProcess(null);
		}
	}

	public void clear () {
		edLog.setText("");
	}

	public void endProcess (String p_sText) {
		if ( inProgress ) {
			if (( p_sText != null ) && ( p_sText.length() > 0 ))
				setLog(LogType.MESSAGE, 0, p_sText);
		}
		inProgress = false;
		updateDisplay();
	}

	public void endTask (String p_sText) {
		if ( inProgress ) {
			if (( p_sText != null ) && ( p_sText.length() > 0 ))
				setLog(LogType.MESSAGE, 0, p_sText);
		}
	}

	public boolean error (String p_sText) {
		return setLog(LogType.ERROR, 0, p_sText);
	}

	public long getCallerData () {
		return data;
	}

	public int getErrorAndWarningCount () {
		return errorCount+warningCount;
	}

	public int getErrorCount () {
		return errorCount;
	}

	public int getWarningCount () {
		return warningCount;
	}

	public boolean inProgress () {
		return inProgress;
	}

	public boolean message (String p_sText) {
		return setLog(LogType.MESSAGE, 0, p_sText);
	}

	public boolean newLine () {
		return setLog(LogType.MESSAGE, 0, "\n");
	}

	public void save (String path) {
		// Not implemented for this implementation
	}

	public void setCallerData (long newData) {
		// Not used, just store it
		data = newData;
	}

	public void setHelp (String p_sPath) {
		//m_sHelp = p_sPath;
	}

	public boolean setLog (int p_nType,
		int p_nValue,
		String p_sValue)
	{
		switch ( p_nType ) {
		case LogType.ERROR:
			edLog.insert("Error: " + p_sValue + "\n");
			errorCount++;
			break;
		case LogType.WARNING:
			edLog.insert("Warning: " + p_sValue + "\n");
			warningCount++;
			break;
		case LogType.MESSAGE:
			edLog.insert(p_sValue + "\n");
			break;
		case LogType.SUBPROGRESS:
		case LogType.MAINPROGRESS:
			break;
		case LogType.USERFEEDBACK:
		default:
			break;
		}
		return canContinue();
	}

	public void setMainProgressMode (int p_nValue) {
		if ( p_nValue < 0 ) p_nValue = 0;
		if ( p_nValue > 100 ) p_nValue = 100;
		//pbPrimary. .s.set.setValue(p_nValue);
	}

	public boolean setOnTop (boolean p_bValue) {
		//TODO boolean bRes = isAlwaysOnTop();
		//setAlwaysOnTop(p_bValue);
		return false; //bRes;
	}

	public void setSubProgressMode (int p_nValue) {
		if ( p_nValue < 0 ) p_nValue = 0;
		if ( p_nValue > 100 ) p_nValue = 100;
		//pbSecondary.setValue(p_nValue);
	}

	public boolean warning (String p_sText) {
		return setLog(LogType.WARNING, 0, p_sText);
	}

	public void hide () {
		shell.setVisible(false);
	}

	public void setTitle (String text) {
		shell.setText(text);
	}

	public void show () {
		shell.setVisible(true);
		if ( shell.getMinimized() ) shell.setMinimized(false);
	}

	public boolean isVisible() {
		return shell.isVisible();
	}
}
