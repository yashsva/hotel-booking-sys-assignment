package com.rupeek.hotelbooking.infrastructure.search;

import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.search.PropertyFilter;
import com.rupeek.hotelbooking.domain.search.PropertyFilterFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code minRating=4}: only properties rated at least this many stars. */
@Component
public class StarRatingFilterFactory implements PropertyFilterFactory {

    static final String MIN_RATING = "minRating";

    @Override
    public Optional<PropertyFilter> create(Map<String, List<String>> parameters) {
        return FilterParameters.integer(parameters, MIN_RATING).map(minRating -> {
            if (minRating < 1 || minRating > 5) {
                throw new DomainValidationException(MIN_RATING + " must be between 1 and 5");
            }
            return offer -> offer.property().starRating() >= minRating;
        });
    }
}
