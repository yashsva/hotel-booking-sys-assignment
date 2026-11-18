package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.search.RoomOffer;

import java.util.Comparator;
import java.util.List;

/** A property with the room offers that matched the search, cheapest first. */
public record PropertySearchResult(Property property, List<RoomOffer> offers) {

    public PropertySearchResult {
        offers = offers.stream().sorted(Comparator.comparing(RoomOffer::totalPrice)).toList();
    }

    public Money lowestTotalPrice() {
        return offers.getFirst().totalPrice();
    }
}
