package com.rupeek.hotelbooking.domain.booking;

import java.util.EnumSet;
import java.util.Set;

/**
 * Booking lifecycle. The enum is the single source of truth for legal transitions:
 * <pre>
 *   PENDING_PAYMENT -> CONFIRMED | PAYMENT_FAILED | CANCELLED
 *   CONFIRMED       -> CANCELLED
 *   PAYMENT_FAILED, CANCELLED are terminal
 * </pre>
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED;

    public Set<BookingStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, PAYMENT_FAILED, CANCELLED);
            case CONFIRMED -> EnumSet.of(CANCELLED);
            case PAYMENT_FAILED, CANCELLED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canTransitionTo(BookingStatus next) {
        return allowedNext().contains(next);
    }

    public void ensureCanTransitionTo(BookingStatus next) {
        if (!canTransitionTo(next)) {
            throw new InvalidBookingStateException(this, next);
        }
    }

    /** Whether a booking in this state is holding room inventory. */
    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
