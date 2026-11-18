package com.rupeek.hotelbooking.application.command;

import com.rupeek.hotelbooking.domain.common.Address;
import com.rupeek.hotelbooking.domain.property.Amenity;

import java.util.List;
import java.util.Set;

public record NewProperty(String name, Address address, int starRating, Set<Amenity> amenities,
                          List<NewRoomType> roomTypes) {
}
