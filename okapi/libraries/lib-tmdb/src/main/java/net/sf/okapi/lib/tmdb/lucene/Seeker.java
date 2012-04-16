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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.lib.search.lucene.analysis.NgramAnalyzer;
import net.sf.okapi.lib.search.lucene.query.TmFuzzyQuery;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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

public class Seeker {

	private static final Logger LOGGER = Logger.getLogger(Seeker.class.getName());

	private final static NgramAnalyzer defaultFuzzyAnalyzer = new NgramAnalyzer(Locale.ENGLISH, 4);
	private final static float MAX_HITS_RATIO = 0.01f;
	private final static int MIN_MAX_HITS = 500;
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
	public Seeker (Directory indexDir)
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
	public Seeker (IndexWriter indexWriter)
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

	private BooleanQuery createQuery (HashMap<String, String> attributes,
		BooleanQuery prevQuery)
	{
		// Anything to add?
		if ( Util.isEmpty(attributes) ) return prevQuery;
		// If yes, create a new query
		BooleanQuery bQuery = new BooleanQuery();
		if ( prevQuery != null ) {
			// Add the existing one if needed
			bQuery.add(prevQuery, BooleanClause.Occur.MUST);
		}
		// Add the terms
		for (String name : attributes.keySet() ) {
			bQuery.add(new TermQuery(new Term(name, attributes.get(name))),
				BooleanClause.Occur.MUST);
		}
		return bQuery;
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
	String getFieldValue (Document doc,
		String fieldName)
	{
		String fieldValue = null;
		Fieldable tempField = doc.getFieldable(fieldName);
		if ( tempField != null ) {
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

	protected IndexReader openIndexReader ()
		throws CorruptIndexException, IOException
	{
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

	private List<TmHit> getTopHits (Query query,
		String tmId,
		String locale,
		HashMap<String, String> attributes)
		throws IOException
	{
		IndexSearcher is = getIndexSearcher();
		int maxHits = 0;
		List<TmHit> tmHitCandidates = new ArrayList<TmHit>(maxTopDocuments);

		String gtextFName = TmEntry.GTEXT_PREFIX+locale;
		String codesFName = TmEntry.CODES_PREFIX+locale;

		// Set filter data (TM id and other fields)
		QueryWrapperFilter filter = null;
		BooleanQuery bq = null;
		if ( tmId != null ) {
			bq = new BooleanQuery();
			bq.add(new TermQuery(new Term(TmEntry.TMID_FIELDNAME, tmId)), BooleanClause.Occur.MUST);
		}
		bq = createQuery(attributes, bq);
		if ( bq != null ) {
			filter = new QueryWrapperFilter(bq);
		}

		// Collect hits in increments of maxTopDocuments until we have all the possible candidate hits
		TopScoreDocCollector topCollector;
		do {
			maxHits += maxTopDocuments;
			topCollector = TopScoreDocCollector.create(maxHits, true);
			is.search(query, filter, topCollector);
		}
		while ( topCollector.getTotalHits() >= maxHits );

		// Go through the candidates and create TmHits from them
		TopDocs topDocs = topCollector.topDocs();
		for ( int i=0; i<topDocs.scoreDocs.length; i++ ) {
			ScoreDoc scoreDoc = topDocs.scoreDocs[i];
			Document doc = getIndexSearcher().doc(scoreDoc.doc);
			// Build the hit
			TmHit tmHit = new TmHit();
			tmHit.setId(getFieldValue(doc, TmEntry.ID_FIELDNAME));
			tmHit.setScore(scoreDoc.score);
			tmHit.setSegKey(getFieldValue(doc, TmEntry.SEGKEY_FIELDNAME));
			Variant variant = new Variant(locale, getFieldValue(doc, gtextFName), getFieldValue(doc, codesFName));
			tmHit.setVariant(variant);
			// Add it to the list
			tmHitCandidates.add(tmHit);
		}

		// Remove duplicate hits
		ArrayList<TmHit> noDups = new ArrayList<TmHit>(new LinkedHashSet<TmHit>(tmHitCandidates));
		return noDups;
	}

	public List<TmHit> searchFuzzy (String genericText,
		String codesAsString,
		String tmId,
		String locale,
		int max,
		int threshold,
		HashMap<String, String> attributes)
	{
		float searchThreshold = (float)threshold;
		if ( threshold < 0 ) searchThreshold = 0.0f;
		if ( threshold > 100 ) searchThreshold = 100.0f;

		String queryText = genericText;

		String gtextFName = TmEntry.GTEXT_PREFIX+locale;
		Locale javaLoc = new Locale(locale);
		
		// create basic ngram analyzer to tokenize query
		TokenStream queryTokenStream;
		if ( javaLoc.getLanguage() == Locale.ENGLISH.getLanguage() ) {
			queryTokenStream = defaultFuzzyAnalyzer.tokenStream(gtextFName, new StringReader(queryText));			
		}
		else {
			queryTokenStream = new NgramAnalyzer(javaLoc, 4).tokenStream(gtextFName, new StringReader(queryText));
		}
		
		// Get the TermAttribute from the TokenStream
		CharTermAttribute termAtt = (CharTermAttribute)queryTokenStream.addAttribute(CharTermAttribute.class);
		TmFuzzyQuery fQuery = new TmFuzzyQuery(searchThreshold, gtextFName);
		
		try {
			queryTokenStream.reset();
			while ( queryTokenStream.incrementToken() ) {
				//Term t = new Term(keyIndexField, new String(termAtt.buffer()));
				Term t = new Term(gtextFName, termAtt.toString());
				fQuery.add(t);
			}
			queryTokenStream.end();
			queryTokenStream.close();
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e.getMessage(), e);
		}

		return getFuzzyHits(fQuery, genericText, codesAsString, tmId, locale, max, searchThreshold, attributes);
	}
	
	private List<TmHit> getFuzzyHits (Query query,
		String genericText,
		String codesAsString,
		String tmId,
		String locale,
		int max,
		float threshold,
		HashMap<String, String> attributes)
	{
		List<TmHit> tmHitCandidates;
		List<TmHit> tmHitsToRemove = new LinkedList<TmHit>();

		try {
			tmHitCandidates = getTopHits(query, tmId, locale, attributes);
			
			for ( TmHit tmHit : tmHitCandidates ) {
				
				String gtextValue = tmHit.getVariant().getGenericTextField().stringValue();
				Field codesField = tmHit.getVariant().getCodesField();
				String codesValue;
				if ( codesField == null ) codesValue = "";
				else codesValue = codesField.stringValue();

				MatchType matchType = MatchType.FUZZY;
				Float score = tmHit.getScore();
				
				// These are 100%, adjust match type and penalize for whitespace
				// and case difference
				if (( score >= 100.0f ) && genericText.equals(gtextValue) ) {
					matchType = MatchType.EXACT;
				}
//				else if (( score >= 100.0f ) && sourceTextOnly.equals(queryFrag.getText()) ) {
//					matchType = MatchType.EXACT_TEXT_ONLY;
//				}
				else if ( score >= 100.0f ) {
					// must be a whitespace or case difference
					score -= WHITESPACE_OR_CASE_PENALTY;
				}
				// Check code miss-match
				tmHit.setCodeMismatch(false);
				if ( codesAsString == null )  codesAsString = "";
				if ( !codesAsString.equals(codesValue) ) {
					tmHit.setCodeMismatch(true);
					//TODO: calculate code penality per code
					score -= SINGLE_CODE_DIFF_PENALTY;
				}
				
//				// code penalty
//				if ( queryCodes.size() != tmCodes.size() ) {
//					score -= (SINGLE_CODE_DIFF_PENALTY
//						* (float)Math.abs(queryCodes.size()-tmCodes.size()));
//				}

				tmHit.setScore(score);
				tmHit.setMatchType(matchType);

				// Check if the penalties have pushed the match below threshold
				// add any such hits to a list for later removal
				if ( tmHit.getScore() < threshold ) {
					tmHitsToRemove.add(tmHit);
				}
			}
			
			// Remove hits that went below the threshold						
			tmHitCandidates.removeAll(tmHitsToRemove);

			// Sort hits on MatchType, Score and Source String
			Collections.sort(tmHitCandidates);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Could not complete query.", e);
		}

		if ( max >= tmHitCandidates.size() ) {
			return tmHitCandidates;
		}
		// Else: return the start of the list
		return tmHitCandidates.subList(0, max);
	}

//	List<OTmHit> getFuzzyHits (int max,
//		float threshold,
//		Query query,
//		TextFragment queryFrag,
//		OFields fields,
//		String locale,
//		String idName)
//	{
//		List<OTmHit> tmHitCandidates;
//		List<OTmHit> tmHitsToRemove = new LinkedList<OTmHit>();
//		List<Code> queryCodes = queryFrag.getCodes();
//
//		String keyExactField = "EXACT_"+DbUtil.TEXT_PREFIX+locale.toString();
//		String keyCodesField = DbUtil.CODES_PREFIX+locale.toString();
//		
//		try {
//			tmHitCandidates = getTopHits(query, fields, locale);
//			
//			for ( OTmHit tmHit : tmHitCandidates ) {
//				
//				String tmCodesAsString = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyCodesField);
//				List<Code> tmCodes = Code.stringToCodes(tmCodesAsString);
//				
//				String tmCodedText = getFieldValue(getIndexSearcher().doc(tmHit.getDocId()), keyExactField);
//
//				// remove codes so we can compare text only
//				String sourceTextOnly = TextFragment.getText(tmCodedText);
//
//				MatchType matchType = MatchType.FUZZY;
//				Float score = tmHit.getScore();
//				
//				// check code missmatch
//				tmHit.setCodeMismatch(false);
//				if (queryCodes.size() != tmCodes.size()) {
//					tmHit.setCodeMismatch(true);
//				}
//
//				// These are 100%, adjust match type and penalize for whitespace
//				// and case difference
//				if ( score >= 100.0f && tmCodedText.equals(queryFrag.getCodedText()) ) {
//					matchType = MatchType.EXACT;
//				}
//				else if ( score >= 100.0f && sourceTextOnly.equals(queryFrag.getText()) ) {
//					matchType = MatchType.EXACT_TEXT_ONLY;
//				}
//				else if ( score >= 100.0f ) {
//					// must be a whitespace or case difference
//					score -= WHITESPACE_OR_CASE_PENALTY;
//				}
//				// code penalty
//				if ( queryCodes.size() != tmCodes.size() ) {
//					score -= (SINGLE_CODE_DIFF_PENALTY
//						* (float)Math.abs(queryCodes.size()-tmCodes.size()));
//				}
//
//				tmHit.setScore(score);
//				tmHit.setMatchType(matchType);
//
//				// check if the penalties have pushed the match below threshold
//				// add any such hits to a list for later removal
//				if ( tmHit.getScore() < threshold ) {
//					tmHitsToRemove.add(tmHit);
//				}
//			}
//			
//			// remove hits that went below the threshold						
//			tmHitCandidates.removeAll(tmHitsToRemove);
//
//			/*
//			 * System.out.println(queryFrag.toString()); System.out.println(tmHit.getScore());
//			 * System.out.println(tmHit.getMatchType()); System.out.println(tmHit.getTu().toString());
//			 * System.out.println();
//			 */
//
//			// sort TmHits on MatchType, Score and Source String
//			Collections.sort(tmHitCandidates);
//		} catch (IOException e) {
//			throw new OkapiIOException("Could not complete query.", e);
//		}
//
//		int lastHitIndex = max;
//		if (max >= tmHitCandidates.size()) {
//			lastHitIndex = tmHitCandidates.size();
//		}
//		return tmHitCandidates.subList(0, lastHitIndex);
//	}
	
//	/**
//	 * Creates a {@link TranslationUnit} for a given document.
//	 * 
//	 * @param doc the document from which to create the new translation unit.
//	 * @param srcCodedText the source coded text to re-use.
//	 * @param srcCodes the source codes to re-use.
//	 * @return a new translation unit for the given document.
//	 */
//	private OTranslationUnitResult createTranslationUnit (Document doc,
//		String resultCodedText,
//		List<Code> resultCodes,
//		OFields fields,
//		String locale,
//		String idName)
//	{
//		//--RESULT TRANSLATION UNIT--
//		TextFragment resultFrag = new TextFragment();
//		resultFrag.setCodedText(resultCodedText, resultCodes, false);
//		OTranslationUnitVariant resultTuv = new OTranslationUnitVariant(locale, resultFrag);
//		OTranslationUnitResult resultTu = new OTranslationUnitResult(getFieldValue(doc, idName), resultTuv);
//		
//		OFields resultFields = new OFields();
//		//TODO: EITHER REMOVE OR COMPLEMENT, IN FACT REMOVE THE WHOLE METHOD AND STORE IN DB
//		if(fields != null && fields.values()!=null){
//			for (OField field : fields.values()) {
//				resultFields.put(field.getName(), new OField(field.getName(), getFieldValue(doc, field.getValue())));
//			}
//			resultTu.setFields(resultFields);
//		}
//		
//		return resultTu;
//	}
	
	public void close() {
		try {
			if (indexSearcher != null) {
				indexSearcher.close();
			}

			if (indexReader != null) {
				indexReader.close();
			}
		}
		catch ( IOException e ) {
			LOGGER.log(Level.WARNING, "Exception closing TM index.", e); //$NON-NLS-1$
		}
	}
}
