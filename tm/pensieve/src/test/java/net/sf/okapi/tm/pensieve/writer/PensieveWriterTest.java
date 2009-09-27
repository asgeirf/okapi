/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.tm.pensieve.writer;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.tm.pensieve.Helper;
import net.sf.okapi.tm.pensieve.common.*;
import static net.sf.okapi.tm.pensieve.common.TranslationUnitField.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.RAMDirectory;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * User: Christian Hargraves
 * Date: Aug 11, 2009
 * Time: 6:35:45 AM
 */
public class PensieveWriterTest {

    PensieveWriter tmWriter;
    IndexWriter writer;
    static final File GOOD_DIR = new File("../data/");
    static final File GOOD_FILE = new File(GOOD_DIR, "apache1.0.txt");
    RAMDirectory dir;

    @Before
    public void init() throws IOException {
        dir = new RAMDirectory();
        tmWriter = new PensieveWriter(dir, true);
        writer = tmWriter.getIndexWriter();
    }

    @Test
    public void constructorCreateNew() throws IOException {
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joe", "Jo","1"));
        tmWriter.endIndex();
        tmWriter = new PensieveWriter(dir, true);
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joseph", "Yosep","2"));
        tmWriter.endIndex();
        assertEquals("# of docs in tm", 1, tmWriter.getIndexWriter().numDocs());
    }

    @Test
    public void constructorCreateNew2 () throws IOException {
        tmWriter.indexTranslationUnit2(Helper.createTU("EN", "KR", "Joe", "Jo","1"));
        tmWriter.endIndex();
        tmWriter = new PensieveWriter(dir, true);
        tmWriter.indexTranslationUnit2(Helper.createTU("EN", "KR", "Joseph", "Yosep","2"));
        tmWriter.endIndex();
        assertEquals("# of docs in tm", 1, tmWriter.getIndexWriter().numDocs());
    }

    @Test
    public void constructorAppend() throws IOException {
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joe", "Jo","1"));
        tmWriter.endIndex();
        tmWriter = new PensieveWriter(dir, false);
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joseph", "Yosep","2"));
        tmWriter.endIndex();
        assertEquals("# of docs in tm", 2, tmWriter.getIndexWriter().numDocs());
    }

    @Test
    public void getIndexWriterSameDirectory(){
        assertSame("ram directory", dir, tmWriter.getIndexWriter().getDirectory());
    }

    @Test
    public void indexTranslationUnitMetaData() throws IOException, ParseException {
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joe", "Jo","1"));
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Jane", "Jaen","2"));
        writer.commit();

        assertEquals("# of docs found for id=1", 1, getNumOfHitsFor(MetadataType.ID.fieldName(), "1"));
        assertEquals("# of docs found for id=2", 1, getNumOfHitsFor(MetadataType.ID.fieldName(), "2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNullTu() throws IOException, ParseException {
        tmWriter.update(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateEmptyId() throws IOException, ParseException {
        tmWriter.update(new TranslationUnit());
    }

    @Test
    public void update() throws IOException, ParseException {
        TranslationUnit tu1 = Helper.createTU("EN", "KR", "Joe", "Jo","1");
        TranslationUnit tu2 = Helper.createTU("EN", "KR", "Jane", "Jaen","2");
        tmWriter.indexTranslationUnit(tu1);
        tmWriter.indexTranslationUnit(tu2);
        writer.commit();

        tu1.getTarget().setContent(new TextFragment("Ju"));
        tmWriter.update(tu1);
        writer.commit();
        Document doc1 = findDocument(MetadataType.ID.fieldName(), "1");
        Document doc2 = findDocument(MetadataType.ID.fieldName(), "2");
        assertEquals("source text", tu1.getSource().getContent().toString(), doc1.getField(TranslationUnitField.SOURCE.name()).stringValue());
        assertEquals("target text", tu1.getTarget().getContent().toString(), doc1.getField(TranslationUnitField.TARGET.name()).stringValue());
        assertEquals("target text", tu2.getTarget().getContent().toString(), doc2.getField(TranslationUnitField.TARGET.name()).stringValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNullId() throws IOException, ParseException {
        tmWriter.delete(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteEmptyId() throws IOException, ParseException {
        tmWriter.delete("");
    }

    @Test
    public void deleteWithId() throws IOException, ParseException {
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Joe", "Jo","1"));
        tmWriter.indexTranslationUnit(Helper.createTU("EN", "KR", "Jane", "Jaen","2"));
        writer.commit();

        tmWriter.delete("1");
        writer.commit();
        assertEquals("# of docs found for id=1", 0, getNumOfHitsFor(MetadataType.ID.fieldName(), "1"));
        assertEquals("# of docs found for id=2", 1, getNumOfHitsFor(MetadataType.ID.fieldName(), "2"));
    }

    @Test
    public void addMetadataToDocument(){
        Metadata md = new Metadata();
        md.put(MetadataType.FILE_NAME, "some/file");
        md.put(MetadataType.GROUP_NAME, "some group");
        md.put(MetadataType.ID, "someId");
        md.put(MetadataType.TYPE, "someType");
        Document doc = new Document();
        tmWriter.addMetadataToDocument(doc, md);
        assertEquals("Document's file name field", "some/file", getFieldValue(doc, MetadataType.FILE_NAME.fieldName()));
        assertEquals("Document's group name field", "some group", getFieldValue(doc, MetadataType.GROUP_NAME.fieldName()));
        assertEquals("Document's id field", "someId", getFieldValue(doc, MetadataType.ID.fieldName()));
        assertEquals("Document's type field", "someType", getFieldValue(doc, MetadataType.TYPE.fieldName()));
    }

    @Test
    public void constructorCreatesWriter(){
        assertNotNull("the tmWriter tmWriter was not created as expected", tmWriter);
    }

    @Test
    public void constructorUsesExpectedDirectory(){
        assertTrue("The index directory should end with 'target/test-classes'", writer.getDirectory() instanceof RAMDirectory);
    }

//    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
//TODO: Fix me please
    //    @Test(expected = OkapiIOException.class)
    public void endIndexHandlesIOException() throws Exception {
//        IndexWriter spyWriter = spy(writer);
//        doThrow(new IOException("some text")).when(spyWriter).commit();
//        Helper.setPrivateMember(tmWriter, "indexWriter", spyWriter);
//        tmWriter.endIndex();
    }

    @Test(expected = AlreadyClosedException.class)
    public void endIndexClosesWriter() throws IOException {
        tmWriter.endIndex();
        tmWriter.getIndexWriter().commit();
    }

    @Test
    public void endIndexThrowsNoException() throws IOException {
        tmWriter.endIndex();
        tmWriter.endIndex();
    }

    public void endIndexCommits() throws IOException {
        tmWriter.indexTranslationUnit(new TranslationUnit(new TranslationUnitVariant("EN",
                new TextFragment("dax")), new TranslationUnitVariant("ES", new TextFragment("is funny (sometimes)"))));
        tmWriter.endIndex();
        IndexReader reader = IndexReader.open(dir, true);
        assertEquals("num of docs indexed after endIndex", 1, reader.maxDoc());
    }

    @Test(expected = NullPointerException.class)
    public void getDocumentNoSourceContent(){
        tmWriter.getDocument(new TranslationUnit(null, new TranslationUnitVariant("EN",
                new TextFragment("some target"))));
    }

    @Test(expected = NullPointerException.class)
    public void getDocumentEmptySourceContent(){
        tmWriter.getDocument(new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment("")),
                new TranslationUnitVariant("EN", new TextFragment("some target"))));
    }

    @Test(expected = NullPointerException.class)
    public void getDocumentNullTU(){
        tmWriter.getDocument(null);
    }

    @Test
    public void getDocumentValues(){
        String text = "blah blah blah";
        TranslationUnit tu = new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment(text)),
                new TranslationUnitVariant("FR", new TextFragment("someone")));
        Metadata md = tu.getMetadata();
        md.put(MetadataType.ID, "someId");
        Document doc = tmWriter.getDocument(tu);
        assertEquals("Document's content field", "blah blah blah", getFieldValue(doc, SOURCE.name()));
        assertEquals("Document's content exact field", "blah blah blah", getFieldValue(doc, SOURCE_EXACT.name()));
        assertEquals("Document's target field", "someone", getFieldValue(doc, TARGET.name()));
        assertEquals("Document's source lang field", "EN", getFieldValue(doc, SOURCE_LANG.name()));
        assertEquals("Document's target lang field", "FR", getFieldValue(doc, TARGET_LANG.name()));
        assertEquals("Document's id field", "someId", getFieldValue(doc, MetadataType.ID.fieldName()));
    }

    @Test
    public void testCreateDocument (){
    	TextFragment srcFrag = new TextFragment("blah ");
    	srcFrag.append(TagType.OPENING, "b", "<b>");
    	srcFrag.append("bold");
    	srcFrag.append(TagType.CLOSING, "b", "</b>");
    	String srcCT = srcFrag.getCodedText();
    	String srcCodes = Code.codesToString(srcFrag.getCodes());
    	TextFragment trgFrag = new TextFragment("blah ");
    	trgFrag.append(TagType.OPENING, "i", "<i>");
    	trgFrag.append("gras");
    	trgFrag.append(TagType.CLOSING, "i", "</i>");
    	String trgCT = trgFrag.getCodedText();
    	String trgCodes = Code.codesToString(trgFrag.getCodes());

    	TranslationUnit tu = new TranslationUnit(new TranslationUnitVariant("EN", srcFrag),
   			new TranslationUnitVariant("FR", trgFrag));
    	Metadata md = tu.getMetadata();
    	md.put(MetadataType.ID, "someId");
    	Document doc = tmWriter.createDocument(tu);
    	assertEquals("Document's content field", srcCT, getFieldValue(doc, SOURCE.name()));
    	assertEquals("Document's content exact field", srcCT, getFieldValue(doc, SOURCE_EXACT.name()));
    	assertEquals("Document's target field", trgCT, getFieldValue(doc, TARGET.name()));
    	assertEquals("Document's source lang field", "EN", getFieldValue(doc, SOURCE_LANG.name()));
    	assertEquals("Document's target lang field", "FR", getFieldValue(doc, TARGET_LANG.name()));
    	assertEquals("Document's id field", "someId", getFieldValue(doc, MetadataType.ID.fieldName()));
    	assertEquals("Document's source codes", srcCodes, getFieldValue(doc, SOURCE_CODES.name()));
    	assertEquals("Document's target codes", trgCodes, getFieldValue(doc, TARGET_CODES.name()));
    }

    @Test
    public void getDocumentNoTarget(){
        Document doc = tmWriter.getDocument(new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment("blah blah blah")), null));
        assertNull("Document's target field should be null", doc.getField(TARGET.name()));
    }

    @Test(expected = NullPointerException.class)
    public void indexTranslationUnitNull() throws IOException {
        tmWriter.indexTranslationUnit(null);
    }

    @Test(expected = NullPointerException.class)
    public void indexTranslationUnitNull2() throws IOException {
        tmWriter.indexTranslationUnit2(null);
    }

    @Test
    public void indexTranslationUnitNoIndexedDocsBeforeCall() throws IOException {
        assertEquals("num of docs indexed", 0, tmWriter.getIndexWriter().numDocs());
    }

    @Test
    public void indexTranslationUnitBeforeCommit() throws IOException {
        tmWriter.indexTranslationUnit(new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment("dax")), new TranslationUnitVariant("EN", new TextFragment("is funny (sometimes)"))));
        IndexReader reader = IndexReader.open(dir, true);
        assertEquals("num of docs indexed before endIndex", 0, reader.maxDoc());
    }

    @Test
    public void indexTextUnit() throws IOException {
        tmWriter.indexTranslationUnit(new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment("joe")), new TranslationUnitVariant("EN", new TextFragment("schmoe"))));
        assertEquals("num of docs indexed", 1, tmWriter.getIndexWriter().numDocs());
    }

    @Test
    public void indexTextUnit2() throws IOException {
        tmWriter.indexTranslationUnit2(new TranslationUnit(new TranslationUnitVariant("EN", new TextFragment("joe")), new TranslationUnitVariant("EN", new TextFragment("schmoe"))));
        assertEquals("num of docs indexed", 1, tmWriter.getIndexWriter().numDocs());
    }

    private String getFieldValue(Document doc, String fieldName) {
        return doc.getField(fieldName).stringValue();
    }

    private int getNumOfHitsFor(String fieldName, String fieldValue) throws IOException {
        IndexSearcher is = new IndexSearcher(dir, true);
        PhraseQuery q = new PhraseQuery();
        q.add(new Term(fieldName, fieldValue));
        return is.search(q, 10).scoreDocs.length;
    }

    private Document findDocument(String fieldName, String fieldValue) throws IOException {
        IndexSearcher is = new IndexSearcher(dir, true);
        PhraseQuery q = new PhraseQuery();
        q.add(new Term(fieldName, fieldValue));
        TopDocs hits = is.search(q, 1);
        ScoreDoc scoreDoc = hits.scoreDocs[0];
        return is.doc(scoreDoc.doc);
    }

}
