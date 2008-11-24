package net.sf.okapi.apptest.resource;

import net.sf.okapi.apptest.annotation.TargetsAnnotation;

public class TextUnit extends BaseReferenceable {

	private TextFragment source;
	
	public TextUnit () {
		super();
		source = new TextFragment(this);
	}

	public TextUnit (String id,
		String sourceText)
	{
		super();
		create(id, sourceText, false);
	}

	public TextUnit (String id,
		String sourceText,
		boolean isReferent)
	{
		super();
		create(id, sourceText, isReferent);
	}

	private void create (String id,
		String sourceText,
		boolean isReferent)
	{
		this.id = id;
		this.isReferent = isReferent;
		source = new TextFragment(this);
		if ( sourceText != null ) source.append(sourceText);
	}

	@Override
	public String toString () {
		return source.toString();
	}
	
	public TextFragment getContent () {
		return source;
	}
	
	public void setContent (TextFragment content) {
		source = content;
		// We don't change the current annotations
	}

	/**
	 * Gets the target TextUnit of a given source TextUnit, for a given language.
	 * If the target does not exists a null is returned, except if the option to create
	 * the target is set.
	 * @param language The language to look for. 
	 * @param creationOptions The creation option: 0=do not create, 1=create if the
	 * target does not exist, and leave it empty, 2=create if the target does not
	 * exist and copy the text of the source. 
	 * @return The target TextUnit, or null if none if available for the given lamguage.
	 */
	public TextUnit getTarget (String language,
		int creationOptions)
	{
		TargetsAnnotation ta = annotations.get(TargetsAnnotation.class);
		if ( ta == null ) {
			if ( creationOptions > 0 ) {
				ta = new TargetsAnnotation();
				annotations.set(ta);
			}
			else return null;
		}
		TextUnit trgTu = ta.get(language);
		if ( trgTu == null ) {
			if ( creationOptions > 0 ) {
				trgTu = new TextUnit(id, "");
				if ( creationOptions > 1 ) {
					TextFragment tf = getContent().clone();
					trgTu.setContent(tf);
				}
				ta.set(language, trgTu);
			}
		}
		return trgTu;
	}

}
