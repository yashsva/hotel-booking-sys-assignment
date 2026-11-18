package com.rupeek.hotelbooking.infrastructure.policy;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WeekendSurchargePricingTest {

    private final RoomType room = new RoomType("rt-1", "Double", 2, 1, Money.of("1000", "INR"));

    @Test
    void basePricingIsNightlyRateTimesNights() {
        DateRange threeNights = DateRange.of(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 4));
        assertThat(new BaseNightlyPricing().quote(room, threeNights, 2)).isEqualTo(Money.of("3000", "INR"));
    }

    @Test
    void fridayAndSaturdayNightsCarryTheSurcharge() {
        // Thu 4 Feb 2027 -> Sun 7 Feb: Thu (base) + Fri (+20%) + Sat (+20%)
        DateRange thuToSun = DateRange.of(LocalDate.of(2027, 2, 4), LocalDate.of(2027, 2, 7));
        assertThat(new WeekendSurchargePricing(20).quote(room, thuToSun, 2)).isEqualTo(Money.of("3400", "INR"));
    }
}
