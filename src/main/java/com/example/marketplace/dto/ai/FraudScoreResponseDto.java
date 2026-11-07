package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class FraudScoreResponseDto {
    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("risk_level")
    private String riskLevel; // LOW, MEDIUM, HIGH

    private List<String> reasons = new ArrayList<>();

    @JsonProperty("flagged_for_review")
    private Boolean flaggedForReview;

    public FraudScoreResponseDto() {}

    public FraudScoreResponseDto(Long paymentId, Double riskScore, String riskLevel, List<String> reasons, Boolean flaggedForReview) {
        this.paymentId = paymentId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.flaggedForReview = flaggedForReview;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public Boolean getFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(Boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }
}
