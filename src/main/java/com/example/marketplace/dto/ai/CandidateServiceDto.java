package com.example.marketplace.dto.ai;

public class CandidateServiceDto {
    private Long id;
    private String title;
    private String category;
    private Double price;
    private Double rating;
    private String city;
    private Double providerResponseRate = 0.95;
    private Long providerId = 0L;

    public CandidateServiceDto() {}

    public CandidateServiceDto(Long id, String title, String category, Double price, Double rating, String city, Double providerResponseRate, Long providerId) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.city = city;
        this.providerResponseRate = providerResponseRate;
        this.providerId = providerId;
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

    public Double getProviderResponseRate() { return providerResponseRate; }
    public void setProviderResponseRate(Double providerResponseRate) { this.providerResponseRate = providerResponseRate; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
}
