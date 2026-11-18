package com.rupeek.hotelbooking.domain.repository;

import com.rupeek.hotelbooking.domain.booking.Booking;

import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);
}
