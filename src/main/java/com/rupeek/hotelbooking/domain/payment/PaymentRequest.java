package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

import java.util.Map;

/** What a processor needs to charge a booking. {@code details} are method specific (card token, VPA, wallet id ...). */
public record PaymentRequest(String bookingId, Money amount, Map<String, String> details) {

    public PaymentRequest {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String detail(String key) {
        return details.get(key);
    }
}
