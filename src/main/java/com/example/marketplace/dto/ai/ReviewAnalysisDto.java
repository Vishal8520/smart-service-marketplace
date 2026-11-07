package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ReviewAnalysisDto {
    @JsonProperty("review_id")
    private Long reviewId;

    private String sentiment; // POSITIVE, NEUTRAL, NEGATIVE

    @JsonProperty("sentiment_score")
    private Double sentimentScore;

    @JsonProperty("is_suspicious_or_fake")
    private Boolean isSuspiciousOrFake;

    private Double confidence;

    private List<String> flags = new ArrayList<>();

    public ReviewAnalysisDto() {}

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }

    public Boolean getIsSuspiciousOrFake() { return isSuspiciousOrFake; }
    public void setIsSuspiciousOrFake(Boolean isSuspiciousOrFake) { this.isSuspiciousOrFake = isSuspiciousOrFake; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }
}
