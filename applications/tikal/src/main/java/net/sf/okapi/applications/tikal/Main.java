/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.applications.tikal;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.BaseContext;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.ui.InputDialog;
import net.sf.okapi.common.ui.filters.FilterConfigurationsDialog;
import net.sf.okapi.common.ui.genericeditor.GenericEditor;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.lib.translation.IQuery;
import net.sf.okapi.lib.translation.QueryResult;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.formatconversion.FormatConversionStep;
import net.sf.okapi.steps.formatconversion.Parameters;
import net.sf.okapi.steps.formatconversion.TableFilterWriterParameters;
import net.sf.okapi.steps.leveraging.LeveragingStep;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import net.sf.okapi.connectors.globalsight.GlobalSightTMConnector;
import net.sf.okapi.connectors.google.GoogleMTConnector;
import net.sf.okapi.connectors.mymemory.MyMemoryTMConnector;
import net.sf.okapi.connectors.opentran.OpenTranTMConnector;
import net.sf.okapi.connectors.pensieve.PensieveTMConnector;
import net.sf.okapi.connectors.translatetoolkit.TranslateToolkitTMConnector;

public class Main {
	
	protected final static int CMD_EXTRACT = 0;
	protected final static int CMD_MERGE = 1;
	protected final static int CMD_EDITCONFIG = 2;
	protected final static int CMD_QUERYTRANS = 3;
	protected final static int CMD_CONV2PO = 4;
	protected final static int CMD_CONV2TMX = 5;
	protected final static int CMD_CONV2TABLE = 6;
	protected final static int CMD_CONV2PEN = 7;
	
	private static final String DEFAULT_SEGRULES = "-";

	private static PrintStream ps;
	
	protected ArrayList<String> inputs;
	protected String skeleton;
	protected String output;
	protected String specifiedConfigId;
	protected String configId;
	protected String inputEncoding;
	protected String outputEncoding;
	protected String srcLang = Locale.getDefault().getLanguage();
	protected String trgLang = "fr";
	protected int command = -1;
	protected String query;
	protected boolean useGoogle;
	protected boolean useOpenTran;
	protected boolean useTransToolkit;
	protected String transToolkitParams;
	protected boolean useGlobalSight;
	protected String globalSightParams;
	protected boolean useMyMemory;
	protected String myMemoryParams;
	protected boolean usePensieve;
	protected String pensieveDir;
	protected boolean genericOutput = false;
	protected String tableConvFormat;
	protected String tableConvCodes;
	protected int convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_TARGETOREMPTY;
	protected String segRules;
	protected boolean showTraceHint = true;
	
	private FilterConfigurationMapper fcMapper;
	private Hashtable<String, String> extensionsMap;
	private Hashtable<String, String> filtersMap;

	/**
	 * Try the guess the encoding of the console.
	 * @return the guessed name of the console's encoding.
	 */
	private static String getConsoleEncodingName () {
		String osName = System.getProperty("os.name");
		if ( osName.startsWith("Mac OS")) {
			return "UTF-8"; // Apparently the default for bash on Mac
		}
		if ( osName.startsWith("Windows") ) {
			//TODO: Get DOS code-pages per locale
			return "cp850"; // Not perfect, but covers many languages
		}
		// Default: Assumes unique encoding overall 
		return Charset.defaultCharset().name();
	}
	
	public static void main (String[] originalArgs) {
		Main prog = new Main();
		boolean showTrace = false;
		try {
			// Create an encoding-aware output for the console
			// System.out uses the default system encoding that
			// may not be the right one (e.g. windows-1252 vs cp850)
			ps = new PrintStream(System.out, true, getConsoleEncodingName());
			// Disable root console handler
			Handler[] handlers = Logger.getLogger("").getHandlers();
			for ( Handler handler : handlers ) {
				Logger.getLogger("").removeHandler(handler);
			}
			// Create our own handler
			LogHandler logHandler = new LogHandler(ps);
			logHandler.setLevel(Level.INFO);
			Logger.getLogger("").addHandler(logHandler); //$NON-NLS-1$
			
			// Remove all empty arguments
			// This is to work around the "$1" issue in bash
			ArrayList<String> args = new ArrayList<String>();
			for ( String tmp : originalArgs ) {
				if ( tmp.length() > 0 ) args.add(tmp);
			}
			
			prog.printBanner();
			if ( args.size() == 0 ) {
				prog.printUsage();
				return;
			}
			if ( args.contains("-?") ) {
				prog.printUsage();
				return; // Overrides all arguments 
			}
			if ( args.contains("-h") || args.contains("--help") || args.contains("-help") ) {
				prog.showHelp();
				return; // Overrides all arguments
			}
			if ( args.contains("-trace") ) {
				// Check early so the option does not get 'eaten' by a bad syntax
				showTrace = true;
			}
			
			for ( int i=0; i<args.size(); i++ ) {
				String arg = args.get(i);
				if ( arg.equals("-fc") ) {
					prog.specifiedConfigId = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-sl") ) {
					prog.srcLang = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-tl") ) {
					prog.trgLang = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-ie") ) {
					prog.inputEncoding = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-oe") ) {
					prog.outputEncoding = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-x") ) {
					prog.command = CMD_EXTRACT;
				}
				else if ( arg.equals("-m") ) {
					prog.command = CMD_MERGE;
				}
				else if ( arg.equals("-2po") ) {
					prog.command = CMD_CONV2PO;
				}
				else if ( arg.equals("-2tmx") ) {
					prog.command = CMD_CONV2TMX;
				}
				else if ( arg.equals("-2tbl") ) {
					prog.command = CMD_CONV2TABLE;
				}
				else if ( arg.equals("-csv") ) {
					prog.tableConvFormat = "csv";
				}
				else if ( arg.equals("-tab") ) {
					prog.tableConvFormat = "tab";
				}
				else if ( arg.equals("-xliff") ) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_XLIFF;
				}
				else if ( arg.equals("-xliffgx") ) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_XLIFFGX;
				}
				else if ( arg.equals("-tmx") ) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_TMX;
				}
				else if ( arg.equals("-trgsource") ) {
					prog.convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_FORCESOURCE;
				}
				else if ( arg.equals("-trgempty") ) {
					prog.convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_FORCEEMPTY;
				}
				else if ( arg.equals("-imp") ) {
					prog.command = CMD_CONV2PEN;
					prog.pensieveDir = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-e") ) {
					prog.command = CMD_EDITCONFIG;
					if ( args.size() > i+1 ) {
						if ( !args.get(i+1).startsWith("-") ) {
							prog.specifiedConfigId = args.get(++i);
						}
					}
				}
				else if ( arg.equals("-generic") ) {
					prog.genericOutput = true;
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_GENERIC;
				}
				else if ( arg.equals("-q") ) {
					prog.command = CMD_QUERYTRANS;
					prog.query = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-google") ) {
					prog.useGoogle = true;
				}
				else if ( arg.equals("-opentran") ) {
					prog.useOpenTran = true;
				}
				else if ( arg.equals("-tt") ) {
					prog.useTransToolkit = true;
					prog.transToolkitParams = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-gs") ) {
					prog.useGlobalSight = true;
					prog.globalSightParams = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-mm") ) {
					prog.useMyMemory = true;
					prog.myMemoryParams = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-pen") ) {
					prog.usePensieve = true;
					prog.pensieveDir = prog.getArgument(args, ++i);
				}
				else if ( arg.equals("-listconf") || arg.equals("-lfc") ) {
					prog.showAllConfigurations();
					return;
				}
				else if ( arg.equals("-seg") ) {
					prog.segRules = DEFAULT_SEGRULES; // Default
					if ( args.size() > i+1 ) {
						if ( !args.get(i+1).startsWith("-") ) {
							prog.segRules = args.get(++i);
						}
					}
				}
				else if ( arg.equals("-trace") ) {
					// Trace aAlready set. this is just to avoid
					// seeing -trace as invalid parameter
				}
				//=== Input file or error
				else if ( !arg.startsWith("-") ) {
					prog.inputs.add(args.get(i));
				}
				else {
					prog.showTraceHint = false; // Using trace is not helpful to the user for this error
					throw new InvalidParameterException(
						String.format("Invalid command-line argument '%s'.", args.get(i)));
				}
			}

			// Forgive having the extension .fprm from configuration ID if there is one
			if ( prog.specifiedConfigId != null ) {
				if ( prog.specifiedConfigId.endsWith(FilterConfigurationMapper.CONFIGFILE_EXT) ) {
					prog.specifiedConfigId = Util.getFilename(prog.specifiedConfigId, false);
				}
			}
			
			// Check inputs and command
			if ( prog.command == -1 ) {
				ps.println("No command specified. Please use one of the command described below:");
				prog.printUsage();
				return;
			}
			if ( prog.command == CMD_EDITCONFIG ) {
				if ( prog.specifiedConfigId == null ) {
					prog.editAllConfigurations();
				}
				else {
					prog.editConfiguration();
				}
				return;
			}
			if ( prog.command == CMD_QUERYTRANS ) {
				prog.processQuery();
				return;
			}
			if ( prog.inputs.size() == 0 ) {
				throw new RuntimeException("No input document specified.");
			}
			
			// Process all input files
			for ( int i=0; i<prog.inputs.size(); i++ ) {
				if ( i > 0 ) {
					ps.println("------------------------------------------------------------"); //$NON-NLS-1$
				}
				prog.process(prog.inputs.get(i));
			}
		}
		catch ( Throwable e ) {
			if ( showTrace ) e.printStackTrace();
			else {
				ps.println("ERROR: "+e.getMessage());
				Throwable e2 = e.getCause();
				if ( e2 != null ) ps.println(e2.getMessage());
				if ( prog.showTraceHint ) ps.println("You can use the -trace option for more details.");
			}
			System.exit(1); // Error
		}
	}

	public Main () {
		inputs = new ArrayList<String>();
	}
	
	protected String getArgument (ArrayList<String> args, int index) {
		if ( index >= args.size() ) {
			showTraceHint = false; // Using trace is not helpful to the user for this error
			throw new RuntimeException(String.format(
				"Missing parameter after '%s'", args.get(index-1)));
		}
		return args.get(index);
	}
	
	private void initialize () {
		// Create the mapper and load it with all parameters editor info
		// Do not load the filter configurations yet (time consuming)
		fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, false, false);
		
		// Instead create a map with extensions -> filter
		extensionsMap = new Hashtable<String, String>();
		filtersMap = new Hashtable<String, String>();
		
		extensionsMap.put(".docx", "okf_openxml");
		extensionsMap.put(".pptx", "okf_openxml");
		extensionsMap.put(".xlsx", "okf_openxml");
		filtersMap.put("okf_openxml", "net.sf.okapi.filters.openxml.OpenXMLFilter");

		extensionsMap.put(".odt", "okf_openoffice");
		extensionsMap.put(".swx", "okf_openoffice");
		extensionsMap.put(".ods", "okf_openoffice");
		extensionsMap.put(".swc", "okf_openoffice");
		extensionsMap.put(".odp", "okf_openoffice");
		extensionsMap.put(".sxi", "okf_openoffice");
		extensionsMap.put(".odg", "okf_openoffice");
		extensionsMap.put(".sxd", "okf_openoffice");
		filtersMap.put("okf_openoffice", "net.sf.okapi.filters.openoffice.OpenOfficeFilter");

		extensionsMap.put(".htm", "okf_html");
		extensionsMap.put(".html", "okf_html");
		filtersMap.put("okf_html", "net.sf.okapi.filters.html.HtmlFilter");
		
		extensionsMap.put(".xlf", "okf_xliff");
		extensionsMap.put(".xlif", "okf_xliff");
		extensionsMap.put(".xliff", "okf_xliff");
		filtersMap.put("okf_xliff", "net.sf.okapi.filters.xliff.XLIFFFilter");
		
		extensionsMap.put(".tmx", "okf_tmx");
		filtersMap.put("okf_tmx", "net.sf.okapi.filters.tmx.TmxFilter");
		
		extensionsMap.put(".properties", "okf_properties");
		filtersMap.put("okf_properties", "net.sf.okapi.filters.properties.PropertiesFilter");
		
		extensionsMap.put(".po", "okf_po");
		filtersMap.put("okf_po", "net.sf.okapi.filters.po.POFilter");
		
		extensionsMap.put(".xml", "okf_xml");
		extensionsMap.put(".resx", "okf_xml-resx");
		filtersMap.put("okf_xml", "net.sf.okapi.filters.xml.XMLFilter");
		
		extensionsMap.put(".srt", "okf_regex-srt");
		filtersMap.put("okf_regex", "net.sf.okapi.filters.regex.RegexFilter");
		
		extensionsMap.put(".dtd", "okf_dtd");
		extensionsMap.put(".ent", "okf_dtd");
		filtersMap.put("okf_dtd", "net.sf.okapi.filters.dtd.DTDFilter");
		
		extensionsMap.put(".ts", "okf_ts");
		filtersMap.put("okf_ts", "net.sf.okapi.filters.ts.TsFilter");
		
		extensionsMap.put(".txt", "okf_plaintext");
		filtersMap.put("okf_plaintext", "net.sf.okapi.filters.plaintext.PlainTextFilter");

		extensionsMap.put(".csv", "okf_table_csv");
		filtersMap.put("okf_table", "net.sf.okapi.filters.table.TableFilter");

		extensionsMap.put(".json", "okf_json");
		filtersMap.put("okf_json", "net.sf.okapi.filters.json.JSONFilter");

		filtersMap.put("okf_pensieve", "net.sf.okapi.steps.formatconversion.PensieveFilter");
	}
	
	private String getConfigurationId (String ext) {
		// Get the configuration for the extension
		String id = extensionsMap.get(ext);
		if ( id == null ) {
			throw new RuntimeException(String.format(
				"Could not guess the configuration for the extension '%s'", ext));
		}
		return id;
	}
	
	private void editAllConfigurations () {
		initialize();
		// Add all the pre-defined configurations
		DefaultFilters.setMappings(fcMapper, false, true);
		// Add the custom configurations
		fcMapper.updateCustomConfigurations();
		
		// Edit
		FilterConfigurationsDialog dlg = new FilterConfigurationsDialog(null, false, fcMapper, null);
		dlg.showDialog(specifiedConfigId);
	}
	
	private void editConfiguration () {
		initialize();
		
		if ( specifiedConfigId == null ) {
			throw new RuntimeException("You must specified the configuration to edit.");
		}
		configId = specifiedConfigId;
		if ( !prepareFilter(configId) ) return; // Next input

		FilterConfiguration config = fcMapper.getConfiguration(configId);
		if ( config == null ) {
			throw new RuntimeException(String.format(
				"Cannot find the configuration for '%s'.", configId));
		}
		IParameters params = fcMapper.getParameters(config);
		if ( params == null ) {
			throw new RuntimeException(String.format(
				"Cannot load parameters for '%s'.", config.configId));
		}
		
		IParametersEditor editor = fcMapper.createConfigurationEditor(configId);
		if ( editor != null ) {
			if ( !editor.edit(params, !config.custom, new BaseContext()) ) return; // Cancel
		}
		else {
			// Try to see if we can edit with the generic editor
			IEditorDescriptionProvider descProv = fcMapper.getDescriptionProvider(params.getClass().getCanonicalName());
			if ( descProv != null ) {
				// Edit the data
				GenericEditor genEditor = new GenericEditor();
				if ( !genEditor.edit(params, descProv, !config.custom, new BaseContext()) ) return; // Cancel
				// The params object gets updated if edit not canceled.
			}
			else { // Else: fall back to the plain text editor
				InputDialog dlg  = new InputDialog(null,
					String.format("Filter Parameters (%s)", config.configId), "Parameters:",
					params.toString(), null, 0, 200, 600);
				dlg.setReadOnly(!config.custom); // Pre-defined configurations should be read-only
				String data = dlg.showDialog();
				if ( data == null ) return; // Cancel
				if ( !config.custom ) return; // Don't save pre-defined parameters
				data = data.replace("\r\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				params.fromString(data.replace("\r", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		// If not canceled and if custom configuration: save the changes
		if ( config.custom ) {
			// Save the configuration filefcMapper
			fcMapper.saveCustomParameters(config, params);
		}
	}
	
	private void showAllConfigurations () {
		initialize();
		DefaultFilters.setMappings(fcMapper, true, true);
		// Add the Pensieve filter to the list (for now)
		// The filter may be added in the default list at some point, but is not yet.
		fcMapper.addConfigurations(filtersMap.get("okf_pensieve"));

		ps.println("List of all filter configurations available:");
		Iterator<FilterConfiguration> iter = fcMapper.getAllConfigurations();
		FilterConfiguration config;
		while ( iter.hasNext() ) {
			config = iter.next();
			ps.println(String.format(" - %s = %s",
				config.configId, config.description));
		}
	}
	
	private boolean prepareFilter (String configId) {
		// Is it a default configuration?
		if ( filtersMap.containsKey(configId) ) {
			// Configuration ID is a default one:
			// Add its filter to the configuration mapper
			fcMapper.addConfigurations(filtersMap.get(configId));
			return true;
		}
		
		// Else: Try to find the filter for that configuration
		for ( String tmp : filtersMap.keySet() ) {
			if ( configId.startsWith(tmp) ) {
				fcMapper.addConfigurations(filtersMap.get(tmp));
				// If the given configuration is not one of the pre-defined
				if ( fcMapper.getConfiguration(configId) == null ) {
					// Assume it is a custom one
					fcMapper.addCustomConfiguration(configId);
				}
				return true;
			}
		}
		
		// Could not guess
		ps.println(String.format(
			"ERROR: Could not guess the filter for the configuration '%s'", configId));
		return false;
	}

	private void guessMissingParameters (String inputOfConfig) {
		if ( specifiedConfigId == null ) {
			String ext = Util.getExtension(inputOfConfig);
			if ( Util.isEmpty(ext) ) {
				throw new RuntimeException(String.format(
					"The input file '%s' has no extension to guess the filter from.", inputOfConfig));
			}
			configId = getConfigurationId(ext.toLowerCase());
		}
		else {
			configId = specifiedConfigId;
		}
		
		if ( outputEncoding == null ) {
			if ( inputEncoding != null ) outputEncoding = inputEncoding;
			else outputEncoding = Charset.defaultCharset().name();
		}
		if ( inputEncoding == null ) {
			inputEncoding = Charset.defaultCharset().name();
		}
	}
	
	private void guessMergingArguments (String input) {
		
		String ext = Util.getExtension(input);
		if ( !ext.equals(".xlf") ) {
			throw new RuntimeException(String.format(
				"The input file '%s' does not have the expected .xlf extension.", input));
		}
		
		int n = input.lastIndexOf('.');
		skeleton = input.substring(0, n);
		
		ext = Util.getExtension(skeleton);
		n = skeleton.lastIndexOf('.');
		output = skeleton.substring(0, n) + ".out" + ext;
	}
	
	protected void process (String input) throws URISyntaxException {
		initialize();
		RawDocument rd;
		File file;
		
		switch ( command ) {
//		case CMD_EXTRACT:
//			guessMissingParameters(input);
//			if ( !prepareFilter(configId) ) return; // Next input
//			
//			file = new File(input);
//			rd = new RawDocument(file.toURI(), inputEncoding, srcLang, trgLang);
//			rd.setFilterConfigId(configId);
//			
//			ps.println("Source language: "+srcLang);
//			ps.print("Target language: ");
//			ps.println(trgLang);
//			ps.println(" Input encoding: "+inputEncoding);
//			ps.println("  Configuration: "+configId);
//			ps.println(" Input document: "+input);
//			ps.print("Output document: ");
//			if ( output == null ) ps.println("<auto-defined>");
//			else ps.println(output);
//			ps.print("Extaction...");
//			
//			XLIFFExtractionStep stepExt = new XLIFFExtractionStep(fcMapper);
//			stepExt.handleRawDocument(rd);
//			ps.println(" Done");
//			break;
		
		case CMD_EXTRACT:
			guessMissingParameters(input);
			if ( !prepareFilter(configId) ) return; // Next input
			file = new File(input);
			rd = new RawDocument(file.toURI(), inputEncoding, srcLang, trgLang);
			rd.setFilterConfigId(configId);
			ps.println("Extaction");
			extractFile(rd);
			ps.println("Done");
			break;
			
		case CMD_MERGE:
			guessMergingArguments(input);
			guessMissingParameters(skeleton);
			if ( !prepareFilter(configId) ) return; // Next input
			XLIFFMergingStep stepMrg = new XLIFFMergingStep(fcMapper);

			file = new File(skeleton);
			RawDocument skelRawDoc = new RawDocument(file.toURI(), inputEncoding, srcLang, trgLang);
			skelRawDoc.setFilterConfigId(configId);
			stepMrg.setXliffPath(input);
			stepMrg.setOutputPath(output);
			stepMrg.setOutputEncoding(outputEncoding);
			
			ps.println("Source language: "+srcLang);
			ps.print("Target language: ");
			ps.println(trgLang);
			ps.println(" Input encoding: "+inputEncoding);
			ps.println("Output encoding: "+outputEncoding);
			ps.println("  Configuration: "+configId);
			ps.println(" XLIFF document: "+input);
			ps.print("Output document: ");
			if ( output == null ) ps.println("<auto-defined>");
			else ps.println(output);
			ps.println("Merging");

			stepMrg.handleRawDocument(skelRawDoc);
			ps.println("Done");
			break;
			
		case CMD_CONV2PO:
		case CMD_CONV2TMX:
		case CMD_CONV2PEN:
		case CMD_CONV2TABLE:
			guessMissingParameters(input);
			if ( !prepareFilter(configId) ) return; // Next input
			
			file = new File(input);
			String output = input;
			if ( command == CMD_CONV2PO ) {
				output += ".po";
			}
			else if ( command == CMD_CONV2TMX ) {
				output += ".tmx";
			}
			else if ( command == CMD_CONV2TABLE) {
				output += ".txt";
			}
			else { // Pensieve
				output = pensieveDir;
			}
			URI outputURI = new File(output).toURI();
			rd = new RawDocument(file.toURI(), inputEncoding, srcLang, trgLang);
			rd.setFilterConfigId(configId);
			
			ps.println("Source language: "+srcLang);
			ps.println("Target language: "+trgLang);
			ps.println(" Input encoding: "+inputEncoding);
			ps.println("  Configuration: "+configId);
			ps.println(" Input document: "+input);
			ps.println("Output document: "+output);
			if ( command == CMD_CONV2PO ) {
				ps.println("Conversion to PO");
			}
			else if ( command == CMD_CONV2TMX ) {
				ps.println("Conversion to TMX");
			}
			else if ( command == CMD_CONV2TABLE ) {
				ps.println("Conversion to Table");
			}
			else {
				ps.println("Importing to Pensieve TM");
			}
			
			convertFile(rd, outputURI);
			ps.println("Done");
			break;
		}
	}
	
	private void printBanner () {
		ps.println("-------------------------------------------------------------------------------"); //$NON-NLS-1$
		ps.println("Okapi Tikal - Localization Toolset");
		// The version will show as 'null' until the code is build as a JAR.
		ps.println(String.format("Version: %s", getClass().getPackage().getImplementationVersion()));
		ps.println("-------------------------------------------------------------------------------"); //$NON-NLS-1$
	}

	private String getRootDirectory () {
		URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
		return Util.getDirectoryName(Util.getDirectoryName(url.getPath()));
	}
	
	private void showHelp () throws MalformedURLException {
		// Get the path/URL of the help file 
		String path = getRootDirectory();
		path += "/help/applications/tikal/index.html"; //$NON-NLS-1$
		// Opens the file
		ps.println("Help: "+path);
		Util.openURL((new File(path)).toURL().toString());
	}
	
	private void printUsage () {
		ps.println("Show this screen:");
		ps.println("   -?");
		ps.println("Open the user guide page:");
		ps.println("   -h or --help");
		ps.println("List all available filter configurations:");
		ps.println("   -lfc or -listconf");
		ps.println("Edit or view filter configurations (UI-dependent command):");
		ps.println("   -e [[-fc] configId]");
		ps.println("Extract a file to XLIFF (and optionally segment and pre-translate):");
		ps.println("   -x inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-sl sourceLang] [-tl targetLang] [-seg [srxFile]]");
		ps.println("      [-tt hostname[:port]|-mm key|-pen tmDirectory|-google|-gs configFile]");
		ps.println("Merge an XLIFF document back to its original format:");
		ps.println("   -m xliffFile [xliffFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-oe encoding] [-sl sourceLang] [-tl targetLang]");
		ps.println("Query translation resources:");
		ps.println("   -q \"source text\" [-sl sourceLang] [-tl targetLang] [-google] [-opentran]");
		ps.println("      [-tt hostname[:port]] [-mm key] [-pen tmDirectory] [-gs configFile]");
		ps.println("Conversion to PO format:");
		ps.println("   -2po inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-sl sourceLang] [-tl targetLang] [-generic] [-trgsource|-trgempty]");
		ps.println("Conversion to TMX format:");
		ps.println("   -2tmx inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-sl sourceLang] [-tl targetLang] [-trgsource|-trgempty]");
		ps.println("Conversion to table format:");
		ps.println("   -2tbl inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-sl sourceLang] [-tl targetLang] [-trgsource|-trgempty]");
		ps.println("      [-csv|-tab] [-xliff|-xliffgx|-tmx|-generic]");
		ps.println("Import to Pensieve TM:");
		ps.println("   -imp tmDirectory inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		ps.println("      [-sl sourceLang] [-tl targetLang] [-trgsource|-trgempty]");
	}

	private void displayQuery (IQuery conn) {
		int count;
		if ( conn.getClass().getName().endsWith("PensieveTMConnector")
			|| conn.getClass().getName().endsWith("GoogleMTConnector")
			|| conn.getClass().getName().endsWith("GlobalSightTMConnector") ) {
			count = conn.query(parseToTextFragment(query));
		}
		else { // Raw text otherwise
			count = conn.query(query);
		}
		if ( count > 0 ) {
			QueryResult qr;
			while ( conn.hasNext() ) {
				qr = conn.next();
				ps.println(String.format("Result: From %s (%s->%s, score: %d)", conn.getName(),
					conn.getSourceLanguage(), conn.getTargetLanguage(), qr.score));
				ps.println(String.format("  Source: \"%s\"", qr.source.toString()));
				ps.println(String.format("  Target: \"%s\"", qr.target.toString()));
			}
		}
		else {
			ps.println(String.format("Result: From %s (%s->%s)", conn.getName(),
				conn.getSourceLanguage(), conn.getTargetLanguage()));
			ps.println(String.format("  Source: \"%s\"", query));
			ps.println("  <Not translation has been found>");
		}	
	}
	
	private void processQuery () {
		if ( !useGoogle && !useOpenTran && !useTransToolkit && !useMyMemory
			&& !usePensieve && ! useGlobalSight ) {
			useGoogle = true; // Default if none is specified
		}
		
		IQuery conn;
		if ( useGoogle ) {
			conn = new GoogleMTConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass().getName()));
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if ( usePensieve ) {
			conn = new PensieveTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass().getName()));
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if ( useTransToolkit ) {
			conn = new TranslateToolkitTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass().getName()));
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if ( useGlobalSight ) {
			conn = new GlobalSightTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass().getName()));
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if ( useMyMemory ) {
			conn = new MyMemoryTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass().getName()));
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if ( useOpenTran ) {
			conn = new OpenTranTMConnector();
			conn.setLanguages(srcLang, trgLang);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
	}

	private void convertFile (RawDocument rd, URI outputURI) {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);

		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);
		
		FormatConversionStep fcStep = new FormatConversionStep();
		net.sf.okapi.steps.formatconversion.Parameters params = fcStep.getParameters();
		if ( command == CMD_CONV2PO ) {
			params.setOutputFormat(Parameters.FORMAT_PO);
			params.setOutputPath("output.po");
		}
		else if ( command == CMD_CONV2TMX ) {
			params.setOutputFormat(Parameters.FORMAT_TMX);
			params.setOutputPath("output.tmx");
		}
		else if ( command == CMD_CONV2TABLE ) {
			params.setOutputFormat(Parameters.FORMAT_TABLE);
			TableFilterWriterParameters opt = new TableFilterWriterParameters();
			opt.fromArguments(tableConvFormat, tableConvCodes);
			params.setFormatOptions(opt.toString());
			params.setOutputPath("output.txt");
		}
		else if ( command == CMD_CONV2PEN ) {
			params.setOutputFormat(Parameters.FORMAT_PENSIEVE);
			params.setOutputPath(pensieveDir);
		}
		
		params.setSingleOutput(command==CMD_CONV2PEN);
		params.setUseGenericCodes(genericOutput);
		params.setTargetStyle(convTargetStyle);
		driver.addStep(fcStep);
		
		driver.addBatchItem(rd, outputURI, outputEncoding);
		driver.processBatch();
	}

	private void extractFile (RawDocument rd) throws URISyntaxException {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);

		// Raw document to filter events step 
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);
		
		// Add segmentation step if requested
		if ( segRules != null ) {
			if ( segRules.equals(DEFAULT_SEGRULES) ) { // Defaults
				segRules = getRootDirectory();
				segRules += File.separator + "config" + File.separator + "defaultSegmentation.srx";
			}
			else {
				if ( Util.isEmpty(Util.getExtension(segRules)) ) {
					segRules += ".srx";
				}
			}
			SegmentationStep segStep = new SegmentationStep();
			net.sf.okapi.steps.segmentation.Parameters segParams
				= (net.sf.okapi.steps.segmentation.Parameters)segStep.getParameters();
			segParams.segmentSource = true;
			segParams.segmentTarget = true;
			File f = new File(segRules);
			segParams.sourceSrxPath = f.getAbsolutePath();
			segParams.targetSrxPath = f.getAbsolutePath();
			driver.addStep(segStep);
			ps.println("Segmentation: " + segRules);
		}
		
		// Add leveraging step if requested
		if ( useGoogle || useTransToolkit || useMyMemory || usePensieve ) {
			LeveragingStep levStep = new LeveragingStep();
			net.sf.okapi.steps.leveraging.Parameters levParams
				= (net.sf.okapi.steps.leveraging.Parameters)levStep.getParameters();
			if ( usePensieve ) {
				levParams.setResourceClassName(PensieveTMConnector.class.getName());
			}
			else if ( useTransToolkit ) {
				levParams.setResourceClassName(TranslateToolkitTMConnector.class.getName());
			}
			else if ( useMyMemory ) {
				levParams.setResourceClassName(MyMemoryTMConnector.class.getName());
			}
			else if ( useGoogle ) {
				levParams.setResourceClassName(GoogleMTConnector.class.getName());
			}
			IParameters p = prepareConnectorParameters(levParams.getResourceClassName());
			if ( p != null ) levParams.setResourceParameters(p.toString());
			driver.addStep(levStep);
		}
		
		// Filter events to raw document final step (using the XLIFF writer)
		FilterEventsWriterStep fewStep = new FilterEventsWriterStep();
		fewStep.setFilterWriter(new XLIFFWriter());
		fewStep.setDocumentRoots(System.getProperty("user.dir"));
		driver.addStep(fewStep);

		// Create the raw document and set the output
		String tmp = rd.getInputURI().getPath();
		// If the input is a directory, it ends with a separator, then we remove it
		if ( tmp.endsWith("/") || tmp.endsWith("\\") ) {
			tmp = tmp.substring(0, tmp.length()-1);
		}
		tmp += ".xlf";
		driver.addBatchItem(rd, new URI(tmp), outputEncoding);

		// Process
		driver.processBatch();
	}

	private IParameters prepareConnectorParameters (String connectorClassName) {
		if ( connectorClassName.equals(PensieveTMConnector.class.getName()) ) {
			net.sf.okapi.connectors.pensieve.Parameters params
				= new net.sf.okapi.connectors.pensieve.Parameters();
			params.setDbDirectory(pensieveDir);
			return params;
		}

		if ( connectorClassName.equals(TranslateToolkitTMConnector.class.getName()) ) {
			net.sf.okapi.connectors.translatetoolkit.Parameters params
				= new net.sf.okapi.connectors.translatetoolkit.Parameters();
			// Parse the parameters hostname:port
			int n = transToolkitParams.lastIndexOf(':');
			if ( n == -1 ) {
				params.setHost(transToolkitParams);
			}
			else {
				params.setPort(Integer.valueOf(transToolkitParams.substring(n+1)));
				params.setHost(transToolkitParams.substring(0, n));
			}
			return params;
		}

		if ( connectorClassName.equals(MyMemoryTMConnector.class.getName()) ) {
			net.sf.okapi.connectors.mymemory.Parameters params
				= new net.sf.okapi.connectors.mymemory.Parameters();
			params.setKey(myMemoryParams);
			return params;
		}
		
		if ( connectorClassName.equals(GlobalSightTMConnector.class.getName()) ) {
			net.sf.okapi.connectors.globalsight.Parameters params
				= new net.sf.okapi.connectors.globalsight.Parameters();
			URI paramURI = (new File(globalSightParams).toURI());
			params.load(paramURI, false);
			return params;
		}
		
		// Other connector: no parameters
		return null;
	}

	/**
	 * Converts the plain text string into a TextFragment, using HTML-like patterns are inline codes.
	 * @param text the plain text to convert to TextFragment
	 * @return a new TextFragment (with possibly inline codes).
	 */	
	public TextFragment parseToTextFragment (String text) {
		// Parses any thing within <...> into opening codes
		// Parses any thing within </...> into closing codes
		// Parses any thing within <.../> into placeholder codes
		Pattern patternOpening = Pattern.compile("\\<(\\w+)[ ]*[^\\>/]*\\>");
		Pattern patternClosing = Pattern.compile("\\</(\\w+)[ ]*[^\\>]*\\>");
		Pattern patternPlaceholder = Pattern.compile("\\<(\\w+)[ ]*[^\\>]*/\\>");
		
		TextFragment tf = new TextFragment();
		tf.setCodedText(text);

		int n;
		int start = 0;
		int diff = 0;
		Matcher m = patternOpening.matcher(text);
		while ( m.find(start) ) {
			n = m.start();
			diff += tf.changeToCode(n+diff, (n+diff)+m.group().length(),
				TagType.OPENING, m.group(1));
			start = (n+m.group().length());
		}
		
		text = tf.getCodedText();
		start = diff = 0;
		m = patternClosing.matcher(text);
		while ( m.find(start) ) {
			n = m.start();
			diff += tf.changeToCode(n+diff, (n+diff)+m.group().length(),
				TagType.CLOSING, m.group(1));
			start = (n+m.group().length());
		}
		
		text = tf.getCodedText();
		start = diff = 0;
		m = patternPlaceholder.matcher(text);
		while ( m.find(start) ) {
			n = m.start();
			diff += tf.changeToCode(n+diff, (n+diff)+m.group().length(),
				TagType.PLACEHOLDER, null);
			start = (n+m.group().length());
		}
		return tf;
	}

}
