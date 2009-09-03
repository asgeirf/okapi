/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.okapi.tm.pensieve.tmx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.tmx.TmxFilter;

public class TMXHandler {

    public static List<TranslationUnit> importTMX(String filename, String srcLang, String trgLang) {

        List<TranslationUnit> tus = new ArrayList<TranslationUnit>();

        List<TextUnit> textunits = getTextUnit(getEvents(filename, srcLang, trgLang));

        for (TextUnit textunit : textunits) {
            TranslationUnit tu = new TranslationUnit();
            tu.setSource(textunit.getSourceContent());
            tu.setTarget(textunit.getTargetContent(trgLang));
            tus.add(tu);
        }

        return tus;
    }

    private static List<Event> getEvents(String filename, String srcLang, String trgLang) {
        URI fileURI;
        try {
            fileURI = TMXHandler.class.getResource(filename).toURI();
        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        }

        IFilter filter = new TmxFilter();
        ArrayList<Event> list = new ArrayList<Event>();
        filter.open(new RawDocument(fileURI, null, srcLang, trgLang));
        while (filter.hasNext()) {
            Event event = filter.next();
            list.add(event);
        }
        filter.close();
        return list;
    }

    private static List<TextUnit> getTextUnit(List<Event> list) {
        List<TextUnit> tus = new ArrayList<TextUnit>();
        for (Event event : list) {
            if (event.getEventType() == EventType.TEXT_UNIT) {
                tus.add((TextUnit) event.getResource());
            }
        }
        return tus;
    }
}
