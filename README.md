# Search-Engines
- Implemented a text-based large scale search engine indexed with Lucene API on corpus of 500,000+ documents from ClueWeb09 dataset.
- Developed a custom search engine with diversification capabilities, query expansion capabilities and learning to rank capability. Trained a SVM classifier to rank documents by learning from manually assessed relevance judgments, using document dependent (tfidf and scores from different retrieval models, etc.) and document independent features (pageRank, spamScore, etc.).
- Supported retrieval algorithms/models including Unranked/Ranked Boolean, Okapi BM25, language statistic model like Indri, Le2R and etc.
- Evaluated the models developed by varying parameter values, analyzed trends, ambiguities discovered from the conducted experiments
