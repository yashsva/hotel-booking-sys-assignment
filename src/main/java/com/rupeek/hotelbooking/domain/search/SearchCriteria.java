package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.common.DateRange;

import java.util.Optional;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/** The mandatory part of a search: where, when and for how many. Optional narrowing lives in {@link PropertyFilter}s. */
public record SearchCriteria(String city, String locality, DateRange stay, int guests) {

    public SearchCriteria {
        city = requireNonBlank(city, "city");
        locality = locality == null || locality.isBlank() ? null : locality.trim();
        requireNonNull(stay, "stay");
        require(guests > 0, "guests must be at least 1");
    }

    public Optional<String> localityFilter() {
        return Optional.ofNullable(locality);
    }
}
