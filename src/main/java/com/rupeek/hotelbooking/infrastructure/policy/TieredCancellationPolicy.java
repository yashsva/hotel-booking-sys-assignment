package com.rupeek.hotelbooking.infrastructure.policy;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.RefundDecision;
import com.rupeek.hotelbooking.domain.common.Money;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Refund depends on how long before check-in the guest cancels:
 * at least {@code fullRefundHours} -> 100%, at least {@code partialRefundHours} -> {@code partialRefundPercent}%,
 * otherwise nothing.
 */
public class TieredCancellationPolicy implements CancellationPolicy {

    private final ZoneId zone;
    private final LocalTime checkInTime;
    private final long fullRefundHours;
    private final long partialRefundHours;
    private final int partialRefundPercent;

    public TieredCancellationPolicy(ZoneId zone, LocalTime checkInTime, long fullRefundHours,
                                    long partialRefundHours, int partialRefundPercent) {
        if (partialRefundHours > fullRefundHours) {
            throw new IllegalArgumentException("partialRefundHours must not exceed fullRefundHours");
        }
        this.zone = zone;
        this.checkInTime = checkInTime;
        this.fullRefundHours = fullRefundHours;
        this.partialRefundHours = partialRefundHours;
        this.partialRefundPercent = partialRefundPercent;
    }

    @Override
    public RefundDecision evaluate(Booking booking, Instant now) {
        Instant checkIn = booking.stay().checkIn().atTime(checkInTime).atZone(zone).toInstant();
        long hoursBeforeCheckIn = Duration.between(now, checkIn).toHours();
        Money paid = booking.amount();

        if (hoursBeforeCheckIn >= fullRefundHours) {
            return new RefundDecision(paid, "Cancelled at least " + fullRefundHours + "h before check-in: full refund");
        }
        if (hoursBeforeCheckIn >= partialRefundHours) {
            return new RefundDecision(paid.percentage(partialRefundPercent),
                    "Cancelled " + partialRefundHours + "-" + fullRefundHours + "h before check-in: "
                            + partialRefundPercent + "% refund");
        }
        return new RefundDecision(Money.zero(paid.currency()),
                "Cancelled less than " + partialRefundHours + "h before check-in: no refund");
    }
}
