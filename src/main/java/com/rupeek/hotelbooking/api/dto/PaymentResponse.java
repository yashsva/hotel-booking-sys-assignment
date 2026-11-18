package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.PaymentOutcome;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;

public record PaymentResponse(String paymentId, String bookingId, String method, MoneyDto amount,
                              PaymentStatus paymentStatus, String failureReason, BookingStatus bookingStatus,
                              boolean replayed) {

    public static PaymentResponse from(PaymentOutcome outcome) {
        Payment p = outcome.payment();
        return new PaymentResponse(p.id(), p.bookingId(), p.method(), MoneyDto.from(p.amount()), p.status(),
                p.failureReason().orElse(null), outcome.booking().status(), outcome.replayed());
    }
}
