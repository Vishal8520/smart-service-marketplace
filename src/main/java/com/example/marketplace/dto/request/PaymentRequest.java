package com.example.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "UPI Reference ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_@]{6,100}$", message = "Please enter a valid UPI transaction reference string (6-100 characters)")
    private String upiReferenceId;

    public PaymentRequest() {
    }

    public PaymentRequest(Long bookingId, String upiReferenceId) {
        this.bookingId = bookingId;
        this.upiReferenceId = upiReferenceId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getUpiReferenceId() {
        return upiReferenceId;
    }

    public void setUpiReferenceId(String upiReferenceId) {
        this.upiReferenceId = upiReferenceId;
    }
}
