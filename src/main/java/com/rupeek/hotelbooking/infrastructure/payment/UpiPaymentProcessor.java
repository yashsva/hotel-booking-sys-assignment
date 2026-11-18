package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

@Component
public class UpiPaymentProcessor extends AbstractPaymentProcessor {

    private static final Pattern VPA = Pattern.compile("^[\\w.\\-]{2,256}@[a-zA-Z]{2,64}$");

    public UpiPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public String method() {
        return "UPI";
    }

    @Override
    protected String resolveInstrument(PaymentRequest request) {
        String vpa = requireNonBlank(request.detail("vpa"), "vpa");
        require(VPA.matcher(vpa).matches(), "vpa is not a valid UPI id");
        return vpa;
    }
}
