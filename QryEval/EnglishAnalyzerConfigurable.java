/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

/**
 * {@link Analyzer} for English.
 */
public final class EnglishAnalyzerConfigurable extends StopwordAnalyzerBase {

  public enum StemmerType {
    NONE, PORTER, KSTEM
  };

  private final CharArraySet stemExclusionSet;
  private Boolean doLowerCase = true;
  private Boolean doStopwordRemoval = true;
  private StemmerType stemmer = StemmerType.PORTER;

  /**
   * Returns an unmodifiable instance of the default stop words set.
   * 
   * @return default stop words set.
   */
  public static CharArraySet getDefaultStopSet() {
    return DefaultSetHolder.DEFAULT_STOP_SET;
  }

  /**
   * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
   * class accesses the static final set the first time.;
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
  }

  /**
   * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
   * @param matchVersion
   *          lucene compatibility version
   */
  public EnglishAnalyzerConfigurable(Version matchVersion) {
    this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
  }

  /**
   * Builds an analyzer with the given stop words.
   * 
   * @param matchVersion
   *          lucene compatibility version
   * @param stopwords
   *          a stopword set
   */
  public EnglishAnalyzerConfigurable(Version matchVersion, CharArraySet stopwords) {
    this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
  }

  /**
   * Builds an analyzer with the given stop words. If a non-empty stem exclusion
   * set is provided this analyzer will add a {@link SetKeywordMarkerFilter}
   * before stemming.
   * 
   * @param matchVersion
   *          lucene compatibility version
   * @param stopwords
   *          a stopword set
   * @param stemExclusionSet
   *          a set of terms not to be stemmed
   */
  public EnglishAnalyzerConfigurable(Version matchVersion, CharArraySet stopwords,
      CharArraySet stemExclusionSet) {
    super(matchVersion, stopwords);
    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion,
        stemExclusionSet));
  }

  /**
   * Enable or disable the conversion of text to lower case.
   * @param onOff If true, convert text to lower case, otherwise don't.
   */
  public void setLowercase(Boolean onOff) {
    this.doLowerCase = onOff;
  }

  /**
   * Enable or disable stopword removal.
   * @param onOff If true, remove stopwords, otherwise don't.
   */
  public void setStopwordRemoval(Boolean onOff) {
    this.doStopwordRemoval = onOff;
  }

  /**
   * Control whether and how stemming is done. See StemmerType.
   * @param s the stemmer type
   */
  public void setStemmer(StemmerType s) {
    this.stemmer = s;
  }

  /**
   * Creates a {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * which tokenizes all the text in the provided {@link Reader}.
   * 
   * @return A {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from an {@link StandardTokenizer} filtered with
   *         {@link StandardFilter}, {@link EnglishPossessiveFilter},
   *         {@link LowerCaseFilter}, {@link StopFilter} ,
   *         {@link SetKeywordMarkerFilter} if a stem exclusion set is provided
   *         and {@link PorterStemFilter}.
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
    final Tokenizer source = new StandardTokenizer(matchVersion, reader);
    TokenStream result = new StandardFilter(matchVersion, source);
    // prior to this we get the classic behavior, standardfilter does it for us.

    result = new EnglishPossessiveFilter(matchVersion, result);

    if (this.doLowerCase)
      result = new LowerCaseFilter(matchVersion, result);

    if (this.doStopwordRemoval)
      result = new StopFilter(matchVersion, result, stopwords);

    if (!stemExclusionSet.isEmpty())
      result = new SetKeywordMarkerFilter(result, stemExclusionSet);

    if (this.stemmer == StemmerType.PORTER)
      result = new PorterStemFilter(result);
    else if (this.stemmer == StemmerType.KSTEM)
      result = new KStemFilter(result);

    return new TokenStreamComponents(source, result);
  }
}
