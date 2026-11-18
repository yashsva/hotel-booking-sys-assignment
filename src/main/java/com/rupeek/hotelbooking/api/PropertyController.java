package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.PropertyResponse;
import com.rupeek.hotelbooking.api.dto.SearchResultResponse;
import com.rupeek.hotelbooking.application.OnboardingService;
import com.rupeek.hotelbooking.application.SearchService;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Discovery", description = "Search and browse properties")
public class PropertyController {

    private final SearchService search;
    private final OnboardingService onboarding;

    public PropertyController(SearchService search, OnboardingService onboarding) {
        this.search = search;
        this.onboarding = onboarding;
    }

    /**
     * Optional filters arrive as plain query parameters and are interpreted by the registered
     * filter factories, so a new filter needs no change here.
     */
    @GetMapping("/search")
    @Operation(summary = "Find properties with rooms available for the whole stay",
            description = "Optional filters: minPrice, maxPrice (average nightly price), "
                    + "amenities (comma separated, all required), minRating (1-5)")
    public List<SearchResultResponse> search(
            @RequestParam String city,
            @RequestParam(required = false) String locality,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> allParameters) {
        SearchCriteria criteria = new SearchCriteria(city, locality, DateRange.of(checkIn, checkOut), guests);
        return search.search(criteria, allParameters).stream().map(SearchResultResponse::from).toList();
    }

    @GetMapping("/{propertyId}")
    @Operation(summary = "Get a property's details")
    public PropertyResponse get(@PathVariable String propertyId) {
        return PropertyResponse.from(onboarding.getProperty(propertyId));
    }
}
