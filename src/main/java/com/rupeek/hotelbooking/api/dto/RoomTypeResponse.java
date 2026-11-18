package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.RoomType;

public record RoomTypeResponse(String id, String name, int maxOccupancy, int totalUnits, MoneyDto pricePerNight) {

    public static RoomTypeResponse from(RoomType roomType) {
        return new RoomTypeResponse(roomType.id(), roomType.name(), roomType.maxOccupancy(), roomType.totalUnits(),
                MoneyDto.from(roomType.basePricePerNight()));
    }
}
