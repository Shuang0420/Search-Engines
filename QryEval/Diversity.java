import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Diversity {
    // map<qid, map<docid, list<score>>>
    private Map<Integer, Map<Integer, List<Double>>> iniRankingMap = new HashMap();
    // initial query's score list, map<qid,List<docid>>
    private Map<Integer, List<Integer>> iniRanking = new HashMap();

    // map<qid,list<query>>
    private Map<Integer, List<String>> queryIntentMap = new HashMap();
    // map<qid, list<sum for qi>>
    private Map<Integer, List<Double>> maxMap = new HashMap();
    private double lambda;
    private int maxInputRankingsLength;
    private int maxResultRankingLength;
    boolean norm = false;

    Diversity(String initialRankingFile, String intentsFile, String lambda, String maxInputRankingsLength,
            String maxResultRankingLength) throws Exception {
        this.lambda = Double.parseDouble(lambda);
        this.maxInputRankingsLength = Integer.parseInt(maxInputRankingsLength);
        this.maxResultRankingLength = Integer.parseInt(maxResultRankingLength);
        // test();
        // // read document rankings for query q from initialRankingFile
        setQueryIntentMap(intentsFile);
        // System.out.println(Arrays.asList(queryIntentMap));
        // // read document rankings for query qi from initialRankingFile
        readRankingMap(initialRankingFile);
        // System.out.println("size " + iniRankingMap.get(157).size());
        // System.out.println(Arrays.asList(iniRankingMap));
        // System.out.println(Arrays.asList(maxMap));
    }

    Diversity(String intentsFile, String lambda, String maxInputRankingsLength, String maxResultRankingLength)
            throws Exception {
        this.lambda = Double.parseDouble(lambda);
        this.maxInputRankingsLength = Integer.parseInt(maxInputRankingsLength);
        this.maxResultRankingLength = Integer.parseInt(maxResultRankingLength);
        setQueryIntentMap(intentsFile);
    }

    public List<String> getQueryList(String queryFilePath) throws IOException {
        BufferedReader input = null;
        // List<String> newQueryList = new ArrayList<>();
        ArrayList<String> initialQueryList = new ArrayList<>();
        try {
            String qLine = null;

            input = new BufferedReader(new FileReader(queryFilePath));

            // Each pass of the loop processes one query.

            while ((qLine = input.readLine()) != null) {
                int d = qLine.indexOf(':');

                if (d < 0) {
                    throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                }

                // int qid = Integer.parseInt(qLine.substring(0, d));
                // String query = qLine.substring(d + 1);

                initialQueryList.add(qLine);
                // queryIntents = (ArrayList) queryIntentMap.get(qid);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
        System.out.println("initial query" + Arrays.asList(initialQueryList));
        return initialQueryList;
    }
    // new query file
    // iniRankingMap

    public void setQueryIntentMap(String intentsFile) throws IOException {
        BufferedReader input = null;
        try {
            String line = null;
            input = new BufferedReader(new FileReader(intentsFile));
            int lastQid = -1;
            while ((line = input.readLine()) != null) {
                int d = line.indexOf(':');
                String queryPart = line.substring(0, d);
                int dot = queryPart.indexOf(".");
                if (dot > 0) {
                    int qid = Integer.parseInt(queryPart.substring(0, dot));
                    if (lastQid == -1)
                        lastQid = qid;
                    if (qid != lastQid) {
                        setMaxMap(lastQid);
                        lastQid = qid;
                    }
                    queryIntentMap.putIfAbsent(qid, new ArrayList<>());
                    ArrayList qiS = (ArrayList) queryIntentMap.get(qid);
                    // String query = line.substring(d + 1);
                    qiS.add(line);
                    queryIntentMap.put(qid, qiS);
                }
            }
            // the last one
            setMaxMap(lastQid);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
    }

    public void setMaxMap(int qid) {
        int size = queryIntentMap.get(qid).size();
        List<Double> iniMaxVals = new ArrayList(size);
        for (int i = 0; i <= size; i++) {
            iniMaxVals.add(0.0);
        }
        maxMap.put(qid, iniMaxVals);
    }

    public void normDocScores(int qid, List<Double> maxVals) {
        System.out.println("maxVals " + Arrays.asList(maxVals));
        Map<Integer, List<Double>> map = iniRankingMap.get(qid);
        Iterator<Entry<Integer, List<Double>>> iter = map.entrySet().iterator();
        double normFactor = Collections.max(maxVals);
        System.out.println("normFactor " + normFactor);
        System.out.println("doc#" + map.size());
        // iterate documents
        while (iter.hasNext()) {
            Entry<Integer, List<Double>> entry = iter.next();
            int docid = entry.getKey();
            List<Double> scores = entry.getValue();
            // System.out.println("ini scores " + Arrays.asList(scores));
            for (int i = 0; i < scores.size(); i++) {
                scores.set(i, scores.get(i) / normFactor);
            }
            // System.out.println("norm scores " + Arrays.asList(scores));
            map.put(docid, scores);
        }
        iniRankingMap.put(qid, map);

    }

    public void normDocScores() {
        for (Entry<Integer, Map<Integer, List<Double>>> entry : iniRankingMap.entrySet()) {
            int qid = entry.getKey();
            List<Double> maxVals = maxMap.get(qid);
            Map<Integer, List<Double>> map = iniRankingMap.get(qid);

            System.out.println("qid " + qid + " maxVals " + Arrays.asList(maxVals));

            // Iterator<Entry<Integer, List<Double>>> iter =
            // map.entrySet().iterator();
            double normFactor = Collections.max(maxVals);
            System.out.println("normFactor " + normFactor);
            System.out.println("doc#" + map.size());
            // iterate documents
            for (Entry<Integer, List<Double>> e : map.entrySet()) {
                int docid = e.getKey();
                List<Double> scores = e.getValue();
                // System.out.println("ini scores " + Arrays.asList(scores));
                for (int i = 0; i < scores.size(); i++) {
                    scores.set(i, scores.get(i) / normFactor);
                }
                // System.out.println("norm scores " + Arrays.asList(scores));
                map.put(docid, scores);
            }
            iniRankingMap.put(qid, map);
        }
    }

    public void readRankingMap(String initialRankingFile) throws Exception {
        BufferedReader input = null;
        // map<initial qid, scoreList>
        Map<String, ScoreList> initialQueryResList = new TreeMap<>();
        Map<String, ScoreList> queryIntentResList = new TreeMap<>();
        String lastQid = null;
        ScoreList scoreList = null;
        boolean initialQuery = true;
        try {
            String line = null;
            input = new BufferedReader(new FileReader(initialRankingFile));
            while ((line = input.readLine()) != null) {
                line = line.trim();
                line = line.replace("\t", " ");
                // first space
                int d1 = line.indexOf(" ");
                String qid = line.substring(0, d1);

                // check if in the same group
                if (!qid.equals(lastQid) && lastQid != null) {
                    scoreList.truncate(this.maxInputRankingsLength);
                    if (initialQuery == true) {
                        initialQueryResList.put(lastQid, scoreList);
                    } else {
                        queryIntentResList.put(lastQid, scoreList);
                    }
                }

                if (!qid.equals(lastQid)) {
                    scoreList = new ScoreList();
                    lastQid = qid;
                }

                int dot = qid.indexOf(".");
                // check if it is query intents
                if (dot > 0) {
                    qid = qid.substring(0, dot);
                    initialQuery = false;
                } else {
                    initialQuery = true;
                }

                // get doc id

                // second space
                int d2 = line.indexOf(" ", d1 + 1);
                // third space
                int d3 = line.indexOf(" ", d2 + 1);
                // document
                // String externalDocid = line.substring(d2 + 1, d3);
                int docid = Idx.getInternalDocid(line.substring(d2 + 1, d3));

                // get doc score

                // 4th space
                int d4 = line.indexOf(" ", d3 + 1);
                // 5th space
                int d5 = line.indexOf(" ", d4 + 1);
                double score = Double.parseDouble(line.substring(d4 + 1, d5));
                scoreList.add(docid, score);
            }
            scoreList.truncate(this.maxInputRankingsLength);
            if (initialQuery == true) {
                initialQueryResList.put(lastQid, scoreList);
            } else {
                queryIntentResList.put(lastQid, scoreList);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }

        // System.out.println(initialQueryResList.keySet());
        // System.out.println(queryIntentResList.keySet());
        // System.out.println("iniRankingMap " + initialQueryResList);
        setCandidateDocuments(initialQueryResList);
        // System.out.println("queryIntentResList " + queryIntentResList);
         setQueryIntentDocuments(queryIntentResList);

    }

    public void setIniRankingMap(String initialRankingFile) throws Exception {
        BufferedReader input = null;
        // boolean norm = false;
        try {
            String line = null;
            input = new BufferedReader(new FileReader(initialRankingFile));
            int lastQid = -1;
            List<Double> maxVals = null;
            // int count = 0;
            boolean initialQuery = true;
            List<Integer> allowedDocs = new ArrayList<>();

            while ((line = input.readLine()) != null) {
                line = line.trim();
                line = line.replace("\t", " ");
                // first space
                int d1 = line.indexOf(" ");
                String queryPart = line.substring(0, d1);
                int dot = queryPart.indexOf(".");
                int qi = 0;
                int qid = -1;
                // check if it is query intents
                if (dot > 0) {
                    qi = Integer.parseInt(queryPart.substring(dot + 1));
                    qid = Integer.parseInt(queryPart.substring(0, dot));
                    initialQuery = false;
                } else {
                    // it is original query
                    qid = Integer.parseInt(queryPart);
                }
                if (qid != lastQid) {
                    iniRankingMap.put(qid, new HashMap<>());
                    // count = 0;
                    if (lastQid != -1) {
                        maxMap.put(lastQid, maxVals);
                    }
                    lastQid = qid;
                    // System.out.println(lastQid+ " enter
                    // "+Arrays.asList(maxMap));
                    maxVals = maxMap.get(lastQid);
                }

                // get doc id

                // second space
                int d2 = line.indexOf(" ", d1 + 1);
                // third space
                int d3 = line.indexOf(" ", d2 + 1);
                // document
                // String externalDocid = line.substring(d2 + 1, d3);
                int docid = Idx.getInternalDocid(line.substring(d2 + 1, d3));

                // if (dot < 0 && (!allowedDocs.contains(docid))) {
                // continue;
                // }

                // get doc score

                // 4th space
                int d4 = line.indexOf(" ", d3 + 1);
                // 5th space
                int d5 = line.indexOf(" ", d4 + 1);
                double score = Double.parseDouble(line.substring(d4 + 1, d5));
                if (score > 1.0) {
                    norm = true;
                }

                // // // if original query
                // if (dot > 0 && count < this.maxInputRankingsLength) {
                // allowedDocs.add(docid);
                // count++;
                // }
                double newScore = score + maxVals.get(qi);
                maxVals.set(qi, newScore);
                // put
                // map<docid, list<score>>
                HashMap docScoreList = (HashMap) iniRankingMap.get(lastQid);
                ArrayList<Double> scoreList = null;
                if (docScoreList.containsKey(docid)) {
                    // list<score>
                    scoreList = (ArrayList) docScoreList.get(docid);
                } else {
                    // System.out.println("maxVaal "+Arrays.asList(maxVals));
                    scoreList = new ArrayList();
                    // initialize all qi's score with 0
                    int size = queryIntentMap.get(lastQid).size();
                    for (int i = 0; i <= size; i++) {
                        scoreList.add(0.0);
                    }
                }
                scoreList.set(qi, score);
                docScoreList.put(docid, scoreList);
            }
            // the last one
            maxMap.put(lastQid, maxVals);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
        }
    }

    public void test() {
        // private Map<Integer, Map<Integer, List<Double>>> iniRankingMap = new
        // HashMap();
        // // map<qid,list<query>>
        // private Map<Integer, List<String>> queryIntentMap = new HashMap();
        HashMap<Integer, List<Double>> map = new HashMap<>();
        ArrayList list = new ArrayList();
        list.add(0.7);
        list.add(0.7);
        list.add(0.2);
        map.put(1, list);

        list = new ArrayList();
        list.add(0.69);
        list.add(0.8);
        list.add(0.1);
        map.put(2, list);

        list = new ArrayList();
        list.add(0.68);
        list.add(0.6);
        list.add(0.3);
        map.put(3, list);

        list = new ArrayList();
        list.add(0.67);
        list.add(0.2);
        list.add(0.7);
        map.put(4, list);

        list = new ArrayList();
        list.add(0.66);
        list.add(0.3);
        list.add(0.8);
        map.put(5, list);
        iniRankingMap.put(0, map);
        lambda = 0.4;

        list = new ArrayList();
        list.add("q1");
        list.add("q2");
        queryIntentMap.put(0, list);

        // private double lambda;
    }

    public ScoreList xQuAD(int qid) {
        ScoreList divScoreList = new ScoreList();
        int qSize = queryIntentMap.get(qid).size();
        // p(qi/q)
        double intentWeight = 1.0 / qSize;
        // docid,list<score>
        int docSize = iniRankingMap.get(qid).size();
        HashMap<Integer, List<Double>> docList = (HashMap) iniRankingMap.get(qid);
        HashMap<Integer, List<Double>> scoreList = new HashMap<Integer, List<Double>>();
        String str = null;
        while (divScoreList.size() < docSize) {
            Iterator<Entry<Integer, List<Double>>> iter = docList.entrySet().iterator();
            // System.out.println("===============================");
            // store max value in this iteration
            double maxScore = -1;
            int maxScoreDoc = -1;
            ArrayList maxQiScore = null;
            // iterate documents
            while (iter.hasNext()) {
                Entry<Integer, List<Double>> entry = iter.next();
                int docid = entry.getKey();
                ArrayList qiScore = (ArrayList) entry.getValue();
                // calculate query intent score part
                double intentScore = 0;
                // iterate scores for each query intent for a given document
                for (int i = 1; i < qiScore.size(); i++) {
                    // get current coverage, how well scoreList already covers
                    // intent qi
                    double currentCoverage = 1;
                    // iterate scoreList
                    for (Integer rankedDocid : scoreList.keySet()) {
                        currentCoverage *= (1 - scoreList.get(rankedDocid).get(i));
                    }
                    intentScore += intentWeight * (double) qiScore.get(i) * currentCoverage;
                }

                double score = (1 - lambda) * ((double) qiScore.get(0)) + lambda * intentScore;
                // System.out
                // .println((1 - lambda) + "*" + qiScore.get(0) + "+" + lambda +
                // "*" + intentScore + "=" + score);
                // update max value
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreDoc = docid;
                    maxQiScore = qiScore;
                    // str=(1 - lambda) + "*" + qiScore.get(0) + "+" + lambda +
                    // "*" + intentScore + "=" + score;
                }
            }
            // System.out.println(maxScoreDoc+"\t"+maxQiScore+"\t"+str);
            scoreList.put(maxScoreDoc, maxQiScore);
            docList.remove(maxScoreDoc);
            divScoreList.add(maxScoreDoc, maxScore);
        }
        // System.out.println(divScoreList);
        return divScoreList;
    }

    public ScoreList PM2(int qid) {
        ScoreList divScoreList = new ScoreList();
        HashMap<Integer, List<Double>> docList = (HashMap) iniRankingMap.get(qid);
        // HashMap<Integer, List<Double>> scoreList = new HashMap<Integer,
        // List<Double>>();
        // get doc list size
        int docSize = iniRankingMap.get(qid).size();
        // query intent size
        int qSize = queryIntentMap.get(qid).size();
        // p(qi/q)
        double intentWeight = 1.0 / qSize;
        // votes
        double vi = intentWeight * this.maxResultRankingLength;
        // slots
        double[] s = new double[qSize];
        // quotientScores
        double[] qt = new double[qSize];
        // initialize slots
        for (int i = 0; i < qSize; i++) {
            s[i] = 0;
        }

        // PM2 scores

        ArrayList<Double> lastQiScoreList = null;
        // iterate
        while (divScoreList.size() < docSize) {
            double maxScore = -1;
            int maxScoreDoc = -1;
            ArrayList maxQiScore = null;
            double curMaxQt = -Double.MAX_VALUE;
            int indexArgmaxQt = -1;

            // selected intent
            double sumScore = 0;
            // iterate scoreList and get total score for all qis as
            // denominator
            if (divScoreList.size() > 0) {
                for (int j = 1; j < lastQiScoreList.size(); j++) {
                    sumScore += lastQiScoreList.get(j);
                }
            }

            // update s, qt and argmax qt
            for (int i = 0; i < qSize; i++) {
                // update s
                // ! ignore original query, only calculate query intents
                if (divScoreList.size() > 0 && sumScore != 0) {
                    s[i] += lastQiScoreList.get(i + 1) / sumScore;
                } else {
                    s[i] = 0;
                }
                // update qt
                qt[i] = vi / (2 * s[i] + 1);
                // update argmax qt
                if (qt[i] > curMaxQt) {
                    curMaxQt = qt[i];
                    indexArgmaxQt = i;
                }
                // System.out.println("sumScore " + sumScore + "s " +
                // Arrays.toString(s));
                // System.out.println("qt " + Arrays.toString(qt));
                // System.out.println("i " + i + "curMaxQt " + curMaxQt + " qti
                // " + qt[i]);
            }
            double[] noMaxQt = new double[qSize];
            System.arraycopy(qt, 0, noMaxQt, 0, qSize);
            // System.out.println("indexArgmaxQt " + indexArgmaxQt);
            noMaxQt[indexArgmaxQt] = -1;

            // iterate documents
            Iterator<Entry<Integer, List<Double>>> iter = docList.entrySet().iterator();

            // String str = null;
            while (iter.hasNext()) {
                Entry<Integer, List<Double>> entry = iter.next();
                int docid = entry.getKey();
                ArrayList<Double> qiScore = (ArrayList) entry.getValue();
                double score = 0;
                double coverQi = 0;
                double coverOther = 0;

                // System.arraycopy(qt, indexArgmaxQt + 2, noMaxQt,
                // indexArgmaxQt, qSize - indexArgmaxQt - 1);
                coverQi = lambda * qt[indexArgmaxQt] * qiScore.get(indexArgmaxQt + 1);

                for (int i = 0; i < noMaxQt.length; i++) {
                    if (noMaxQt[i] == -1) {
                        continue;
                    }
                    coverOther += noMaxQt[i] * qiScore.get(i + 1);
                }
                coverOther *= (1 - lambda);
                score = coverQi + coverOther;

                if (score > maxScore) {
                    maxScore = score;
                    maxScoreDoc = docid;
                    maxQiScore = qiScore;
                    // str = lambda + "*" + qt[indexArgmaxQt] + "*" +
                    // qiScore.get(indexArgmaxQt + 1) + "+" + (1 - lambda)
                    // + "*" + coverOther;
                }

                // System.out.println("vi \t" + vi);
                // System.out.println("qt \t" + Arrays.toString(qt));
                // System.out.println("s \t" + Arrays.toString(s));
                // System.out.println(curMaxQt);
                for (int i = 0; i < noMaxQt.length; i++) {
                    if (noMaxQt[i] == -1) {
                        continue;
                    }
                    // System.out.println(lambda + "*" + qt[indexArgmaxQt] + "*"
                    // + qiScore.get(indexArgmaxQt + 1) + "+"
                    // + (1 - lambda) + "*" + coverOther);
                }
                // System.out.println("score " + score);

            }
            // System.out.println("maxScoreDoc " + maxScoreDoc + " maxScore " +
            // maxScore + "\t" + str);
            // System.out.println("===========================================================");
            // scoreList.put(maxScoreDoc, maxQiScore);
            lastQiScoreList = (ArrayList<Double>) docList.get(maxScoreDoc);
            docList.remove(maxScoreDoc);
            divScoreList.add(maxScoreDoc, maxScore);
            if (maxScore == 0) {
                break;
            }
        }
        // divScoreList.sort();
        return divScoreList;
    }

    /**
     * initiate candidate documents
     * 
     * @param intermediateOutput
     */
    public void setCandidateDocuments(Map<String, ScoreList> intermediateOutput) {
        // boolean norm = false;

        for (Entry<String, ScoreList> entry : intermediateOutput.entrySet()) {
            int qid = Integer.parseInt(entry.getKey());

            ScoreList sl = entry.getValue();
            // System.out.println("scoreList " + sl.toString());

            double sumScore = 0;
            HashMap<Integer, List<Double>> docScoreList = new HashMap<>();
            int qiSize = queryIntentMap.get(qid).size();
            List<Double> maxVals = new ArrayList<>(qiSize);

            // iterate scorelist
            for (int i = 0; i < sl.size(); i++) {
                // get doc id
                int docid = sl.getDocid(i);

                // get doc score
                double score = sl.getDocidScore(i);
                if (score > 1.0) {
                    norm = true;
                }

                // update q0 max
                sumScore += score;

                // set scorelist for this doc
                ArrayList<Double> scoreList = new ArrayList<>(qiSize + 1);
                scoreList.add(score);
                // initialize all qi's score with 0
                for (int j = 0; j < qiSize; j++) {
                    scoreList.add(0.0);
                }
                docScoreList.put(docid, scoreList);
            }
            // handle this qid
            iniRankingMap.put(qid, docScoreList);
            maxVals.add(sumScore);
            // initialize all qi's sum value with 0
            for (int j = 0; j < qiSize; j++) {
                maxVals.add(0.0);
            }
            maxMap.put(qid, maxVals);
        }
    }

    public void setQueryIntentDocuments(Map<String, ScoreList> intermediateOutput) {
        List<Double> maxVals = null;

        for (Entry<String, ScoreList> entry : intermediateOutput.entrySet()) {
            String queryPart = entry.getKey();
            int dot = queryPart.indexOf(".");
            int qid = -1;
            int qi = 0;
            // check if it is query intents
            if (dot > 0) {
                qi = Integer.parseInt(queryPart.substring(dot + 1));
                qid = Integer.parseInt(queryPart.substring(0, dot));
            }
            maxVals = this.maxMap.get(qid);
            Map<Integer, List<Double>> docScoreList = this.iniRankingMap.get(qid);
            // System.out.println("doc lenth "+docScoreList.size());
            // System.out.println("qid " + qid + Arrays.asList(docScoreList));
            double sumScore = 0;

            ScoreList sl = entry.getValue();
            // System.out.println("scoreList " + sl.toString());
            // iterate scorelist for this qid
            for (int i = 0; i < sl.size(); i++) {
                // get doc id
                int docid = sl.getDocid(i);

                if (!docScoreList.containsKey(docid)) {
                    continue;
                }

                // get doc score
                double score = sl.getDocidScore(i);

                sumScore += score;

                List<Double> scoreList = docScoreList.get(docid);

                scoreList.set(qi, score);
                docScoreList.put(docid, scoreList);
            }

            // handle this qid
            iniRankingMap.put(qid, docScoreList);
            // System.out.println(qi+" "+sumScore);
            maxVals.set(qi, sumScore);
            maxMap.put(qid, maxVals);
        }

        System.out.println("norm " + norm);
        if (norm == true) {
            normDocScores();
        }
    }

    
    public String getNewQueryFile(String queryFilePath, RetrievalModel model) throws IOException {
        BufferedReader input = null;
        BufferedWriter output = null;
        BufferedWriter bw = null;
        String newQueryFile = "newQueryFile";

        try {
            String qLine = null;

            input = new BufferedReader(new FileReader(queryFilePath));
            output = new BufferedWriter(new FileWriter(newQueryFile));

            // Each pass of the loop processes one query.

            while ((qLine = input.readLine()) != null) {
                int d = qLine.indexOf(':');

                if (d < 0) {
                    throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                }

                int qid = Integer.parseInt(qLine.substring(0, d));
                String query = qLine.substring(d + 1);

                System.out.println("Query " + qLine);
                output.write(qLine);
                output.newLine();
                ArrayList<String> queryIntents = (ArrayList) queryIntentMap.get(qid);
                System.out.println("query intents " + Arrays.asList(queryIntents));
                for (String qi : queryIntents) {
                    output.write(qi);
                    output.newLine();
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            input.close();
            output.close();
        }
        return newQueryFile;

    }
}
