package net.sf.okapi.tm.pensieve.queries;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.okapi.tm.pensieve.scorers.TmFuzzyScorer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

@SuppressWarnings("serial")
public class TmFuzzyQuery extends Query {
	float threshold;
	List<Term> terms;

	public TmFuzzyQuery(float threshold) {
		this.threshold = threshold;
		terms = new LinkedList<Term>();
	}

	public void add(Term term) {
		terms.add(term);
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new TmFuzzyWeight(searcher);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void extractTerms(Set terms) {
		terms.add(terms);
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		return this;
	}

	@Override
	public String toString(String field) {
		return terms.toString();
	}

	protected class TmFuzzyWeight extends Weight {
		Similarity similarity;

		public TmFuzzyWeight(Searcher searcher) throws IOException {
			super();
			this.similarity = searcher.getSimilarity();
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			return new Explanation(getValue(), toString());
		}

		@Override
		public Query getQuery() {
			return TmFuzzyQuery.this;
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {

			// optimize zero-term or no match case
			if (terms.size() == 0)
				return null;

			return new TmFuzzyScorer(threshold, similarity, terms, reader);
		}

		@Override
		public float getValue() {
			return 1.0f;
		}

		@Override
		public void normalize(float norm) {
		}

		@Override
		public float sumOfSquaredWeights() throws IOException {
			return 1.0f;
		}
	}
}
