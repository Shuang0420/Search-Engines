/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopSum extends QrySop {

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

        if (r instanceof RetrievalModelBM25) {
                int doc_id = this.docIteratorGetMatch();
                double score=0;
                for (Qry qry:this.args) {
                        if (qry.docIteratorHasMatch(r) && qry.docIteratorGetMatch()==doc_id)
                            score += ((QrySop) qry).getScore(r);
                }
                return score;
        }
        else{
                throw new IllegalArgumentException
                              (r.getClass().getName() + " doesn't support the SUM operator.");
        }
}


}
