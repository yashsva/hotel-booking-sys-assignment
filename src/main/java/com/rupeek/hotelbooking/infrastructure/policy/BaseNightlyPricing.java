package com.rupeek.hotelbooking.infrastructure.policy;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.domain.property.RoomType;

/** Base nightly rate times the number of nights. Price is per room, not per guest. */
public class BaseNightlyPricing implements PricingStrategy {

    @Override
    public Money quote(RoomType roomType, DateRange stay, int guests) {
        return roomType.basePricePerNight().times(stay.nights());
    }
}
