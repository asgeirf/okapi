package net.sf.okapi.steps.copysourcetotarget;

import java.util.Set;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;

@UsingParameters(Parameters.class)
public class CopySourceToTargetStep extends BasePipelineStep {
	private Parameters params;

	public CopySourceToTargetStep() {
		params = new Parameters();
	}

	public String getDescription() {
		return "Copy the source segments to the specified target. Create the target if needed.";
	}

	public String getName() {
		return "Copy Source To Target";
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters) params;
	}

	@Override
	public Parameters getParameters() {
		return params;
	}

	@Override
	public Event handleStartDocument(Event event) {
		StartDocument sd = (StartDocument) event.getResource();
		Set<LocaleId> tls = sd.getTargetLocales();
		// if there is only one target language then use it as the default
		// target to copy source to
		if (tls.size() == 1) {
			getParameters().targetLocale = tls.iterator().next();
		}
		
		return event;
	}

	@Override
	public Event handleTextUnit(Event event) {
		TextUnit tu = (TextUnit) event.getResource();
		tu.createTarget(getParameters().targetLocale, false, IResource.COPY_ALL);
		
		return event;
	}
}
