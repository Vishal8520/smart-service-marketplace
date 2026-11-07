package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ListingAssistRequestDto {
    private String category;

    @JsonProperty("title_keywords")
    private String titleKeywords;

    @JsonProperty("raw_notes")
    private String rawNotes;

    private String city;

    @JsonProperty("benchmark_prices")
    private List<Double> benchmarkPrices = new ArrayList<>();

    public ListingAssistRequestDto() {}

    public ListingAssistRequestDto(String category, String titleKeywords, String rawNotes, String city, List<Double> benchmarkPrices) {
        this.category = category;
        this.titleKeywords = titleKeywords;
        this.rawNotes = rawNotes;
        this.city = city;
        this.benchmarkPrices = benchmarkPrices != null ? benchmarkPrices : new ArrayList<>();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitleKeywords() { return titleKeywords; }
    public void setTitleKeywords(String titleKeywords) { this.titleKeywords = titleKeywords; }

    public String getRawNotes() { return rawNotes; }
    public void setRawNotes(String rawNotes) { this.rawNotes = rawNotes; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public List<Double> getBenchmarkPrices() { return benchmarkPrices; }
    public void setBenchmarkPrices(List<Double> benchmarkPrices) { this.benchmarkPrices = benchmarkPrices; }
}
