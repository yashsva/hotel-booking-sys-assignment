package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.command.CreateBookingCommand;
import com.rupeek.hotelbooking.domain.common.DateRange;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotBlank String propertyId,
        @NotBlank String roomTypeId,
        @NotBlank String guestName,
        @Min(1) int guests,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut) {

    public CreateBookingCommand toCommand() {
        return new CreateBookingCommand(propertyId, roomTypeId, guestName, guests, DateRange.of(checkIn, checkOut));
    }
}
