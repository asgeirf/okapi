package net.sf.okapi.lib.search;

public class FastLevenshtein {
	/**
	 * <p>
	 * Similarity returns a number that is 1.0f or less (including negative numbers) based on how similar the Term is
	 * compared to a target term. It returns exactly 0.0f when
	 * 
	 * <pre>
	 * editDistance &lt; maximumEditDistance
	 * </pre>
	 * 
	 * Otherwise it returns:
	 * 
	 * <pre>
	 * 1 - (editDistance / length)
	 * </pre>
	 * 
	 * where length is the length of the shortest term (text or target) including a prefix that are identical and
	 * editDistance is the Levenshtein distance for the two words.
	 * </p>
	 * 
	 * <p>
	 * Embedded within this algorithm is a fail-fast Levenshtein distance algorithm. The fail-fast algorithm differs
	 * from the standard Levenshtein distance algorithm in that it is aborted if it is discovered that the minimum
	 * distance between the words is greater than some threshold.
	 * 
	 * <p>
	 * To calculate the maximum distance threshold we use the following formula:
	 * 
	 * <pre>
	 * (1 - minimumSimilarity) * length
	 * </pre>
	 * 
	 * where length is the shortest term including any prefix that is not part of the similarity comparison. This
	 * formula was derived by solving for what maximum value of distance returns false for the following statements:
	 * 
	 * <pre>
	 * similarity = 1 - ((float) distance / (float) (prefixLength + Math.min(textlen, targetlen)));
	 * return (similarity &gt; minimumSimilarity);
	 * </pre>
	 * 
	 * where distance is the Levenshtein distance for the two words.
	 * </p>
	 * <p>
	 * Levenshtein distance (also known as edit distance) is a measure of similarity between two strings where the
	 * distance is measured as the number of character deletions, insertions or substitutions required to transform one
	 * string to the other string.
	 * 
	 * @param target
	 *            the target word or phrase
	 * @return the similarity, 0.0 or less indicates that it matches less than the required threshold and 1.0 indicates
	 *         that the text and target are identical
	 */
//	public final float similarity(final String target) {
//		final int m = target.length();
//		final int n = text.length();
//		if (n == 0) {
//			// we don't have anything to compare. That means if we just add
//			// the letters for m we get the new word
//			return prefix.length() == 0 ? 0.0f : 1.0f - ((float) m / prefix.length());
//		}
//		if (m == 0) {
//			return prefix.length() == 0 ? 0.0f : 1.0f - ((float) n / prefix.length());
//		}
//
//		final int maxDistance = calculateMaxDistance(m);
//
//		if (maxDistance < Math.abs(m - n)) {
//			// just adding the characters of m to n or vice-versa results in
//			// too many edits
//			// for example "pre" length is 3 and "prefixes" length is 8. We can see that
//			// given this optimal circumstance, the edit distance cannot be less than 5.
//			// which is 8-3 or more precisely Math.abs(3-8).
//			// if our maximum edit distance is 4, then we can discard this word
//			// without looking at it.
//			return 0.0f;
//		}
//
//		// init matrix d
//		for (int i = 0; i <= n; ++i) {
//			p[i] = i;
//		}
//
//		// start computing edit distance
//		for (int j = 1; j <= m; ++j) { // iterates through target
//			int bestPossibleEditDistance = m;
//			final char t_j = target.charAt(j - 1); // jth character of t
//			d[0] = j;
//
//			for (int i = 1; i <= n; ++i) { // iterates through text
//				// minimum of cell to the left+1, to the top+1, diagonally left and up +(0|1)
//				if (t_j != text.charAt(i - 1)) {
//					d[i] = Math.min(Math.min(d[i - 1], p[i]), p[i - 1]) + 1;
//				} else {
//					d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1]);
//				}
//				bestPossibleEditDistance = Math.min(bestPossibleEditDistance, d[i]);
//			}
//
//			// After calculating row i, the best possible edit distance
//			// can be found by found by finding the smallest value in a given column.
//			// If the bestPossibleEditDistance is greater than the max distance, abort.
//
//			if (j > maxDistance && bestPossibleEditDistance > maxDistance) { // equal is okay, but not greater
//				// the closest the target can be to the text is just too far away.
//				// this target is leaving the party early.
//				return 0.0f;
//			}
//
//			// copy current distance counts to 'previous row' distance counts: swap p and d
//			int _d[] = p;
//			p = d;
//			d = _d;
//		}
//
//		// our last action in the above loop was to switch d and p, so p now
//		// actually has the most recent cost counts
//
//		// this will return less than 0.0 when the edit distance is
//		// greater than the number of characters in the shorter word.
//		// but this was the formula that was previously used in FuzzyTermEnum,
//		// so it has not been changed (even though minimumSimilarity must be
//		// greater than 0.0)
//		return 1.0f - ((float) p[n] / (float) (prefix.length() + Math.min(n, m)));
//	}

	/**
	 * The max Distance is the maximum Levenshtein distance for the text compared to some other value that results in
	 * score that is better than the minimum similarity.
	 * 
	 * @param m
	 *            the length of the "other value"
	 * @return the maximum levenshtein distance that we care about
	 */
//	private int calculateMaxDistance(int m) {
//		return (int) ((1 - minimumSimilarity) * (Math.min(text.length(), m) + prefix.length()));
//	}
}
