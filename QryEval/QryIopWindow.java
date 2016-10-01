/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The SYN operator for all retrieval models.
 */
public class QryIopWindow extends QryIop {
int n;
public QryIopWindow(int n){
        this.n=n;
}

public void initialize(RetrievalModel r) throws IOException {
        super.initialize(r);
        process(r);
}


public void process(RetrievalModel r) throws IOException {
        QryIop q = (QryIop)this.args.get(0);
        while (q.docIteratorHasMatch(r)) {
                // get the doc that contains first argument
                int doc_id = q.docIteratorGetMatch();

                q.docIteratorAdvanceTo(doc_id);
                // get the positions of first argument in the matched doc
                Vector<Integer> final_pos= q.docIteratorGetMatchPosting().positions;

                for (int i=1; i<this.args.size(); i++) {
                        // check the final matched position of last arument
                        final_pos=recursive(i,doc_id,final_pos,r);
                        if (final_pos.isEmpty()) break;
                }

                if (!final_pos.isEmpty()) {
                        Collections.sort(final_pos);
                        this.invertedList.appendPosting (doc_id, final_pos);
                }
                q.docIteratorAdvancePast(doc_id);
        }

}


/**
 *  Evaluate the query operator; the result is an internal inverted
 *  list that may be accessed via the internal iterators.
 *  @throws IOException Error accessing the Lucene index.
 */
protected void evaluate () throws IOException {
        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.
        this.invertedList = new InvList (this.getField());

        if (args.size () < 2) {
                return;
        }
}



/**
 *  Returns all the matched positions of current argument
 *  @param i The current argument to process.
 *  @param doc_id The current doc to process.
 *  @param curr_pos The current final positions of last argument.
 *  @param r The retrieval model that determines how scores are calculated.
 *  @return The final matched positions of current argument.
 *  @throws IOException Error accessing the Lucene index.
 */
private Vector<Integer> recursive(int i, int doc_id, Vector<Integer> curr_pos,RetrievalModel r) throws IOException {
        Vector<Integer> final_pos = new Vector<Integer>();
        Iterator<Integer> left_postings = curr_pos.iterator();
        QryIop right=(QryIop)this.args.get(i);
        right.docIteratorAdvanceTo(doc_id);
        // check if current doc contains current argument
        if (!right.docIteratorHasMatch(r) || right.docIteratorGetMatch() != doc_id)
                return final_pos;
        Iterator<Integer> right_postings = right.docIteratorGetMatchPosting().positions.iterator();
        if (!(left_postings.hasNext()&&right_postings.hasNext())) {
                return final_pos;
        }
        int left_loc=left_postings.next();
        int right_loc=right_postings.next();
        while (true) {
                //System.out.println("i "+i+"Left: "+left_loc+"Right: "+right_loc);
                //while (left_loc>right_loc) {
                if (left_loc>right_loc) {
                        right_postings.remove();
                        if (!right_postings.hasNext()) {
                                return final_pos;
                        }
                        right_loc=right_postings.next();
                        //System.out.println("i "+i+"Left: "+left_loc+"Right: "+right_loc);
                }
                // now left_loc<=right_loc
                else if (right_loc-left_loc<=this.n) {// then go for the next argument
                        final_pos.add(right_loc);
                        left_postings.remove();
                        right_postings.remove();
                        if (!(left_postings.hasNext()&&right_postings.hasNext())) {
                                return final_pos;
                        }
                        left_loc=left_postings.next();
                        right_loc=right_postings.next();
                        //  System.out.println("i "+i+"Left: "+left_loc+"Right: "+right_loc);
                }

                else { //right_loc-left_loc>this.n
                        left_postings.remove();
                        if (!left_postings.hasNext()) {
                                return final_pos;
                        }
                        left_loc=left_postings.next();
                        //  System.out.println("i "+i+"Left: "+left_loc+"Right: "+right_loc);

                }
        }
}
}
