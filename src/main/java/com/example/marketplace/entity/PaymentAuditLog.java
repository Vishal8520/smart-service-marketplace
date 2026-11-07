package com.example.marketplace.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_audit_logs")
public class PaymentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private Long paymentId;

    @Column(name = "from_status", length = 50, updatable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50, updatable = false)
    private String toStatus;

    @Column(nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "actor_email", nullable = false, updatable = false)
    private String actorEmail;

    @Column(length = 500, updatable = false)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PaymentAuditLog() {
    }

    public PaymentAuditLog(Long id, Long paymentId, String fromStatus, String toStatus, String action,
            String actorEmail, String notes, Instant createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.action = action;
        this.actorEmail = actorEmail;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long paymentId;
        private String fromStatus;
        private String toStatus;
        private String action;
        private String actorEmail;
        private String notes;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder paymentId(Long paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder fromStatus(String fromStatus) {
            this.fromStatus = fromStatus;
            return this;
        }

        public Builder toStatus(String toStatus) {
            this.toStatus = toStatus;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder actorEmail(String actorEmail) {
            this.actorEmail = actorEmail;
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

        public PaymentAuditLog build() {
            return new PaymentAuditLog(id, paymentId, fromStatus, toStatus, action, actorEmail, notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getAction() {
        return action;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
