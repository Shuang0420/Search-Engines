/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The SYN operator for all retrieval models.
 */
public class QryIopSyn extends QryIop {

  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   */
  protected void evaluate () throws IOException {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.
    
    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }

    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    while (true) {

      //  Find the minimum next document id.  If there is none, we're done.

      int minDocid = Qry.INVALID_DOCID;

      for (Qry q_i: this.args) {
        if (q_i.docIteratorHasMatch (null)) {
          int q_iDocid = q_i.docIteratorGetMatch ();
          
          if ((minDocid > q_iDocid) ||
              (minDocid == Qry.INVALID_DOCID)) {
            minDocid = q_iDocid;
          }
        }
      }

      if (minDocid == Qry.INVALID_DOCID)
        break;				// All docids have been processed.  Done.
      
      //  Create a new posting that is the union of the posting lists
      //  that match the minDocid.  Save it.
      //  Note:  This implementation assumes that a location will not appear
      //  in two or more arguments.  #SYN (apple apple) would break it.

      List<Integer> positions = new ArrayList<Integer>();

      for (Qry q_i: this.args) {
        if (q_i.docIteratorHasMatch (null) &&
            (q_i.docIteratorGetMatch () == minDocid)) {
          Vector<Integer> locations_i =
            ((QryIop) q_i).docIteratorGetMatchPosting().positions;
	  positions.addAll (locations_i);
          q_i.docIteratorAdvancePast (minDocid);
	}
      }

      Collections.sort (positions);
      this.invertedList.appendPosting (minDocid, positions);
    }
  }

}
