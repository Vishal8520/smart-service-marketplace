package com.example.marketplace.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class SmartSearchResponseDto {
    private String query;
    private String detectedCategory;
    private List<SearchResultItemDto> results = new ArrayList<>();
    private int total;

    public SmartSearchResponseDto() {}

    public SmartSearchResponseDto(String query, String detectedCategory, List<SearchResultItemDto> results, int total) {
        this.query = query;
        this.detectedCategory = detectedCategory;
        this.results = results != null ? results : new ArrayList<>();
        this.total = total;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getDetectedCategory() { return detectedCategory; }
    public void setDetectedCategory(String detectedCategory) { this.detectedCategory = detectedCategory; }

    public List<SearchResultItemDto> getResults() { return results; }
    public void setResults(List<SearchResultItemDto> results) { this.results = results; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
