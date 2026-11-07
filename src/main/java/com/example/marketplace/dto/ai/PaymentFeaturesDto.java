package com.example.marketplace.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentFeaturesDto {
    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("user_id")
    private Long userId;

    private Double amount;

    @JsonProperty("expected_amount")
    private Double expectedAmount;

    @JsonProperty("upi_reference_id")
    private String upiReferenceId;

    @JsonProperty("velocity_last_10m")
    private Integer velocityLast10m = 0;

    @JsonProperty("account_age_hours")
    private Double accountAgeHours = 24.0;

    @JsonProperty("consecutive_failed_payments")
    private Integer consecutiveFailedPayments = 0;

    @JsonProperty("is_reference_reused")
    private Boolean isReferenceReused = false;

    public PaymentFeaturesDto() {}

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(Double expectedAmount) { this.expectedAmount = expectedAmount; }

    public String getUpiReferenceId() { return upiReferenceId; }
    public void setUpiReferenceId(String upiReferenceId) { this.upiReferenceId = upiReferenceId; }

    public Integer getVelocityLast10m() { return velocityLast10m; }
    public void setVelocityLast10m(Integer velocityLast10m) { this.velocityLast10m = velocityLast10m; }

    public Double getAccountAgeHours() { return accountAgeHours; }
    public void setAccountAgeHours(Double accountAgeHours) { this.accountAgeHours = accountAgeHours; }

    public Integer getConsecutiveFailedPayments() { return consecutiveFailedPayments; }
    public void setConsecutiveFailedPayments(Integer consecutiveFailedPayments) { this.consecutiveFailedPayments = consecutiveFailedPayments; }

    public Boolean getIsReferenceReused() { return isReferenceReused; }
    public void setIsReferenceReused(Boolean isReferenceReused) { this.isReferenceReused = isReferenceReused; }
}
