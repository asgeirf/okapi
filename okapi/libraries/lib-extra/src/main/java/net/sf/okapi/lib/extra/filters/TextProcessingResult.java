package net.sf.okapi.lib.extra.filters;

/**
 * 
 * 
 * @version 0.1, 09.06.2009
 */

public enum TextProcessingResult {
	
	/**
	 * Indefinite
	 */
	NONE,
	
	/**
	 * Text was not processed
	 */
	REJECTED,
	
	/**
	 * Test was processed, events were created and sent
	 */
	ACCEPTED, 	
		
	/**
	 * Text was not processed, processing will be done later
	 */
	DELAYED_DECISION
}
