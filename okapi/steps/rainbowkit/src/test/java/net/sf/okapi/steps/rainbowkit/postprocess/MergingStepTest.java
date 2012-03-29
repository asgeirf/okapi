/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.rainbowkit.postprocess;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.openoffice.OpenOfficeFilter;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.filters.rainbowkit.RainbowKitFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.Before;
import org.junit.Test;

public class MergingStepTest {
	
	private String root;
	private FilterConfigurationMapper fcMapper;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		root = TestUtil.getParentDir(this.getClass(), "/test01.properties");
		fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		fcMapper.addConfigurations(OpenOfficeFilter.class.getName());
		fcMapper.addConfigurations(RainbowKitFilter.class.getName());
		fcMapper.addConfigurations(XLIFFFilter.class.getName());
		fcMapper.setCustomConfigurationsDirectory(root);
		fcMapper.updateCustomConfigurations();
	}

	@Test
	public void testXLIFFMerging ()
		throws URISyntaxException
	{
		deleteOutputDir("xliffPack/done", true);
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(Util.deleteLastChar(root), Util.deleteLastChar(root)); // Don't include final separator
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		pdriver.addStep(mrgStep);
		
		Parameters prm = (Parameters)mrgStep.getParameters();
		prm.setReturnRawDocument(true);
		
		URI inputURI = new File(root+"xliffPack/manifest.rkm").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = new File(root+"xliffPack/done/test01.out.properties");
		assertTrue(file.exists());
		file = new File(root+"xliffPack/done/sub Dir/test01.out.odt");
		assertTrue(file.exists());
		
	}
	
	@Test
	public void testXINIMerging ()
		throws URISyntaxException
	{
		deleteOutputDir("xiniPack/translated", true);
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(Util.deleteLastChar(root), Util.deleteLastChar(root)); // Don't include final separator
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		pdriver.addStep(mrgStep);
		
		Parameters prm = (Parameters)mrgStep.getParameters();
		prm.setReturnRawDocument(true);
		
		URI inputURI = new File(root+"xiniPack/manifest.rkm").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = new File(root+"xiniPack/translated/test1.out.xlf");
		assertTrue(file.exists());
	}
	
	@Test
	public void testXINIMergingWithOutputPath ()
		throws URISyntaxException
	{
		deleteOutputDir("xiniPack/translated", true);
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(Util.deleteLastChar(root), Util.deleteLastChar(root)); // Don't include final separator
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		pdriver.addStep(mrgStep);
		
		Parameters prm = (Parameters)mrgStep.getParameters();
		prm.setReturnRawDocument(true);
		prm.setOverrideOutputPath(root+"output");
		
		URI inputURI = new File(root+"xiniPack/manifest.rkm").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = new File(root+"output/test1.out.xlf");
		assertTrue(file.exists());
	}
	
	@Test
	public void testPOMerging ()
		throws URISyntaxException
	{
		deleteOutputDir("poPack/done", true);
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(Util.deleteLastChar(root),Util.deleteLastChar(root)); // Don't include final separator
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		pdriver.addStep(new MergingStep());
		
		URI inputURI = new File(root+"poPack/manifest.rkm").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = new File(root+"poPack/done/test01.out.properties");
		assertTrue(file.exists());
		file = new File(root+"poPack/done/sub Dir/test01.out.odt");
		assertTrue(file.exists());
		
	}
	
	@Test
	public void testOmegaTFMerging ()
		throws URISyntaxException
	{
		deleteOutputDir("omegatPack/done", true);
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(Util.deleteLastChar(root), Util.deleteLastChar(root)); // Don't include final separator
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		pdriver.addStep(new MergingStep());
		
		URI inputURI = new File(root+"omegatPack/manifest.rkm").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = new File(root+"omegatPack/done/test01.out.properties");
		assertTrue(file.exists());
		file = new File(root+"omegatPack/done/sub Dir/test01.out.odt");
		assertTrue(file.exists());
		
	}

    public boolean deleteOutputDir (String dirname, boolean relative) {
    	File d;
    	if ( relative ) d = new File(root + File.separator + dirname);
    	else d = new File(dirname);
    	if ( d.isDirectory() ) {
    		String[] children = d.list();
    		for ( int i=0; i<children.length; i++ ) {
    			boolean success = deleteOutputDir(d.getAbsolutePath() + File.separator + children[i], false);
    			if ( !success ) {
    				return false;
    			}
    		}
    	}
    	if ( d.exists() ) return d.delete();
    	else return true;
    }
    

}
