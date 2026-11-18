package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.payment.Payment;

/** {@code replayed} is true when the idempotency key matched an earlier payment and nothing was charged. */
public record PaymentOutcome(Payment payment, Booking booking, boolean replayed) {
}
