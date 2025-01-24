package com.example.websearch;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebSearchResult {
    private static Map<String, Double> results = new LinkedHashMap<>();

    public static Map<String, Double> getResults() {
        return results;
    }

    public static void setResults(Map<String, Double> newResults) {
        results = newResults;
    }
}


