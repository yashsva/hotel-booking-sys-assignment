package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.OnboardingResult;

import java.util.List;

public record OnboardingResponse(OwnerResponse owner, List<PropertyResponse> properties) {

    public static OnboardingResponse from(OnboardingResult result) {
        return new OnboardingResponse(OwnerResponse.from(result.owner()),
                result.properties().stream().map(PropertyResponse::from).toList());
    }
}
