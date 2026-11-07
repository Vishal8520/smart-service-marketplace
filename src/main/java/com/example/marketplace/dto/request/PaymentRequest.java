package com.example.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "UPI Reference ID is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "UPI Reference ID must be a valid 12-digit UTR number")
    private String upiReferenceId;

    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    public PaymentRequest() {
    }

    public PaymentRequest(Long bookingId, String upiReferenceId) {
        this.bookingId = bookingId;
        this.upiReferenceId = upiReferenceId;
    }

    public PaymentRequest(Long bookingId, String upiReferenceId, String idempotencyKey) {
        this.bookingId = bookingId;
        this.upiReferenceId = upiReferenceId;
        this.idempotencyKey = idempotencyKey;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
