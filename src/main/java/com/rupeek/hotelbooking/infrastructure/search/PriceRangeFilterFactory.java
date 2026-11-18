package com.rupeek.hotelbooking.infrastructure.search;

import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.search.PropertyFilter;
import com.rupeek.hotelbooking.domain.search.PropertyFilterFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code minPrice} / {@code maxPrice}: bounds on the average nightly price of a room offer. */
@Component
public class PriceRangeFilterFactory implements PropertyFilterFactory {

    static final String MIN_PRICE = "minPrice";
    static final String MAX_PRICE = "maxPrice";

    @Override
    public Optional<PropertyFilter> create(Map<String, List<String>> parameters) {
        Optional<BigDecimal> min = FilterParameters.decimal(parameters, MIN_PRICE);
        Optional<BigDecimal> max = FilterParameters.decimal(parameters, MAX_PRICE);
        if (min.isEmpty() && max.isEmpty()) {
            return Optional.empty();
        }
        if (min.isPresent() && max.isPresent() && min.get().compareTo(max.get()) > 0) {
            throw new DomainValidationException(MIN_PRICE + " must not exceed " + MAX_PRICE);
        }
        return Optional.of(offer -> {
            BigDecimal nightly = offer.averageNightlyPrice().amount();
            return min.map(m -> nightly.compareTo(m) >= 0).orElse(true)
                    && max.map(m -> nightly.compareTo(m) <= 0).orElse(true);
        });
    }
}
