package com.rupeek.hotelbooking.domain.booking;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final RoomType DOUBLE = new RoomType("rt-1", "Double", 2, 3, Money.of("2000", "INR"));
    private static final DateRange STAY = DateRange.of(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 3));
    private static final Instant NOW = Instant.parse("2027-01-10T06:30:00Z");

    private Booking newBooking(int guests) {
        return Booking.create("b-1", "p-1", DOUBLE, "Asha", guests, STAY, Money.of("4000", "INR"), NOW);
    }

    @Test
    void startsPendingPayment() {
        Booking booking = newBooking(2);
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.paymentId()).isEmpty();
    }

    @Test
    void rejectsMoreGuestsThanTheRoomFits() {
        assertThatThrownBy(() -> newBooking(3))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at most 2 guests");
    }

    @Test
    void rejectsZeroGuests() {
        assertThatThrownBy(() -> newBooking(0)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void confirmRecordsThePayment() {
        Booking booking = newBooking(2);
        booking.confirm("pay-1");
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.paymentId()).contains("pay-1");
    }

    @Test
    void cancelRecordsRefundAndTime() {
        Booking booking = newBooking(2);
        booking.confirm("pay-1");
        booking.cancel(Money.of("2000", "INR"), NOW);
        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.refundAmount()).contains(Money.of("2000", "INR"));
        assertThat(booking.cancelledAt()).contains(NOW);
    }

    @Test
    void cannotConfirmAFailedOrCancelledBooking() {
        Booking failed = newBooking(2);
        failed.markPaymentFailed();
        assertThatThrownBy(() -> failed.confirm("pay-1")).isInstanceOf(InvalidBookingStateException.class);

        Booking cancelled = newBooking(2);
        cancelled.cancel(Money.of("0", "INR"), NOW);
        assertThatThrownBy(() -> cancelled.confirm("pay-1")).isInstanceOf(InvalidBookingStateException.class);
        assertThatThrownBy(() -> cancelled.cancel(Money.of("0", "INR"), NOW))
                .isInstanceOf(InvalidBookingStateException.class);
    }
}
