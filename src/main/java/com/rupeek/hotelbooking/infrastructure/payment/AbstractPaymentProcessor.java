package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentProcessor;
import com.rupeek.hotelbooking.domain.payment.PaymentRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentResult;

/**
 * Template for processors: each method only says how to turn its details into a gateway
 * instrument (validating them on the way); charging is shared.
 */
public abstract class AbstractPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    protected AbstractPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public final PaymentResult process(PaymentRequest request) {
        String instrument = resolveInstrument(request);
        return gateway.charge(instrument, request.amount(), request.bookingId());
    }

    /** Validates method-specific details and returns the instrument identifier sent to the gateway. */
    protected abstract String resolveInstrument(PaymentRequest request);
}
