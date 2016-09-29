/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the unranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelIndri extends RetrievalModel {
  float mu;
  float lambda;
  public RetrievalModelIndri(float mu,float lambda) {
    this.mu=mu;
    this.lambda=lambda;
  }
  public String defaultQrySopName () {
    return new String ("#and");
  }

}
