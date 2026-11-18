package com.rupeek.hotelbooking.application;

/** One lock key per booking, shared by every use case that changes a booking's state. */
final class BookingLocks {

    private BookingLocks() {
    }

    static String key(String bookingId) {
        return "booking:" + bookingId;
    }
}
