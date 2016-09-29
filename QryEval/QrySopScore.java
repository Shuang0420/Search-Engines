/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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
    }
    else if (r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean (r);
    }
    else if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25(r);
    }
    else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri(r);
    }
    else
    {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }




  public void locAdvance () throws IOException {
    Qry q = this.args.get(0);
    if (q instanceof QryIop) {
      ((QryIop) q).locIteratorAdvance();
  }
}


  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    }
      return 1.0;

  }


  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    Qry q = this.args.get(0);
    if (q.docIteratorHasMatch(r))
        return ((QryIop) q).docIteratorGetMatchPosting().tf;
    return 0;
  }


  public double getScoreBM25 (RetrievalModel r) throws IOException {
    Qry q = this.args.get(0);
    if (q.docIteratorHasMatch(r)) {
      // get idf
      long N=Idx.getNumDocs();
      double tf=((QryIop) q).docIteratorGetMatchPosting().tf;
      double df=((QryIop) q).getDf();

      double idf= Math.max(0,Math.log((N-df+0.5)/(df+0.5)));

      // get tf weight
      double k_1=((RetrievalModelBM25) r).k_1;
      double b=((RetrievalModelBM25) r).b;
      double k_3=((RetrievalModelBM25) r).k_3;

      String field=((QryIop) q).getField();
      double doc_len=Idx.getFieldLength(field,q.docIteratorGetMatch());
      double avg_len=Idx.getSumOfFieldLengths(field)/(double)Idx.getDocCount(field);

      double tf_weight=tf/(tf+k_1*(1-b+b*doc_len/avg_len));

      // get user weight
      double user_weight=(k_3+1)* 1/(k_3+1);// qtf, term frequency in query.
      //System.out.println("idf "+idf+"tf_wight "+tf_weight+ "user_weight "+user_weight+"final "+idf*tf_weight*user_weight);
      return idf*tf_weight*user_weight;
    }
    return 0;
  }

  public double getScoreIndri (RetrievalModel r) throws IOException {
    return 0;
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
