package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rupeek.hotelbooking.application.TestHotel.property;
import static com.rupeek.hotelbooking.application.TestHotel.room;
import static com.rupeek.hotelbooking.application.TestHotel.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private static final Map<String, List<String>> NO_FILTERS = Map.of();
    private static final DateRange STAY = stay(5, 2);

    private final TestHotel hotel = new TestHotel();

    private List<PropertySearchResult> search(String city, int guests, Map<String, List<String>> filters) {
        return hotel.search.search(new SearchCriteria(city, null, STAY, guests), filters);
    }

    private static List<String> names(List<PropertySearchResult> results) {
        return results.stream().map(r -> r.property().name()).toList();
    }

    @Test
    void findsPropertiesInTheCityCheapestFirstWithQuotedPrices() {
        hotel.onboard(property("Grand", "Bengaluru", 5, Set.of(), room("King", 2, 2, "8000")));
        hotel.onboard(property("Budget", "bengaluru", 2, Set.of(), room("Basic", 2, 2, "1500")));
        hotel.onboard(property("Elsewhere", "Mumbai", 3, Set.of(), room("Basic", 2, 2, "1500")));

        List<PropertySearchResult> results = search("Bengaluru", 2, NO_FILTERS);

        assertThat(names(results)).containsExactly("Budget", "Grand");
        assertThat(results.getFirst().offers().getFirst().totalPrice()).isEqualTo(Money.of("3000", "INR"));
    }

    @Test
    void excludesRoomTypesTooSmallForTheParty() {
        hotel.onboard(property("Mixed", "Bengaluru", 3, Set.of(), room("Single", 1, 5, "1000"), room("Family", 4, 1, "4000")));
        hotel.onboard(property("Tiny", "Bengaluru", 3, Set.of(), room("Single", 1, 5, "1000")));

        List<PropertySearchResult> results = search("Bengaluru", 3, NO_FILTERS);

        assertThat(names(results)).containsExactly("Mixed");
        assertThat(results.getFirst().offers()).extracting(o -> o.roomType().name()).containsExactly("Family");
    }

    @Test
    void returnsOnlyPropertiesAvailableForTheWholeStayAndShowsThemAgainAfterCancellation() {
        Property solo = hotel.singleRoomHotel();
        Booking booking = hotel.book(solo, stay(6, 1)); // takes the second night of STAY

        assertThat(search("Bengaluru", 2, NO_FILTERS)).isEmpty();
        assertThat(hotel.search.search(new SearchCriteria("Bengaluru", null, stay(7, 2), 2), NO_FILTERS)).hasSize(1);

        hotel.cancellations.cancel(booking.id());

        assertThat(names(search("Bengaluru", 2, NO_FILTERS))).containsExactly("Solo Inn");
    }

    @Test
    void appliesPriceAmenityAndRatingFiltersTogether() {
        hotel.onboard(property("Spa Resort", "Goa", 5, Set.of(Amenity.SPA, Amenity.POOL), room("Villa", 2, 1, "9000")));
        hotel.onboard(property("Pool Hotel", "Goa", 4, Set.of(Amenity.POOL), room("Double", 2, 1, "4000")));
        hotel.onboard(property("Hostel", "Goa", 2, Set.of(Amenity.WIFI), room("Dorm", 2, 1, "800")));

        assertThat(names(search("Goa", 1, Map.of("amenities", List.of("POOL"))))).containsExactly("Pool Hotel", "Spa Resort");
        assertThat(names(search("Goa", 1, Map.of("minRating", List.of("4"), "maxPrice", List.of("5000")))))
                .containsExactly("Pool Hotel");
        assertThat(names(search("Goa", 1, Map.of("minPrice", List.of("1000"), "amenities", List.of("WIFI"))))).isEmpty();
    }

    @Test
    void filtersByLocalityWhenGiven() {
        hotel.onboard(property("Central Inn", "Bengaluru", 3, Set.of(), room("Double", 2, 1, "2000")));

        assertThat(hotel.search.search(new SearchCriteria("Bengaluru", "central", STAY, 1), NO_FILTERS)).hasSize(1);
        assertThat(hotel.search.search(new SearchCriteria("Bengaluru", "Whitefield", STAY, 1), NO_FILTERS)).isEmpty();
    }

    @Test
    void rejectsStaysStartingInThePast() {
        assertThatThrownBy(() -> hotel.search.search(new SearchCriteria("Bengaluru", null, stay(-1, 2), 1), NO_FILTERS))
                .isInstanceOf(DomainValidationException.class);
    }
}
