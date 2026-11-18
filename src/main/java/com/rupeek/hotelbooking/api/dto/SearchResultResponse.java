package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.PropertySearchResult;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record SearchResultResponse(String propertyId, String name, AddressDto address, int starRating,
                                   Set<Amenity> amenities, List<RoomOfferResponse> availableRooms) {

    public static SearchResultResponse from(PropertySearchResult result) {
        Property p = result.property();
        return new SearchResultResponse(p.id(), p.name(), AddressDto.from(p.address()), p.starRating(),
                new TreeSet<>(p.amenities()), result.offers().stream().map(RoomOfferResponse::from).toList());
    }
}
