package com.example.marketplace.entity;

public enum PaymentStatus {
    PENDING,
    AUTO_CONFIRMED,
    VERIFIED,
    REVERSED,
    @Deprecated
    CONFIRMED,
    @Deprecated
    REJECTED;

    public boolean canTransitionTo(PaymentStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == AUTO_CONFIRMED || target == VERIFIED || target == REVERSED;
            case AUTO_CONFIRMED -> target == VERIFIED || target == REVERSED;
            case VERIFIED, CONFIRMED -> target == REVERSED;
            case REVERSED, REJECTED -> false;
        };
    }
}
