package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.OnboardOwnerRequest;
import com.rupeek.hotelbooking.api.dto.OnboardingResponse;
import com.rupeek.hotelbooking.api.dto.OwnerResponse;
import com.rupeek.hotelbooking.api.dto.PropertyRequest;
import com.rupeek.hotelbooking.api.dto.PropertyResponse;
import com.rupeek.hotelbooking.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owners")
@Tag(name = "Onboarding", description = "Onboard standalone properties and multi-property owners")
public class OwnerController {

    private final OnboardingService onboarding;

    public OwnerController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard an owner with one (standalone) or more (chain) properties")
    public OnboardingResponse onboard(@Valid @RequestBody OnboardOwnerRequest request) {
        return OnboardingResponse.from(onboarding.onboardOwner(request.name(),
                request.properties().stream().map(PropertyRequest::toCommand).toList()));
    }

    @PostMapping("/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add another property to an existing owner")
    public PropertyResponse addProperty(@PathVariable String ownerId, @Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(onboarding.addProperty(ownerId, request.toCommand()));
    }

    @GetMapping("/{ownerId}")
    @Operation(summary = "Get an owner and the ids of its properties")
    public OwnerResponse get(@PathVariable String ownerId) {
        return OwnerResponse.from(onboarding.getOwner(ownerId));
    }
}
