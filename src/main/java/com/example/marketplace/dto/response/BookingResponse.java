package com.example.marketplace.dto.response;

import com.example.marketplace.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class BookingResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long serviceId;
    private String serviceTitle;
    private BigDecimal price;
    private Long providerId;
    private String providerName;
    private Instant scheduledAt;
    private BookingStatus status;
    private String notes;
    private Instant createdAt;

    public BookingResponse() {
    }

    public BookingResponse(Long id, Long customerId, String customerName, Long serviceId, String serviceTitle,
            BigDecimal price, Long providerId, String providerName, Instant scheduledAt, BookingStatus status,
            String notes, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.serviceId = serviceId;
        this.serviceTitle = serviceTitle;
        this.price = price;
        this.providerId = providerId;
        this.providerName = providerName;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long customerId;
        private String customerName;
        private Long serviceId;
        private String serviceTitle;
        private BigDecimal price;
        private Long providerId;
        private String providerName;
        private Instant scheduledAt;
        private BookingStatus status;
        private String notes;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder serviceTitle(String serviceTitle) {
            this.serviceTitle = serviceTitle;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder providerId(Long providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder scheduledAt(Instant scheduledAt) {
            this.scheduledAt = scheduledAt;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
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

        public BookingResponse build() {
            return new BookingResponse(id, customerId, customerName, serviceId, serviceTitle, price, providerId,
                    providerName, scheduledAt, status, notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceTitle() {
        return serviceTitle;
    }

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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
