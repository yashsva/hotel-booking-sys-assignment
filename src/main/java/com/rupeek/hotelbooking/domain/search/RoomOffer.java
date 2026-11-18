package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

/** A room type that is available for the searched stay, with its quoted price. */
public record RoomOffer(Property property, RoomType roomType, int availableUnits, Money totalPrice, int nights) {

    public Money averageNightlyPrice() {
        return totalPrice.dividedBy(nights);
    }
}
