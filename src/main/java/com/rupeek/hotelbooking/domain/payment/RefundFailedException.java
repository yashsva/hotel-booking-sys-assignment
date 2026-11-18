package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.DomainException;

public class RefundFailedException extends DomainException {

    public RefundFailedException(String paymentId, String reason) {
        super("Refund failed for payment " + paymentId + ": " + reason);
    }
}
