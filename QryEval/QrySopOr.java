/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

/**
 *  Indicates whether the query has a match.
 *  @param r The retrieval model that determines what is a match
 *  @return True if the query matches, otherwise false.
 */
public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);
}

/**
 *  Get a score for the document that docIteratorHasMatch matched.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The document score.
 *  @throws IOException Error accessing the Lucene index
 */
public double getScore (RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelUnrankedBoolean) {
                return this.getScoreUnrankedBoolean (r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
                return this.getScoreRankedBoolean(r);
        }
        else{
                throw new IllegalArgumentException
                              (r.getClass().getName() + " doesn't support the OR operator.");
        }
}

/**
 *  getScore for the UnrankedBoolean retrieval model.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The document score.
 *  @throws IOException Error accessing the Lucene index
 */
private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
                return 0.0;
        } else {
                return 1.0;
        }
}

/** score is the MAX(tf) in the document
 *  getScore for the RankedBoolean retrieval model.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The document score.
 *  @throws IOException Error accessing the Lucene index
 */
private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
        // max_score of the same document for different args
        int doc_id = this.docIteratorGetMatch();
        double max_score=0;
        for (Qry qry:this.args) {
                if (qry.docIteratorHasMatch(r)) {
                        if (qry.docIteratorGetMatch() != doc_id)
                                continue;
                        double score = (double)((QrySop) qry).getScore(r);
                        if (score>max_score)
                                max_score=score;

                }
        }
        return max_score;
}

}
