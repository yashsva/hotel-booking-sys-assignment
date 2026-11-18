package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentRequest;
import org.springframework.stereotype.Component;

import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

/** Card payments use a token from the client-side card vault; we never handle raw card numbers. */
@Component
public class CardPaymentProcessor extends AbstractPaymentProcessor {

    public CardPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public String method() {
        return "CARD";
    }

    @Override
    protected String resolveInstrument(PaymentRequest request) {
        return requireNonBlank(request.detail("cardToken"), "cardToken");
    }
}
