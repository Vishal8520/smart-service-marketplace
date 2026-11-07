package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ReviewSummaryDto {
    @JsonProperty("service_id")
    private Long serviceId;

    @JsonProperty("total_reviews")
    private Integer totalReviews;

    @JsonProperty("positive_percentage")
    private Double positivePercentage;

    private String summary;
    private List<String> pros = new ArrayList<>();
    private List<String> cons = new ArrayList<>();

    public ReviewSummaryDto() {}

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Double getPositivePercentage() { return positivePercentage; }
    public void setPositivePercentage(Double positivePercentage) { this.positivePercentage = positivePercentage; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getPros() { return pros; }
    public void setPros(List<String> pros) { this.pros = pros; }

    public List<String> getCons() { return cons; }
    public void setCons(List<String> cons) { this.cons = cons; }
}
