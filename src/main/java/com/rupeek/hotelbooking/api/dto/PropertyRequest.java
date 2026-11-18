package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.command.NewProperty;
import com.rupeek.hotelbooking.domain.property.Amenity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record PropertyRequest(
        @NotBlank String name,
        @NotNull @Valid AddressDto address,
        @Min(1) @Max(5) int starRating,
        Set<Amenity> amenities,
        @NotEmpty @Valid List<RoomTypeRequest> roomTypes) {

    public NewProperty toCommand() {
        return new NewProperty(name, address.toAddress(), starRating, amenities,
                roomTypes.stream().map(RoomTypeRequest::toCommand).toList());
    }
}
