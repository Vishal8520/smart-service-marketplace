package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RankedServiceItemDto {
    private Long id;
    private String title;
    private String category;
    private Double price;
    private Double rating;
    private String city;

    @JsonProperty("final_score")
    private Double finalScore;

    @JsonProperty("why_recommended")
    private String whyRecommended;

    public RankedServiceItemDto() {}

    public RankedServiceItemDto(Long id, String title, String category, Double price, Double rating, String city, Double finalScore, String whyRecommended) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.city = city;
        this.finalScore = finalScore;
        this.whyRecommended = whyRecommended;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getWhyRecommended() { return whyRecommended; }
    public void setWhyRecommended(String whyRecommended) { this.whyRecommended = whyRecommended; }
}
