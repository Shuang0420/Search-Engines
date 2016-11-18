import java.util.Map;

public class RetrievalModelLetor extends RetrievalModel {
	String queryFilePath;
	String trainingQueryFile;
	String trainingQrelsFile;
	String trainingFeatureVectorsFile;
	String pageRankFile;
	String featureDisable;
	String svmRankLearnPath;
	String svmRankClassifyPath;
	String svmRankModelFile;
	String testingFeatureVectorsFile;
	String testingDocumentScores;
	double k_1, b, k_3;
	double mu, lambda;
	double svmRankParamC;

	public RetrievalModelLetor(Map<String, String> parameters) {
		this.k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
		this.b = Double.parseDouble(parameters.get("BM25:b"));
		this.k_3 = Double.parseDouble(parameters.get("BM25:k_3"));
		assert k_1 >= 0.0 && b >= 0.0 && b <= 1.0 && k_3 >= 0;
		this.mu = Double.parseDouble(parameters.get("Indri:mu"));
		this.lambda = Double.parseDouble(parameters.get("Indri:lambda"));
		assert mu >= 0 && lambda >= 0 && lambda <= 1.0;
		this.trainingQueryFile = parameters.get("letor:trainingQueryFile");
		this.trainingQrelsFile = parameters.get("letor:trainingQrelsFile");
		this.trainingFeatureVectorsFile = parameters.get("letor:trainingFeatureVectorsFile");
		this.pageRankFile = parameters.get("letor:pageRankFile");
		this.featureDisable = parameters.get("letor:featureDisable");
		this.svmRankParamC = Double.parseDouble(parameters.get("letor:svmRankParamC"));
		this.svmRankLearnPath = parameters.get("letor:svmRankLearnPath");
		this.svmRankClassifyPath = parameters.get("letor:svmRankClassifyPath");
		this.svmRankModelFile = parameters.get("letor:svmRankModelFile");
		this.testingFeatureVectorsFile = parameters.get("letor:testingFeatureVectorsFile");
		this.testingDocumentScores = parameters.get("letor:testingDocumentScores");
		this.queryFilePath = parameters.get("queryFilePath");
	}

	@Override
	public String defaultQrySopName() {
		// TODO Auto-generated method stub
		return null;
	}

}
