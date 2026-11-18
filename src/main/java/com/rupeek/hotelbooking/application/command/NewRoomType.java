package com.rupeek.hotelbooking.application.command;

import com.rupeek.hotelbooking.domain.common.Money;

public record NewRoomType(String name, int maxOccupancy, int totalUnits, Money basePricePerNight) {
}
