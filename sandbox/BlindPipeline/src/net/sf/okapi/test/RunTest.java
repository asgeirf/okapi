package net.sf.okapi.test;

import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Okapi;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterHarvester;
import net.sf.okapi.common.pipeline.FilterEventsToRawDocumentStep;
import net.sf.okapi.common.pipeline.FilterEventsWriterStep;
import net.sf.okapi.common.pipeline.IPipeline;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.pipeline.PipelineDriver;
import net.sf.okapi.common.pipeline.RawDocumentToFilterEventsStep;
import net.sf.okapi.common.pipeline.RawDocumentWriterStep;
import net.sf.okapi.steps.bomconversion.BOMConversionStep;
import net.sf.okapi.steps.textmodification.TextModificationStep;

public class RunTest {
	
	private ArrayList<ProjectItem> proj = new ArrayList<ProjectItem>();
	private PipelineDriver driver;
	private FilterConfigurationMapper fcMapper;

	public static void main (String[] args) {
		RunTest rt = new RunTest();
		rt.run();
	}
	public RunTest () {
		Okapi.context.setBoolean("allowPrompt", true);
		
		URL url = RunTest.class.getResource("/input1_en.properties");
		String root = Util.getDirectoryName(url.getPath());
		root = Util.getDirectoryName(root) + "/data/";

		ProjectItem pi = new ProjectItem();
		pi.inputPaths[0] = root+"input1_en.properties";
		pi.encodings[0] = "UTF-8";
		pi.filterConfigs[0] = "okapi.properties";
		proj.add(pi);

		// Use a custom config: only the second entry of the file should be seen as translatable
		pi = new ProjectItem();
		pi.inputPaths[0] = root+"input2_en.properties";
		pi.encodings[0] = "UTF-8";
		pi.filterConfigs[0] = "okapi.properties-TextKeysOnly";
		proj.add(pi);

		fcMapper = new FilterConfigurationMapper();
		FilterConfiguration baseFc = fcMapper.getConfiguration("okapi.properties");
		FilterConfiguration fc = new FilterConfiguration(); 
		fc.configId = "okapi.properties-TextKeysOnly";
		fc.custom = true;
		fc.filterClass = baseFc.filterClass;
		fc.name = "My Properties Files Config";
		fc.description = "properties file with key names that includes 'text'";
		fc.parameters = "okapi.properties-TextKeysOnly.fprm";
		fcMapper.addConfiguration(fc, MimeTypeMapper.PROPERTIES_MIME_TYPE);

		FilterHarvester.harvestFilterConfigurations("C:\\Tmp\\TestOSGi\\Test.jar", fcMapper);

		driver = new PipelineDriver();
	}

	public void run () {
		driver.setPipeline(createPipelineOne());
		feedDriver();
		//driver.processBatch();

		driver.setPipeline(createPipelineTwo());
		feedDriver();
		//driver.processBatch();

		driver.setPipeline(createPipelineThree());
		feedDriver();
		driver.processBatch();
	}
	
	private void feedDriver () {
		driver.resetItems();
		if ( driver.inputCountRequested() > 3 ) {
			throw new RuntimeException("Application does not support more than 3 input at the same time.");
		}
		for ( ProjectItem item : proj ) {
			driver.addBatchItem(item);
		}
	}
	
	private IPipeline createPipelineOne () {
		// First pipeline: simple BOM conversion
		IPipeline pipeline = new Pipeline();
		pipeline.getContext().setFilterConfigurationMapper(fcMapper);
	
		BOMConversionStep step = new BOMConversionStep();
		net.sf.okapi.steps.bomconversion.Parameters params
			= (net.sf.okapi.steps.bomconversion.Parameters)step.getParameters();
		params.removeBOM = true; // Remove the BOM
		pipeline.addStep(step);
		
		pipeline.addStep(new RawDocumentWriterStep());
		return pipeline;
	}
	
	private IPipeline createPipelineTwo () {
		IPipeline pipeline = new Pipeline();
		pipeline.getContext().setFilterConfigurationMapper(fcMapper);
		
		pipeline.addStep(new RawDocumentToFilterEventsStep());
		
		// Text modification step
		// The params are set here. Basically they would be changed by the application
		// not after starting a batch.
		TextModificationStep step = new TextModificationStep();
		net.sf.okapi.steps.textmodification.Parameters params
			= (net.sf.okapi.steps.textmodification.Parameters)step.getParameters();
		params.type = net.sf.okapi.steps.textmodification.Parameters.TYPE_EXTREPLACE;
		pipeline.addStep(step);
		
		pipeline.addStep(new FilterEventsWriterStep());
		return pipeline;
	}
	
	private IPipeline createPipelineThree () {
		IPipeline pipeline = new Pipeline();
		pipeline.getContext().setFilterConfigurationMapper(fcMapper);
		
		// Convert Raw document to filter events
		pipeline.addStep(new RawDocumentToFilterEventsStep());

		// Text modification step
		// The params are set here. Basically they would be changed by the application
		// not after starting a batch.
		TextModificationStep step1 = new TextModificationStep();
		net.sf.okapi.steps.textmodification.Parameters params1
			= (net.sf.okapi.steps.textmodification.Parameters)step1.getParameters();
		params1.type = net.sf.okapi.steps.textmodification.Parameters.TYPE_EXTREPLACE;
		pipeline.addStep(step1);

		// Convert back filter events to raw document
		pipeline.addStep(new FilterEventsToRawDocumentStep());
		
		// Remove the BOM
		BOMConversionStep step2 = new BOMConversionStep();
		net.sf.okapi.steps.bomconversion.Parameters params2
			= (net.sf.okapi.steps.bomconversion.Parameters)step2.getParameters();
		params2.removeBOM = true; // Remove thge BOM
		pipeline.addStep(step2);
		
		// Write the output
		pipeline.addStep(new RawDocumentWriterStep());
		return pipeline;
	}
	
}
