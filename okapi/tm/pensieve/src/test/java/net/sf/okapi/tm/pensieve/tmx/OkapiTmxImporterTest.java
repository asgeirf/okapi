/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.tm.pensieve.tmx;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.tm.pensieve.common.MetadataType;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.tm.pensieve.writer.ITmWriter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OkapiTmxImporterTest {

    URI sampleTMX;
    OkapiTmxImporter tmxImporter;
    ITmWriter mockTmWriter;
    IFilter mockFilter;
    LocaleId locEN = LocaleId.fromString("EN");
    LocaleId locFR = LocaleId.fromString("FR");
    LocaleId locIT = LocaleId.fromString("IT");

    @Before
    public void setUp() throws URISyntaxException, IOException {
        mockTmWriter = mock(ITmWriter.class);

        String[][] properties = {{"tuid", "helloid"},
            {"datatype", "plaintext"},
            {"Txt::FileName", "StringInfoForTest3.info"},
            {"Txt::GroupName", "APCCalibrateTimeoutAction0"}
        };
        mockFilter = mock(IFilter.class);
        when(mockFilter.hasNext())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(mockFilter.next())
                .thenReturn(createEvent("1", "hello", "ciao", locIT, properties))
                .thenReturn(createEvent("2", "world", "mondo", locIT, null))
                .thenReturn(new Event(EventType.DOCUMENT_PART, new TextUnit("holy cow")));

        sampleTMX = new URI("test.tmx");
        tmxImporter = new OkapiTmxImporter(locEN, mockFilter);
    }


    @Test
    public void importTMXMetadataWithData() throws IOException {
        tmxImporter.importTmx(sampleTMX, locIT, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("ID", "helloid", tuCapture.getAllValues().get(0).getMetadata().get(MetadataType.ID));
        assertEquals("TYPE", "plaintext", tuCapture.getAllValues().get(0).getMetadata().get(MetadataType.TYPE));
        assertEquals("FILE_NAME", "StringInfoForTest3.info", tuCapture.getAllValues().get(0).getMetadata().get(MetadataType.FILE_NAME));
        assertEquals("GROUP_NAME", "APCCalibrateTimeoutAction0", tuCapture.getAllValues().get(0).getMetadata().get(MetadataType.GROUP_NAME));
        assertEquals("# of metadata", 0, tuCapture.getAllValues().get(1).getMetadata().size());
    }

    @Test
    public void importTMXMetadataWithoutData() throws IOException {
        tmxImporter.importTmx(sampleTMX, locIT, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("# of metadata", 0, tuCapture.getAllValues().get(1).getMetadata().size());
    }

    @Test
    public void importTmxNullFile() throws IOException {
        String errMsg = null;
        try {
            tmxImporter.importTmx(null, locFR, mockTmWriter);
        } catch (IllegalArgumentException iae) {
            errMsg = iae.getMessage();
        }
        assertEquals("Error message", "'tmxUri' was not set", errMsg);
    }

    @Test
    public void importTmxNullTarget() throws IOException {
        String errMsg = null;
        try {
            tmxImporter.importTmx(sampleTMX, null, mockTmWriter);
        } catch (IllegalArgumentException iae) {
            errMsg = iae.getMessage();
        }
        assertEquals("Error message", "'targetLang' was not set", errMsg);
    }

    @Test
    public void importTmxNullTmWriter() throws IOException {
        String errMsg = null;
        try {
            tmxImporter.importTmx(sampleTMX, locFR, null);
        } catch (IllegalArgumentException iae) {
            errMsg = iae.getMessage();
        }
        assertEquals("Error message", "'tmWriter' was not set", errMsg);
    }

    @Test
    public void constructorEmptySourceLang() {
        String errMsg = null;
        try {
            new OkapiTmxImporter(LocaleId.EMPTY, mockFilter);
        } catch (IllegalArgumentException iae) {
            errMsg = iae.getMessage();
        }
        assertEquals("Error message", "'sourceLang' must be set", errMsg);
    }

    @Test
    public void constructorEmptyFilter() {
        String errMsg = null;
        try {
            new OkapiTmxImporter(locEN, null);
        } catch (IllegalArgumentException iae) {
            errMsg = iae.getMessage();
        }
        assertEquals("Error message", "'filter' must be set", errMsg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importTMXEmptyTargetLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, LocaleId.EMPTY, mockTmWriter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importTMXNullTargetLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, null, mockTmWriter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importTMXNullTMWriter() throws IOException {
        tmxImporter.importTmx(sampleTMX, locFR, null);
    }

    @Test
    public void importTmxExistingLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, locIT, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("number of TUs", 2, tuCapture.getAllValues().size());
    }

    @Test
    public void importTmxNonExistingLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, locFR, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("number of TUs", 2, tuCapture.getAllValues().size());
        assertNull("targets content should be null", tuCapture.getAllValues().get(0).getTarget().getContent());
        assertEquals("target lang", locFR, tuCapture.getAllValues().get(0).getTarget().getLanguage());
    }

    @Test
    public void sourceAndTargetForExistingLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, locIT, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("first match source", "hello", tuCapture.getAllValues().get(0).getSource().getContent().toText());
        assertEquals("first match target", "ciao", tuCapture.getAllValues().get(0).getTarget().getContent().toText());
    }

    @Test
    public void sourceAndTargetForNonExistingLang() throws IOException {
        tmxImporter.importTmx(sampleTMX, locFR, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("first match source", "hello",
                tuCapture.getAllValues().get(0).getSource().getContent().toText());
        assertNull("target for non-existant language should be null",
                tuCapture.getAllValues().get(0).getTarget().getContent());
    }

    //An example of a Stub. I will likely change this to a Mock later
    @Test
    public void importTMXDocCount() throws IOException {
        tmxImporter.importTmx(sampleTMX, locEN, mockTmWriter);
        ArgumentCaptor<TranslationUnit> tuCapture = verifyIndexTU();
        assertEquals("entries indexed", 2, tuCapture.getAllValues().size());
    }

    private ArgumentCaptor<TranslationUnit> verifyIndexTU() throws IOException {
        ArgumentCaptor<TranslationUnit> tuCapture = ArgumentCaptor.forClass(TranslationUnit.class);
        verify(mockTmWriter, times(2)).indexTranslationUnit(tuCapture.capture());
        return tuCapture;
    }

    private Event createEvent(String id, String source, String target, LocaleId targetLang, String[][] properties) {
        ITextUnit tu = new TextUnit(id, source);
        tu.setTargetContent(targetLang, new TextFragment(target));
        //populate properties
        if (properties != null) {
            for (String[] prop : properties) {
                tu.setProperty(new Property(prop[0], prop[1]));
            }
        }
        return new Event(EventType.TEXT_UNIT, tu);
    }


}
