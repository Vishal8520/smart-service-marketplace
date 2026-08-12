package com.example.marketplace.port;

import java.math.BigDecimal;

/**
 * Extension Point / Port for Payment Gateway integration.
 * In a future phase, a live Payment Gateway provider (e.g. Razorpay, Cashfree,
 * PhonePe)
 * can implement this port to trigger live payment collections.
 */
public interface PaymentGatewayPort {

    /**
     * Create an order on the external payment service provider gateway.
     *
     * @param amount      Payment amount
     * @param currency    Currency (e.g. INR)
     * @param referenceId Internal booking or payment reference ID
     * @return Gateway-specific Order ID
     */
    String createOrder(BigDecimal amount, String currency, String referenceId);

    /**
     * Verify payment signature from webhook or frontend callback.
     *
     * @param orderId   Gateway order ID
     * @param paymentId Gateway payment transaction ID
     * @param signature Gateway cryptographically signed payload
     * @return true if valid signature
     */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);
}
