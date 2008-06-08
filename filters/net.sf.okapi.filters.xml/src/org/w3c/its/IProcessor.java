package org.w3c.its;

import org.w3c.dom.Document;

public interface IProcessor {
	
	public static final int DC_LANGINFO          = 0x0001;
	public static final int DC_TRANSLATE         = 0x0002;
	public static final int DC_WITHINTEXT        = 0x0004;
	public static final int DC_LOCNOTE           = 0x0008;
	public static final int DC_TERMINOLOGY       = 0x0010;
	public static final int DC_DIRECTIONALITY    = 0x0020;
	public static final int DC_RUBY              = 0x0040;
	public static final int DC_ALL               = 0xFFFF;
	
	/**
	 * Adds a set of global rules to the document to process. The rules are added
	 * to the internal storage of the document, not to the document tree.
	 * Use this method to add one rule set or more before calling applyRules().
	 * @param docRules Document where the global rules are declared.
	 * @param docPath path of the document. This is needed because xlink:href need
	 * a initial location.
	 */
	void addExternalRules (Document rulesDoc,
		String docPath);

	/**
	 * Adds a set of global rules to the document to process.
	 * See {@link #addExternalRules(Document, String)} for more details.
	 * @param docPath Path of the document that conatins the rules to add.
	 */
	void addExternalRules (String docPath);

	/**
	 * Applies the current ITS rules to the document. This method decorates
	 * the document tree with special flags that are used for getting the
	 * different ITS information later.
	 * @param dataCategories Flag indicating what data categories to apply.
	 * The value must be one of the DC_* values or several combined with 
	 * a OR operator. For example:
	 * applyRules(DC_TRANSLATE | DC_LOCNOTE);
	 */
	void applyRules (int dataCategories);
	
	/**
	 * Removes all the special attributes added when applying the ITS rules.
	 * Once you have called this method you should call applyRules() again to be able
	 * to use any ITSState again.
	 */
	void disapplyRules ();
}
