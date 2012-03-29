package net.sf.okapi.lib.tmdb.lucene;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.lib.tmdb.DbUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;

public class OIndexTest {

    static final String locEN = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
    static final String locFR = DbUtil.toOlifantLocaleCode(LocaleId.FRENCH);
    static final String locES = DbUtil.toOlifantLocaleCode(LocaleId.SPANISH);

    OWriter tmWriter;
	OSeeker tmSeeker;
	IndexWriter writer;
	Directory dir;
	
	String enText = "Text EN 1";
	String frText = "Text FR 1";
	String esText = "Text ES 1";
	
	@Before
	public void init() throws IOException {
		dir = new RAMDirectory();
		//dir = FSDirectory.open(new File("C:\\PENSIEVE")); 

		tmWriter = new OWriter(dir, true);
		tmSeeker = new OSeeker(tmWriter.getIndexWriter());
		writer = tmWriter.getIndexWriter();
	}
	
	@Test
	public void indexRecord() throws IOException {
		
	    OTranslationUnitVariant tuvEN = new OTranslationUnitVariant(locEN, new TextFragment(enText));
	    OTranslationUnitVariant tuvFR = new OTranslationUnitVariant(locFR, new TextFragment(frText));
	    OTranslationUnitVariant tuvES = new OTranslationUnitVariant(locES, new TextFragment(esText));

	    OTranslationUnitInput inputTu = new OTranslationUnitInput("1");
	    inputTu.add(tuvEN);
	    inputTu.add(tuvFR);
	    inputTu.add(tuvES);
	    
		tmWriter.index(inputTu);
		tmWriter.commit();

		assertEquals("# of docs found for segKey=1", 1,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));

		Document doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
		
		String keyExactField_EN = "EXACT_"+DbUtil.TEXT_PREFIX+locEN.toString();
		String keyExactField_FR = "EXACT_"+DbUtil.TEXT_PREFIX+locFR.toString();
		String keyExactField_ES = "EXACT_"+DbUtil.TEXT_PREFIX+locES.toString();
		
		assertEquals("en text", "Text EN 1", 
				doc1.getFieldable(keyExactField_EN).stringValue());
		assertEquals("en text", "Text FR 1", 
				doc1.getFieldable(keyExactField_FR).stringValue());
		assertEquals("en text", "Text ES 1", 
				doc1.getFieldable(keyExactField_ES).stringValue());
		
		tuvEN.getContent().append(" - updated EN");
		tuvFR.getContent().append(" - updated FR");
		tuvES.getContent().append(" - updated ES");
		
		tmWriter.update(inputTu);
		tmWriter.commit();

		assertEquals("# of docs found for segKey=1", 1,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));
		
		doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
		
		assertEquals("en text", "Text EN 1 - updated EN", 
				doc1.getFieldable(keyExactField_EN).stringValue());
		assertEquals("en text", "Text FR 1 - updated FR", 
				doc1.getFieldable(keyExactField_FR).stringValue());
		assertEquals("en text", "Text ES 1 - updated ES", 
				doc1.getFieldable(keyExactField_ES).stringValue());
		
		tmWriter.delete("1");
		tmWriter.commit();

		assertEquals("# of docs found for id=1", 0,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));
		
		tmWriter.close();		
	}
	
	
	@Test
	public void indexOlifantRecord() throws IOException {
		
		List<Code> codeList = new ArrayList<Code>();
		codeList.add(new Code(TagType.PLACEHOLDER, "br", "[br]"));
		
		LinkedHashMap<String, Object> segMap = new LinkedHashMap<String, Object>();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", enText);
		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
		segMap.put(DbUtil.TEXT_PREFIX+"FR", frText);
		segMap.put(DbUtil.CODES_PREFIX+"FR", Code.codesToString(codeList));
		segMap.put(DbUtil.TEXT_PREFIX+"ES", esText);
		segMap.put(DbUtil.CODES_PREFIX+"ES", Code.codesToString(codeList));
		segMap.put("segfield1", "value1");
		segMap.put("segfield2", "value2");
		
		OTranslationUnitInput inputTu = DbUtil.getFieldsAsIndexable("1", segMap);
		
		tmWriter.index(inputTu);
		tmWriter.commit();

		assertEquals("# of docs found for segKey=1", 1,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));

		Document doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
		
		String keyExactField_EN = "EXACT_"+DbUtil.TEXT_PREFIX+locEN.toString();
		String keyExactField_FR = "EXACT_"+DbUtil.TEXT_PREFIX+locFR.toString();
		String keyExactField_ES = "EXACT_"+DbUtil.TEXT_PREFIX+locES.toString();
		
		assertEquals("en text", enText, 
				doc1.getFieldable(keyExactField_EN).stringValue());
		assertEquals("fr text", frText, 
				doc1.getFieldable(keyExactField_FR).stringValue());
		assertEquals("es text", esText, 
				doc1.getFieldable(keyExactField_ES).stringValue());

		
		//--update values--
		segMap.put(DbUtil.TEXT_PREFIX+"EN", enText + " - updated EN");
		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
		segMap.put(DbUtil.TEXT_PREFIX+"FR", frText + " - updated FR");
		segMap.put(DbUtil.CODES_PREFIX+"FR", Code.codesToString(codeList));
		segMap.put(DbUtil.TEXT_PREFIX+"ES", esText + " - updated ES");
		segMap.put(DbUtil.CODES_PREFIX+"ES", Code.codesToString(codeList));
		segMap.put("segfield1", "value1 - updated ");
		segMap.put("segfield2", "value2 - updated ");

		inputTu = DbUtil.getFieldsAsIndexable("1", segMap);
		
		tmWriter.update(inputTu);
		tmWriter.commit();

		assertEquals("# of docs found for segKey=1", 1,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));
		
		doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
		
		assertEquals("en text", "Text EN 1 - updated EN", 
				doc1.getFieldable(keyExactField_EN).stringValue());
		assertEquals("en text", "Text FR 1 - updated FR", 
				doc1.getFieldable(keyExactField_FR).stringValue());
		assertEquals("en text", "Text ES 1 - updated ES", 
				doc1.getFieldable(keyExactField_ES).stringValue());
		
/*		tmWriter.delete("1");
		tmWriter.commit();

		assertEquals("# of docs found for id=1", 0,
				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));*/
		
		tmWriter.close();		
	}
	
	
	@Test
	public void testToTranslationUnit(){
		
		List<Code> codeList = new ArrayList<Code>();
		codeList.add(new Code(TagType.PLACEHOLDER, "br", "[br]"));
		
		LinkedHashMap<String, Object> segMap = new LinkedHashMap<String, Object>();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 1");
		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
		segMap.put(DbUtil.TEXT_PREFIX+"FR", "Text FR 1");
		segMap.put("segfield1", "value1");
		segMap.put("segfield2", "value2");

		OTranslationUnitInput inTu = DbUtil.getFieldsAsIndexable("1", segMap);		
		
		assertEquals("two tuvs", 2, inTu.getVariants().size()); 
		assertEquals("two tuvs", 2, inTu.getFields().size());
	}
	
	
	
	/**
	 * Copied rom PensieveWriterTest
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 * @throws IOException
	 */
	private int getNumOfHitsFor(String fieldName, String fieldValue)
			throws IOException {
		IndexSearcher is = new IndexSearcher(dir, true);
		PhraseQuery q = new PhraseQuery();
		q.add(new Term(fieldName, fieldValue));
		return is.search(q, 10).scoreDocs.length;
	}

	/**
	 * Copied rom PensieveWriterTest
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 * @throws IOException
	 */
	private Document findDocument(String fieldName, String fieldValue)
			throws IOException {
		IndexSearcher is = new IndexSearcher(dir, true);
		PhraseQuery q = new PhraseQuery();
		q.add(new Term(fieldName, fieldValue));
		TopDocs hits = is.search(q, 1);
		if (hits.totalHits == 0)
			return null;
		ScoreDoc scoreDoc = hits.scoreDocs[0];
		return is.doc(scoreDoc.doc);
	}
	
	
	
	
	/**
	 * Copied rom PensieveWriterTest
	 * @param srcLang
	 * @param targetLang
	 * @param source
	 * @param target
	 * @param id
	 * @return
	 */
	  /*public static TranslationUnit createTU(LocaleId srcLang, LocaleId targetLang, String source, String target, String id){
	        TranslationUnitVariant tuvS = new TranslationUnitVariant(srcLang, new TextFragment(source));
	        TranslationUnitVariant tuvT = new TranslationUnitVariant(targetLang, new TextFragment(target));
	        TranslationUnit tu = new TranslationUnit(tuvS, tuvT);
	        tu.getMetadata().put(MetadataType.ID, id);
	        return tu;
	    }*/
}
