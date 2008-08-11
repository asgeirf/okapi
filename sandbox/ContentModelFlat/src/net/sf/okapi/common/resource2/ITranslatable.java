package net.sf.okapi.common.resource2;

public interface ITranslatable extends IContainable {

	/**
	 * Indicates if the content of the resource is translatable.
	 * @return True if the content is translatable, false otherwise.
	 */
	public boolean isTranslatable ();
	
	/**
	 * Sets the flag indicating if the content of the resource is translatable.
	 * @param value The new value to set.
	 */
	public void setIsTranslatable (boolean value);
	
	/**
	 * Gets the current parent of the object.
	 * @return The parent of the resource or null if it has no parent.
	 */
	public ITranslatable getParent ();
	
	/**
	 * Sets the parent for the resource.
	 * @param value The new parent of the resource.
	 */
	public void setParent (ITranslatable value);

	/**
	 * Indicates if the resource has at least one child.
	 * @return True if the resource has one child or more, false if it has none.
	 */
	public boolean hasChild ();
	
}
