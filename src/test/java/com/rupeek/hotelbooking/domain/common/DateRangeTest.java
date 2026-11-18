package com.rupeek.hotelbooking.domain.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    private static final LocalDate JAN_10 = LocalDate.of(2027, 1, 10);

    @Test
    void countsNightsExcludingCheckOutDay() {
        DateRange stay = DateRange.of(JAN_10, JAN_10.plusDays(3));
        assertThat(stay.nights()).isEqualTo(3);
        assertThat(stay.nightsOfStay()).containsExactly(JAN_10, JAN_10.plusDays(1), JAN_10.plusDays(2));
    }

    @Test
    void checkOutMustBeAfterCheckIn() {
        assertThatThrownBy(() -> DateRange.of(JAN_10, JAN_10)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> DateRange.of(JAN_10, JAN_10.minusDays(1))).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsOverlyLongStays() {
        assertThatThrownBy(() -> DateRange.of(JAN_10, JAN_10.plusDays(DateRange.MAX_NIGHTS + 1)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void backToBackStaysDoNotOverlap() {
        DateRange first = DateRange.of(JAN_10, JAN_10.plusDays(2));
        DateRange next = DateRange.of(JAN_10.plusDays(2), JAN_10.plusDays(4));
        DateRange clashing = DateRange.of(JAN_10.plusDays(1), JAN_10.plusDays(3));

        assertThat(first.overlaps(next)).isFalse();
        assertThat(first.overlaps(clashing)).isTrue();
        assertThat(clashing.overlaps(first)).isTrue();
    }

    @Test
    void cannotStartInThePast() {
        DateRange stay = DateRange.of(JAN_10, JAN_10.plusDays(1));
        assertThatThrownBy(() -> stay.requireNotStartingBefore(JAN_10.plusDays(1)))
                .isInstanceOf(DomainValidationException.class);
        stay.requireNotStartingBefore(JAN_10); // same day is fine
    }
}
