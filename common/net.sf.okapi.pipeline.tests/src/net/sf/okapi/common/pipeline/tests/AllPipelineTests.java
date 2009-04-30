package net.sf.okapi.common.pipeline.tests;

import net.sf.okapi.common.pipeline.tests.integration.XsltPipelineTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

@RunWith(Suite.class)
@SuiteClasses( { EventsToRawDocumentStepTest.class, FilterRoundtripTest.class, FilebasedPipelineTest.class, SimplePipelineTest.class, SimplePipelineWithCancelTest.class, XsltPipelineTest.class})
public class AllPipelineTests {

	public static Test suite() {
		return new JUnit4TestAdapter(AllPipelineTests.class);
	}

}
