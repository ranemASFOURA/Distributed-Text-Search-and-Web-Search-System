package com.example.websearch;

import java.io.Serializable;
import java.util.Map;

public class SearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Double> response;

    public SearchResponse(Map<String, Double> response) {
        this.response = response;
    }

    public Map<String, Double> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Double> response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
                "response=" + response +
                '}';
    }
}

