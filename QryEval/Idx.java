/** 
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *  Idx manages and provides access to Lucene indexes and auxiliary
 *  data structures.
 *  <p>
 *  Most homework assignments only require a single index.  However,
 *  several distinct indexes can be open simultaneously (e.g., for
 *  federated search).  The Idx class designates one index the
 *  <i>current</i> index.  All requests are satisfied from the current
 *  index.  setCurrentIndex changes the current index.
 *  </p>
 */
public class Idx {

  //  --------------- Constants and variables ---------------------

  /**
   *  The Lucene index that is considered the current index.
   */
  public static IndexReader INDEXREADER=null;

  private static DocLengthStore DOCLENGTHSTORE=null;

  private static HashMap<String,IndexReader> openIndexReaders =
    new HashMap<String,IndexReader> ();
  private static HashMap<String,DocLengthStore> openDocLengthStores =
    new HashMap<String,DocLengthStore> ();

  //  --------------- Methods ---------------------------------------

  /**
   *  Get the specified attribute from the specified document.
   *  @param attributeName Name of attribute
   *  @param docid The internal docid in the lucene index.
   *  @return the attribute value
   *  @throws IOException Error accessing the Lucene index.
   */
  public static String getAttribute (String attributeName, int docid)
    throws IOException {

    Document d = Idx.INDEXREADER.document (docid);
    return d.get (attributeName);
  }

  /**
   *  Get the number of documents that contain the specified field.
   *  @param fieldName the field name
   *  @return the number of documents that contain the field
   *  @throws IOException Error accessing the Lucene index.
   */
  public static int getDocCount (String fieldName)
    throws IOException {
    return Idx.INDEXREADER.getDocCount (fieldName);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id.
   *  @param iid The internal document id of the document.
   *  @return the external document id
   *  @throws IOException Error accessing the Lucene index.
   */
  public static String getExternalDocid(int iid) throws IOException {
    Document d = Idx.INDEXREADER.document(iid);
    String eid = d.get("externalId");
    return eid;
  }

  /**
   *  Get the length of the specified field in the specified document.
   *  @param fieldName Name of field to access lengths.
   *  @param docid The internal docid in the Lucene index.
   *  @return the length of the field, including stopword positions.
   *  @throws IOException Error accessing the Lucene index.
   */
  public static int getFieldLength (String fieldName, int docid)
    throws IOException {
    return (int) Idx.DOCLENGTHSTORE.getDocLength (fieldName, docid);
  }

  /**
   * Get the internal document id for a document specified by its
   * external id, e.g. clueweb09-enwp00-88-09710. If no such document
   * exists, throw an exception.
   * @param externalId The external docid in the Lucene index.
   * @return iternal docid.
   * @throws Exception Could not read the internal document id from the index.
   */
  public static int getInternalDocid(String externalId)
    throws Exception {

    Query q = new TermQuery(new Term("externalId", externalId));

    IndexSearcher searcher = new IndexSearcher(Idx.INDEXREADER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   *  Get the total number of documents in the corpus.
   *  @return The total number of documents.
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getNumDocs () throws IOException {
    return Idx.INDEXREADER.numDocs();
  }

  /**
   *  Get the total number of term occurrences contained in all
   *  instances of the specified field in the corpus (e.g., add up the
   *  lengths of every TITLE field in the corpus).
   *  @param fieldName The field name.
   *  @return The total number of term occurrence
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getSumOfFieldLengths (String fieldName)
    throws IOException {
    return Idx.INDEXREADER.getSumTotalTermFreq (fieldName);
  }


  /**
   *  Get the collection term frequency (ctf) of a term in
   *  a field (e.g., the total number of times the term 'apple'
   *  occurs in title fields.
   *  @param fieldName The field name.
   *  @param term The term.
   *  @return The total number of term occurrence
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getTotalTermFreq (String fieldName, String term)
    throws IOException {
    return INDEXREADER.totalTermFreq (new Term (fieldName, new BytesRef (term)));
  }


  /**
   *  Open a Lucene index and the associated DocLengthStore.
   *  @param indexPath A directory that contains a Lucene index.
   *  @throws IllegalArgumentException Unable to open the index.
   *  @throws IOException Error accessing the index.
   */
  public static void open (String indexPath)
    throws IllegalArgumentException, IOException {

    IndexReader indexReader;
    DocLengthStore docLengthStore;

    //  Open the Lucene index

    indexReader = 
      DirectoryReader.open (FSDirectory.open (new File (indexPath)));
  
    if (indexReader == null) {
      throw new IllegalArgumentException ("Unable to open the index.");
    }
  
    //  Lucene doesn't store field lengths the way that we want them,
    //  so we have our own document length store.

    docLengthStore = new DocLengthStore (indexReader);
  
    if (docLengthStore == null) {
      throw new IllegalArgumentException ("Unable to open the document length store.");
    }

    //  Keep track of the open indexes.

    openIndexReaders.put (indexPath, indexReader);
    openDocLengthStores.put (indexPath, docLengthStore);

    //  The current index defaults to the first open index.

    if (Idx.INDEXREADER == null) {
      Idx.INDEXREADER = indexReader;
      Idx.DOCLENGTHSTORE = docLengthStore;
    }
  }

  /**
   *  Change the current index to another open Lucene index.
   *  @param indexPath A directory that contains an open Lucene index.
   *  @throws IllegalArgumentException The specified index isn't open.
   */
  public static void setCurrentIndex (String indexPath)
    throws IllegalArgumentException {

    IndexReader indexReader = openIndexReaders.get (indexPath);
    DocLengthStore docLengthStore = openDocLengthStores.get (indexPath);

    if ((indexReader == null) || (docLengthStore == null)) {
      throw new IllegalArgumentException (
        "An index must be open before it can be the current index");
    }

    Idx.INDEXREADER = indexReader;
    Idx.DOCLENGTHSTORE = docLengthStore;
  }
}
