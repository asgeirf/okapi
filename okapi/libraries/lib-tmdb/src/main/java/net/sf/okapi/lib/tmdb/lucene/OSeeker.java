/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.search.lucene.analysis.NgramAnalyzer;
import net.sf.okapi.lib.search.lucene.query.TmFuzzyQuery;
import net.sf.okapi.lib.tmdb.DbUtil;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

/**
 * All files in this package are based on the files by @author HaslamJD and @author HARGRAVEJE
 * in the okapi-tm-pensieve project amd in most cases there are only minor changes.
 * @author fliden
 */
public class OSeeker {

	private static final Logger LOGGER = Logger.getLogger(OSeeker.class.getName());

	private final static NgramAnalyzer defaultFuzzyAnalyzer = new NgramAnalyzer(Locale.ENGLISH, 4);
	private final static float MAX_HITS_RATIO = 0.01f;
	private final static int MIN_MAX_HITS = 500;
	// TODO: externalize penalties in the future
	private static float SINGLE_CODE_DIFF_PENALTY = 0.5f;
	private static float WHITESPACE_OR_CASE_PENALTY = 1.0f;

	// maxTopDocuments = indexReader.maxDoc * MAX_HITS_CONSTANT
	private int maxTopDocuments;
	private Directory indexDir;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private IndexWriter indexWriter;
	private boolean nrtMode;

	/**
	 * Creates an instance of OSeeker
	 * 
	 * @param indexDir
	 *            The Directory implementation to use for the queries
	 * @throws IllegalArgumentException
	 *             If the indexDir is not set
	 */
	public OSeeker (Directory indexDir)
		throws IllegalArgumentException
	{
		if ( indexDir == null ) {
			throw new IllegalArgumentException("'indexDir' cannot be null!");
		}
		this.indexDir = indexDir;
		nrtMode = false;
	}
	
	/**
	 * Creates an instance of OSeeker.
	 * This constructor is used for near-real-time (NRT) mode to make index changes
	 * visible to a new searcher with fast turn-around time.
	 *
	 * @param indexWriter
	 *            The IndexWriter implementation to use for the queries, needed for NRT
	 * @throws IllegalArgumentException
	 *            If the indexDir is not set
	 */
	public OSeeker (IndexWriter indexWriter)
		throws IllegalArgumentException
	{
		if ( indexWriter == null ) {
			throw new IllegalArgumentException("'indexWriter' cannot be null!");
		}
		this.indexWriter = indexWriter;
		nrtMode = true;
	}	

	/**
	 * Get the current Lucene {@link Directory}
	 * @return the current Lucene {@link Directory}
	 */
	public Directory getIndexDir() {
		return indexDir;
	}

	/**
	 * Create a boolean query from the additional fields
	 * @param fields
	 * @return
	 */
	private BooleanQuery createQuery (OFields fields) {
		return createQuery(fields, null);
	}

	/**
	 * Add all the additional fields to the query
	 * @param fields
	 * @param q
	 * @return
	 */
	private BooleanQuery createQuery (OFields fields,
		Query q)
	{
		BooleanQuery bQuery = new BooleanQuery();
		if (q != null) {
			bQuery.add(q, BooleanClause.Occur.MUST);
		}

		if (fields != null) {
			for (OField field : fields.values()) {
				bQuery.add(new TermQuery(new Term(field.getName(), field.getValue())),
						BooleanClause.Occur.MUST);
			}
		}
		return bQuery;
	}

	/**
	 * Gets a Document's Field Value
	 * 
	 * @param doc
	 *            The document ot get the field value from
	 * @param type
	 *            The field to extract
	 * @return The value of the field
	 */
	String getFieldVdalue(Document doc, String name) {
		return getFieldValue(doc, name);
	}

	/**
	 * Gets a Document's Field Value
	 * 
	 * @param doc
	 *            The document ot get the field value from
	 * @param fieldName
	 *            The name of the field to extract
	 * @return The value of the field
	 */
	String getFieldValue(Document doc, String fieldName) {
		String fieldValue = null;
		Fieldable tempField = doc.getFieldable(fieldName);
		if (tempField != null) {
			fieldValue = tempField.stringValue();
		}
		return fieldValue;
	}

	protected IndexSearcher createIndexSearcher ()
		throws CorruptIndexException, IOException
	{
		if (indexSearcher != null) indexSearcher.close();
		return new IndexSearcher(openIndexReader());
	}

	protected IndexSearcher getIndexSearcher ()
		throws CorruptIndexException, IOException
	{
		if (( indexSearcher != null ) && !nrtMode ) {
			return indexSearcher;
		}
		// In NRT mode always create a new searcher
		indexSearcher = createIndexSearcher();
		return indexSearcher;
	}

	protected IndexReader openIndexReader() throws CorruptIndexException, IOException {
		if ( indexReader == null ) {			
			indexReader = nrtMode ?
				IndexReader.open(indexWriter, true) : 
				IndexReader.open(indexDir, true);
			maxTopDocuments = (int) ((float) indexReader.maxDoc() * MAX_HITS_RATIO);
			if (maxTopDocuments < MIN_MAX_HITS) {
				maxTopDocuments = MIN_MAX_HITS;
			}
		}
		else if ( nrtMode ) {
			indexReader = indexReader.reopen();
		}
		return indexReader;
	}

	private List<OTmHit> getTopHits(Query query,
		OFields fields,
		String locale,
		String idName)
		throws IOException
	{
		IndexSearcher is = getIndexSearcher();
		QueryWrapperFilter filter = null;
		int maxHits = 0;
		List<OTmHit> tmHitCandidates = new ArrayList<OTmHit>(maxTopDocuments);

		String keyExactField = "EXACT_"+DbUtil.TEXT_PREFIX+locale.toString();
		String keyCodesField = DbUtil.CODES_PREFIX+locale.toString();
		
		// create a filter based on the specified metadata
		if (fields != null && !fields.isEmpty()) {
			filter = new QueryWrapperFilter(createQuery(fields));
		}

		// collect hits in increments of maxTopDocuments until we have all the possible candidate hits
		TopScoreDocCollector topCollector;
		do {
			maxHits += maxTopDocuments;
			topCollector = TopScoreDocCollector.create(maxHits, true);
			is.search(query, filter, topCollector);
		} while (topCollector.getTotalHits() >= maxHits);

		// Go through the candidates and create TmHits from them
		TopDocs topDocs = topCollector.topDocs();
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = topDocs.scoreDocs[i];
			OTmHit tmHit = new OTmHit();
			tmHit.setDocId(scoreDoc.doc);
			tmHit.setScore(scoreDoc.score);
			
			//TODO: VERIFY SEGKEY--
			String segKey = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), idName);
			tmHit.setSegKey(segKey);
			
			String codesAsText = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyCodesField); 
			List<Code> tmCodes = Code.stringToCodes(codesAsText);
			
			String tmCodedText = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyExactField);

			tmHit.setTu(createTranslationUnit(getIndexSearcher().doc(tmHit.getDocId()), tmCodedText, tmCodes, fields, locale, idName));
			
			tmHitCandidates.add(tmHit);
		}

		// remove duplicate hits
		ArrayList<OTmHit> noDups = new ArrayList<OTmHit>(new LinkedHashSet<OTmHit>(tmHitCandidates));
		return noDups;
	}

	/**
	 * Search for exact and fuzzy matches
	 * 
	 * @param searchFrag the fragment to query.
	 * @param threshold the minimal score value to return.
	 * @param max the maximum number of hits to return.
	 * @param metadata any associated attributes to use for filter.
	 * @return the list of hits of the given argument.
	 * @throws IllegalArgumentException
	 *             If threshold is greater than 100 or less than 0
	 */
	public List<OTmHit> searchFuzzy(TextFragment searchFrag,
		int threshold,
		int max,
		OFields fields,
		String locale,
		String idName)
	{
		float searchThreshold = (float) threshold;
		if ( threshold < 0 ) searchThreshold = 0.0f;
		if ( threshold > 100 ) searchThreshold = 100.0f;

		String queryText = searchFrag.getText();

		String keyIndexField = DbUtil.TEXT_PREFIX+locale;
		Locale javaLoc = new Locale(locale);
		
		//--todo change from default depending--
		// create basic ngram analyzer to tokenize query
		
		TokenStream queryTokenStream;
		if ( javaLoc.getLanguage() == Locale.ENGLISH.getLanguage() ) {
			queryTokenStream = defaultFuzzyAnalyzer.tokenStream(keyIndexField, new StringReader(queryText));			
		}
		else {
			queryTokenStream = new NgramAnalyzer(javaLoc, 4).tokenStream(keyIndexField, new StringReader(queryText));
		}
		
		// get the TermAttribute from the TokenStream
		CharTermAttribute termAtt = (CharTermAttribute) queryTokenStream.addAttribute(CharTermAttribute.class);
		TmFuzzyQuery fQuery = new TmFuzzyQuery(searchThreshold, keyIndexField);
		
		try {
			queryTokenStream.reset();
			while ( queryTokenStream.incrementToken() ) {
				//Term t = new Term(keyIndexField, new String(termAtt.buffer()));
				Term t = new Term(keyIndexField, termAtt.toString());
				fQuery.add(t);
			}
			queryTokenStream.end();
			queryTokenStream.close();
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e.getMessage(), e);
		}

		return getFuzzyHits(max, searchThreshold, fQuery, searchFrag, fields, locale, idName);
	}
	
	/**
	 * Fuzzy search using default id field name
	 * @param searchFrag
	 * @param threshold
	 * @param max
	 * @param fields
	 * @param locale
	 * @return
	 */
	public List<OTmHit> searchFuzzy(TextFragment searchFrag,
		int threshold,
		int max,
		OFields fields,
		String locale)
	{
		return searchFuzzy(searchFrag, threshold, max, fields, locale, OTranslationUnitResult.DEFAULT_ID_NAME);
	}

	/**
	 * Search for fuzzy matches and adjust hit type and score based on differences with whitespace, codes and casing.
	 * 
	 * @param max the maximum number of hits to return.
	 * @param threshold the minimum score to return (between 0.0 and 1.0)
	 * @param query the query
	 * @param queryFrag the text fragment for the query.
	 * @param metadata any associated attributes to use for filter.
	 * @return the list of hits found for the given arguments (never null).
	 */
	List<OTmHit> getFuzzyHits(int max,
		float threshold,
		Query query,
		TextFragment queryFrag,
		OFields fields,
		String locale,
		String idName)
	{
		List<OTmHit> tmHitCandidates;
		List<OTmHit> tmHitsToRemove = new LinkedList<OTmHit>();
		List<Code> queryCodes = queryFrag.getCodes();

		String keyExactField = "EXACT_"+DbUtil.TEXT_PREFIX+locale.toString();
		String keyCodesField = DbUtil.CODES_PREFIX+locale.toString();
		
		try {
			tmHitCandidates = getTopHits(query, fields, locale, idName);
			
			for (OTmHit tmHit : tmHitCandidates) {
				
				String tmCodesAsString = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyCodesField);
				List<Code> tmCodes = Code.stringToCodes(tmCodesAsString);
				
				String tmCodedText = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyExactField);

				// remove codes so we can compare text only
				String sourceTextOnly = TextFragment.getText(tmCodedText);

				MatchType matchType = MatchType.FUZZY;
				Float score = tmHit.getScore();
				
				// check code missmatch
				tmHit.setCodeMismatch(false);
				if (queryCodes.size() != tmCodes.size()) {
					tmHit.setCodeMismatch(true);
				}

				// These are 100%, adjust match type and penalize for whitespace
				// and case difference
				if ( score >= 100.0f && tmCodedText.equals(queryFrag.getCodedText()) ) {
					matchType = MatchType.EXACT;
				}
				else if ( score >= 100.0f && sourceTextOnly.equals(queryFrag.getText()) ) {
					matchType = MatchType.EXACT_TEXT_ONLY;
				}
				else if ( score >= 100.0f ) {
					// must be a whitespace or case difference
					score -= WHITESPACE_OR_CASE_PENALTY;
				}
				// code penalty
				if ( queryCodes.size() != tmCodes.size() ) {
					score -= (SINGLE_CODE_DIFF_PENALTY
						* (float)Math.abs(queryCodes.size()-tmCodes.size()));
				}

				tmHit.setScore(score);
				tmHit.setMatchType(matchType);

				// check if the penalties have pushed the match below threshold
				// add any such hits to a list for later removal
				if ( tmHit.getScore() < threshold ) {
					tmHitsToRemove.add(tmHit);
				}
			}
			
			// remove hits that went below the threshold						
			tmHitCandidates.removeAll(tmHitsToRemove);

			/*
			 * System.out.println(queryFrag.toString()); System.out.println(tmHit.getScore());
			 * System.out.println(tmHit.getMatchType()); System.out.println(tmHit.getTu().toString());
			 * System.out.println();
			 */

			// sort TmHits on MatchType, Score and Source String
			Collections.sort(tmHitCandidates);
		} catch (IOException e) {
			throw new OkapiIOException("Could not complete query.", e);
		}

		int lastHitIndex = max;
		if (max >= tmHitCandidates.size()) {
			lastHitIndex = tmHitCandidates.size();
		}
		return tmHitCandidates.subList(0, lastHitIndex);
	}

	/**
	 * Creates a {@link TranslationUnit} for a given document.
	 * 
	 * @param doc the document from which to create the new translation unit.
	 * @param srcCodedText the source coded text to re-use.
	 * @param srcCodes the source codes to re-use.
	 * @return a new translation unit for the given document.
	 */
	private OTranslationUnitResult createTranslationUnit(Document doc,
		String resultCodedText,
		List<Code> resultCodes,
		OFields fields,
		String locale,
		String idName)
	{
		//--RESULT TRANSLATION UNIT--
		TextFragment resultFrag = new TextFragment();
		resultFrag.setCodedText(resultCodedText, resultCodes, false);
		OTranslationUnitVariant resultTuv = new OTranslationUnitVariant(locale, resultFrag);
		OTranslationUnitResult resultTu = new OTranslationUnitResult(getFieldValue(doc, idName), resultTuv);
		
		OFields resultFields = new OFields();
		//TODO: EITHER REMOVE OR COMPLEMENT, IN FACT REMOVE THE WHOLE METHOD AND STORE IN DB
		if(fields != null && fields.values()!=null){
			for (OField field : fields.values()) {
				resultFields.put(field.getName(), new OField(field.getName(), getFieldValue(doc, field.getValue())));
			}
			resultTu.setFields(resultFields);
		}
		
		return resultTu;
	}
	
	public void close() {
		try {
			if (indexSearcher != null) {
				indexSearcher.close();
			}

			if (indexReader != null) {
				indexReader.close();
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Exception closing Pensieve index.", e); //$NON-NLS-1$
		}
	}
}
