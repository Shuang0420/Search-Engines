/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.ArrayList;
/**
 *  The root class of all query operators that use a retrieval model
 *  to determine whether a query matches a document and to calculate a
 *  score for the document.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns a document scores (e.g., #AND (a #OR(b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that calculate document scores.
 */
public abstract class QrySop extends Qry {

  ArrayList<Double> weight_list=new ArrayList<Double>();
  //double sum_weight;

  public void setWeight(ArrayList<Double> weight_list) {
    this.weight_list=weight_list;
  }

  public void addWeight(double weight) {
    this.weight_list.add(weight);
  }

  public boolean weightExist() {
    if (this.weight_list.size()>0)
      return true;
    return false;
  }


  public double getSumWeight() {
      double sum = 0;
      for(Double d : weight_list) {
        sum += d;
      }
      return sum;
  }
  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public abstract double getScore (RetrievalModel r)
    throws IOException;


    public abstract double getDefaultScore (RetrievalModel r, int doc_id)
      throws IOException;
  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize(RetrievalModel r) throws IOException {
    for (Qry q_i: this.args) {
      q_i.initialize (r);
    }
  }
}
