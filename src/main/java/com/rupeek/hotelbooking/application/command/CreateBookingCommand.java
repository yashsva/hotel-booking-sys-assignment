package com.rupeek.hotelbooking.application.command;

import com.rupeek.hotelbooking.domain.common.DateRange;

public record CreateBookingCommand(String propertyId, String roomTypeId, String guestName, int guests, DateRange stay) {
}
