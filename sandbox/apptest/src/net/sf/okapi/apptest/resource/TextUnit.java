package net.sf.okapi.apptest.resource;

import java.util.Hashtable;

import net.sf.okapi.apptest.filters.IWriterHelper;

public class TextUnit extends BaseResource {

	private TextFragment source;
	private Hashtable<String, Object> annotations;
	private Hashtable<String, Property> properties;
	
	public TextUnit () {
		source = new TextFragment(this);
		annotations = new Hashtable<String, Object>();
		properties = new Hashtable<String, Property>();
	}

	public TextUnit (String id,
		String sourceText)
	{
		create(id, sourceText, false);
	}

	public TextUnit (String id,
		String sourceText,
		boolean isReferent)
	{
		create(id, sourceText, isReferent);
	}

	private void create (String id,
		String sourceText,
		boolean isReferent)
	{
		this.id = id;
		this.isReferent = isReferent;
		source = new TextFragment(this);
		annotations = new Hashtable<String, Object>();
		properties = new Hashtable<String, Property>();
		if ( sourceText != null ) source.append(sourceText);
	}

	@Override
	public String toString () {
		return source.toString();
	}
	
	public String toString (IWriterHelper writerHelper) {
		TextFragment tf;
		if ( writerHelper.getLanguage() == null ) {
			tf = source;
		}
		else { // TODO: any better way?
			if ( getAnnotation(writerHelper.getLanguage()) == null ) {
				tf = source;
			}
			else {
				tf = ((TextUnit)getAnnotation(writerHelper.getLanguage())).getContent();
			}
		}
		return tf.toString(writerHelper);
	}

	public TextFragment getContent () {
		return source;
	}
	
	public void setContent (TextFragment content) {
		source = content;
		// We don't change the current annotations
	}

	public Object getAnnotation (String name) {
		return annotations.get(name);
	}
	
	public void setAnnotation (String name, Object annotation) {
		annotations.put(name, annotation);
	}
	
	public void setProperty (String name, Property property) {
		properties.put(name, property);
	}
	
	public Property getProperty (String name) {
		return properties.get(name);
	}

}

