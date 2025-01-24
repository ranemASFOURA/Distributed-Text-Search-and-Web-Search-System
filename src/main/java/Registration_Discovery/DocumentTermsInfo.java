package Registration_Discovery;

import java.util.HashMap;
import java.util.Map;

class DocumentTermsInfo implements java.io.Serializable {
    private String documentName;
    private Map<String, Double> termFrequency;

    public DocumentTermsInfo(String documentName) {
        this.documentName = documentName;
        this.termFrequency = new HashMap<>();
    }

    public String getDocumentName() {
        return documentName;
    }

    public Map<String, Double> getTermFrequency() {
        return termFrequency;
    }

    public void addTermFrequency(String term, double frequency) {
        termFrequency.put(term, frequency);
    }

    @Override
    public String toString() {
        return "DocumentTermsInfo{" +
                "documentName='" + documentName + '\'' +
                ", termFrequency=" + termFrequency +
                '}';
    }

    public void setTermFrequency(HashMap<String, Double> termFrequency) {
        this.termFrequency = termFrequency;
    }
}
