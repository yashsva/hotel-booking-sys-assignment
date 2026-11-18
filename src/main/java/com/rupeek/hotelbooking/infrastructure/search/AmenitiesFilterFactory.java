package com.rupeek.hotelbooking.infrastructure.search;

import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.search.PropertyFilter;
import com.rupeek.hotelbooking.domain.search.PropertyFilterFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** {@code amenities=WIFI,POOL}: the property must offer every listed amenity. */
@Component
public class AmenitiesFilterFactory implements PropertyFilterFactory {

    static final String AMENITIES = "amenities";

    @Override
    public Optional<PropertyFilter> create(Map<String, List<String>> parameters) {
        List<String> raw = FilterParameters.multi(parameters, AMENITIES);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        Set<Amenity> required = raw.stream()
                .map(value -> FilterParameters.parse(AMENITIES, value,
                        v -> Amenity.valueOf(v.toUpperCase(Locale.ROOT))))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Amenity.class)));
        return Optional.of(offer -> offer.property().hasAllAmenities(required));
    }
}
