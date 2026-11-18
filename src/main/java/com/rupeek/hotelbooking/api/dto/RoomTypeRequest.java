package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.command.NewRoomType;
import com.rupeek.hotelbooking.domain.common.Money;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record RoomTypeRequest(
        @NotBlank String name,
        @Min(1) int maxOccupancy,
        @Min(1) int totalUnits,
        @NotNull @Positive BigDecimal pricePerNight,
        @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO-4217 code such as INR") String currency) {

    private static final String DEFAULT_CURRENCY = "INR";

    public NewRoomType toCommand() {
        Currency resolved = Currency.getInstance(currency == null ? DEFAULT_CURRENCY : currency);
        return new NewRoomType(name, maxOccupancy, totalUnits, Money.of(pricePerNight, resolved));
    }
}
