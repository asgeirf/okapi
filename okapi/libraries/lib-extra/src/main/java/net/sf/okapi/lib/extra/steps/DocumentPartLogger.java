package net.sf.okapi.lib.extra.steps;

import java.util.logging.Logger;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.StartDocument;

public class DocumentPartLogger extends BasePipelineStep {

	private final Logger logger = Logger.getLogger(getClass().getName());
	private StringBuilder sb;
	private LocaleId srcLoc;
	
	@Override
	public String getName() {
		return "Document Part Logger";
	}

	@Override
	public String getDescription() {
		return "Logs Document Part resources going through the pipeline.";
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
	protected Event handleDocumentPart(Event event) {
		DocumentPart dp = event.getDocumentPart();
		fillSB(sb, dp, srcLoc);
		return super.handleDocumentPart(event);
	}
	
	@Override
	protected Event handleEndBatch(Event event) {
		logger.info(sb.toString());
		return super.handleEndBatch(event);
	}
	
	private static void fillSB(StringBuilder sb, DocumentPart dp, LocaleId srcLoc) {
		sb.append("dp [" + dp.getId() + "]");		
		sb.append(":");
		if (dp.isReferent()) sb.append(" referent");
		sb.append("\n");
		
		if (dp.getAnnotations() != null) {
//			sb.append("             ");
//			sb.append("Source annotations:");
//			sb.append("\n");
			for (IAnnotation annot : dp.getAnnotations()) {
				sb.append("                    ");
				sb.append(annot.getClass().getName());
				sb.append(" ");
				sb.append(annot.toString());
				sb.append("\n");
			}		
		}
		
		if (dp.getPropertyNames() != null && dp.getPropertyNames().size() > 0) {
			sb.append("             ");
			sb.append("Properties:");
			sb.append("\n");
			for (String name : dp.getPropertyNames()) {
				sb.append("                    ");
				sb.append(name);
				sb.append(" ");
				sb.append(dp.getProperty(name).toString());
				sb.append("\n");
			}		
		}
		
		if (dp.getSourcePropertyNames() != null && dp.getSourcePropertyNames().size() > 0) {
			sb.append("             ");
			sb.append("Source properties:");
			sb.append("\n");
			
			for (String name : dp.getSourcePropertyNames()) {
				sb.append("                    ");
				sb.append(name);
				sb.append(" ");
				sb.append(dp.getSourceProperty(name).toString());
				sb.append("\n");
			}		
		}
				
		for (LocaleId locId : dp.getTargetLocales()) {
			if (dp.getTargetPropertyNames(locId) != null && dp.getTargetPropertyNames(locId).size() > 0) {
				sb.append("             ");
				sb.append("Target properties:");
				sb.append("\n");
				
				for (String name : dp.getTargetPropertyNames(locId)) {
					sb.append("                    ");
					sb.append(name);
					sb.append(" ");
					sb.append(dp.getTargetProperty(locId, name).toString());
					sb.append("\n");
				}		
			}
		}		
		
		if (dp.getSkeleton() != null) {
			sb.append(String.format("      Skeleton: %s", dp.getSkeleton().toString()));
			sb.append("\n");
		}
	}
	
	public static String getDpInfo(DocumentPart dp, LocaleId srcLoc) {
		StringBuilder sb = new StringBuilder();
		fillSB(sb, dp, srcLoc);
		return sb.toString();
	}
}
