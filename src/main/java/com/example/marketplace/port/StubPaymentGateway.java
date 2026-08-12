package com.example.marketplace.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub / Reference implementation of PaymentGatewayPort.
 * This class is used during reference-based manual UPI payment workflows.
 * To switch to a real gateway (e.g. Razorpay/Cashfree), implement
 * PaymentGatewayPort in a separate class
 * annotated with @Primary or profile-based wiring.
 */
@Component
public class StubPaymentGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);

    @Override
    public String createOrder(BigDecimal amount, String currency, String referenceId) {
        log.info("[EXTENSION POINT] PaymentGatewayPort.createOrder called for referenceId={} amount={} {}. " +
                "Currently operating in manual UPI reference mode.", referenceId, amount, currency);
        return "STUB_ORDER_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        log.info("[EXTENSION POINT] PaymentGatewayPort.verifyPaymentSignature called for orderId={}.", orderId);
        return true;
    }
}
