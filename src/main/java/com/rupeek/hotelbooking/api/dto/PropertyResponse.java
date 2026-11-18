package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record PropertyResponse(String id, String ownerId, String name, AddressDto address, int starRating,
                               Set<Amenity> amenities, List<RoomTypeResponse> roomTypes) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.id(), property.ownerId(), property.name(),
                AddressDto.from(property.address()), property.starRating(), new TreeSet<>(property.amenities()),
                property.roomTypes().stream().map(RoomTypeResponse::from).toList());
    }
}
