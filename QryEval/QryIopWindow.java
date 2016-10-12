
import java.io.IOException;
import java.util.*;

/**
 * Created by Jiati Le on 2/23/16. Andrew ID: jiati l
 */
public class QryIopWindow extends QryIop {

    private int n;

    public QryIopWindow(int n) {
        this.n = n;
    }

    public void initialize(RetrievalModel r) throws IOException {
        super.initialize(r);
        process(r);
    }

    /**
     * Evaluate the query operator; the result is an internal inverted list that
     * may be accessed via the internal iterators.
     *
     * @throws IOException Error accessing the Lucene index.
     */
    protected void evaluate() throws IOException {
        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.
        this.invertedList = new InvList(this.getField());

        if (args.size() < 2) {
            return;
        }
    }

    public void process(RetrievalModel r) throws IOException {
        // find the min, and let min move advance
        boolean process_flag = true;
        while (process_flag) {
            //System.out.println("process_flag");
            /**
             * ---------------------------------------------------- check doc--------------------------------------
             */
            List<Integer> doc_id_list = new ArrayList<Integer>();
            for (Qry qry : this.args) {
                if (qry.docIteratorHasMatch(r)) {
                    doc_id_list.add(qry.docIteratorGetMatch());
                } else {
                    process_flag = false;
                    break;
                }
                //else
            }
            if (!process_flag) {
                break;
            }
            //System.out.println("process_flag "+process_flag);
            boolean doc_flag = false;
            // if doesn't point to the same doc, move the doc with min index advance
            while (!doc_flag) {
                //System.out.println("not "+doc_flag);
                int min_doc_id = Collections.min(doc_id_list);
                // check whether point to the same doc;
                doc_flag = true;
                //System.out.println(doc_flag);
                for (Qry qry : this.args) {
                    if (qry.docIteratorHasMatch(r) && qry.docIteratorGetMatch() != min_doc_id) {
                        doc_flag = false;
                    }
                    if (!qry.docIteratorHasMatch(r)) {
                        process_flag = false;
                        break;
                    }
                    //System.out.println("testing "+qry.docIteratorGetMatch()+"   "+min_doc_id);
                }
                //System.out.println(doc_flag);
                if (doc_flag) {
                    //    System.out.println("Ture ");
                    break;
                }
                // if not, move the doc with min index advance
                Qry qry = this.args.get(doc_id_list.indexOf(min_doc_id));
                qry.docIteratorAdvancePast(min_doc_id);
                if (qry.docIteratorHasMatch(r)) {
                    doc_id_list.set(doc_id_list.indexOf(min_doc_id), qry.docIteratorGetMatch());
                    //System.out.println("min_doc_id "+qry.docIteratorGetMatch());
                } else {
                    process_flag = false;
                    break;
                }
            }

            //System.out.println("process_flag 2 "+process_flag);
            if (!process_flag) {
                break;
            }

            // get current doc
            int curr_doc_id = this.args.get(0).docIteratorGetMatch();

            //System.out.println("curr_doc_id "+curr_doc_id);
            /**
             * ---------------------------------------------------- check loc--------------------------------------
             */
            Vector<Integer> final_pos = new Vector<Integer>();
            List<Integer> loc_list = new ArrayList<Integer>();

            for (Qry qry : this.args) {
                if (((QryIop) qry).locIteratorHasMatch()) {
                    loc_list.add(((QryIop) qry).locIteratorGetMatch());
                }
            }

            boolean loc_flag = true;
            boolean continue_flag = true;
            while (loc_flag && continue_flag) {
                int max_loc = Collections.max(loc_list);
                int min_loc = Collections.min(loc_list);
                //System.out.println("max_loc "+max_loc+"min_loc "+min_loc+" n "+this.n);
                if (max_loc - min_loc < this.n) {
                    final_pos.add(max_loc);
                    //System.out.println("match");
                    loc_list = new ArrayList<Integer>();
                    for (Qry qry : this.args) {
                        ((QryIop) qry).locIteratorAdvance();
                        if (!((QryIop) qry).locIteratorHasMatch()) {
                            continue_flag = false;
                            break;
                        }
                        loc_list.add(((QryIop) qry).locIteratorGetMatch());
                    }
                } else {
                    Qry qry = this.args.get(loc_list.indexOf(min_loc));
                    ((QryIop) qry).locIteratorAdvance();
                    if (((QryIop) qry).locIteratorHasMatch()) {
                        loc_list.set(loc_list.indexOf(min_loc), ((QryIop) qry).locIteratorGetMatch());
                    } else {
                        continue_flag = false;
                        break;
                    }
                }
            }

            if (!final_pos.isEmpty()) {
                //System.out.println("not empty ");
                this.invertedList.appendPosting(curr_doc_id, final_pos);
                //System.out.println("doc_id "+curr_doc_id);
                //for (Integer i:final_pos) {
                //System.out.println(i);
                //}
            }

            //System.out.println("FInish loop");
            for (Qry qry : this.args) {
                if (qry.docIteratorHasMatch(r)) {
                    qry.docIteratorAdvancePast(curr_doc_id);
                } else {
                    process_flag = false;
                    break;
                }
            }
            //System.out.println("finish "+process_flag);
        }

    }

}
