package com.example.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentRejectRequest {

    @NotBlank(message = "Rejection notes/reason is required")
    @Size(min = 5, max = 500, message = "Rejection notes must be between 5 and 500 characters")
    private String notes;

    public PaymentRejectRequest() {
    }

    public PaymentRejectRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
