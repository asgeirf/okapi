package net.sf.okapi.steps.common.stepannotations.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.okapi.steps.common.stepintrospector.StepParameterAccessType;
import net.sf.okapi.steps.common.stepintrospector.StepIntrospector;
import net.sf.okapi.steps.common.stepintrospector.StepParameter;
import net.sf.okapi.steps.common.stepintrospector.StepParameterType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StepAnnotationIntrospectorTest {	

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getAnnotationsFromXsltTransformStep() {
		HashMap<String, StepParameter> parameters = StepIntrospector.getStepParameters(new XSLTransformStep());
		
		StepParameter p = parameters.get("xsltPath");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.EXTERNAL);
		assertTrue(p.getJavaType() == String.class);
		assertNotNull(p.getRequiredStep());		
		assertEquals("Path to the xslt file used by this step", p.getDescription());
		
		p = parameters.get("errorLogPath");
		assertTrue(p.getAccessType() == StepParameterAccessType.WRITE_ONLY);
		assertTrue(p.getType() == StepParameterType.EXTERNAL);
		assertTrue(p.getJavaType() == String.class);
		assertNotNull(p.getRequiredStep());
		assertEquals("Path to the xslt error log file", p.getDescription());
	}
	
	@Test
	public void getAnnotationsFromSearchAndReplaceStep() {
		HashMap<String, StepParameter> parameters = StepIntrospector.getStepParameters(new SearchAndReplaceStep());
		
		StepParameter p = parameters.get("plainText");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		assertTrue(p.getJavaType() == Boolean.class);
		assertNull(p.getRequiredStep());
		assertEquals("If true the input is a plain text file, not a filtered event", p.getDescription());
		
		p = parameters.get("regEx");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		assertTrue(p.getJavaType() == String.class);
		assertNull(p.getRequiredStep());
		assertEquals("Java Regex used to search", p.getDescription());
		
		p = parameters.get("dotAll");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		assertTrue(p.getJavaType() == Boolean.class);
		assertNull(p.getRequiredStep());
		assertEquals("?????", p.getDescription());
		
		p = parameters.get("ignoreCase");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		assertTrue(p.getJavaType() == Boolean.class);
		assertNull(p.getRequiredStep());
		assertEquals("If true the search is case insensitive", p.getDescription());
		
		p = parameters.get("multiLine");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		assertTrue(p.getJavaType() == Boolean.class);
		assertNull(p.getRequiredStep());
		assertEquals("Search across line boundries", p.getDescription());	
		
		p = parameters.get("rules");
		assertTrue(p.getAccessType() == StepParameterAccessType.READ_ONLY);
		assertTrue(p.getType() == StepParameterType.STEP_CONFIGURATION);
		// why can't I test this statically as below??
		ArrayList<String[]> al = new ArrayList<String[]>();
		assertTrue(p.getJavaType() == al.getClass());
		assertTrue(p.getJavaType() == ArrayList.class);
		assertNull(p.getRequiredStep());
		assertEquals("List of rules", p.getDescription());						
	}
	
	@Test
	public void getHandlersFromSearchAndReplaceStep() {
		List<String> handlers = StepIntrospector.getStepEventHandlers(new SearchAndReplaceStep());
		assertEquals(4, handlers.size());
		for (String h : handlers) {
			assertTrue(h.startsWith("handle"));
		}
	}
	
	@Test
	public void getHandlersFromXsltStep() {
		List<String> handlers = StepIntrospector.getStepEventHandlers(new XSLTransformStep());
		assertEquals(3, handlers.size());
		for (String h : handlers) {
			assertTrue(h.startsWith("handle"));
		}
	}
}
