package com.rupeek.hotelbooking.domain.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/**
 * A stay expressed as [checkIn, checkOut): the guest occupies the room on every night
 * from checkIn up to, but not including, checkOut.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public DateRange {
        requireNonNull(checkIn, "checkIn");
        requireNonNull(checkOut, "checkOut");
        require(checkOut.isAfter(checkIn), "checkOut must be after checkIn");
        require(ChronoUnit.DAYS.between(checkIn, checkOut) <= MAX_NIGHTS, "a stay cannot exceed " + MAX_NIGHTS + " nights");
    }

    public static DateRange of(LocalDate checkIn, LocalDate checkOut) {
        return new DateRange(checkIn, checkOut);
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Every night of the stay (excludes the check-out date). */
    public List<LocalDate> nightsOfStay() {
        return checkIn.datesUntil(checkOut).toList();
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    public void requireNotStartingBefore(LocalDate today) {
        require(!checkIn.isBefore(today), "checkIn must not be in the past");
    }
}
