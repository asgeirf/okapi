/**
 * 
 */
package net.sf.okapi.common.pipeline2;

/**
 * @author HargraveJE
 *
 */
public interface IPipelineStep extends Runnable {
	
	public void start();
	
	public void cancel();
	
	public void pause();
	
	public void resume();
	
}
