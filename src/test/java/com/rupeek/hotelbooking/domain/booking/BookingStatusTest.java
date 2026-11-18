package com.rupeek.hotelbooking.domain.booking;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "PENDING_PAYMENT, CONFIRMED,       true",
            "PENDING_PAYMENT, PAYMENT_FAILED,  true",
            "PENDING_PAYMENT, CANCELLED,       true",
            "PENDING_PAYMENT, PENDING_PAYMENT, false",
            "CONFIRMED,       CANCELLED,       true",
            "CONFIRMED,       PAYMENT_FAILED,  false",
            "CONFIRMED,       CONFIRMED,       false",
            "CONFIRMED,       PENDING_PAYMENT, false",
            "PAYMENT_FAILED,  CONFIRMED,       false",
            "PAYMENT_FAILED,  CANCELLED,       false",
            "CANCELLED,       CONFIRMED,       false",
            "CANCELLED,       CANCELLED,       false",
    })
    void enforcesTheLifecycle(BookingStatus from, BookingStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        if (!allowed) {
            assertThatThrownBy(() -> from.ensureCanTransitionTo(to)).isInstanceOf(InvalidBookingStateException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({"PENDING_PAYMENT, true", "CONFIRMED, true", "PAYMENT_FAILED, false", "CANCELLED, false"})
    void onlyLiveBookingsHoldInventory(BookingStatus status, boolean holds) {
        assertThat(status.holdsInventory()).isEqualTo(holds);
    }
}
