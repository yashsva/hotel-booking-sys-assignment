package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(String id, String propertyId, String roomTypeId, String guestName, int guests,
                              LocalDate checkIn, LocalDate checkOut, int nights, MoneyDto amount,
                              BookingStatus status, String paymentId, MoneyDto refundAmount,
                              Instant createdAt, Instant cancelledAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.id(), b.propertyId(), b.roomTypeId(), b.guestName(), b.guests(),
                b.stay().checkIn(), b.stay().checkOut(), b.stay().nights(), MoneyDto.from(b.amount()),
                b.status(), b.paymentId().orElse(null), b.refundAmount().map(MoneyDto::from).orElse(null),
                b.createdAt(), b.cancelledAt().orElse(null));
    }
}
