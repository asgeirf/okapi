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
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.lib.segmentation.SRXDocument;

/**
 * Align sentences between source and target paragraphs (TextUnits) and produce a TMX file with aligned sentences. This
 * {@link IPipelineStep} (via configuration) can also output aligned (multilingual {@link TextUnit}s)
 * 
 * @author HARGRAVEJE
 * 
 */
@UsingParameters(Parameters.class)
public class SentenceAlignerStep extends BasePipelineStep implements IObserver {
	private static final Logger LOGGER = Logger.getLogger(SentenceAlignerStep.class.getName());

	private Parameters params;
	private IFilter filter = null;
	private XMLWriter writer;
	private TMXWriter tmx;
	private IFilterConfigurationMapper fcMapper;
	private LocaleId targetLocale;
	private LocaleId sourceLocale;
	private RawDocument targetInput = null;
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
		boolean loadDefault = true;
		SRXDocument srxDocument = new SRXDocument();

		// Prepare source segmentation if needed
		if (params.getSegmentSource()) {
			// Load default or custom rules
			if (params.getUseCustomSourceRules()) {
				try {
					srxDocument.loadRules(params.getCustomSourceRulesPath());
					loadDefault = false;
				} catch (Exception e) {
					LOGGER.warning(String
							.format("Custom source segmentation rules file '%s' cannot be read.\nUsing the default rules instead.",
									params.getCustomSourceRulesPath()));
				}
			}
			if (loadDefault) {
				InputStream is = SentenceAlignerStep.class.getResourceAsStream("default.srx");
				srxDocument.loadRules(is);
			}
			// TODO: decide how we deal with leading/trailing spaces
			// srxDocument.setTrimLeadingWhitespaces(false);
			sourceSegmenter = srxDocument.compileLanguageRules(sourceLocale, null);
		}

		// Prepare target segmentation if needed
		if (params.getSegmentTarget()) {
			loadDefault = true;
			// Load default or custom rules
			if (params.getUseCustomTargetRules()) {
				try {
					srxDocument.loadRules(params.getCustomTargetRulesPath());
					loadDefault = false;
				} catch (Exception e) {
					LOGGER.warning(String
							.format("Custom target segmentation rules file '%s' cannot be read.\nUsing the default rules instead.",
									params.getCustomTargetRulesPath()));
				}
			}
			if (loadDefault) {
				InputStream is = SentenceAlignerStep.class.getResourceAsStream("default.srx");
				srxDocument.loadRules(is);
			}
			// TODO: decide how we deal with leading/trailing spaces
			// srxDocument.setTrimLeadingWhitespaces(false);
			targetSegmenter = srxDocument.compileLanguageRules(targetLocale, null);
		}

		return event;
	}

	protected Event handleEndBatch(Event event) {
		if (tmx != null) {
			tmx.writeEndDocument();
			tmx.close();
			tmx = null;
		}
		return event;
	}

	@Override
	protected Event handleStartDocument(Event event) {
		if (targetInput != null) {
			initializeFilter();
		}
		
		// Start TMX writer (one for all input documents)
		if (tmx == null && params.getGenerateTMX()) {
			String mimeType = event.getStartDocument().getMimeType();
			tmx = new TMXWriter(params.getTmxOutputPath());
			tmx.writeStartDocument(sourceLocale, targetLocale, getClass().getName(), null,
					"sentence", null, mimeType);
		}
		
		return event;
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
		TextUnit sourceTu = sourceEvent.getTextUnit();
		TextUnit targetTu = null;

		// Skip non-translatable and empty
		if ( !sourceTu.isTranslatable() || sourceTu.isEmpty() ) {
			return sourceEvent;
		}

		// Segment the source if requested
		if (params.getSegmentSource()) {
			sourceTu.createSourceSegmentation(sourceSegmenter);
		}

		// Move to the next target TU
		if (targetInput != null) {
			Event targetEvent = synchronize(EventType.TEXT_UNIT);
			targetTu = targetEvent.getTextUnit();
		}

		// Segment the target if requested
		if (params.getSegmentTarget()) {
			if (targetTu == null) {
				// TextUnit is bilingual
				sourceTu.createTargetSegmentation(targetSegmenter, targetLocale);
			} else {
				// separate target TextUnit
				targetTu.createSourceSegmentation(targetSegmenter);
			}			
		}

		TextUnit alignedTextUnit;
		if (targetInput == null) {
			// case where the TextUnit is already bilingual
			alignedTextUnit = sentenceAligner.align(sourceTu, sourceLocale, targetLocale);
		} else {
			// case where we have separate source and target TextUnits
			alignedTextUnit = sentenceAligner.align(sourceTu, targetTu, sourceLocale, targetLocale);
		}
		
		// remove leading and trailing whitespace in the aligned TextUnit 
		// for both source and target
		for (TextPart p : alignedTextUnit.getSource().getSegments()) {
			p.text.ltrim();
			p.text.rtrim();
		}
		for (TextPart p : alignedTextUnit.getTarget(targetLocale).getSegments()) {
			p.text.ltrim();
			p.text.rtrim();
		}
		
		// Send the aligned TU to the TMX file
		if (params.getGenerateTMX()) {
			tmx.writeTUFull(alignedTextUnit);
		} else { // Otherwise send each aligned TextUnit downstream
			return new Event(EventType.TEXT_UNIT, alignedTextUnit);
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
			if (params.getGenerateTMX() && (tmx != null)) {
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