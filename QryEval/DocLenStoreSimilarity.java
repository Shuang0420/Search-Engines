/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.search.similarities.TFIDFSimilarity;


public class DocLenStoreSimilarity extends SimilarityBase {
   
  @Override
  protected float score(BasicStats stats, float freq, float docLen) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return null;
  }


  /** Encodes the document length in the same way as {@link TFIDFSimilarity}. */
  @Override
  public long computeNorm(FieldInvertState state) {
    final float numTerms = state.getPosition();
    return (long)numTerms;
  }
  
}
