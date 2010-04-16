package net.sf.okapi.steps.diffleverage;

import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.resource.TextUnit;

/**
 * Marker annotation that shows a TextUnit was updated via a {@link DiffLeverageStep}.
 * 
 * @author HARGRAVEJE
 * 
 */
public class DiffLeverageAnnotation implements IAnnotation {
	private boolean codesensitive;
	private int threshold;

	public DiffLeverageAnnotation(final boolean codesensitive, final int threshold) {
		this.codesensitive = codesensitive;
		setThreshold(threshold);
	}

	/**
	 * Set whether the annotated {@link TextUnit} matched with or without codes.
	 * 
	 * @param codesensitive
	 */
	public void setCodesensitive(final boolean codesensitive) {
		this.codesensitive = codesensitive;
	}

	/**
	 * Did the annotated {@link TextUnit} matched with or without codes?
	 * @return true if codesenstive, false otherwise
	 */
	public boolean isCodesensitive() {
		return codesensitive;
	}

	public void setThreshold(final int threshold) {
		this.threshold = threshold;
	}

	public int getThreshold() {
		return threshold;
	}
}
