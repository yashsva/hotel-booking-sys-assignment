package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.RoomType;

/** Computes the total price of a stay. Search and booking both use it, so a quote and a charge always match. */
public interface PricingStrategy {

    Money quote(RoomType roomType, DateRange stay, int guests);
}
