package net.sf.okapi.apptest.resource;

import net.sf.okapi.apptest.annotation.Annotations;
import net.sf.okapi.apptest.common.IAnnotation;
import net.sf.okapi.apptest.common.IResource;
import net.sf.okapi.apptest.common.ISkeleton;

public class Ending implements IResource {

	protected String id;
	protected ISkeleton skeleton;
	protected Annotations annotations;
	
	public Ending (String id) {
		annotations = new Annotations();
		this.id = id;
	}

	public String getId () {
		return id;
	}
	
	public void setId (String id) {
		this.id = id;
	}

	public ISkeleton getSkeleton () {
		return skeleton;
	}
	
	public void setSkeleton (ISkeleton skeleton) {
		this.skeleton = skeleton;
	}

	@SuppressWarnings("unchecked")
	public <A> A getAnnotation (Class<? extends IAnnotation> type) {
		return (A) annotations.get(type);
	}

	public void setAnnotation (IAnnotation annotation) {
		annotations.set(annotation);
	}

}
