package com.rupeek.hotelbooking.domain.payment;

/** Outcome reported by a payment processor or gateway. */
public record PaymentResult(boolean successful, String gatewayReference, String failureReason) {

    public static PaymentResult success(String gatewayReference) {
        return new PaymentResult(true, gatewayReference, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
