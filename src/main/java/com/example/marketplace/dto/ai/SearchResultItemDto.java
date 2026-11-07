package com.example.marketplace.dto.ai;

public class SearchResultItemDto {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String city;
    private Double price;
    private Double rating;
    private Double relevanceScore;

    public SearchResultItemDto() {}

    public SearchResultItemDto(Long id, String title, String category, String description, String city, Double price, Double rating, Double relevanceScore) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.city = city;
        this.price = price;
        this.rating = rating;
        this.relevanceScore = relevanceScore;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
}
