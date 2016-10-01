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


public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelIndri) {
                return this.getScoreIndri(r);
        }
        else if (r instanceof RetrievalModelBM25) {
                return this.getScoreBM25(r);
        }
        else{
                throw new IllegalArgumentException
                              (r.getClass().getName() + " doesn't support the OR operator.");
        }
}



/**
 *  Get a score for the document that docIteratorHasMatch matched.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The document score.
 *  @throws IOException Error accessing the Lucene index
 */
public double getScoreBM25 (RetrievalModel r) throws IOException {

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


public double getDefaultScore(RetrievalModel r, int doc_id) throws IOException {
  double score=0.0;
  int weight_index=0;
  for (Qry qry:this.args) {
    double weight=(double)(this.weight_list.get(weight_index++));
    score+=(((QrySop) qry).getDefaultScore(r,doc_id)*weight/this.getSumWeight());
  }
  return score;
}
/**
 *  Get a score for the document that docIteratorHasMatch matched.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The document score.
 *  @throws IOException Error accessing the Lucene index
 */
public double getScoreIndri (RetrievalModel r) throws IOException {

        if (!(r instanceof RetrievalModelIndri)) {
                throw new IllegalArgumentException
                              (r.getClass().getName() + " doesn't support the WSUM operator.");
        }

        int doc_id = this.docIteratorGetMatch();
        double score=0;
        System.out.println("weight list "+this.weight_list.size());
        int weight_index=0;
        for (Qry qry:this.args) {
          double weight=(double)(this.weight_list.get(weight_index++));
                if (qry.docIteratorHasMatch(r) && qry.docIteratorGetMatch()==doc_id)
                    score += ((QrySop) qry).getScore(r)*weight/this.getSumWeight();
                else
                    score += ((QrySop) qry).getDefaultScore(r,doc_id)*weight/this.getSumWeight();
        }
        return score;
}


}
