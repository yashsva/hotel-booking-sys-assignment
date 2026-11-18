package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.rupeek.hotelbooking.application.TestHotel.property;
import static com.rupeek.hotelbooking.application.TestHotel.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingServiceTest {

    private final TestHotel hotel = new TestHotel();
    private final OnboardingService onboarding = hotel.onboarding;

    @Test
    void aStandaloneHotelIsAnOwnerWithOneProperty() {
        OnboardingResult result = onboarding.onboardOwner("Asha Residency",
                List.of(property("Asha Residency", "Bengaluru", 3, Set.of(), room("Double", 2, 4, "2500"))));

        assertThat(result.owner().isMultiProperty()).isFalse();
        assertThat(result.owner().propertyIds()).containsExactly(result.properties().getFirst().id());
        assertThat(result.properties().getFirst().ownerId()).isEqualTo(result.owner().id());
    }

    @Test
    void aChainUsesTheSamePathWithSeveralPropertiesAndCanGrow() {
        OnboardingResult chain = onboarding.onboardOwner("Lotus Group", List.of(
                property("Lotus Grand", "Bengaluru", 5, Set.of(Amenity.POOL), room("King", 2, 5, "7500")),
                property("Lotus Bay", "Mumbai", 4, Set.of(), room("Twin", 2, 3, "6500"))));

        Property third = onboarding.addProperty(chain.owner().id(),
                property("Lotus Hills", "Ooty", 4, Set.of(), room("Cottage", 4, 2, "9000")));

        assertThat(chain.owner().isMultiProperty()).isTrue();
        assertThat(onboarding.getOwner(chain.owner().id()).propertyIds()).hasSize(3).contains(third.id());
        assertThat(onboarding.getProperty(third.id()).ownerId()).isEqualTo(chain.owner().id());
    }

    @Test
    void oneInvalidPropertyRejectsTheWholeOnboarding() {
        assertThatThrownBy(() -> onboarding.onboardOwner("Broken Group", List.of(
                property("Fine", "Pune", 3, Set.of(), room("Double", 2, 1, "2000")),
                property("Bad Rating", "Pune", 7, Set.of(), room("Double", 2, 1, "2000")))))
                .isInstanceOf(DomainValidationException.class);

        assertThat(hotel.properties.findByCity("Pune")).isEmpty();
    }

    @Test
    void validatesPropertyDetails() {
        assertThatThrownBy(() -> onboarding.onboardOwner("Owner", List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> hotel.onboard(property("No Rooms", "Pune", 3, Set.of())))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> hotel.onboard(property("Dupes", "Pune", 3, Set.of(),
                room("Double", 2, 1, "2000"), room("double", 2, 1, "2100"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> hotel.onboard(property("Zero", "Pune", 3, Set.of(), room("Double", 2, 0, "2000"))))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void addingToAnUnknownOwnerFails() {
        assertThatThrownBy(() -> onboarding.addProperty("missing",
                property("X", "Pune", 3, Set.of(), room("Double", 2, 1, "2000"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
