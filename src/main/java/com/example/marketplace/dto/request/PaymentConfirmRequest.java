package com.example.marketplace.dto.request;

public class PaymentConfirmRequest {

    private String notes;

    public PaymentConfirmRequest() {
    }

    public PaymentConfirmRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
