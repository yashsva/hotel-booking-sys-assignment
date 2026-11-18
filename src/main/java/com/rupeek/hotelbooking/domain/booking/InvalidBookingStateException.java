package com.rupeek.hotelbooking.domain.booking;

import com.rupeek.hotelbooking.domain.common.DomainException;

public class InvalidBookingStateException extends DomainException {

    public InvalidBookingStateException(BookingStatus from, BookingStatus to) {
        super("Booking cannot move from " + from + " to " + to);
    }
}
