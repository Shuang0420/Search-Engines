
/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;
import java.util.List;

/**
 * The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

	// static long N = 0;

	/**
	 * Document-independent values that should be determined just once. Some
	 * retrieval models have these, some don't.
	 */

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r
	 *            The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	@Override
	public boolean docIteratorHasMatch(RetrievalModel r) {
		return this.docIteratorHasMatchFirst(r);
	}

	/**
	 * Get a score for the document that docIteratorHasMatch matched.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	@Override
	public double getScore(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return this.getScoreUnrankedBoolean(r);
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return this.getScoreRankedBoolean(r);
		} else if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25(r);
		} else if (r instanceof RetrievalModelIndri) {
			return this.getScoreIndri(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the SCORE operator.");
		}
	}

	public void locAdvance() throws IOException {
		Qry q = this.args.get(0);
		if (q instanceof QryIop) {
			((QryIop) q).locIteratorAdvance();
		}
	}

	/**
	 * getScore for the Unranked retrieval model.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		}
		return 1.0;

	}

	public double getScoreRankedBoolean(RetrievalModel r) throws IOException {
		Qry q = this.args.get(0);
		if (q.docIteratorHasMatch(r))
			return ((QryIop) q).docIteratorGetMatchPosting().tf;
		return 0;
	}

	public double getScoreBM25(RetrievalModel r) throws IOException {
		Qry q = this.args.get(0);
		if (q.docIteratorHasMatch(r)) {
			// get idf
			long N = Idx.getNumDocs();
			double tf = ((QryIop) q).docIteratorGetMatchPosting().tf;
			double df = ((QryIop) q).getDf();

			double idf = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5)));

			// get tf weight
			double k_1 = ((RetrievalModelBM25) r).k_1;
			double b = ((RetrievalModelBM25) r).b;
			double k_3 = ((RetrievalModelBM25) r).k_3;

			String field = ((QryIop) q).getField();
			double doc_len = Idx.getFieldLength(field, q.docIteratorGetMatch());
			double avg_len = Idx.getSumOfFieldLengths(field) / (double) Idx.getDocCount(field);

			double tf_weight = tf / (tf + k_1 * (1 - b + b * doc_len / avg_len));

			// get user weight
			double user_weight = (k_3 + 1) * 1 / (k_3 + 1);// qtf, term
															// frequency in
															// query.
			// System.out.println("idf "+idf+"tf_wight "+tf_weight+ "user_weight
			// "+user_weight+"final "+idf*tf_weight*user_weight);
			return idf * tf_weight * user_weight;
		}
		return 0;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, int doc_id) throws IOException {
		Qry q = this.args.get(0);
		double lambda = ((RetrievalModelIndri) r).lambda;
		double mu = ((RetrievalModelIndri) r).mu;

		double ctf = ((QryIop) q).getCtf();

		String field = ((QryIop) q).getField();
		double doc_len = Idx.getFieldLength(field, doc_id);
		double collection_len = Idx.getSumOfFieldLengths(field);

		// mle
		double mle = ctf / collection_len;

		return (1 - lambda) * (mu * mle) / (doc_len + mu) + lambda * mle;
	}

	public double getScoreIndri(RetrievalModel r) throws IOException {
		Qry q = this.args.get(0);
		if (q.docIteratorHasMatch(r)) {

			double tf = ((QryIop) q).docIteratorGetMatchPosting().tf;

			double lambda = ((RetrievalModelIndri) r).lambda;
			double mu = ((RetrievalModelIndri) r).mu;

			double ctf = ((QryIop) q).getCtf();

			String field = ((QryIop) q).getField();
			double doc_len = Idx.getFieldLength(field, q.docIteratorGetMatch());
			double collection_len = Idx.getSumOfFieldLengths(field);

			// mle
			double mle = ctf / collection_len;

			return (1 - lambda) * (tf + mu * mle) / (doc_len + mu) + lambda * mle;
		}
		return 0;
	}

	/**
	 * Initialize the query operator (and its arguments), including any internal
	 * iterators. If the query operator is of type QryIop, it is fully
	 * evaluated, and the results are stored in an internal inverted list that
	 * may be accessed via the internal iterator.
	 * 
	 * @param r
	 *            A retrieval model that guides initialization
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	@Override
	public void initialize(RetrievalModel r) throws IOException {

		Qry q = this.args.get(0);
		q.initialize(r);
	}

	/**
	 * 
	 * @param r
	 * @param docid
	 * @param field
	 * @param qTerms
	 * @return
	 * @throws IOException
	 */
	public double getScoreBM25(RetrievalModel r, int docid, String field, List<String> qTerms) throws IOException {
		double score = 0;
		double doc_len = Idx.getFieldLength(field, docid);
		double avg_len = Idx.getSumOfFieldLengths(field) / (double) Idx.getDocCount(field);
		long N = Idx.getNumDocs();
		TermVector vec = new TermVector(docid, field);
		if (vec.positionsLength() == 0 || vec.stemsLength() == 0)
			return Double.MIN_VALUE;
		for (String stem : qTerms) {
			// get idf
			int i = vec.indexOfStem(stem);
			// System.out.println("bm i"+i);
			if (i == -1) {
				continue;
			}
			double tf = vec.stemFreq(i);
			double df = vec.stemDf(i);

			double idf = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5)));

			// get tf weight
			double k_1 = ((RetrievalModelLetor) r).k_1;
			double b = ((RetrievalModelLetor) r).b;
			double k_3 = ((RetrievalModelLetor) r).k_3;

			double tf_weight = tf / (tf + k_1 * (1 - b + b * doc_len / avg_len));

			// get user weight
			double user_weight = (k_3 + 1) * 1 / (k_3 + 1);// qtf, term
															// frequency in
															// query.
			score += idf * tf_weight * user_weight;
		}
		return score;
	}

	/**
	 * 
	 * @param r
	 * @param docid
	 * @param field
	 * @param qTerms
	 * @return
	 * @throws IOException
	 */
	public double getScoreIndri(RetrievalModel r, int docid, String field, List<String> qTerms) throws IOException {
		double score = 1;
		double doc_len = Idx.getFieldLength(field, docid);
		double collection_len = Idx.getSumOfFieldLengths(field);
		TermVector vec = new TermVector(docid, field);
		boolean match = false;
		if (vec.positionsLength() == 0 || vec.stemsLength() == 0)
			return Double.MIN_VALUE;
		for (String stem : qTerms) {
			double tf = 0;
			double ctf = Idx.getTotalTermFreq(field, stem);
			int i = vec.indexOfStem(stem);
			if (i != -1) {
				match = true;
				tf = vec.stemFreq(i);
			}
			double lambda = ((RetrievalModelLetor) r).lambda;
			double mu = ((RetrievalModelLetor) r).mu;

			// mle
			double mle = ctf / collection_len;
			score *= (1 - lambda) * (tf + mu * mle) / (doc_len + mu) + lambda * mle;
		}
		if (!match)
			return 0;
		score = Math.pow(score, 1.0 / qTerms.size());
		return score;
	}

	/**
	 * 
	 * @param docid
	 * @param field
	 * @param qTerms
	 * @return
	 * @throws IOException
	 */
	public double getScoreOverlap(int docid, String field, List<String> qTerms) throws IOException {
		if (qTerms.size() < 1)
			return 0;
		int count = 0;
		TermVector vec = new TermVector(docid, field);
		if (vec.positionsLength() == 0 || vec.stemsLength() == 0)
			return Double.MIN_VALUE;
		for (String stem : qTerms) {
			int i = vec.indexOfStem(stem);
			if (i == -1)
				continue;
			count++;
		}
		return count / (double) qTerms.size();
	}

	/**
	 * 
	 * @param docid
	 * @param field
	 * @param qTerms
	 * @return
	 * @throws IOException
	 */
	public double getScoreTfidf(int docid, String field, List<String> qTerms) throws IOException {
		double score = 0;
		long N = Idx.getNumDocs();
		TermVector vec = new TermVector(docid, field);
		for (String stem : qTerms) {
			int i = vec.indexOfStem(stem);
			if (i == -1)
				continue;

			int tf = vec.stemFreq(i);
			int df = vec.stemDf(i);

			double idf = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5)));
			score += tf * idf;
		}

		return score;
	}

	/**
	 * Gets feature 18, total term frequency / filed length
	 * 
	 * @param docid
	 * @param field
	 * @param qTerms
	 * @return
	 * @throws IOException
	 */
	public double getScoreTotalTf(int docid, String field, List<String> qTerms) throws IOException {
		double totalTf = 0;
		long N = Idx.getNumDocs();
		TermVector vec = new TermVector(docid, field);
		double fieldLength = vec.positionsLength();
		for (String stem : qTerms) {
			int i = vec.indexOfStem(stem);
			if (i == -1)
				continue;

			totalTf += vec.stemFreq(i);
		}

		return totalTf / fieldLength;
	}
}
