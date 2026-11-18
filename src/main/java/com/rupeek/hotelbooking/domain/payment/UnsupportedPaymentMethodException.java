package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.DomainException;

import java.util.Collection;

public class UnsupportedPaymentMethodException extends DomainException {

    public UnsupportedPaymentMethodException(String method, Collection<String> supported) {
        super("Unsupported payment method '" + method + "'. Supported: " + supported);
    }
}
