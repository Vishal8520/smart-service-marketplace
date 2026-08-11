package com.example.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;

public class PaymentOrderRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    public PaymentOrderRequest() {
    }

    public PaymentOrderRequest(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}
