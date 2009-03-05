package net.sf.okapi.filters.properties.tests;

import java.net.URI;
import java.net.URL;

import net.sf.okapi.common.pipeline.FilterPipelineStepAdaptor;
import net.sf.okapi.common.pipeline.FilterWriterPipelineStepAdaptor;
import net.sf.okapi.common.pipeline.IPipeline;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.writer.GenericFilterWriter;
import net.sf.okapi.filters.properties.PropertiesFilter;

import org.junit.Before;
import org.junit.Test;

public class PropertiesPipelineFilterTest {

	@Test
	public void runPipelineFromStream() throws Exception {
		
		//--get the base path based on a known file--
		String basePath = PropertiesPipelineFilterTest.class.getResource("/Test01.properties").toURI().getPath();
		basePath = "file://"+basePath.replace("/bin/Test01.properties","");
		
		//--First--
		IPipeline pipeline = new Pipeline();
		
		PropertiesFilter propertiesFilter = new PropertiesFilter();
		propertiesFilter.setOptions("en", "windows-1252", true);
		
		GenericSkeletonWriter genericSkeletonWriter = new GenericSkeletonWriter();
		GenericFilterWriter genericFilterWriter = new GenericFilterWriter(genericSkeletonWriter);
		genericFilterWriter.setOptions("en", "windows-1252");
		genericFilterWriter.setOutput("data/Test01_first_trip.properties");
		
		pipeline.addStep(new FilterPipelineStepAdaptor(propertiesFilter));
		pipeline.addStep(new FilterWriterPipelineStepAdaptor(genericFilterWriter));
		
		pipeline.preprocess();		
		pipeline.process(new URI(basePath+"/data/Test02.properties"));
		pipeline.postprocess();
		pipeline.close();
		
		//--Second--
		IPipeline pipeline2 = new Pipeline();

		PropertiesFilter propertiesFilter2 = new PropertiesFilter();
		propertiesFilter2.setOptions("en", "windows-1252", true);
		
		GenericSkeletonWriter genericSkeletonWriter2 = new GenericSkeletonWriter();
		GenericFilterWriter genericFilterWriter2 = new GenericFilterWriter(genericSkeletonWriter2);
		genericFilterWriter2.setOptions("en", "windows-1252");
		genericFilterWriter2.setOutput("data/Test01_second_trip.properties");
		
		pipeline2.addStep(new FilterPipelineStepAdaptor(propertiesFilter2));
		pipeline2.addStep(new FilterWriterPipelineStepAdaptor(genericFilterWriter2));
		
		pipeline2.preprocess();		
		pipeline2.process(new URI(basePath+"/data/Test01_first_trip.properties"));
		pipeline2.postprocess();
		pipeline2.close();
	}
}