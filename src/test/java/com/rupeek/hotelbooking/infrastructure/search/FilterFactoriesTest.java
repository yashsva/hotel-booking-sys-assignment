package com.rupeek.hotelbooking.infrastructure.search;

import com.rupeek.hotelbooking.domain.common.Address;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.search.PropertyFilter;
import com.rupeek.hotelbooking.domain.search.PropertyFilterFactory;
import com.rupeek.hotelbooking.domain.search.RoomOffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterFactoriesTest {

    private static final RoomType ROOM = new RoomType("rt-1", "Double", 2, 1, Money.of("3000", "INR"));
    private static final Property FOUR_STAR = new Property("p-1", "o-1", "Lotus", new Address("", "Central", "Pune"), 4,
            Set.of(Amenity.WIFI, Amenity.POOL), List.of(ROOM));
    /** Two nights at 3000 = 6000 total, 3000 per night. */
    private static final RoomOffer OFFER = new RoomOffer(FOUR_STAR, ROOM, 1, Money.of("6000", "INR"), 2);

    private static PropertyFilter filter(PropertyFilterFactory factory, Map<String, List<String>> params) {
        return factory.create(params).orElseThrow();
    }

    @Test
    void factoriesStayOutOfTheWayWhenTheirParametersAreAbsent() {
        Map<String, List<String>> none = Map.of("city", List.of("Pune"));
        assertThat(new PriceRangeFilterFactory().create(none)).isEmpty();
        assertThat(new AmenitiesFilterFactory().create(none)).isEmpty();
        assertThat(new StarRatingFilterFactory().create(none)).isEmpty();
    }

    @Test
    void priceRangeUsesTheAverageNightlyPrice() {
        assertThat(filter(new PriceRangeFilterFactory(), Map.of("maxPrice", List.of("3000")))).accepts(OFFER);
        assertThat(filter(new PriceRangeFilterFactory(), Map.of("maxPrice", List.of("2999")))).rejects(OFFER);
        assertThat(filter(new PriceRangeFilterFactory(), Map.of("minPrice", List.of("3500")))).rejects(OFFER);
    }

    @Test
    void amenitiesMustAllBePresentAndAcceptCommaOrRepeatedValues() {
        AmenitiesFilterFactory factory = new AmenitiesFilterFactory();
        assertThat(filter(factory, Map.of("amenities", List.of("wifi,pool")))).accepts(OFFER);
        assertThat(filter(factory, Map.of("amenities", List.of("WIFI", "SPA")))).rejects(OFFER);
    }

    @Test
    void starRatingIsAMinimum() {
        assertThat(filter(new StarRatingFilterFactory(), Map.of("minRating", List.of("4")))).accepts(OFFER);
        assertThat(filter(new StarRatingFilterFactory(), Map.of("minRating", List.of("5")))).rejects(OFFER);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThatThrownBy(() -> new PriceRangeFilterFactory().create(Map.of("minPrice", List.of("cheap"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PriceRangeFilterFactory().create(
                Map.of("minPrice", List.of("500"), "maxPrice", List.of("100"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new AmenitiesFilterFactory().create(Map.of("amenities", List.of("JACUZZI"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new StarRatingFilterFactory().create(Map.of("minRating", List.of("6"))))
                .isInstanceOf(DomainValidationException.class);
    }
}
