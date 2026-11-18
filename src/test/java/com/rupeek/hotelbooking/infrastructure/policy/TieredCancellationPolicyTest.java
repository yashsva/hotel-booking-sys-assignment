package com.rupeek.hotelbooking.infrastructure.policy;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.cancellation.RefundDecision;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TieredCancellationPolicyTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalDate CHECK_IN = LocalDate.of(2027, 2, 10);
    /** Check-in moment: 10 Feb 2027 12:00 IST. */
    private static final Instant CHECK_IN_AT = CHECK_IN.atTime(LocalTime.NOON).atZone(IST).toInstant();

    private final TieredCancellationPolicy policy = new TieredCancellationPolicy(IST, LocalTime.NOON, 48, 24, 50);

    private final Booking booking = Booking.create("b-1", "p-1",
            new RoomType("rt-1", "Double", 2, 1, Money.of("2000", "INR")), "Asha", 2,
            DateRange.of(CHECK_IN, CHECK_IN.plusDays(2)), Money.of("4000", "INR"), CHECK_IN_AT.minusSeconds(864000));

    @ParameterizedTest(name = "{0}h before check-in -> refund {1}")
    @CsvSource({
            "240, 4000",
            "48,  4000",
            "47,  2000",
            "24,  2000",
            "23,  0",
            "0,   0",
            "-5,  0",
    })
    void refundDependsOnNoticeGiven(long hoursBeforeCheckIn, String expectedRefund) {
        RefundDecision decision = policy.evaluate(booking, CHECK_IN_AT.minusSeconds(hoursBeforeCheckIn * 3600));

        assertThat(decision.refundAmount()).isEqualTo(Money.of(expectedRefund, "INR"));
        assertThat(decision.reason()).isNotBlank();
    }
}
