package com.rupeek.hotelbooking.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** A standalone hotel sends one property; a chain sends several. */
public record OnboardOwnerRequest(@NotBlank String name, @NotEmpty @Valid List<PropertyRequest> properties) {
}
