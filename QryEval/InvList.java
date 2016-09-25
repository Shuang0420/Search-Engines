/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;
import java.io.*;

import org.apache.lucene.index.*;
import org.apache.lucene.util.*;
import org.apache.lucene.search.*;

/**
 *  This class implements the inverted list data structure and
 *  provides methods for accessing and manipulating inverted lists.
 *  Its purpose is to provide a simpler view of inverted lists than
 *  Lucene's native implementation.
 */
public class InvList {

  //  --------------- Constants and variables -----------------------

  /**
   *  Collection term frequency: The number of times that a term
   *  occurs across all instances of the specified field.
   */
  public int ctf = 0;

  /**
   *  Document frequency: The number of documents that have the term
   *  in the specified field.
   */
  public int df = 0;

  /**
   *  The field covered by the inverted list.
   */
  public String field;

  /**
   *  Postings that contain information about the occurrence of the
   *  term in individual documents.
   */
  public Vector<DocPosting> postings = new Vector<DocPosting>();

  //  --------------- Nested classes --------------------------------

  /**
   *  Utility class that makes it easier to construct postings.
   */
  public class DocPosting {

    /**
     *  The internal id of a document that contains the term in
     *  the specified field.
     */
    public int docid = 0;

    /**
     *  Term frequency:  The number of times the term occurs in
     *  the specified field of the document.
     */
    public int tf = 0;

    /**
     *  The locations where the term occurs in the specified field
     *  of the document.
     */
    public Vector<Integer> positions = new Vector<Integer>();

    public DocPosting(int d, int... locations) {
      this.docid = d;
      this.tf = locations.length;
      for (int i = 0; i < locations.length; i++)
        this.positions.add(locations[i]);
    }

    public DocPosting(int d, List<Integer> locations) {
      this.docid = d;
      this.tf = locations.size();
      for (int i = 0; i < locations.size(); i++)
        this.positions.add(locations.get(i));
    }
  }

  //  --------------- Methods ---------------------------------------

  /**
   *  Constructor.  An empty inverted list. Useful for some query operators.
   */
  public InvList() {
  }

  /**
   *  Get an empty inverted list.
   *  @param fieldString The field that the term occurs in.
   */
  public InvList(String fieldString) {
    this.field = new String (fieldString);
  }

  /**
   *  Get an inverted list from the index.
   *  @param termString The processed (stemmed, lower-cased, etc) term string.
   *  @param fieldString The field that the term occurs in.
   *  @throws IOException Error accessing the Lucene index.
   */
  public InvList(String termString, String fieldString) throws IOException {

    //  Store the field name.  This is used by other query operators.

    this.field = new String (fieldString);

    //  Prepare to access the index.

    BytesRef termBytes = new BytesRef(termString);
    Term term = new Term(fieldString, termBytes);

    if (Idx.INDEXREADER.docFreq(term) < 1)
      return;

    //  Lookup the inverted list.

    DocsAndPositionsEnum iList =
      MultiFields.getTermPositionsEnum(Idx.INDEXREADER,
				       MultiFields.getLiveDocs(Idx.INDEXREADER),
				       fieldString, termBytes);

    //  Copy from Lucene inverted list format to our inverted list
    //  format. This is a little inefficient, but allows query
    //  operators such as #SYN and #NEAR/n to be insulated from the
    //  details of Lucene inverted list implementations.

    while (iList.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

      int tf = iList.freq();
      int[] positions = new int[tf];

      for (int j = 0; j < tf; j++)
        positions[j] = iList.nextPosition();

      this.postings.add(new DocPosting(iList.docID(), positions));
      this.df++;
      this.ctf += tf;
    }
  }

  /**
   *  Append a posting to the posting list.  Posting must be appended
   *  in docid order, otherwise this method fails.
   *  @param docid The internal document id of the posting.
   *  @param positions A list of positions where the term occurs.
   *  @return true if the posting was added successfully, otherwise false.
   */
  public boolean appendPosting (int docid, List<Integer> positions) {
    
    //  A posting can only be appended if its docid is greater than
    //  the last docid.

    if ((this.df > 1) &&
	(this.postings.get(this.df-1).docid >= docid))
      return false;

    DocPosting p = new DocPosting (docid, positions);

    this.postings.add (p);
    this.df ++;
    this.ctf += p.tf;
    return true;
  }

  /**
   *  Get the n'th document id from the inverted list.
   *  @param docid The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int docid) {
    return this.postings.get(docid).docid;
  }

  /**
   *  Get the term frequency in the n'th document of the inverted list.
   *  @param n The index of the requested document term frequency.
   *  @return The document's term frequency.
   */
  public int getTf(int n) {
    return this.postings.get(n).tf;
  }

  /**
   *  Print the inverted list.  This is handy for debugging.
   */
  public void print() {

    System.out.println("df:  " + this.df + ", ctf: " + this.ctf);

    for (int i = 0; i < this.df; i++) {
      System.out.print("docid:  " + this.postings.elementAt(i).docid + ", tf: "
          + this.postings.elementAt(i).tf + ", locs: ");

      for (int j = 0; j < this.postings.elementAt(i).tf; j++) {
        System.out.print(this.postings.elementAt(i).positions.elementAt(j) + " ");
      }

      System.out.println();
    }
  }
}
