package com.rupeek.hotelbooking.domain.payment;

/**
 * Strategy for one payment method (card, UPI, wallet, ...).
 * Adding a method means adding one implementation; nothing else changes.
 */
public interface PaymentProcessor {

    /** Method code clients send, e.g. {@code "CARD"}. Must be unique across processors. */
    String method();

    /** Validates the method-specific details, then charges the amount. */
    PaymentResult process(PaymentRequest request);
}
