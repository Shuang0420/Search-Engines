/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.MultiFields;

/**
 * DocLengthStore is used to access the document lengths of indexed docs.
 */
public class DocLengthStore  {

  private IndexReader reader;
  private  Map<String, NumericDocValues> values = new HashMap<String, NumericDocValues>();

  /**
   * @param reader IndexReader object created in {@link Idx}.
   * @throws IOException Error accessing the Lucene index.
   */
  public DocLengthStore(IndexReader reader) throws IOException {
    this.reader = reader;
    for (String field : MultiFields.getIndexedFields(reader)) {
      this.values.put(field, MultiDocValues.getNormValues(reader, field));      
    }
  }

  /**
   * Returns the length of the specified field in the specified document.
   *
   * @param fieldname Name of field to access lengths. "body" is the default
   * field.
   * @param docid The internal docid in the lucene index.
   * @return long The length of the field.
   * @throws IOException Error accessing the Lucene index.
   */
  public long getDocLength(String fieldname, int docid) throws IOException {
    return values.get(fieldname).get(docid);
  }
}
