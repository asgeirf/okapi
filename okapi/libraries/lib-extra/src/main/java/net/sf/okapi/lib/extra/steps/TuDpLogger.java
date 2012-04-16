package net.sf.okapi.lib.extra.steps;

import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;

public class TuDpLogger extends BasePipelineStep {

	private final Logger logger = Logger.getLogger(getClass().getName());
	private StringBuilder sb;
	private LocaleId srcLoc;
	
	@Override
	public String getName() {
		return "Text Unit and Document Part Logger";
	}

	@Override
	public String getDescription() {
		return "Logs Text Unit and Document Part resources going through the pipeline.";
	}
	
	@Override
	protected Event handleStartBatch(Event event) {
		sb = new StringBuilder("\n\n");
		return super.handleStartBatch(event);
	}

	@Override
	protected Event handleStartDocument(Event event) {
		StartDocument sd = (StartDocument) event.getResource();
		srcLoc = sd.getLocale();
		return super.handleStartDocument(event);
	}
	
	@Override
	protected Event handleTextUnit(Event event) {
		ITextUnit tu = event.getTextUnit();
		sb.append(TextUnitLogger.getTuInfo(tu, srcLoc));
		return super.handleTextUnit(event);
	}
	
	@Override
	protected Event handleDocumentPart(Event event) {
		DocumentPart dp = event.getDocumentPart();
		sb.append(DocumentPartLogger.getDpInfo(dp, srcLoc));
		return super.handleDocumentPart(event);
	}
	
	@Override
	protected Event handleEndBatch(Event event) {
		logger.info(sb.toString());
		return super.handleEndBatch(event);
	}
}
