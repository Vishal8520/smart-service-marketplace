package com.example.marketplace.dto.response;

import com.example.marketplace.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String upiReferenceId;
    private Instant confirmedAt;
    private Long confirmedByAdminId;
    private boolean autoConfirmed;
    private Instant reversedAt;
    private Long reversedByAdminId;
    private String reversalReason;
    private String notes;
    private Instant createdAt;

    public PaymentResponse() {
    }

    public PaymentResponse(Long id, Long bookingId, String customerName, BigDecimal amount, String currency,
            PaymentStatus status, String upiReferenceId, Instant confirmedAt, Long confirmedByAdminId,
            boolean autoConfirmed, Instant reversedAt, Long reversedByAdminId, String reversalReason, String notes,
            Instant createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.upiReferenceId = upiReferenceId;
        this.confirmedAt = confirmedAt;
        this.confirmedByAdminId = confirmedByAdminId;
        this.autoConfirmed = autoConfirmed;
        this.reversedAt = reversedAt;
        this.reversedByAdminId = reversedByAdminId;
        this.reversalReason = reversalReason;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long bookingId;
        private String customerName;
        private BigDecimal amount;
        private String currency;
        private PaymentStatus status;
        private String upiReferenceId;
        private Instant confirmedAt;
        private Long confirmedByAdminId;
        private boolean autoConfirmed = true;
        private Instant reversedAt;
        private Long reversedByAdminId;
        private String reversalReason;
        private String notes;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder bookingId(Long bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder upiReferenceId(String upiReferenceId) {
            this.upiReferenceId = upiReferenceId;
            return this;
        }

        public Builder confirmedAt(Instant confirmedAt) {
            this.confirmedAt = confirmedAt;
            return this;
        }

        public Builder confirmedByAdminId(Long confirmedByAdminId) {
            this.confirmedByAdminId = confirmedByAdminId;
            return this;
        }

        public Builder autoConfirmed(boolean autoConfirmed) {
            this.autoConfirmed = autoConfirmed;
            return this;
        }

        public Builder reversedAt(Instant reversedAt) {
            this.reversedAt = reversedAt;
            return this;
        }

        public Builder reversedByAdminId(Long reversedByAdminId) {
            this.reversedByAdminId = reversedByAdminId;
            return this;
        }

        public Builder reversalReason(String reversalReason) {
            this.reversalReason = reversalReason;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentResponse build() {
            return new PaymentResponse(id, bookingId, customerName, amount, currency, status, upiReferenceId,
                    confirmedAt, confirmedByAdminId, autoConfirmed, reversedAt, reversedByAdminId, reversalReason,
                    notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getUpiReferenceId() {
        return upiReferenceId;
    }

    public void setUpiReferenceId(String upiReferenceId) {
        this.upiReferenceId = upiReferenceId;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getConfirmedByAdminId() {
        return confirmedByAdminId;
    }

    public void setConfirmedByAdminId(Long confirmedByAdminId) {
        this.confirmedByAdminId = confirmedByAdminId;
    }

    public boolean isAutoConfirmed() {
        return autoConfirmed;
    }

    public void setAutoConfirmed(boolean autoConfirmed) {
        this.autoConfirmed = autoConfirmed;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public void setReversedAt(Instant reversedAt) {
        this.reversedAt = reversedAt;
    }

    public Long getReversedByAdminId() {
        return reversedByAdminId;
    }

    public void setReversedByAdminId(Long reversedByAdminId) {
        this.reversedByAdminId = reversedByAdminId;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
