package net.sf.okapi.applications.olifant;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class ExtraFieldPanel extends SashForm {

	private boolean modified;
	private List lbFields;
	private SegmentEditor edExtra;

	ExtraFieldPanel (Composite parent,
		int flags)
	{
		super(parent, flags);
		setLayout(new GridLayout(1, false));
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setOrientation(SWT.HORIZONTAL);
		
		edExtra = new SegmentEditor(this, -1);
		
		lbFields = new List(this, SWT.BORDER);
		lbFields.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		setWeights(new int[] {4, 1});
		setSashWidth(4);
	}

	public boolean setFocus () {
		return edExtra.setFocus();
	}
	
	public void setEnabled (boolean enabled) {
		edExtra.setEnabled(enabled);
	}
	
	public void setEditable (boolean editable) {
		edExtra.setEditable(editable);
	}

	public void clear () {
		edExtra.setText("");
		modified = false;
	}
	
	public boolean isModified () {
		return modified;
	}

	public void setText (String text) {
		edExtra.setEnabled(text != null);
		if ( text == null ) {
			edExtra.setText("");
		}
		else {
			edExtra.setText(text);
		}
		modified = false;
	}

	public String getText () {
		return edExtra.getText();
	}
	
	public void toggleFieldList () {
		if ( getWeights()[1] > 0 ) {
			setWeights(new int[]{1, 0});
			setSashWidth(0);
		}
		else {
			setWeights(new int[]{4, 1});
			setSashWidth(4);
		}
	}

}
