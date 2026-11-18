package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.CancellationOutcome;

public record CancellationResponse(BookingResponse booking, MoneyDto refundAmount, String refundReason) {

    public static CancellationResponse from(CancellationOutcome outcome) {
        return new CancellationResponse(BookingResponse.from(outcome.booking()),
                MoneyDto.from(outcome.refund().refundAmount()), outcome.refund().reason());
    }
}
