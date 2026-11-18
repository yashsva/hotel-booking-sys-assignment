package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Money;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/**
 * A category of identical rooms in a property (e.g. "Deluxe King").
 * {@code totalUnits} is the shared inventory that bookings draw from.
 */
public record RoomType(String id, String name, int maxOccupancy, int totalUnits, Money basePricePerNight) {

    public RoomType {
        requireNonBlank(id, "roomType.id");
        name = requireNonBlank(name, "roomType.name");
        require(maxOccupancy > 0, "maxOccupancy must be positive");
        require(totalUnits > 0, "totalUnits must be positive");
        requireNonNull(basePricePerNight, "basePricePerNight");
        require(!basePricePerNight.isZero(), "basePricePerNight must be positive");
    }

    public boolean canAccommodate(int guests) {
        return guests <= maxOccupancy;
    }
}
