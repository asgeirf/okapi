package net.sf.okapi.common.filters;

import java.util.Stack;

public class LocalizationDirectives {

	private boolean          useLD;
	private boolean          localizeOutside;
	private Stack<Context>   context;
	
	private class Context {
		boolean isGroup;
		boolean extract;
		
		Context () {
			isGroup = true;
			extract = true;
		}
		
		Context (boolean isGroup,
			boolean extract)
		{
			this.isGroup = isGroup;
			this.extract = extract;
		}
	}
	
	public LocalizationDirectives () {
		reset();
	}
	
	public void reset () {
		context = new Stack<Context>();
		setOptions(true, true);
	}
	
	public boolean useLD () {
		return useLD;
	}
	
	public boolean localizeOutside () {
		if ( !useLD ) return true; // Always localize all when LD not used
		return localizeOutside;
	}
	
	public boolean isWithinScope () {
		return (context.size() > 0);
	}
	
	public boolean isLocalizable (boolean popSingle) {
		// If LD not used always localize
		if ( !useLD ) return true;
		// Default
		boolean res = localizeOutside;
		if ( context.size() > 0 ) {
			res = context.peek().extract;
			if ( popSingle ) {
				// Pop only the non-group properties
				if ( !context.peek().isGroup ) {
					context.pop();
				}
			}
		}
		return res;
	}
	
	public void setOptions (boolean useLD,
		boolean localizeOutside)
	{
		this.useLD = useLD;
		this.localizeOutside = localizeOutside;
	}
	
	/**
	 * Evaluates a string that contain localization directives and update the object
	 * state based on the given instructions.
	 * @param content The text to process.
	 */
	public void process (String content) {
		// Check if we need to process
		if (( content == null ) || ( !useLD )) return;

		// Process
		content = content.toLowerCase();
		if ( content.lastIndexOf("_skip") > -1 ) {
			push(false, false);
		}
		else if ( content.lastIndexOf("_bskip") > -1 ) {
			push(true, false);
		}
		else if ( content.lastIndexOf("_eskip") > -1 ) {
			//TODO: check if groups are balanced
			popIfPossible();
		}
		else if ( content.lastIndexOf("_text") > -1 ) {
			push(false, true);
		}
		else if ( content.lastIndexOf("_btext") > -1 ) {
			push(true, true);
		}
		else if ( content.lastIndexOf("_etext") > -1 ) {
			//TODO: check if groups are balanced
			popIfPossible();
		}
	}

	private void push (boolean isGroup,
		boolean extract)
	{
		// Pop top context if it's a single
		if ( context.size() > 0 ) {
			if ( !context.peek().isGroup ) context.pop(); 
		}
		// Add new context
		context.add(new Context(isGroup, extract));
	}

	private void popIfPossible () {
		if ( context.size() > 0 ) context.pop();
	}
	
}
