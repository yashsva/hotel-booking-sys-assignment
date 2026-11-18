package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.inventory.InventoryLedger;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.repository.PropertyRepository;
import com.rupeek.hotelbooking.domain.search.PropertyFilter;
import com.rupeek.hotelbooking.domain.search.PropertyFilterFactory;
import com.rupeek.hotelbooking.domain.search.RoomOffer;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Finds properties with at least one room type that fits the party, is free on every night
 * of the stay and passes all requested filters.
 */
@Service
public class SearchService {

    private final PropertyRepository properties;
    private final InventoryLedger inventory;
    private final PricingStrategy pricing;
    private final List<PropertyFilterFactory> filterFactories;
    private final Clock clock;

    public SearchService(PropertyRepository properties, InventoryLedger inventory, PricingStrategy pricing,
                         List<PropertyFilterFactory> filterFactories, Clock clock) {
        this.properties = properties;
        this.inventory = inventory;
        this.pricing = pricing;
        this.filterFactories = filterFactories;
        this.clock = clock;
    }

    public List<PropertySearchResult> search(SearchCriteria criteria, Map<String, List<String>> filterParameters) {
        criteria.stay().requireNotStartingBefore(LocalDate.now(clock));
        Predicate<RoomOffer> filters = buildFilters(filterParameters);

        return properties.findByCity(criteria.city()).stream()
                .filter(property -> criteria.localityFilter().map(property.address()::isInLocality).orElse(true))
                .map(property -> new PropertySearchResult(property, availableOffers(property, criteria, filters)))
                .filter(result -> !result.offers().isEmpty())
                .sorted(Comparator.comparing(PropertySearchResult::lowestTotalPrice))
                .toList();
    }

    private Predicate<RoomOffer> buildFilters(Map<String, List<String>> parameters) {
        return filterFactories.stream()
                .map(factory -> factory.create(parameters))
                .flatMap(Optional::stream)
                .map(filter -> (Predicate<RoomOffer>) filter)
                .reduce(offer -> true, Predicate::and);
    }

    private List<RoomOffer> availableOffers(Property property, SearchCriteria criteria, Predicate<RoomOffer> filters) {
        return property.roomTypes().stream()
                .filter(roomType -> roomType.canAccommodate(criteria.guests()))
                .map(roomType -> toOffer(property, roomType, criteria))
                .flatMap(Optional::stream)
                .filter(filters)
                .toList();
    }

    private Optional<RoomOffer> toOffer(Property property, RoomType roomType, SearchCriteria criteria) {
        int free = inventory.availableUnits(roomType, criteria.stay());
        if (free < 1) {
            return Optional.empty();
        }
        return Optional.of(new RoomOffer(property, roomType, free,
                pricing.quote(roomType, criteria.stay(), criteria.guests()), criteria.stay().nights()));
    }
}
