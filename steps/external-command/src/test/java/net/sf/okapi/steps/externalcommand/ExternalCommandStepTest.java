package net.sf.okapi.steps.externalcommand;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalCommandStepTest {
	private Pipeline pipeline;
	private ExternalCommandStep externalCommand;	

	@Before
	public void setUp() throws Exception {
		// create pipeline
		pipeline = new Pipeline();

		// configure ExternalCommandStep
		externalCommand = new ExternalCommandStep();

		pipeline.addStep(externalCommand);
	}

	@After
	public void tearDown() throws Exception {
		pipeline.destroy();
	}

	@Test
	public void sortCommand() throws URISyntaxException {
		Parameters p = new Parameters();
		switch (Util.getOS()) {
		case WINDOWS:
			p.setCommand("sort ${inputPath} /O ${outputPath}");
			break;
		case MAC:
			p.setCommand("sort ${inputPath} > ${outputPath}");
			break;
		case LINUX:
			p.setCommand("sort ${inputPath} > ${outputPath}");
			break;
		}		
		p.setTimeout(60);
		externalCommand.setParameters(p);

		URL url = ExternalCommandStepTest.class.getResource("/test.txt");
		RawDocument d = new RawDocument(url.toURI(), "UTF-8", LocaleId.ENGLISH);

		pipeline.startBatch();

		Event e = pipeline.process(d);

		pipeline.endBatch();
		
		assertTrue((new File(e.getRawDocument().getInputURI()).exists()));
	}
}
