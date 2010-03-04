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

package net.sf.okapi.steps.xliffkit.writer;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.leveraging.LeveragingStep;
import net.sf.okapi.steps.textmodification.TextModificationStep;
import net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder.Batch;
import net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder.BatchItem;
import net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder.Pipeline;
import net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder.PipelineStep;
import net.sf.okapi.steps.xliffkit.sandbox.pipelinebuilder.Parameter;

import org.junit.Test;

public class XLIFFKitWriterTest {

	private final String IN_NAME1 = "Gate Openerss.htm";
	private final String IN_NAME2 = "TestDocument01.odt";
	private final String IN_NAME3 = "test.txt";
	
	private Pipeline buildPipeline(String inPath) {
		
		XLIFFKitWriterStep step1 = new XLIFFKitWriterStep();
		// TODO Create outPath parameter, move to constructor
		// Output files are created in /target/test-classes/net/sf/okapi/steps/xliffkit/writer
		String outPath = Util.getDirectoryName(this.getClass().getResource(inPath).getPath()) + "/" + inPath + ".xlf";
		step1.setOutput(outPath);
		//step1.setOptions(LocaleId.FRENCH, "UTF-8");
		
//		LeveragingStep step2 = new LeveragingStep();
//		step2.setsourceLocale(LocaleId.ENGLISH);
//		step2.setTargetLocale(LocaleId.FRENCH);
		
//		TextModificationStep step3 = new TextModificationStep();
//		step3.setTargetLocale(LocaleId.FRENCH);
		
		return
			new Pipeline(
					"Test pipeline for XLIFFKitWriterStep",
					new Batch(
							new BatchItem(
									this.getClass().getResource(inPath),
									"UTF-8",
									LocaleId.ENGLISH,
									LocaleId.FRENCH
							)
					),
					new RawDocumentToFilterEventsStep(),
					
					new PipelineStep(new LeveragingStep(), 
							//new Parameter("resourceClassName", net.sf.okapi.connectors.opentran.OpenTranTMConnector.class.getName()),
							new Parameter("resourceClassName", net.sf.okapi.connectors.google.GoogleMTConnector.class.getName()),
							new Parameter("threshold", 80),
							new Parameter("fillTarget", true)
					),
//					new PipelineStep(new TextModificationStep(), 
//							new Parameter("type", 0),
//							new Parameter("addPrefix", true),
//							new Parameter("prefix", "{START_"),
//							new Parameter("addSuffix", true),
//							new Parameter("suffix", "_END}"),
//							new Parameter("applyToExistingTarget", false),
//							new Parameter("addName", false),
//							new Parameter("addID", true),
//							new Parameter("markSegments", false)
//					),
					step1
			);
	}
	
	@Test
	public void testOutputFile() {
		
		// DEBUG
		//buildPipeline(IN_NAME1).execute();
		//buildPipeline(IN_NAME2).execute();
		//buildPipeline(IN_NAME3).execute();
	}
		
}
