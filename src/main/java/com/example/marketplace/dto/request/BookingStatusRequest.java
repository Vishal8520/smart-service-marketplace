package com.example.marketplace.dto.request;

import com.example.marketplace.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

public class BookingStatusRequest {

    @NotNull(message = "Status is required")
    private BookingStatus status;

    public BookingStatusRequest() {
    }

    public BookingStatusRequest(BookingStatus status) {
        this.status = status;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
