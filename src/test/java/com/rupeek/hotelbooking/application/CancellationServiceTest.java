package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.booking.InvalidBookingStateException;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;
import com.rupeek.hotelbooking.domain.property.Property;
import org.junit.jupiter.api.Test;

import static com.rupeek.hotelbooking.application.TestHotel.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationServiceTest {

    private final TestHotel hotel = new TestHotel();
    private final Property solo = hotel.singleRoomHotel(); // 2000 INR per night

    private Booking confirmedBooking(DateRange stay) {
        Booking booking = hotel.book(solo, stay);
        hotel.payByCard(booking, "tok_visa");
        return booking;
    }

    @Test
    void earlyCancellationRefundsInFullAndReleasesTheRoom() {
        Booking booking = confirmedBooking(stay(5, 2));

        CancellationOutcome outcome = hotel.cancellations.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.refund().refundAmount()).isEqualTo(Money.of("4000", "INR"));
        assertThat(hotel.paymentRepository.findById(booking.paymentId().orElseThrow()).orElseThrow().status())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(hotel.gateway.refunds).hasValue(1);
        assertThat(hotel.inventory.availableUnits(solo.roomTypes().getFirst(), stay(5, 2))).isEqualTo(1);
    }

    @Test
    void lateCancellationFollowsThePolicy() {
        // "now" is noon today; check-in is noon tomorrow -> exactly 24h notice -> 50%.
        Booking tomorrow = confirmedBooking(stay(1, 2));
        assertThat(hotel.cancellations.cancel(tomorrow.id()).refund().refundAmount())
                .isEqualTo(Money.of("2000", "INR"));

        // Check-in today -> no refund and no call to the gateway.
        Booking today = confirmedBooking(stay(0, 1));
        assertThat(hotel.cancellations.cancel(today.id()).refund().refundAmount()).isEqualTo(Money.of("0", "INR"));
        assertThat(hotel.gateway.refunds).hasValue(1);
    }

    @Test
    void cancellingAnUnpaidBookingRefundsNothingButFreesTheRoom() {
        Booking pending = hotel.book(solo, stay(5, 2));

        CancellationOutcome outcome = hotel.cancellations.cancel(pending.id());

        assertThat(outcome.refund().hasRefund()).isFalse();
        assertThat(hotel.gateway.refunds).hasValue(0);
        assertThat(hotel.inventory.availableUnits(solo.roomTypes().getFirst(), stay(5, 2))).isEqualTo(1);
    }

    @Test
    void cancellingTwiceOrAfterAFailedPaymentIsRejectedAndReleasesNothingExtra() {
        Booking booking = confirmedBooking(stay(5, 2));
        hotel.cancellations.cancel(booking.id());
        Booking other = hotel.book(solo, stay(5, 2)); // room re-booked by someone else

        assertThatThrownBy(() -> hotel.cancellations.cancel(booking.id()))
                .isInstanceOf(InvalidBookingStateException.class);
        assertThat(hotel.inventory.availableUnits(solo.roomTypes().getFirst(), stay(5, 2))).isZero();

        Booking failed = hotel.book(solo, stay(10, 1));
        hotel.payByCard(failed, "fail_tok");
        assertThatThrownBy(() -> hotel.cancellations.cancel(failed.id()))
                .isInstanceOf(InvalidBookingStateException.class);
        assertThat(other.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void unknownBookingIsNotFound() {
        assertThatThrownBy(() -> hotel.cancellations.cancel("nope")).isInstanceOf(ResourceNotFoundException.class);
    }
}
