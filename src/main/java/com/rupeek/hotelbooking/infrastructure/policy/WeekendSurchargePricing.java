package com.rupeek.hotelbooking.infrastructure.policy;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Example dynamic pricing: Friday and Saturday nights cost a configurable percentage more. */
public class WeekendSurchargePricing implements PricingStrategy {

    private static final Set<DayOfWeek> WEEKEND_NIGHTS = Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final int surchargePercent;

    public WeekendSurchargePricing(int surchargePercent) {
        this.surchargePercent = surchargePercent;
    }

    @Override
    public Money quote(RoomType roomType, DateRange stay, int guests) {
        Money base = roomType.basePricePerNight();
        return stay.nightsOfStay().stream()
                .map(night -> isWeekend(night) ? base.plus(base.percentage(surchargePercent)) : base)
                .reduce(Money.zero(base.currency()), Money::plus);
    }

    private static boolean isWeekend(LocalDate night) {
        return WEEKEND_NIGHTS.contains(night.getDayOfWeek());
    }
}
