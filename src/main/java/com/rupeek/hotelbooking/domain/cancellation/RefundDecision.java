package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.common.Money;

import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

public record RefundDecision(Money refundAmount, String reason) {

    public RefundDecision {
        requireNonNull(refundAmount, "refundAmount");
        requireNonNull(reason, "reason");
    }

    public boolean hasRefund() {
        return !refundAmount.isZero();
    }
}
