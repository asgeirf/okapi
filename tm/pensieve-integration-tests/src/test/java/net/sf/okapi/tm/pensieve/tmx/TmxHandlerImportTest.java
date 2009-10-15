package net.sf.okapi.tm.pensieve.tmx;

import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.filters.tmx.TmxFilter;
import net.sf.okapi.tm.pensieve.common.MetadataType;
import net.sf.okapi.tm.pensieve.common.TmHit;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.tm.pensieve.seeker.PensieveSeeker;
import net.sf.okapi.tm.pensieve.seeker.ITmSeeker;
import net.sf.okapi.tm.pensieve.writer.PensieveWriter;
import net.sf.okapi.tm.pensieve.writer.ITmWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.List;
import net.sf.okapi.tm.pensieve.seeker.TmSeekerFactory;
import net.sf.okapi.tm.pensieve.writer.TmWriterFactory;

public class TmxHandlerImportTest {

    private final static String INDEX_DIR = "target/test-classes/";

//        @Test
    public void importTmx_exact_really_big_file() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();        
        
        ITmWriter tmWriter = TmWriterFactory.createFileBasedTmWriter(INDEX_DIR, true);
        OkapiTmxImporter tmxImporter = new OkapiTmxImporter("EN-US", tmxFilter);
        long startTime = System.currentTimeMillis();
        tmxImporter.importTmx(this.getClass().getResource("/HalfMillionEntries.tmx").toURI(), "es", tmWriter);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("total time to import TMX: " + totalTime);

        ITmSeeker seeker = TmSeekerFactory.createFileBasedTmSeeker(INDEX_DIR);
        startTime = System.currentTimeMillis();
        List<TmHit> tus = seeker.searchExact(new TextFragment("All Rights Reserved."), 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("exact query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchExact(new TextFragment("Notice to U.S. Government End Users."), 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("exact query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchExact(new TextFragment("Portions copyright 1984-1998 FairCom Corporation."), 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("exact query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchExact(new TextFragment("Second Ed. C:"), 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("exact query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);
    }

//        @Test
    public void importTmx_fuzzy_really_big_file() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        ITmWriter tmWriter = TmWriterFactory.createFileBasedTmWriter(INDEX_DIR, true);
        OkapiTmxImporter tmxHandler = new OkapiTmxImporter("EN-US", tmxFilter);
        long startTime = System.currentTimeMillis();
        tmxHandler.importTmx(this.getClass().getResource("/HalfMillionEntries.tmx").toURI(), "es", tmWriter);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("total time to import TMX: " + totalTime);

        ITmSeeker seeker = TmSeekerFactory.createFileBasedTmSeeker(INDEX_DIR);
        startTime = System.currentTimeMillis();
        List<TmHit> tus = seeker.searchFuzzy(new TextFragment("All Rights Reserved."), 80, 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("fuzzy query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchFuzzy(new TextFragment("Notice to U.S. Government End Users."), 80, 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("fuzzy query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchFuzzy(new TextFragment("Portions copyright 1984-1998 FairCom Corporation."), 80, 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("fuzzy query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchFuzzy(new TextFragment("Second Ed. C:"), 80, 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("fuzzy query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchFuzzy(new TextFragment("Notice to U.S. Government End Users."), 80, 10, null);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("fuzzy query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);
    }

        @Test
    public void importTmx_paragraph_tmx_basics() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
        ITmWriter tmWriter = new PensieveWriter(ramDir, true);
        OkapiTmxImporter tmxHandler = new OkapiTmxImporter("en-us", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/Paragraph_TM.tmx").toURI(), "de-de", tmWriter);

        ITmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact(new TextFragment("Pumps have been paused for 3 minutes. Consider starting a saline drip."), 2, null).get(0).getTu();
        assertEquals("tu target content", "Pumpen wurden 3 Minuten lang angehalten, ggf. NaCl-Infusion starten", tu.getTarget().getContent().toString());
    }

        @Test
    public void importTmx_sample_tmx_basics() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
        ITmWriter tmWriter = new PensieveWriter(ramDir, true);
        OkapiTmxImporter tmxHandler = new OkapiTmxImporter("en", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/sample_tmx.xml").toURI(), "it", tmWriter);

        ITmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact(new TextFragment("hello"), 2, null).get(0).getTu();
        assertEquals("tu target content", "ciao", tu.getTarget().getContent().toString());
        assertEquals("tu source content", "hello", tu.getSource().getContent().toString());
        tu = seeker.searchExact(new TextFragment("world"), 2, null).get(0).getTu();
        assertEquals("tu target content", "mondo", tu.getTarget().getContent().toString());
        assertEquals("tu source content", "world", tu.getSource().getContent().toString());
    }

    @Test
    public void importTmx_sample_metadata() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
        ITmWriter tmWriter = new PensieveWriter(ramDir, true);
        OkapiTmxImporter tmxHandler = new OkapiTmxImporter("en", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/sample_tmx.xml").toURI(), "it", tmWriter);

        ITmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact(new TextFragment("hello"), 2, null).get(0).getTu();
        assertEquals("# of metadata (not ignored)", 4, tu.getMetadata().size());
        assertEquals("tu id", "hello123", tu.getMetadata().get(MetadataType.ID));
        assertEquals("tu FileName", "GeorgeInTheJungle.hdf", tu.getMetadata().get(MetadataType.FILE_NAME));
        assertEquals("tu GroupName", "ImAGroupie", tu.getMetadata().get(MetadataType.GROUP_NAME));
        assertEquals("tu Type", "plaintext", tu.getMetadata().get(MetadataType.TYPE));
    }

}
