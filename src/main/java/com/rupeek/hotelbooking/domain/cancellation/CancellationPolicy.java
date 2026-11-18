package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.booking.Booking;

import java.time.Instant;

/** Decides how much of a confirmed booking's amount is refunded when it is cancelled at {@code now}. */
public interface CancellationPolicy {

    RefundDecision evaluate(Booking booking, Instant now);
}
