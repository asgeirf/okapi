package net.sf.okapi.common.filters;

import java.util.List;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FilterConfigurationTest {

	private FilterConfiguration fc1;
	private FilterConfiguration fc2;
	
	@Before
	public void setUp () throws Exception {
		fc1 = new FilterConfiguration("config1",
			MimeTypeMapper.PROPERTIES_MIME_TYPE,
			"net.sf.okapi.filters.xml.XMLFilter",
			"Config1",
			"Description for Config1.");
		fc2 = new FilterConfiguration("config2",
			MimeTypeMapper.PROPERTIES_MIME_TYPE,
			"net.sf.okapi.filters.xml.XMLFilter",
			"Config2",
			"Description for Config2.");
		fc2.custom = true;
	}
	
	@Test
	public void simpleOverrideTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc1);
		FilterConfiguration cfg = fcm.getDefaultConfiguration(MimeTypeMapper.PROPERTIES_MIME_TYPE);
		assertEquals(cfg.configId, "config1");
		fcm.removeConfiguration(cfg.configId);
		fcm.addConfiguration(fc2);
		cfg = fcm.getDefaultConfiguration(MimeTypeMapper.PROPERTIES_MIME_TYPE);
		assertEquals(cfg.configId, "config2");
	}

	@Test
	public void simpleConfigTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc1);
		FilterConfiguration cfg = fcm.getConfiguration(fc1.configId);
		assertNotNull(cfg);
		assertEquals(cfg, fc1);
	}

	@Test
	public void getDefaultFromMimeTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfigurations(StubFilter.class.getName());
		FilterConfiguration cfg = fcm.getDefaultConfiguration("text/foo");
		assertNotNull("config should not be null", cfg);
		assertEquals("The Config ID", "foobar", cfg.configId);
	}

	@Test
	public void getFilterConfigTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc1);
		List<FilterConfiguration> list = fcm.getFilterConfigurations(fc1.filterClass);
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	@Test
	public void getMimeConfigTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc1);
		List<FilterConfiguration> list = fcm.getMimeConfigurations(fc1.mimeType);
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	@Test
	public void clearConfigTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc2);
		fcm.addConfiguration(fc1);
		assertNotNull(fcm.getConfiguration(fc1.configId));
		assertNotNull(fcm.getConfiguration(fc2.configId));
		fcm.clearConfigurations(false);
		assertNull(fcm.getConfiguration(fc1.configId));
		assertNull(fcm.getConfiguration(fc2.configId));
	}

	@Test
	public void clearCustomConfigTest () {
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfiguration(fc2); // Make sure custom is first
		fcm.addConfiguration(fc1);
		assertNotNull(fcm.getConfiguration(fc1.configId));
		assertNotNull(fcm.getConfiguration(fc2.configId));
		fcm.clearConfigurations(true);
		assertNotNull(fcm.getConfiguration(fc1.configId));
		assertNull(fcm.getConfiguration(fc2.configId));
	}

	@Test
	public void createFilterTestWithDefaultFilter () {
		String configId = "foobar";
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfigurations(StubFilter.class.getName());
		FilterConfiguration cfg = fcm.getConfiguration(configId);
		IFilter filter = fcm.createFilter(configId);
		assertNotNull("filter should not be null", filter);
		assertEquals(filter.getClass().getName(), cfg.filterClass);
	}

	@Test
	public void createFilterTestWithNonDefaultFilter () {
		String configId = "foobar-srt";
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfigurations(StubFilter.class.getName());
		FilterConfiguration cfg = fcm.getConfiguration(configId);
		IFilter filter = fcm.createFilter(configId);
		assertNotNull("filter should not be null", filter);
		assertEquals(filter.getClass().getName(), cfg.filterClass);
	}

	@Test
	public void removeFilterTest () {
		String configId = "foobar-srt";
		String filterClass = "net.sf.okapi.common.filters.StubFilter";
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfigurations(filterClass);
		FilterConfiguration cfg = fcm.getConfiguration(configId);
		assertNotNull(cfg);
		// Now remove
		fcm.removeConfigurations(filterClass);
		cfg = fcm.getConfiguration(configId);
		assertNull("Config should have not been found.", cfg);
	}

	@Test
	public void createEditorTest () {
		String configId = "foobar-srt";
		String editorClass = "net.sf.okapi.common.filters.StubEditor";
		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		fcm.addConfigurations("net.sf.okapi.common.filters.StubFilter");
		// Get the parameters class name for this filter
		IFilter filter = fcm.createFilter(configId);
		assertNotNull(filter);
		IParameters params = filter.getParameters();
		// Add it to the mapper
		fcm.addEditor(editorClass, params.getClass().getName());
		// Try to instantiate the editor object
		IParametersEditor editor = fcm.createConfigurationEditor(configId);
		assertNotNull("Editor should have been created.", editor);
		assertEquals("Editor class name.", editorClass, editor.getClass().getName());
	}

}
