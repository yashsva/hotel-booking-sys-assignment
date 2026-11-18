package com.rupeek.hotelbooking.domain.inventory;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.DomainException;

public class RoomUnavailableException extends DomainException {

    public RoomUnavailableException(String roomTypeId, DateRange stay) {
        super("No rooms of type " + roomTypeId + " available from " + stay.checkIn() + " to " + stay.checkOut());
    }
}
