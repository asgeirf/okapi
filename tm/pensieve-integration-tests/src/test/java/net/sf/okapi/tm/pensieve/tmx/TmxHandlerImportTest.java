package net.sf.okapi.tm.pensieve.tmx;

import net.sf.okapi.filters.tmx.TmxFilter;
import net.sf.okapi.tm.pensieve.common.MetadataType;
import net.sf.okapi.tm.pensieve.common.TmHit;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.tm.pensieve.seeker.PensieveSeeker;
import net.sf.okapi.tm.pensieve.seeker.TmSeeker;
import net.sf.okapi.tm.pensieve.writer.PensieveWriter;
import net.sf.okapi.tm.pensieve.writer.TmWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class TmxHandlerImportTest {

//        @Test
    public void importTmx_really_big_file() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory directory = new NIOFSDirectory(new File("target/test-classes/"));
        TmWriter tmWriter = new PensieveWriter(directory);
        OkapiTmxHandler tmxHandler = new OkapiTmxHandler("EN-US", tmxFilter);
        long startTime = System.currentTimeMillis();
        tmxHandler.importTmx(this.getClass().getResource("/HalfMillionEntries.tmx").toURI(), "es", tmWriter);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("total time to import TMX: " + totalTime);

        TmSeeker seeker = new PensieveSeeker(directory);
        startTime = System.currentTimeMillis();
        List<TmHit> tus = seeker.searchExact("All Rights Reserved.", 10);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchExact("Notice to U.S. Government End Users.", 10);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);

        startTime = System.currentTimeMillis();
        tus = seeker.searchExact("Portions copyright 1984-1998 FairCom Corporation.", 10);

        totalTime = System.currentTimeMillis() - startTime;
        System.out.println("query time " + totalTime);
        System.out.println("number found " + tus.size());
        assertTrue("Didn't find something", tus.size() > 0);
    }

    //    @Test
    public void importTmx_paragraph_tmx_basics() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
//        Directory ramDir = new NIOFSDirectory(new File("target/test-classes/"));
        TmWriter tmWriter = new PensieveWriter(ramDir);
        OkapiTmxHandler tmxHandler = new OkapiTmxHandler("en-us", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/Paragraph_TM.tmx").toURI(), "de-de", tmWriter);

        TmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact("Touch Retry tout content enresume the procedure.\n" +
                "                ", 2).get(0).getTu();
        assertEquals("tu target content", "Zur Fortsetzungut content dedes Verfahrens 'Wiederholen' drücken.\n" +
                "                ", tu.getTarget().getContent().toString());
    }

    //    @Test
    public void importTmx_sample_tmx_basics() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
//        Directory ramDir = new NIOFSDirectory(new File("target/test-classes/"));
        TmWriter tmWriter = new PensieveWriter(ramDir);
        OkapiTmxHandler tmxHandler = new OkapiTmxHandler("EN", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/sample_tmx.xml").toURI(), "IT", tmWriter);

        TmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact("hello", 2).get(0).getTu();
        assertEquals("tu target content", "ciao", tu.getTarget().getContent().toString());
        assertEquals("tu source content", "hello", tu.getSource().getContent().toString());
        tu = seeker.searchExact("world", 2).get(0).getTu();
        assertEquals("tu target content", "mondo", tu.getTarget().getContent().toString());
        assertEquals("tu source content", "world", tu.getSource().getContent().toString());
    }

//    @Test
    public void importTmx_sample_metadata() throws Exception {
        TmxFilter tmxFilter = new TmxFilter();
        Directory ramDir = new RAMDirectory();
//        Directory ramDir = new NIOFSDirectory(new File("target/test-classes/"));
        TmWriter tmWriter = new PensieveWriter(ramDir);
        OkapiTmxHandler tmxHandler = new OkapiTmxHandler("EN", tmxFilter);
        tmxHandler.importTmx(this.getClass().getResource("/sample_tmx.xml").toURI(), "IT", tmWriter);

        TmSeeker seeker = new PensieveSeeker(ramDir);
        TranslationUnit tu = seeker.searchExact("hello", 2).get(0).getTu();
        assertEquals("# of metadata (not ignored)", 4, tu.getMetadata().size());
        assertEquals("tu id", "hello123", tu.getMetadata().get(MetadataType.ID));
        assertEquals("tu FileName", "GeorgeInTheJungle.hdf", tu.getMetadata().get(MetadataType.FILE_NAME));
        assertEquals("tu GroupName", "ImAGroupie", tu.getMetadata().get(MetadataType.GROUP_NAME));
        assertEquals("tu Type", "plaintext", tu.getMetadata().get(MetadataType.TYPE));
    }

    @Test
    public void hookup(){
        assertTrue("not hooked up", true);
    }
}
