package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.PayCommand;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.booking.InvalidBookingStateException;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;
import com.rupeek.hotelbooking.domain.payment.UnsupportedPaymentMethodException;
import com.rupeek.hotelbooking.domain.property.Property;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.rupeek.hotelbooking.application.TestHotel.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final TestHotel hotel = new TestHotel();
    private final Property solo = hotel.singleRoomHotel();
    private final Booking booking = hotel.book(solo, stay(5, 2));

    private PaymentOutcome pay(String key, String method, Map<String, String> details) {
        return hotel.payments.pay(new PayCommand(booking.id(), key, method, details));
    }

    @Test
    void successfulPaymentConfirmsTheBooking() {
        PaymentOutcome outcome = pay("k1", "UPI", Map.of("vpa", "asha@okbank"));

        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(outcome.payment().amount()).isEqualTo(booking.amount());
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.paymentId()).contains(outcome.payment().id());
    }

    @Test
    void declinedPaymentFailsTheBookingAndReleasesTheRoom() {
        PaymentOutcome outcome = pay("k1", "CARD", Map.of("cardToken", "fail_tok"));

        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.payment().failureReason()).isPresent();
        assertThat(booking.status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(hotel.inventory.availableUnits(solo.roomTypes().getFirst(), stay(5, 2))).isEqualTo(1);
    }

    @Test
    void retryWithTheSameIdempotencyKeyReturnsTheOriginalPaymentWithoutChargingAgain() {
        PaymentOutcome first = pay("k1", "WALLET", Map.of("walletId", "w-42"));
        PaymentOutcome retry = pay("k1", "WALLET", Map.of("walletId", "w-42"));

        assertThat(retry.replayed()).isTrue();
        assertThat(retry.payment()).isSameAs(first.payment());
        assertThat(hotel.gateway.charges).hasValue(1);
    }

    @Test
    void aSecondPaymentWithANewKeyIsRejectedOnceConfirmed() {
        pay("k1", "CARD", Map.of("cardToken", "tok_visa"));

        assertThatThrownBy(() -> pay("k2", "CARD", Map.of("cardToken", "tok_visa")))
                .isInstanceOf(InvalidBookingStateException.class);
        assertThat(hotel.gateway.charges).hasValue(1);
    }

    @Test
    void anIdempotencyKeyCannotBeReusedForAnotherBooking() {
        pay("shared", "CARD", Map.of("cardToken", "tok_visa"));
        Booking other = hotel.book(solo, stay(10, 1));

        assertThatThrownBy(() -> hotel.payments.pay(
                new PayCommand(other.id(), "shared", "CARD", Map.of("cardToken", "tok_visa"))))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void unknownMethodsAndBadDetailsAreRejectedBeforeAnyCharge() {
        assertThatThrownBy(() -> pay("k1", "CRYPTO", Map.of()))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("CARD").hasMessageContaining("UPI").hasMessageContaining("WALLET");
        assertThatThrownBy(() -> pay("k2", "UPI", Map.of("vpa", "not-a-vpa")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> pay("k3", "CARD", Map.of()))
                .isInstanceOf(DomainValidationException.class);

        assertThat(hotel.gateway.charges).hasValue(0);
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void methodNamesAreCaseInsensitive() {
        assertThat(pay("k1", "upi", Map.of("vpa", "asha@okbank")).payment().method()).isEqualTo("UPI");
    }

    @Test
    void cannotPayForACancelledBooking() {
        hotel.cancellations.cancel(booking.id());

        assertThatThrownBy(() -> pay("k1", "CARD", Map.of("cardToken", "tok_visa")))
                .isInstanceOf(InvalidBookingStateException.class);
    }
}
