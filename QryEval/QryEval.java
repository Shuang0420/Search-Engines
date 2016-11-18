
/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * This software illustrates the architecture for the portion of a search engine
 * that evaluates queries. It is a guide for class homework assignments, so it
 * emphasizes simplicity over efficiency. It implements an unranked Boolean
 * retrieval model, however it is easily extended to other retrieval models. For
 * more information, see the ReadMe.txt file.
 */
public class QryEval {

	// --------------- Constants and variables ---------------------

	private static final String USAGE = "Usage:  java QryEval paramFile\n\n";

	private static final String[] TEXT_FIELDS = { "body", "title", "url", "inlink" };

	private static Map<String, String> parameters = null;

	// --------------- Methods ---------------------------------------

	/**
	 * @param args
	 *            The only argument is the parameter file name.
	 * @throws Exception
	 *             Error accessing the Lucene index.
	 */
	public static void main(String[] args) throws Exception {

		// This is a timer that you may find useful. It is used here to
		// time how long the entire program takes, but you can move it
		// around to time specific parts of your code.

		Timer timer = new Timer();
		timer.start();

		// Check that a parameter file is included, and that the required
		// parameters are present. Just store the parameters. They get
		// processed later during initialization of different system
		// components.

		if (args.length < 1) {
			throw new IllegalArgumentException(USAGE);
		}

		parameters = readParameterFile(args[0]);

		// Open the index and initialize the retrieval model.

		Idx.open(parameters.get("indexPath")); // Open a Lucene index and the
												// associated DocLengthStore.
		RetrievalModel model = initializeRetrievalModel();

		// Perform experiments.
		if (!(model instanceof RetrievalModelLetor)) {
			processQueryFile(parameters.get("queryFilePath"), parameters.get("trecEvalOutputPath"), model);
		}
		timer.stop();
		System.out.println("Time:  " + timer);
	}

	/**
	 * @param score_list
	 * @return
	 * @throws IOException
	 */
	private static String expandQuery(ScoreList score_list) throws IOException {
		/**
		 * class InvertedObject{ double mle; ArrayList<Integer> doc_list; public
		 * InvertedObject(double mle,ArrayList<Integer> doc_list) {
		 * this.doc_list=doc_list; this.mle=mle; } }
		 */

		double fbMu = Double.parseDouble(parameters.get("fbMu"));
		int fbDocs = Integer.parseInt(parameters.get("fbDocs"));
		int fbTerms = Integer.parseInt(parameters.get("fbTerms"));
		int docNum = Math.min(fbDocs, score_list.size());
		double cLength = Idx.getSumOfFieldLengths("body");
		// Map<String, InvertedObject> invertedList = new HashMap();
		Map<String, ArrayList<Integer>> invertedList = new HashMap();
		Map<String, Double> mleList = new HashMap();
		// map<term, score>
		Map<String, Double> termScore = new HashMap();
		// get expanded term
		for (int i = 0; i < docNum; i++) {
			int doc_id = score_list.getDocid(i);
			TermVector vec = new TermVector(doc_id, "body");
			// termVecMap.put(doc_id, vec);
			// p(I/d)
			double docScore = score_list.getDocidScore(i);
			// System.out.println("doc_id "+doc_id+" doc score "+docScore);
			double docLen = Idx.getFieldLength("body", doc_id);
			// for each term
			for (int j = 1; j < vec.stemsLength(); j++) {
				String term = vec.stemString(j);
				// ignore any candidate expansion term that contains a period
				// ('.') or a comma (',')
				if (term.contains(".") || term.contains(",")) {
					continue;
				}
				// score potential expansion term for current doc
				long tf = vec.stemFreq(j);
				long ctf = vec.totalStemFreq(j);
				double mle = ctf / cLength;
				// p(t|d)
				double Ptd = (tf + fbMu * mle) / (docLen + fbMu);
				// a form of idf
				double idf = Math.log(1 / mle);
				double cur_doc_score = Ptd * docScore * idf;
				if (termScore.containsKey(term)) {
					termScore.put(term, termScore.get(term) + cur_doc_score);
				} else {
					termScore.put(term, cur_doc_score);
				}
				// update inverted list for current term
				if (invertedList.containsKey(term)) {
					ArrayList<Integer> cur_inverted_list = invertedList.get(term);
					cur_inverted_list.add(doc_id);
					invertedList.put(term, cur_inverted_list);
				} else {
					ArrayList<Integer> cur_inverted_list = new ArrayList();
					cur_inverted_list.add(doc_id);
					invertedList.put(term, cur_inverted_list);
				}
				mleList.putIfAbsent(term, mle);
				/**
				 * // update inverted list for current term if
				 * (invertedList.containsKey(term)) { InvertedObject obj =
				 * invertedList.get(term); obj.doc_list.add(doc_id);
				 * invertedList.put(term, obj); } else { ArrayList
				 * <Integer> cur_doc_list = new ArrayList();
				 * cur_doc_list.add(doc_id); invertedList.put(term, new
				 * InvertedObject(mle,cur_doc_list)); }
				 */
			}
		}

		// update score for docs where tf=0
		for (String term : invertedList.keySet()) {
			// InvertedObject obj = invertedList.get(term);
			ArrayList<Integer> doc_list = invertedList.get(term);
			for (int i = 0; i < docNum; i++) {
				int doc_id = score_list.getDocid(i);
				// if (obj.doc_list.contains(doc_id)) continue;
				if (doc_list.contains(doc_id))
					continue;
				// if docid is not in inverted list
				TermVector vec = new TermVector(doc_id, "body");
				double docScore = score_list.getDocidScore(i);
				double docLen = Idx.getFieldLength("body", doc_id);
				long tf = 0;
				// double mle = obj.mle;
				double mle = mleList.get(term);
				// p(t|d)
				double Ptd = (tf + fbMu * mle) / (docLen + fbMu);
				// a form of idf
				double idf = Math.log(1 / mle);
				double cur_doc_score = Ptd * docScore * idf;
				if (termScore.containsKey(term)) {
					termScore.put(term, termScore.get(term) + cur_doc_score);
				} else {
					termScore.put(term, cur_doc_score);
				}
			}
		}

		// get top k terms
		PriorityQueue<Map.Entry<String, Double>> termScorePq = new PriorityQueue<Map.Entry<String, Double>>(
				termScore.size(), new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Map.Entry<String, Double> m1, Map.Entry<String, Double> m2) {
						return m2.getValue().compareTo(m1.getValue());
					}
				});
		termScorePq.addAll(termScore.entrySet());
		System.out.println("term size " + termScorePq.size());

		// get new query
		String expandedQuery = "#wand ( ";
		for (int i = 0; i < fbTerms; i++) {
			String score = String.format("%.4f", termScorePq.peek().getValue());
			String term = termScorePq.peek().getKey();
			expandedQuery = expandedQuery + " " + score + " " + term;
			termScorePq.poll();
		}
		expandedQuery += " )";
		System.out.println("expandedQuery " + expandedQuery);
		return expandedQuery;
	}

	private static void printExpandedQuery(BufferedWriter bw, String qid, String expandedQry) throws IOException {
		bw.write(qid + ": " + expandedQry + "\n");
		// bw.close();
	}

	/**
	 * 
	 * @param fbInitialRankingFile
	 * @return
	 */
	private static Map<Integer, ScoreList> readRankingFile(String fbInitialRankingFile) {
		// System.out.println("filename "+fbInitialRankingFile);
		Map<Integer, ScoreList> scoreList_map = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fbInitialRankingFile))) {
			String str;
			int last_qry = -1;
			ScoreList score_list = new ScoreList();
			while ((str = br.readLine()) != null) {
				String[] data = str.split(" ");
				int cur_qry = Integer.parseInt(data[0].trim());
				if (last_qry == -1) {
					last_qry = cur_qry;
				}
				if (cur_qry != last_qry) {
					scoreList_map.put(last_qry, score_list);
					last_qry = cur_qry;
					score_list = new ScoreList();
				}
				score_list.add(Idx.getInternalDocid(data[2].trim()), Double.parseDouble(data[4].trim()));
			}
			// add the last query and scorelist
			scoreList_map.put(last_qry, score_list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return scoreList_map;
	}

	/**
	 * Allocate the retrieval model and initialize it using parameters from the
	 * parameter file.
	 * 
	 * @return The initialized retrieval model
	 * @throws Exception
	 */
	private static RetrievalModel initializeRetrievalModel() throws Exception {

		RetrievalModel model = null;
		String modelString = parameters.get("retrievalAlgorithm").toLowerCase();

		if (modelString.equals("unrankedboolean")) {
			model = new RetrievalModelUnrankedBoolean();
		} else if (modelString.equals("rankedboolean")) {
			model = new RetrievalModelRankedBoolean();
		} else if (modelString.equals("bm25")) {
			float k_1 = Float.parseFloat(parameters.get("BM25:k_1"));
			float b = Float.parseFloat(parameters.get("BM25:b"));
			float k_3 = Float.parseFloat(parameters.get("BM25:k_3"));
			assert k_1 >= 0.0 && b >= 0.0 && b <= 1.0 && k_3 >= 0;
			model = new RetrievalModelBM25(k_1, b, k_3);
		} else if (modelString.equals("indri")) {
			float mu = Float.parseFloat(parameters.get("Indri:mu"));
			float lambda = Float.parseFloat(parameters.get("Indri:lambda"));
			assert mu >= 0 && lambda >= 0 && lambda <= 1.0;
			model = new RetrievalModelIndri(mu, lambda);
		} else if (modelString.equals("letor")) {
			model = new RetrievalModelLetor(parameters);
			// initialize training feature vector file
			FeatureVector fVec = new FeatureVector((RetrievalModelLetor) model);
			fVec.setPageRank(((RetrievalModelLetor) model).pageRankFile);
			fVec.setRel(((RetrievalModelLetor) model).trainingQrelsFile);
			fVec.getFeatures((RetrievalModelLetor) model, 0);
			trainSVM((RetrievalModelLetor) model);
			System.out.println("SVM training completed");

			// initialize test feature vector file
			RetrievalModel bm25Model = new RetrievalModelBM25(((RetrievalModelLetor) model).k_1,
					((RetrievalModelLetor) model).b, ((RetrievalModelLetor) model).k_3);
			Map<Integer, Map<String, Integer>> relMap = processQueryFile(parameters.get("queryFilePath"), bm25Model);
			fVec.setRel(relMap);
			Map<Integer, List<String>> docList = fVec.getFeatures((RetrievalModelLetor) model, 1);

			testSVM((RetrievalModelLetor) model);
			System.out.println("SVM test completed");

			getLetorScore(((RetrievalModelLetor) model).testingDocumentScores, parameters.get("trecEvalOutputPath"),
					model, docList);
		} else {
			throw new IllegalArgumentException("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
		}

		return model;
	}

	public static void trainSVM(RetrievalModelLetor model) throws Exception {
		Process cmdProc = Runtime.getRuntime().exec(new String[] { model.svmRankLearnPath, "-c",
				String.valueOf(model.svmRankParamC), model.trainingFeatureVectorsFile, model.svmRankModelFile });
		consume(cmdProc);
	}

	public static void testSVM(RetrievalModelLetor model) throws Exception {
		Process cmdProc = Runtime.getRuntime().exec(new String[] { model.svmRankClassifyPath,
				model.testingFeatureVectorsFile, model.svmRankModelFile, model.testingDocumentScores });
		consume(cmdProc);
	}

	public static void consume(Process cmdProc) throws Exception {
		// The stdout/stderr consuming code MUST be included.
		// It prevents the OS from running out of output buffer space and
		// stalling.

		// consume stdout and print it out for debugging purposes
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
		String line;
		while ((line = stdoutReader.readLine()) != null) {
			System.out.println(line);
		}
		// consume stderr and print it for debugging purposes
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
			System.out.println(line);
		}

		// get the return value from the executable. 0 means success, non-zero
		// indicates a problem
		int retValue = cmdProc.waitFor();
		if (retValue != 0) {
			throw new Exception("SVM Rank crashed.");
		}
	}

	static Queue<Double> readScores(String testingDocumentScores) throws IOException {
		BufferedReader input = null;
		// List<Double> svmScores = new ArrayList<>();
		Queue<Double> svmScores = new ArrayDeque<>();

		try {
			String docScore = null;

			input = new BufferedReader(new FileReader(testingDocumentScores));

			int count = 0;
			while ((docScore = input.readLine()) != null) {
				count++;
				svmScores.offer(Double.parseDouble(docScore.trim()));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
			return svmScores;
		}
	}

	static void getLetorScore(String testingDocumentScores, String trecEvalOutputPath, RetrievalModel model,
			Map<Integer, List<String>> docList) throws Exception {
		ArrayDeque<Double> svmScores = (ArrayDeque<Double>) readScores(testingDocumentScores);
		Iterator<Entry<Integer, List<String>>> iter = docList.entrySet().iterator();
		BufferedWriter output = new BufferedWriter(new FileWriter(trecEvalOutputPath));

		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> docs = entry.getValue();
			int len = docs.size();
			ScoreList r = new ScoreList();
			List<Double> qScores = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				// System.out.println(entry.getKey() + " " +
				// svmScores.peekFirst());
				// System.out.println(docs.get(i));
				// System.out.println(Idx.getInternalDocid(docs.get(i)));
				if (!svmScores.isEmpty()) {
					r.add(Idx.getInternalDocid(docs.get(i)), svmScores.pollFirst());
				} else {
					System.out.println("Empty");
				}
			}
			if (r != null) {
				printResults(String.valueOf(entry.getKey()), r, output);
				System.out.println();
			}
		}
		output.close();
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 *
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc)
			runtime.gc();

		System.out
				.println("Memory used:  " + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
	}

	/**
	 * Process one query.
	 * 
	 * @param qString
	 *            A string that contains a query.
	 * @param model
	 *            The retrieval model determines how matching and scoring is
	 *            done.
	 * @return Search results
	 * @throws IOException
	 *             Error accessing the index
	 */
	static ScoreList processQuery(String qString, RetrievalModel model) throws IOException {

		QryParser.initiate();
		// System.out.println("qString " + qString);
		Qry q = QryParser.getQuery(qString); // get query tree
		// Show the query that is evaluated
		System.out.println(q.args.size());
		System.out.println("    --> " + q);

		if (q != null) {
			ScoreList r = new ScoreList();
			if (q.args.size() > 0) { // Ignore empty queries
				q.initialize(model);
				while (q.docIteratorHasMatch(model)) {
					int docid = q.docIteratorGetMatch();
					double score = ((QrySop) q).getScore(model);
					if (score >= 0)
						r.add(docid, score);
					q.docIteratorAdvancePast(docid);
				}
			}
			return r;
		} else
			return null;
	}

	static Map<Integer, Map<String, Integer>> processQueryFile(String queryFilePath, RetrievalModel model)
			throws Exception {

		BufferedReader input = null;
		Map<Integer, Map<String, Integer>> relMap = new HashMap<>();
		try {
			String qLine = null;

			input = new BufferedReader(new FileReader(queryFilePath));

			// Each pass of the loop processes one query.

			while ((qLine = input.readLine()) != null) {
				int d = qLine.indexOf(':');

				if (d < 0) {
					throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
				}

				printMemoryUsage(false);

				String qid = qLine.substring(0, d);
				String query = qLine.substring(d + 1);

				System.out.println("Query " + qLine);

				ScoreList r = null;

				String defaultOp = model.defaultQrySopName();
				query = defaultOp + "(" + query + ")";
				r = processQuery(query, model);

				Map<String, Integer> topDocs = new HashMap<String, Integer>();

				if (r != null) {
					r.sort();
					int result_range = 100;
					if (r.size() < 100)
						result_range = r.size();
					if (r.size() < 1) {
						System.out.println("\tNo results.");
					} else {
						for (int i = 0; i < result_range; i++) {
							topDocs.put(Idx.getExternalDocid(r.getDocid(i)), 0);
						}
					}
				}
				relMap.putIfAbsent(Integer.parseInt(qid), topDocs);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
			return relMap;
		}
	}

	/**
	 * Process the query file.
	 * 
	 * @param queryFilePath
	 * @param model
	 * @throws Exception
	 */
	static void processQueryFile(String queryFilePath, String trecEvalOutputPath, RetrievalModel model)
			throws Exception {

		BufferedReader input = null;
		BufferedWriter output = null;
		BufferedWriter bw = null;

		try {
			String qLine = null;

			input = new BufferedReader(new FileReader(queryFilePath));
			output = new BufferedWriter(new FileWriter(trecEvalOutputPath));
			bw = new BufferedWriter(new FileWriter(parameters.get("fbExpansionQueryFile")));

			// Each pass of the loop processes one query.

			while ((qLine = input.readLine()) != null) {
				int d = qLine.indexOf(':');

				if (d < 0) {
					throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
				}

				printMemoryUsage(false);

				String qid = qLine.substring(0, d);
				String query = qLine.substring(d + 1);

				System.out.println("Query " + qLine);

				ScoreList r = null;

				String defaultOp = model.defaultQrySopName();
				query = defaultOp + "(" + query + ")";
				// if not expand query
				if (!(parameters.containsKey("fb") && parameters.get("fb").equals("true"))) {
					r = processQuery(query, model);
				} else { // if expand query
					// check parameters
					if (!(parameters.containsKey("fbTerms") && parameters.containsKey("fbMu")
							&& parameters.containsKey("fbOrigWeight")
							&& parameters.containsKey("fbExpansionQueryFile"))) {
						throw new IllegalArgumentException("Required parameters were missing from the parameter file.");
					}
					// check if there's ranking file
					if (!parameters.containsKey("fbInitialRankingFile")) {
						r = processQuery(query, model);
						r.sort();
					} else {
						Map<Integer, ScoreList> score_list_map = readRankingFile(
								parameters.get("fbInitialRankingFile"));
						if (!score_list_map.containsKey(Integer.parseInt(qid))) {
							throw new Exception("No query " + qid + " in ranking file!");
						}
						r = score_list_map.get(Integer.parseInt(qid));
					}
					// r.sort();
					String expandedQuery = expandQuery(r);
					printExpandedQuery(bw, qid, expandedQuery);
					double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
					String newQuery = "#wand (" + String.valueOf(fbOrigWeight) + " " + query + " "
							+ String.valueOf(1 - fbOrigWeight) + " " + expandedQuery + " )";
					// System.out.println(" new Query " + newQuery);
					r = processQuery(newQuery, model);
				}

				if (r != null) {
					printResults(qid, r, output);
					System.out.println();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			input.close();
			output.close();
			bw.close();
		}
	}

	/**
	 * Print the query results.
	 *
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 *
	 * QueryID Q0 DocID Rank Score RunID
	 *
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            A list of document ids and scores
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	static void printResults(String queryName, ScoreList result, BufferedWriter output) throws IOException {
		System.out.println(queryName + ":  ");
		result.sort();
		int result_range = 100;
		if (result.size() < 100)
			result_range = result.size();
		if (result.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			for (int i = 0; i < result_range; i++) {
				// System.out.println("\t" + i + ": " +
				// Idx.getExternalDocid(result.getDocid(i)) + ", "
				// + result.getDocidScore(i));
				// format: QueryID Q0 DocID Rank Score RunID
				String str = String.format("%s\t%s\t%s\t%d\t%s\t%s\n", queryName, "Q0",
						Idx.getExternalDocid(result.getDocid(i)), i + 1, result.getDocidScore(i), "runID");
				System.out.print(str);
				output.write(str);
			}
		}
	}

	/**
	 * Read the specified parameter file, and confirm that the required
	 * parameters are present. The parameters are returned in a HashMap. The
	 * caller (or its minions) are responsible for processing them.
	 * 
	 * @return The parameters, in <key, value> format.
	 */
	private static Map<String, String> readParameterFile(String parameterFileName) throws IOException {

		Map<String, String> parameters = new HashMap<String, String>();

		File parameterFile = new File(parameterFileName);

		if (!parameterFile.canRead()) {
			throw new IllegalArgumentException("Can't read " + parameterFileName);
		}

		Scanner scan = new Scanner(parameterFile);
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			parameters.put(pair[0].trim(), pair[1].trim());
		} while (scan.hasNext());

		scan.close();

		if (!(parameters.containsKey("indexPath") && parameters.containsKey("queryFilePath")
				&& parameters.containsKey("trecEvalOutputPath") && parameters.containsKey("retrievalAlgorithm"))) {
			throw new IllegalArgumentException("Required parameters were missing from the parameter file.");
		}

		return parameters;
	}

}
