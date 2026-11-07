package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminFeedbackRequestDto {
    private String feature;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("is_correct")
    private Boolean isCorrect;

    @JsonProperty("feedback_notes")
    private String feedbackNotes;

    public AdminFeedbackRequestDto() {}

    public AdminFeedbackRequestDto(String feature, String referenceId, Boolean isCorrect, String feedbackNotes) {
        this.feature = feature;
        this.referenceId = referenceId;
        this.isCorrect = isCorrect;
        this.feedbackNotes = feedbackNotes;
    }

    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public String getFeedbackNotes() { return feedbackNotes; }
    public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }
}
