/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

/**
 *  A simple utility for inspecting Lucene indexes.  Run it to see a
 *  simple usage message.
 */
public class InspectIndex {

    static String usage =
	"Usage:  java " +
	System.getProperty("sun.java.command") +
	" -index INDEX_PATH\n\n" +
	"where options include\n" +
        "    -list-docids\tlist the external docids of each document\n" +
        "    -list-edocid IDOCID\n" +
        "\t\t\tlist the external docid of the document\n" +
        "\t\t\twith internal docid of IDOCID\n" +
	"    -list-fields\tlist the fields in the index\n" +
	"    -list-postings TERM FIELD\n" +
	"\t\t\tdisplay the posting list entries for\n" +
	"\t\t\tterm TERM in field FIELD\n" +
	"    -list-postings-sample TERM FIELD\n" +
	"\t\t\tdisplay the first few posting list entries for\n" +
	"\t\t\tterm TERM in field FIELD\n" +
	"    -list-stats\n" +
	"\t\t\tdisplay corpus statistics\n" +
	"    -list-terms FIELD" +
	"\tdisplay the term dictionary for field FIELD\n" +
	"    -list-termvector DOCID\n" +
	"\t\t\tdisplay the term vectors for all fields in the document\n" +
	"\t\t\twith internal DOCID\n" +
	"    -list-termvector-field DOCID FIELD\n" +
	"\t\t\tdisplay the term vector for FIELD in the document\n" +
	"\t\t\twith internal DOCID\n";

    public static void main(String[] args) throws IOException {

	IndexReader reader = null;

	/*
	 *  Opening the index first simplifies the processing of the
	 *  rest of the command line arguments.
	 */
	for (int i=0; i < args.length; i++) {
	    if (("-index".equals (args[i])) &&
		((i+1) < args.length)) {
		reader = DirectoryReader.open (
			     FSDirectory.open (new File (args[i+1])));

		if (reader == null) {
		    System.err.println ("Error:  Can't open index " +
					args[i+1]);
		    System.exit (1);
		};

		break;
	    };
	};

	if (reader == null) {
	    System.err.println (usage);
	    System.exit (1);
	};

	/*
	 *  Process the command line arguments sequentially.
	 */
	for (int i=0; i < args.length; i++) {

	    if ("-index".equals(args[i])) {

		/*
		 *  Handled in the previous loop, so just skip the argument.
		 */
		i++;

	    } else if ("-list-edocid".equals(args[i])) {

	      System.out.println  ("-list-edocid:");
	      
	      if ((i+1) >= args.length) {
		System.out.println (usage);
		break;
	      };

	      Document d = reader.document (Integer.parseInt (args[i+1]));

	      System.out.println ("Internal docid --> External docid: " +
				  args[i+1] + " --> " + d.get ("externalId"));

	      i += 1;
	    } else if ("-list-docids".equals(args[i])) {

	      System.out.println  ("-list-docids:");
	      
	      for (int j=0; j<reader.numDocs(); j++) {
		Document d = reader.document (j);
		System.out.println ("Internal --> external docid: " +
				    j + " --> " + d.get ("externalId"));
	      };

	    } else if ("-list-fields".equals(args[i])) {

		Fields fields = MultiFields.getFields (reader);

		System.out.print ("\nNumber of fields:  ");

		if (fields == null)
		    System.out.println ("0");
		else {
		    System.out.println (fields.size());

		    Iterator<String> is = fields.iterator();

		    while (is.hasNext()) {
			System.out.println ("\t" + is.next());
		    };
		};

	    } else if ("-list-postings".equals(args[i])) {

		if ((i+2) >= args.length) {
		    System.out.println (usage);
		    break;
		};

		listPostings (reader, args[i+1], args [i+2],
			      Integer.MAX_VALUE);
		i += 2;

	    } else if ("-list-postings-sample".equals(args[i])) {

		if ((i+2) >= args.length) {
		    System.out.println (usage);
		    break;
		};

		listPostings (reader, args[i+1], args [i+2], 5);
		i += 2;

	    } else if ("-list-stats".equals(args[i])) {

	      System.out.println ("Corpus statistics:");
	      System.out.println ("\tnumdocs\t\t" + reader.numDocs());
	      System.out.println ("\turl:\t" +
				  "\tnumdocs=" +
				  reader.getDocCount ("url") +
				  "\tsumTotalTF=" +
				  reader.getSumTotalTermFreq("url") +
				  "\tavglen="+
				  reader.getSumTotalTermFreq("url") /
				  (float) reader.getDocCount ("url"));

	      System.out.println ("\tkeywords:" +
				  "\tnumdocs=" +
				  reader.getDocCount ("keywords") +
				  "\tsumTotalTF=" +
				  reader.getSumTotalTermFreq("keywords") +
				  "\tavglen="+
				  reader.getSumTotalTermFreq("keywords") /
				  (float) reader.getDocCount ("keywords"));

	      System.out.println ("\ttitle:\t" +
				  "\tnumdocs=" +
				  reader.getDocCount ("title") +
				  "\tsumTotalTF=" +
				  reader.getSumTotalTermFreq("title") +
				  "\tavglen="+
				  reader.getSumTotalTermFreq("title") /
				  (float) reader.getDocCount ("title"));

	      System.out.println ("\tbody:\t" +
				  "\tnumdocs=" +
				  reader.getDocCount ("body") +
				  "\tsumTotalTF=" +
				  reader.getSumTotalTermFreq("body") +
				  "\tavglen="+
				  reader.getSumTotalTermFreq("body") /
				  (float) reader.getDocCount ("body"));

	      System.out.println ("\tinlink:\t" +
				  "\tnumdocs=" +
				  reader.getDocCount ("inlink") +
				  "\tsumTotalTF=" +
				  reader.getSumTotalTermFreq("inlink") +
				  "\tavglen="+
				  reader.getSumTotalTermFreq("inlink") /
				  (float) reader.getDocCount ("inlink"));

	    } else if ("-list-terms".equals(args[i])) {

		if ((i+1) >= args.length) {
		    System.out.println (usage);
		    break;
		};

		listTermDictionary (reader, args[i+1]);
		i += 1;

	    } else if ("-list-termvector".equals(args[i])) {

		if ((i+1) >= args.length) {
		    System.out.println (usage);
		    break;
		};

		listTermVectors (reader, args[i+1]);
		i += 1;

	    } else if ("-list-termvector-field".equals(args[i])) {

		if ((i+2) >= args.length) {
		    System.out.println (usage);
		    break;
		};

		listTermVectorField (reader, args[i+1], args[i+2]);
		i += 2;

	    } else
		System.err.println ("\nWarning:  Unknown argument " + args[i]
				    + " ignored.");
	};

	/*
	 *  Close the index and exit gracefully.
	 */
        reader.close();
    }

    /*
     *  listPostings displays the first n postings for a term in a
     *  field in an index (specified by reader).  Set n to MAX_VALUE
     *  to display all postings.
     */
    static void listPostings (IndexReader reader, String termString,
			      String field, Integer n) throws IOException  {

	System.out.println ("\nPostings:  " + termString + " " + field);

	/*
	 *  Prepare to access the index.
	 */
	BytesRef termBytes = new BytesRef (termString);
	Term term = new Term (field, termBytes);
	Bits liveDocs = MultiFields.getLiveDocs (reader);

	/*
	 *  Lookup the collection term frequency (ctf).
	 */
	long df = reader.docFreq (term);
	System.out.println ("\tdf:  " + df);

	long ctf = reader.totalTermFreq (term);
	System.out.println ("\tctf:  " + ctf);

	if (df < 1)
	    return;

	/*
	 *  Lookup the inverted list.
	 */
	DocsAndPositionsEnum postings =
	    MultiFields.getTermPositionsEnum (reader, liveDocs, field,
					      termBytes); 

	/*
	 *  Iterate through the first n postings.
	 */
	long count = 0;

	while ((count < n) &&
	       (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS)) {
	    
	    System.out.println ("\tdocid: " + postings.docID());
	    int tf = postings.freq();
	    System.out.println("\ttf: " + tf);
	    System.out.print("\tPositions: ");

	    for (int j=0; j<tf; j++) {
		int pos = postings.nextPosition();
		System.out.print (pos + " ");
	    }
			
	    System.out.println ("");
	    
	    count++;
	};

	return;
    }

    /*
     *  listTermDictionary displays the term dictionary for a field.
     */
    static void listTermDictionary (IndexReader reader, String fieldName)
	throws IOException {

	System.out.println ("\nTerm Dictionary:  field " + fieldName);

	/*
	  Grant says:
	  MultiFields.getTerms(IndexReader, fieldName)
	*/

	Terms terms = MultiFields.getTerms(reader, fieldName);

	if ((terms == null) ||
	    (terms.size () == -1))
	    System.out.println ("    The term dictionary is empty.");
	else {
	    System.out.println ("    Vocabulary size: " +
				terms.size () + " terms");
	    
	    TermsEnum ithTerm = terms.iterator (null);

	    /*
	     *  Iterate over the terms in this document.
	     *  Information about a term's occurrences (tf and
	     *  positions) is accessed via the indexing API, which
	     *  returns inverted lists that describe (only) the
	     *  current document.
	     */
	    while (ithTerm.next() != null){
		System.out.format ("      %-30s %d %d\n",
				   ithTerm.term().utf8ToString(),
				   ithTerm.docFreq (),
				   ithTerm.totalTermFreq ());

	    };
	};
    }

    /*
     *  listTermVectors displays the term vectors for all of the fields
     *  in a document in an index (specified by reader).
     */
    static void listTermVectors (IndexReader reader, String docidString)
	throws IOException {

	System.out.println ("\nTermVector:  docid " + docidString);

	int docid = Integer.parseInt (docidString);

	if ((docid < 0) ||
	    (docid >= reader.numDocs ())) {
		System.out.println ("ERROR:  " +
				    docidString + " is a bad document id.");
		return;
	};

	/*
	 *  Iterate over the fields in this document.
	 */
	Fields fields = reader.getTermVectors (docid);
	Iterator<String> fieldIterator = fields.iterator ();

	while (fieldIterator.hasNext()) {
	    String fieldName = fieldIterator.next();
	    System.out.println ("  Field: " + fieldName);

	    Terms terms = fields.terms (fieldName);
	    termVectorDisplay (terms);
	};
    }

    /*
     *  listTermVectorField displays the term vector for a field in
     *  a document in an index (specified by reader).
     */
    static void listTermVectorField (IndexReader reader,
				     String docidString,
				     String field) throws IOException {

	System.out.println ("\nTermVector:  docid " +
			    docidString + ", field " + field);

	int docid = Integer.parseInt (docidString);

	if ((docid < 0) ||
	    (docid >= reader.numDocs ())) {
		System.out.println ("ERROR:  " +
				    docidString + " is a bad document id.");
		return;
	};

	Terms terms = reader.getTermVector (docid, field);
	termVectorDisplay (terms);
    }

    /*
     *  Utility function to display a term vector.
     */
    static void termVectorDisplay (Terms terms) throws IOException {

	if ((terms == null) ||
	    (terms.size () == -1))
	    System.out.println ("    The field is not stored.");
	else {
	    /*
	     *  The terms for this field are stored.
	     */
	    System.out.println ("    Vocabulary size: " +
				terms.size () + " terms");
	    
	    TermsEnum ithTerm = terms.iterator (null);

	    /*
	     *  Iterate over the terms in this document.
	     *  Information about a term's occurrences (tf and
	     *  positions) is accessed via the indexing API, which
	     *  returns inverted lists that describe (only) the
	     *  current document.
	     */
	    while (ithTerm.next() != null){
		System.out.format ("      %10d %-20s %d ",
				   ithTerm.ord(),
				   ithTerm.term().utf8ToString(),
				   ithTerm.totalTermFreq ());

		DocsAndPositionsEnum currDoc = ithTerm.docsAndPositions (null, null);
		currDoc.nextDoc ();

		for (int jthPosition=0; jthPosition<ithTerm.totalTermFreq(); jthPosition++)
		    System.out.print (currDoc.nextPosition () + " ");

		System.out.println ();
	    };
	};
    }
}
