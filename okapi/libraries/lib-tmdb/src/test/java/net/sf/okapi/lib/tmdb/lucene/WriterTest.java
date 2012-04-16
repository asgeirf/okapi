/*===========================================================================
Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.tmdb.lucene;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import net.sf.okapi.common.LocaleId;
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

public class WriterTest {

    static final String locEN = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
    static final String locFR = DbUtil.toOlifantLocaleCode(LocaleId.FRENCH);
    static final String locES = DbUtil.toOlifantLocaleCode(LocaleId.SPANISH);

    Writer tmWriter;
	IndexWriter writer;
	Directory dir;
	
	@Before
	public void init() throws IOException {
		dir = new RAMDirectory();
		tmWriter = new Writer(dir, true);
		writer = tmWriter.getIndexWriter();
	}
	
	@Test
	public void indexRecord () throws IOException {
	    TmEntry entry = new TmEntry("1", "tmId1_", locEN, "Text EN 1", null);
	    entry.addVariant(new Variant(locFR, "Text FR 1", null));
		tmWriter.index(entry);
		tmWriter.commit();

		String entryId = "tmId1_1";
		assertEquals(1, getNumOfHitsFor(TmEntry.ID_FIELDNAME, entryId));

		Document doc1 = findDocument(TmEntry.ID_FIELDNAME, entryId);
		assertEquals("EN text", "Text EN 1", 
			doc1.getFieldable(TmEntry.GTEXT_PREFIX+locEN).stringValue());
		assertEquals("FR text", "Text FR 1", 
			doc1.getFieldable(TmEntry.GTEXT_PREFIX+locFR).stringValue());
		
		entry.getVariant(locEN).setGenericTextField("Updated EN 1");
		tmWriter.update(entry);
		tmWriter.commit();

		assertEquals(1, getNumOfHitsFor(TmEntry.ID_FIELDNAME, entryId));
		
		doc1 = findDocument(TmEntry.ID_FIELDNAME, entryId);
		assertEquals("Updated EN text", "Updated EN 1", 
			doc1.getFieldable(TmEntry.GTEXT_PREFIX+locEN).stringValue());
		
		tmWriter.delete(entryId);
		tmWriter.commit();

		assertEquals(0, getNumOfHitsFor(TmEntry.ID_FIELDNAME, entryId));

		tmWriter.close();		
	}
	
	
//	@Test
//	public void indexOlifantRecord() throws IOException {
//		
//		List<Code> codeList = new ArrayList<Code>();
//		codeList.add(new Code(TagType.PLACEHOLDER, "br", "[br]"));
//		
//		LinkedHashMap<String, Object> segMap = new LinkedHashMap<String, Object>();
//		segMap.put(DbUtil.TEXT_PREFIX+"EN", enText);
//		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
//		segMap.put(DbUtil.TEXT_PREFIX+"FR", frText);
//		segMap.put(DbUtil.CODES_PREFIX+"FR", Code.codesToString(codeList));
//		segMap.put(DbUtil.TEXT_PREFIX+"ES", esText);
//		segMap.put(DbUtil.CODES_PREFIX+"ES", Code.codesToString(codeList));
//		segMap.put("segfield1", "value1");
//		segMap.put("segfield2", "value2");
//		
//		OTranslationUnitInput inputTu = DbUtil.getFieldsAsIndexable("1", segMap);
//		
//		tmWriter.index(inputTu);
//		tmWriter.commit();
//
//		assertEquals("# of docs found for segKey=1", 1,
//				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));
//
//		Document doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
//		
//		String keyExactField_EN = "EXACT_"+DbUtil.TEXT_PREFIX+locEN.toString();
//		String keyExactField_FR = "EXACT_"+DbUtil.TEXT_PREFIX+locFR.toString();
//		String keyExactField_ES = "EXACT_"+DbUtil.TEXT_PREFIX+locES.toString();
//		
//		assertEquals("en text", enText, 
//				doc1.getFieldable(keyExactField_EN).stringValue());
//		assertEquals("fr text", frText, 
//				doc1.getFieldable(keyExactField_FR).stringValue());
//		assertEquals("es text", esText, 
//				doc1.getFieldable(keyExactField_ES).stringValue());
//
//		
//		//--update values--
//		segMap.put(DbUtil.TEXT_PREFIX+"EN", enText + " - updated EN");
//		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
//		segMap.put(DbUtil.TEXT_PREFIX+"FR", frText + " - updated FR");
//		segMap.put(DbUtil.CODES_PREFIX+"FR", Code.codesToString(codeList));
//		segMap.put(DbUtil.TEXT_PREFIX+"ES", esText + " - updated ES");
//		segMap.put(DbUtil.CODES_PREFIX+"ES", Code.codesToString(codeList));
//		segMap.put("segfield1", "value1 - updated ");
//		segMap.put("segfield2", "value2 - updated ");
//
//		inputTu = DbUtil.getFieldsAsIndexable("1", segMap);
//		
//		tmWriter.update(inputTu);
//		tmWriter.commit();
//
//		assertEquals("# of docs found for segKey=1", 1,
//				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));
//		
//		doc1 = findDocument(OTranslationUnitBase.DEFAULT_ID_NAME, "1");
//		
//		assertEquals("en text", "Text EN 1 - updated EN", 
//				doc1.getFieldable(keyExactField_EN).stringValue());
//		assertEquals("en text", "Text FR 1 - updated FR", 
//				doc1.getFieldable(keyExactField_FR).stringValue());
//		assertEquals("en text", "Text ES 1 - updated ES", 
//				doc1.getFieldable(keyExactField_ES).stringValue());
//		
///*		tmWriter.delete("1");
//		tmWriter.commit();
//
//		assertEquals("# of docs found for id=1", 0,
//				getNumOfHitsFor(OTranslationUnitBase.DEFAULT_ID_NAME, "1"));*/
//		
//		tmWriter.close();		
//	}
	
	
//	@Test
//	public void testToTranslationUnit(){
//		
//		List<Code> codeList = new ArrayList<Code>();
//		codeList.add(new Code(TagType.PLACEHOLDER, "br", "[br]"));
//		
//		LinkedHashMap<String, Object> segMap = new LinkedHashMap<String, Object>();
//		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 1");
//		segMap.put(DbUtil.CODES_PREFIX+"EN", Code.codesToString(codeList));
//		segMap.put(DbUtil.TEXT_PREFIX+"FR", "Text FR 1");
//		segMap.put("segfield1", "value1");
//		segMap.put("segfield2", "value2");
//
//		OTranslationUnitInput inTu = DbUtil.getFieldsAsIndexable("1", segMap);		
//		
//		assertEquals("two tuvs", 2, inTu.getVariants().size()); 
//		assertEquals("two tuvs", 2, inTu.getFields().size());
//	}
	
	
	
	private int getNumOfHitsFor (String fieldName,
		String fieldValue)
		throws IOException
	{
		IndexSearcher is = new IndexSearcher(dir, true);
		PhraseQuery q = new PhraseQuery();
		q.add(new Term(fieldName, fieldValue));
		return is.search(q, 10).scoreDocs.length;
	}

	private Document findDocument (String fieldName,
		String fieldValue)
		throws IOException
	{
		IndexSearcher is = new IndexSearcher(dir, true);
		PhraseQuery q = new PhraseQuery();
		q.add(new Term(fieldName, fieldValue));
		TopDocs hits = is.search(q, 1);
		if ( hits.totalHits == 0 ) return null;
		ScoreDoc scoreDoc = hits.scoreDocs[0];
		return is.doc(scoreDoc.doc);
	}
	
}
