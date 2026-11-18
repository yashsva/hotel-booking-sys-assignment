package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.search.RoomOffer;

public record RoomOfferResponse(String roomTypeId, String name, int maxOccupancy, int availableUnits,
                                MoneyDto averageNightlyPrice, MoneyDto totalPrice) {

    public static RoomOfferResponse from(RoomOffer offer) {
        return new RoomOfferResponse(offer.roomType().id(), offer.roomType().name(), offer.roomType().maxOccupancy(),
                offer.availableUnits(), MoneyDto.from(offer.averageNightlyPrice()), MoneyDto.from(offer.totalPrice()));
    }
}
