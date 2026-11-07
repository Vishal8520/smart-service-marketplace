package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ListingAssistResponseDto {
    @JsonProperty("suggested_title")
    private String suggestedTitle;

    @JsonProperty("generated_description")
    private String generatedDescription;

    @JsonProperty("suggested_price_min")
    private Double suggestedPriceMin;

    @JsonProperty("suggested_price_max")
    private Double suggestedPriceMax;

    @JsonProperty("suggested_price_median")
    private Double suggestedPriceMedian;

    @JsonProperty("policy_violations")
    private List<String> policyViolations = new ArrayList<>();

    @JsonProperty("is_compliant")
    private Boolean isCompliant;

    public ListingAssistResponseDto() {}

    public String getSuggestedTitle() { return suggestedTitle; }
    public void setSuggestedTitle(String suggestedTitle) { this.suggestedTitle = suggestedTitle; }

    public String getGeneratedDescription() { return generatedDescription; }
    public void setGeneratedDescription(String generatedDescription) { this.generatedDescription = generatedDescription; }

    public Double getSuggestedPriceMin() { return suggestedPriceMin; }
    public void setSuggestedPriceMin(Double suggestedPriceMin) { this.suggestedPriceMin = suggestedPriceMin; }

    public Double getSuggestedPriceMax() { return suggestedPriceMax; }
    public void setSuggestedPriceMax(Double suggestedPriceMax) { this.suggestedPriceMax = suggestedPriceMax; }

    public Double getSuggestedPriceMedian() { return suggestedPriceMedian; }
    public void setSuggestedPriceMedian(Double suggestedPriceMedian) { this.suggestedPriceMedian = suggestedPriceMedian; }

    public List<String> getPolicyViolations() { return policyViolations; }
    public void setPolicyViolations(List<String> policyViolations) { this.policyViolations = policyViolations; }

    public Boolean getIsCompliant() { return isCompliant; }
    public void setIsCompliant(Boolean isCompliant) { this.isCompliant = isCompliant; }
}
