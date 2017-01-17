import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class FeatureVector {
	Set<Integer> featureDisable = new HashSet<>();
	// Map<queryid, Map<external_id, relevance_score>>
	Map<Integer, Map<String, Integer>> relMap = new HashMap<>();
	// Map<external_id, PageRank_score>
	Map<String, Double> prMap = new HashMap<>();
	static int featureNum = 19;
	static double[] max = new double[featureNum];
	static double[] min = new double[featureNum];

	public FeatureVector(RetrievalModelLetor model) {
		// initilize featureDisable set
		if (model.featureDisable != null) {
			for (String s : model.featureDisable.split(",")) {
				this.featureDisable.add(Integer.parseInt(s));
			}
		}
	}

	private void initilizeMaxMin() {
		min = new double[featureNum];
		max = new double[featureNum];
		for (int i = 0; i < featureNum; i++) {
			min[i] = Double.MAX_VALUE;
			// cannot use Double.MIN_VALUE which is greater than 0
			max[i] = -Double.MAX_VALUE;
		}
	}

	/**
	 * Returns PageRank Map. Map<external_id, PageRank_score>
	 * 
	 * @param prFile
	 * @return
	 * @throws IOException
	 */
	public void setPageRank(String prFile) throws IOException {
		BufferedReader input = null;
		try {
			String line = null;

			input = new BufferedReader(new FileReader(prFile));

			while ((line = input.readLine()) != null) {
				line = line.trim();
				int d = line.indexOf("\t");
				String externalDocid = line.substring(0, d);
				double pr = Double.parseDouble(line.substring(d + 1));
				prMap.putIfAbsent(externalDocid, pr);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
		}
	}

	/**
	 * Returns relevance Map. Map<queryid, Map<external_id, relevance_score>>
	 * 
	 * @param relFile
	 * @return
	 * @throws IOException
	 */
	public void setRel(String relFile) throws IOException {
		BufferedReader input = null;
		try {
			String line = null;

			input = new BufferedReader(new FileReader(relFile));

			while ((line = input.readLine()) != null) {
				line = line.trim();
				// first space
				int d1 = line.indexOf(" ");
				int qid = Integer.parseInt(line.substring(0, d1));
				// second space
				int d2 = line.indexOf(" ", d1 + 1);
				// third space
				int d3 = line.indexOf(" ", d2 + 1);
				String externalDocid = line.substring(d2 + 1, d3);
				int rel = Integer.parseInt(line.substring(d3 + 1));
				this.relMap.putIfAbsent(qid, new HashMap<String, Integer>());
				this.relMap.get(qid).put(externalDocid, rel);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
		}
	}

	private void normHelper(int i, double score) {
		if (score == Double.MIN_VALUE)
			return;
		max[i] = Math.max(max[i], score);
		min[i] = Math.min(min[i], score);
	}

	private Map<Integer, String> getQuery(String trainingQueryFile) throws IOException {
		Map<Integer, String> queryMap = new HashMap<Integer, String>();
		BufferedReader input = null;
		try {
			String line = null;
			input = new BufferedReader(new FileReader(trainingQueryFile));
			while ((line = input.readLine()) != null) {
				int d = line.indexOf(':');
				int qid = Integer.parseInt(line.substring(0, d));
				String query = line.substring(d + 1);
				queryMap.putIfAbsent(qid, query);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
		}
		return queryMap;
	}

	public void setRel(Map<Integer, Map<String, Integer>> relMap) {
		this.relMap = relMap;
	}

	private List<String> tokenizeQuery(String query) throws IOException {
		List<String> stems = new ArrayList<String>();
		String[] terms = query.split("\\s+");
		for (String term : terms) {
			String[] tokens = QryParser.tokenizeString(term);
			for (String t : tokens) {
				stems.add(t);
			}
		}
		return stems;
	}

	public Map<Integer, List<String>> getFeatures(RetrievalModelLetor model, int type) throws Exception {
		BufferedWriter output = null;
		QrySopScore sop = new QrySopScore();
		Map<Integer, String> queryMap = null;
		List<Integer> sortedQid = null;
		Map<Integer, List<String>> docList = new TreeMap<>();

		if (type == 0) {
			queryMap = getQuery(model.trainingQueryFile);
		} else if (type == 1) {
			queryMap = getQuery(model.queryFilePath);
		}
		// sort query id in qMap
		sortedQid = new ArrayList<Integer>(queryMap.size());
		sortedQid.addAll(queryMap.keySet());
		Collections.sort(sortedQid);

		Map<String, Map<Integer, Double>> vecMap = new HashMap<String, Map<Integer, Double>>();

		try {

			output = (type == 0) ? new BufferedWriter(new FileWriter(model.trainingFeatureVectorsFile))
					: new BufferedWriter(new FileWriter(model.testingFeatureVectorsFile));

			// Each pass of the loop processes one query.
			for (int qid : sortedQid) {
				String query = queryMap.get(qid);
				List<String> qTerms = tokenizeQuery(query);
				initilizeMaxMin();

				List<String> docs = new ArrayList<>();

				// System.out.println("Query " + query);
				Iterator<Entry<String, Integer>> iter = this.relMap.get(qid).entrySet().iterator();
				while (iter.hasNext()) {
					Entry<String, Integer> entry = iter.next();
					String externalDocid = entry.getKey();
					docs.add(externalDocid);

					int docid = Idx.getInternalDocid(externalDocid);
					if (docid == -1)
						continue;
					// vector for one document
					Map<Integer, Double> vec = new HashMap<>();

					// f1: Spam score for d (read from index).
					// Hint: The spam score is stored in your index as the score
					// attribute.
					// (We know
					// that this is a terrible name. Sorry.)
					// int spamScore = Integer.parseInt (Idx.getAttribute
					// ("score",
					// docid));
					if (!this.featureDisable.contains(1)) {
						Double spamScore = Double.MIN_VALUE;
						spamScore = Double.parseDouble(Idx.getAttribute("score", docid));
						normHelper(1, spamScore);
						vec.put(1, spamScore);
					}

					String rawUrl = Idx.getAttribute("rawUrl", docid).replaceAll("http://", "");

					// f2: Url depth for d(number of '/' in the rawUrl field).
					// Hint: The raw URL is stored in your index as the rawUrl
					// attribute.
					// String rawUrl = Idx.getAttribute ("rawUrl", docid);
					if (!this.featureDisable.contains(2)) {
						double urlDepth = Double.MIN_VALUE;
						for (int i = 0; i < rawUrl.length(); i++) {
							if (rawUrl.charAt(i) == '/') {
								urlDepth++;
							}
						}
						// double urlDepth = rawUrl.length() -
						// rawUrl.replaceAll("/", "").length();
						// if (rawUrl.endsWith("/"))
						// urlDepth--;
						normHelper(2, urlDepth);
						vec.put(2, urlDepth);
					}
					// f3: FromWikipedia score for d (1 if the rawUrl contains
					// "wikipedia.org",
					// otherwise 0).
					if (!this.featureDisable.contains(3)) {
						double wikiScore = rawUrl.contains("wikipedia.org") ? 1 : 0;
						// String rawUrl = Idx.getAttribute ("rawUrl", docid);
						normHelper(3, wikiScore);
						vec.put(3, wikiScore);
					}
					// f4: PageRank score for d (read from file).
					if (!this.featureDisable.contains(4)) {
						double pageRank = Double.MIN_VALUE;
						if (this.prMap.containsKey(externalDocid)) {
							pageRank = this.prMap.get(externalDocid);
						}
						normHelper(4, pageRank);
						vec.put(4, pageRank);
					}

					String[] fields = { "body", "title", "url", "inlink" };
					// f5-f16
					for (int i = 0; i < fields.length; i++) {
						// f5: BM25 score for <q, field>.
						if (!this.featureDisable.contains(5 + i * 3)) {

							double score = sop.getScoreBM25(model, docid, fields[i], qTerms);
							normHelper(5 + i * 3, score);
							vec.put(5 + i * 3, score);
						}
						// f6: Indri score for <q, dbody>.
						if (!this.featureDisable.contains(6 + i * 3)) {
							double score = sop.getScoreIndri(model, docid, fields[i], qTerms);
							normHelper(6 + i * 3, score);
							vec.put(6 + i * 3, score);
						}
						// f7: Term overlap score for <q, dbody>.
						if (!this.featureDisable.contains(7 + i * 3)) {
							double score = sop.getScoreOverlap(docid, fields[i], qTerms);
							normHelper(7 + i * 3, score);
							vec.put(7 + i * 3, score);
							// System.out.println("Overlap0");
						}
					}

					// f17: A custom feature - use your imagination.
					// tfidf score
					if (!this.featureDisable.contains(17)) {
						double score = sop.getScoreOverlap(docid, "body", qTerms);
						normHelper(17, score);
						vec.put(17, score);
					}
					// f18: A custom feature - use your imagination.
					if (!this.featureDisable.contains(18)) {
						double score = sop.getScoreOverlap(docid, "body", qTerms);
						normHelper(18, score);
						vec.put(18, score);
					}
					// System.out.println("aa");
					vecMap.put(externalDocid, vec);
				}
				docList.put(qid, docs);
				// normalize
				Iterator<Entry<String, Map<Integer, Double>>> vecIter = vecMap.entrySet().iterator();
				Map<String, Integer> relScores = this.relMap.get(qid);
				while (vecIter.hasNext()) {
					Entry<String, Map<Integer, Double>> entry = vecIter.next();
					String externalDocid = entry.getKey();
					int relScore = relScores.containsKey(externalDocid) ? relScores.get(externalDocid) : 0;
					StringBuilder str = new StringBuilder(String.format("%d\tqid:%d", relScore, qid));
					Map<Integer, Double> featureVec = entry.getValue();
					for (int idx = 1; idx < featureNum; idx++) {
						double minVal = min[idx];
						double maxVal = max[idx];
						double normVal = 0;
						double score = featureVec.containsKey(idx) ? featureVec.get(idx) : Double.MIN_VALUE;
						if (score != Double.MIN_VALUE) {
							normVal = (maxVal == minVal) ? 0 : ((score - minVal) / (maxVal - minVal));
						} 
						str.append(String.format("\t%d:%.14f", idx, normVal));
					}
					str.append(String.format("\t#\t%s\n", externalDocid));
					// System.out.print(str.toString());
					output.write(str.toString());
				}

			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			output.close();
			return docList;
		}

	}

}
