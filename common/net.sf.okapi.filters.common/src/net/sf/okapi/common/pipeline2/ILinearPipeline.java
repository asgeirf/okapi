package net.sf.okapi.common.pipeline2;

public interface ILinearPipeline {
	
	public boolean execute();
	
	public void cancel();
	
	public void pause();
	
	public void resume();
	
	public void addPipleLineStep(IPipelineStep step);
	
	public void addPipleLineStep(IPipelineStep step, int numThreads);
}