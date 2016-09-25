/** 
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The root class in the query operator hierarchy.  Most of this
 *  class is abstract, because different <i>types</i> of query
 *  operators (Sop, Iop) have different subclasses, and each
 *  <i>query operator</i> has its own subclass.  This class defines
 *  the common interface to query operators, and is a place to store
 *  data structures and methods that are common to all query
 *  operators.
 *  <p>
 *  Document-at-a-time (DAAT) processing is implemented as iteration
 *  over (virtual or materialized) lists of document ids or document
 *  locations.  To evaluate query q using the UnrankedBoolean retrieval
 *  model:
 *  </p>
 *  <pre>
 *    RetrievalModel r = new RetrievalModelUnrankedBoolean ();
 *    q.initialize (r);
 * 
 *    while (q.docIteratorHasMatch (r)) {
 *      int docid = q.docIteratorGetMatch ();
 *      double score = ((QrySop) q).getScore (model);
 *      System.out.println ("internal docid: " + docid + ", score: " score);
 *      q.docIteratorAdvancePast (docid);
 *    }
 *  </pre>
 *  <p>
 *  The Qry class defines the iteration interface and provides general
 *  methods that each subclass may override or use.  Note that the 
 *  iteration interface <i>does not</i> conform to the standard Java
 *  iteration interface.  It has different characteristics and capabilities.
 *  For example, getting the current element <i>does not</i> consume the
 *  element; the iterator must be advanced explicitly, and it can be
 *  advanced in different ways, which provides opportunities to evaluate
 *  the query more efficiently.
 *  </p><p>
 *  The Qry class has two subclasses.  QrySop ("score operators") contains
 *  query operators that compute document scores (e.g., AND, OR, SCORE).
 *  QryIop ("inverted list operators") contains query operators that
 *  produce inverted lists (e.g., SYN, NEAR, TERM).  
 *  </p><p>
 *  The docIterator for query operators in the QrySop hierarchy iterates
 *  over a virtual list.  The next document id is determined dynamically
 *  when hasMatch is called.  Thus, the iterator needs to be part of
 *  the query operator, because different query operators may have
 *  different strategies for determining what matches and how scores
 *  are calculated.  When hasMatch identifies a match, the match is
 *  cached so that it can be accessed efficiently by getMatch and
 *  getScore methods.
 *  </p><p>
 *  The inverted lists of query operators in the QryIop hierarchy are
 *  materialized when the query operator is initialized.  It is not
 *  possible to produce them in a document-at-a-time mode because
 *  the df and ctf statistics are not known until the inverted list
 *  is fully constructed.  QryIop operators provide a document-at-a-time
 *  interface to the inverted lists via docIterators.
 *  </p><p>
 *  The data structure that stores query arguments (args) is accessible
 *  by subclasses.  If it is accessed via a standard Java iterator, the
 *  search engine creates and then discards many (many) iterators during
 *  query evaluation, which reduces computational efficiency.
 *  </p>
 */
public abstract class Qry {

  //  --------------- Constants and variables ---------------------

  /**
   *  An invalid internal document id.
   */
   public static final int INVALID_DOCID = Integer.MIN_VALUE;

  /**
   *  The arguments to this query operator.  The TERM query operator
   *  has 0 arguments.  The SCORE query operator has 1 argument.  All
   *  other query operators have 1 or more arguments (e.g., #SYN (blue aqua)).
   *  Some query operators have parameters (e.g., #NEAR/8) or weights
   *  (e.g., #WSUM (0.4 blue 0.6 skies) that are not considered arguments.
   */
  protected ArrayList<Qry> args = new ArrayList<Qry>();

  /**
   *  The string to use when the query is displayed.  Some query
   *  operators (e.g., QrySopAnd) may be represented by more than
   *  one name (e.g., #COMBINE, #AND).
   */
  private String displayName = new String ("Unnamed");

  /**
   *  docIteratorHasMatch caches the matching docid so that
   *  docIteratorGetMatch and getScore don't have to recompute it.
   */
  private int docIteratorMatchCache = Qry.INVALID_DOCID;
  
  private boolean matchStored = false;	// Operators can cache matches
  private int matchingDocid;

  //  --------------- Methods ---------------------------------------

  /**
   *  Append an argument to the list of query operator arguments.  
   *  @param q The query argument (query operator) to append.
   *  @throws IllegalArgumentException q is an invalid argument
   */
  public void appendArg(Qry q) throws IllegalArgumentException {

    //  The query parser and query operator type system are too simple
    //  to detect some kinds of query syntax errors.  appendArg does
    //  additional syntax checking while creating the query tree.  It
    //  also inserts SCORE operators between QrySop operators and QryIop
    //  arguments, and propagates field information from QryIop
    //  children to parents.  Basically, it creates a well-formed
    //  query tree.
    
    if (this instanceof QryIopTerm) {
      throw new IllegalArgumentException
        ("The TERM operator has no arguments.");
    }

    //  SCORE operators can have only a single argument of type QryIop.
    
    if (this instanceof QrySopScore) {
      if (this.args.size () > 0) {
        throw new IllegalArgumentException
          ("Score operators can have only one argument");
      } else if (! (q instanceof QryIop)) {
        throw new IllegalArgumentException
          ("The argument to a SCORE operator must be of type QryIop.");
      } else {
        this.args.add(q);
        return;
      }
    }

    //  Check whether it is necessary to insert an implied SCORE
    //  operator between a QrySop operator and a QryIop argument.

    if ((this instanceof QrySop) && (q instanceof QryIop)) {
      Qry impliedOp = new QrySopScore ();
      impliedOp.setDisplayName ("#SCORE");
      impliedOp.appendArg (q);
      this.args.add (impliedOp);
      return;
    }

    //  QryIop operators must have QryIop arguments in the same field.
    
    if ((this instanceof QryIop) && (q instanceof QryIop)) {
      
      if (this.args.size() == 0) {
        ((QryIop) this).field = new String (((QryIop) q).getField());
      } else {
        if (! ((QryIop) this).field.equals (((QryIop) q).getField())) {
          throw new IllegalArgumentException
            ("Arguments to QryIop operators must be in the same field.");
        }
      }

      this.args.add(q);
      return;
    }


    //  QrySop operators and their arguments must be of the same type.

    if ((this instanceof QrySop) && (q instanceof QrySop)) {
      this.args.add(q);
      return;
    }

    throw new IllegalArgumentException
      ("Objects of type " + 
       q.getClass().getName() +
       " cannot be an argument to a query operator of type " +
       this.getClass().getName());
  }

  /**
   *  Advance the internal document iterator beyond the specified
   *  document.
   *  @param docid An internal document id.
   */
  public void docIteratorAdvancePast (int docid) {

      for (Qry q_i: this.args) {
        q_i.docIteratorAdvancePast (docid);
      }

    this.docIteratorClearMatchCache ();
    }

  /**
   *  Advance the internal document iterator to the specified
   *  document, or beyond if it doesn't.
   *  @param docid An internal document id.
   */
  public void docIteratorAdvanceTo (int docid) {
    
    for (Qry q_i: this.args) {
      q_i.docIteratorAdvanceTo (docid);
    }
    
    this.docIteratorClearMatchCache ();
  }

  /**
   *  Clear the docIterator's matching docid cache.  The cache should
   *  be cleared whenever a docIterator is advanced.
   */
  private void docIteratorClearMatchCache () {
    this.docIteratorMatchCache = Qry.INVALID_DOCID;
  }

  /**
   *  Return the id of the document that the iterator points to now.
   *  Use docIteratorHasMatch to determine whether the iterator
   *  currently points to a document.  If the iterator doesn't point
   *  to a document, an invalid document id is returned.
   *  @return The internal id of the current document.
   */
  public int docIteratorGetMatch () {
    if (this.docIteratorHasMatchCache ()) {
      return this.docIteratorMatchCache;
    } else {
      throw new IllegalStateException("No matching docid was cached.");
    }
  }

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public abstract boolean docIteratorHasMatch (RetrievalModel r);

  /**
   *  An instantiation of docIteratorHasMatch that is true if the
   *  query has a document that matches all query arguments; some
   *  subclasses may choose to use this implementation.  
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  protected boolean docIteratorHasMatchAll (RetrievalModel r) {

    boolean matchFound = false;

    // Keep trying until a match is found or no match is possible.

    while (! matchFound) {

      // Get the docid of the first query argument.
      
      Qry q_0 = this.args.get (0);

      if (! q_0.docIteratorHasMatch (r)) {
	return false;
      }

      int docid_0 = q_0.docIteratorGetMatch ();

      // Other query arguments must match the docid of the first query
      // argument.
      
      matchFound = true;

      for (int i=1; i<this.args.size(); i++) {
	Qry q_i = this.args.get(i);

	q_i.docIteratorAdvanceTo (docid_0);

	if (! q_i.docIteratorHasMatch (r)) {	// If any argument is exhausted
	  return false;				// there are no more matches.
	}

	int docid_i = q_i.docIteratorGetMatch ();

	if (docid_0 != docid_i) {	// docid_0 can't match.  Try again.
	  q_0.docIteratorAdvanceTo (docid_i);
	  matchFound = false;
	  break;
	}
      }

      if (matchFound) {
        docIteratorSetMatchCache (docid_0);
      }
    }

    return true;
  }

  /**
   *  An instantiation of docIteratorHasMatch that is true if the
   *  query has a document that matches the first query argument;
   *  some subclasses may choose to use this implementation.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  protected boolean docIteratorHasMatchFirst (RetrievalModel r) {

    Qry q_0 = this.args.get(0);

    if (q_0.docIteratorHasMatch (r)) {
      int docid = q_0.docIteratorGetMatch ();
      this.docIteratorSetMatchCache (docid);
      return true;
    } else {
      return false;
    }
  }

  /**
   *  An instantiation of docIteratorHasMatch that is true if the
   *  query has a document that matches at least one query argument;
   *  the match is the smallest docid to match; some subclasses may
   *  choose to use this implementation.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  protected boolean docIteratorHasMatchMin (RetrievalModel r) {

    int minDocid = Qry.INVALID_DOCID;

    for (int i=0; i<this.args.size(); i++) {
      Qry q_i = this.args.get(i);

      if (q_i.docIteratorHasMatch (r)) {
        int q_iDocid = q_i.docIteratorGetMatch ();

        if ((minDocid > q_iDocid) ||
            (minDocid == Qry.INVALID_DOCID)) {
          minDocid = q_iDocid;
        }
      }
    }

    if (minDocid != Qry.INVALID_DOCID) {
      docIteratorSetMatchCache (minDocid);
      return true;
    } else {
      return false;
    }
  }

  /**
   *  Return the status of the cache.
   *  @return True if a match is cached, otherwise false.
   */
  protected boolean docIteratorHasMatchCache () {
    return (this.docIteratorMatchCache != Qry.INVALID_DOCID);
  }

  /**
   *  Set the matching docid cache.
   *  @param docid The internal document id to store in the cache.
   */
  private void docIteratorSetMatchCache (int docid) {
    this.docIteratorMatchCache = docid;
  }

  /**
   *  Get the i'th query argument.  The main value of this method
   *  is that it casts the argument to the correct type.
   *  @param i The index of the query argument to return.
   *  @return The query argument.
   */
  public QryIop getArg (int i) {
    return ((QryIop) this.args.get(i));
  }

  /**
   *  Every operator has a display name that can be used by
   *  toString for debugging or other user feedback.  
   *  @return The query operator's display name
   */
  public String getDisplayName () {
    return this.displayName;
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators; this method must be called before iteration
   *  can begin.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public abstract void initialize(RetrievalModel r) throws IOException;

  /**
   *  Removes an argument from the list of query operator arguments.
   *  @param i The index of the query operator to remove.
   */
  public void removeArg (int i) {
    this.args.remove(i);
  };

  /**
   *  Every operator must have a display name that can be used by
   *  toString for debugging or other user feedback.  
   *  @param name The query operator's display name
   */
  public void setDisplayName (String name) {
    this.displayName = new String (name);
  }

  /**
   *  Get a string version of this query operator.  This is a generic
   *  method that works for most query operators.  However, some query
   *  operators (e.g., #NEAR/n or #WEIGHT) may need to override this
   *  method with something more specific.
   *  @return The string version of this query operator.
   */
  @Override public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i) + " ";

    return (this.displayName + "( " + result + ")");
  }

}
