package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

/** Our abstraction over an external payment provider. */
public interface PaymentGateway {

    PaymentResult charge(String instrument, Money amount, String reference);

    PaymentResult refund(String gatewayReference, Money amount);
}
