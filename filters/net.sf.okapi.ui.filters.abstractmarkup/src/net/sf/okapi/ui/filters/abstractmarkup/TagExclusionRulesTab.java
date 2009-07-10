package net.sf.okapi.ui.filters.abstractmarkup;

import net.sf.okapi.ui.common.dialogs.IDialogPage;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

public class TagExclusionRulesTab extends Composite implements IDialogPage {

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TagExclusionRulesTab(Composite parent, int style) {
		super(parent, style);

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public boolean canClose(boolean isOK) {		
		return true;
	}

	public void interop(Widget speaker) {
	}

	public boolean load(Object data) {
		return true;
	}

	public boolean save(Object data) {
		return true;
	}
}
