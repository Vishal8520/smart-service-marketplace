package com.example.marketplace.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private PaymentStatus status = PaymentStatus.AUTO_CONFIRMED;

    @Column(name = "upi_reference_id", length = 150)
    private String upiReferenceId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_admin_id")
    private User confirmedByAdmin;

    @Column(name = "auto_confirmed", nullable = false)
    private boolean autoConfirmed = true;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_admin_id")
    private User reversedByAdmin;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Payment() {
    }

    public Payment(Long id, Booking booking, BigDecimal amount, String currency, PaymentStatus status,
            String upiReferenceId, Instant confirmedAt, User confirmedByAdmin, boolean autoConfirmed,
            Instant reversedAt, User reversedByAdmin, String reversalReason, String notes, Instant createdAt) {
        this.id = id;
        this.booking = booking;
        this.amount = amount;
        this.currency = currency != null ? currency : "INR";
        this.status = status != null ? status : PaymentStatus.AUTO_CONFIRMED;
        this.upiReferenceId = upiReferenceId;
        this.confirmedAt = confirmedAt;
        this.confirmedByAdmin = confirmedByAdmin;
        this.autoConfirmed = autoConfirmed;
        this.reversedAt = reversedAt;
        this.reversedByAdmin = reversedByAdmin;
        this.reversalReason = reversalReason;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Booking booking;
        private BigDecimal amount;
        private String currency = "INR";
        private PaymentStatus status = PaymentStatus.AUTO_CONFIRMED;
        private String upiReferenceId;
        private Instant confirmedAt;
        private User confirmedByAdmin;
        private boolean autoConfirmed = true;
        private Instant reversedAt;
        private User reversedByAdmin;
        private String reversalReason;
        private String notes;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder booking(Booking booking) {
            this.booking = booking;
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

        public Builder confirmedByAdmin(User confirmedByAdmin) {
            this.confirmedByAdmin = confirmedByAdmin;
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

        public Builder reversedByAdmin(User reversedByAdmin) {
            this.reversedByAdmin = reversedByAdmin;
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

        public Payment build() {
            return new Payment(id, booking, amount, currency, status, upiReferenceId, confirmedAt, confirmedByAdmin,
                    autoConfirmed, reversedAt, reversedByAdmin, reversalReason, notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
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

    public User getConfirmedByAdmin() {
        return confirmedByAdmin;
    }

    public void setConfirmedByAdmin(User confirmedByAdmin) {
        this.confirmedByAdmin = confirmedByAdmin;
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

    public User getReversedByAdmin() {
        return reversedByAdmin;
    }

    public void setReversedByAdmin(User reversedByAdmin) {
        this.reversedByAdmin = reversedByAdmin;
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
