package net.sf.okapi.apptest.dummyutility;

import net.sf.okapi.apptest.common.INameable;
import net.sf.okapi.apptest.common.IResource;
import net.sf.okapi.apptest.filters.FilterEvent;
import net.sf.okapi.apptest.resource.Property;
import net.sf.okapi.apptest.resource.TextContainer;
import net.sf.okapi.apptest.resource.TextUnit;
import net.sf.okapi.apptest.utilities.IUtility;

public class PseudoTranslate implements IUtility {

	private String trgLang;
	
	public String getName () {
		return "PseudoTranlate";
	}
	
	public void handleEvent (FilterEvent event) {
		switch ( event.getEventType() ) {
		case TEXT_UNIT:
			processTU((TextUnit)event.getResource());
			// Fall thru
		case DOCUMENT_PART:
		case START_DOCUMENT:
		case START_SUBDOCUMENT:
		case START_GROUP:
			processProperties((INameable)event.getResource());
		}
	}

	private void processProperties (INameable resource) {
		Property prop = resource.getProperty("href");
		if ( prop != null ) {
			if ( prop.isReadOnly() ) return; // Can't modify it
			// Else: create a localized href value if there is no target property yet
			if ( !resource.hasTargetProperty(trgLang, "href") ) {
				Property trgProp = resource.createTargetProperty(
					trgLang, prop.getName(), false, IResource.COPY_ALL);
				trgProp.setValue(trgLang+"_"+prop.getValue());
			}
			return;
		}
		
		prop = resource.getTargetProperty(trgLang, "changeid");
		if ( prop != null ) {
			prop.setValue("TestUtility");
		}
	}
	
	private void processTU (TextUnit tu) {
		//if ( !tu.hasTarget(trgLang) ) {
			TextContainer tt = tu.createTarget(trgLang, false, TextUnit.COPY_ALL);
			tt.getContent().setCodedText(
				tt.getContent().getCodedText().replace("e", "\u00CA"));
		//}
	}

	public void doEpilog () {
		// Nothing to do in this utility
		System.out.println("PseudoTranlate: doEpilog() called");
	}

	public void doProlog () {
		// Nothing to do in this utility
		System.out.println("PseudoTranlate: doProlog() called");
	}

	public void setOptions (String targetLanguage) {
		trgLang = targetLanguage;
	}

}
