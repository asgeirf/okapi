/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.gcaligner;

import java.io.InputStream;
import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.XMLWriter;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.TMXWriter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.observer.IObservable;
import net.sf.okapi.common.observer.IObserver;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.lib.segmentation.SRXDocument;

/**
 * Align sentences between two source and target paragraphs (TextUnits) and produce a TMX file with aligned sentences.
 * This {@link IPipelineStep} (via configuration) can also output aligned (multilingual {@link TextUnit}s)
 * 
 * @author HARGRAVEJE
 * 
 */
@UsingParameters(Parameters.class)
public class SentenceAlignerStep extends BasePipelineStep implements IObserver {
	private static final Logger LOGGER = Logger.getLogger(SentenceAlignerStep.class.getName());

	private Parameters params;
	private IFilter filter;
	private XMLWriter writer;
	private TMXWriter tmx;
	private IFilterConfigurationMapper fcMapper;
	private LocaleId targetLocale;
	private LocaleId sourceLocale;
	private RawDocument targetInput;
	private SentenceAligner sentenceAligner;
	private ISegmenter sourceSegmenter;
	private ISegmenter targetSegmenter;

	public SentenceAlignerStep() {
		params = new Parameters();
		sentenceAligner = new SentenceAligner();
	}

	@StepParameterMapping(parameterType = StepParameterType.FILTER_CONFIGURATION_MAPPER)
	public void setFilterConfigurationMapper(IFilterConfigurationMapper fcMapper) {
		this.fcMapper = fcMapper;
	}

	@StepParameterMapping(parameterType = StepParameterType.SOURCE_LOCALE)
	public void setSourceLocale(LocaleId sourceLocale) {
		this.sourceLocale = sourceLocale;
	}

	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale(LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	@StepParameterMapping(parameterType = StepParameterType.SECOND_INPUT_RAWDOC)
	public void setSecondInput(RawDocument secondInput) {
		this.targetInput = secondInput;
	}

	@Override
	public String getName() {
		return "Sentence Alignment";
	}

	@Override
	public String getDescription() {
		return "Aligns sentences within text units (paragraphs). Produces sentence alignments as bilingual text units or a TMX file.";
	}

	@Override
	public IParameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters) params;
	}

	@Override
	protected Event handleStartBatch(Event event) {
		SRXDocument srxDocument = new SRXDocument();
		srxDocument.setTrimLeadingWhitespaces(false);
		InputStream is = SentenceAlignerStep.class.getResourceAsStream("default.srx");
		srxDocument.loadRules(is);
		srxDocument.setTrimLeadingWhitespaces(false);
		if (!params.isSourceAlreadySegmented()) {
			sourceSegmenter = srxDocument.compileLanguageRules(sourceLocale, null);
		}
		targetSegmenter = srxDocument.compileLanguageRules(targetLocale, null);

		// Start TMX writer (one for all input documents)
		if (params.isGenerateTMX()) {
			tmx = new TMXWriter(params.getTmxPath());
			// TODO: how to get filter mime type here???
			tmx.writeStartDocument(sourceLocale, targetLocale, getClass().getName(), null,
					"sentence", null, "text/plain");
		}

		return event;
	}

	protected Event handleEndBatch(Event event) {
		if (params.isGenerateTMX() && (tmx != null)) {
			tmx.writeEndDocument();
			tmx.close();
			tmx = null;
		}

		return event;
	}

	@Override
	protected Event handleStartDocument(Event event1) {
		initializeFilter();
		return event1;
	}

	@Override
	protected Event handleEndDocument(Event event) {
		if (filter != null) {
			filter.close();
		}
		return event;
	}

	@Override
	protected Event handleTextUnit(Event sourceEvent) {
		TextUnit sourceTu = (TextUnit) sourceEvent.getResource();
		if (!params.isSourceAlreadySegmented()) {
			sourceTu.createSourceSegmentation(sourceSegmenter);
		}

		// Move to the next target TU
		Event targetEvent = synchronize(EventType.TEXT_UNIT);

		// Skip non-translatable
		if (!sourceTu.isTranslatable())
			return sourceEvent;

		TextUnit targetTu = (TextUnit) targetEvent.getResource();
		targetTu.createSourceSegmentation(targetSegmenter);

		if (!sourceTu.getSource().hasBeenSegmented() || !targetTu.getSource().hasBeenSegmented()) {
			// we must have hit some empty content that did not segment
			LOGGER.warning("Found unsegmented TextUnit. Possibly a TextUnit with empty content.");
			return sourceEvent;
		}

		TextUnit alignedTextUnit = sentenceAligner.align(sourceTu, targetTu, sourceLocale,
				targetLocale);

		// send the aligned TU to the TMX file
		if (params.isGenerateTMX()) {
			tmx.writeTUFull(alignedTextUnit);
		} else { // otherwise send each aligned TextUnit downstream as a multi event
			Event e = new Event(EventType.TEXT_UNIT, alignedTextUnit);
			return e;
		}

		return sourceEvent;
	}

	private void initializeFilter() {
		// Initialize the filter to read the translation to compare
		filter = fcMapper.createFilter(targetInput.getFilterConfigId(), null);

		// Open the second input for this batch item
		filter.open(targetInput);

		if (writer != null) {
			writer.close();
		}
	}

	private Event synchronize(EventType untilType) {
		boolean found = false;
		Event event = null;
		while (!found && filter.hasNext()) {
			event = filter.next();
			found = (event.getEventType() == untilType);
		}
		if (!found) {
			if (params.isGenerateTMX() && (tmx != null)) {
				tmx.writeEndDocument();
				tmx.close();
				tmx = null;
			}
			throw new RuntimeException(
					"Different number of source or target TextUnits. The source and target documents are not paragraph aligned.");
		}
		return event;
	}

	@Override
	public void update(IObservable o, Object event) {
	}
}
