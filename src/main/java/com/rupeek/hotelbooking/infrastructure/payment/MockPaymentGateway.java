package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Stand-in for a real provider. Deterministic so demos and tests can force a decline:
 * any instrument starting with "fail" (e.g. card token {@code fail_tok}, VPA {@code fail@upi}) is declined.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final String DECLINE_PREFIX = "fail";

    @Override
    public PaymentResult charge(String instrument, Money amount, String reference) {
        if (instrument.toLowerCase(Locale.ROOT).startsWith(DECLINE_PREFIX)) {
            log.info("Mock gateway declined {} for {}", amount, reference);
            return PaymentResult.failure("Declined by issuer");
        }
        String gatewayReference = "pg_" + UUID.randomUUID();
        log.info("Mock gateway charged {} for {} -> {}", amount, reference, gatewayReference);
        return PaymentResult.success(gatewayReference);
    }

    @Override
    public PaymentResult refund(String gatewayReference, Money amount) {
        log.info("Mock gateway refunded {} against {}", amount, gatewayReference);
        return PaymentResult.success("rf_" + UUID.randomUUID());
    }
}
