package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentRequest;
import org.springframework.stereotype.Component;

import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

@Component
public class WalletPaymentProcessor extends AbstractPaymentProcessor {

    public WalletPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public String method() {
        return "WALLET";
    }

    @Override
    protected String resolveInstrument(PaymentRequest request) {
        return requireNonBlank(request.detail("walletId"), "walletId");
    }
}
