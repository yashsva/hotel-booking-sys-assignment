package com.rupeek.hotelbooking.infrastructure.config;

import com.rupeek.hotelbooking.application.OnboardingResult;
import com.rupeek.hotelbooking.application.OnboardingService;
import com.rupeek.hotelbooking.application.command.NewProperty;
import com.rupeek.hotelbooking.application.command.NewRoomType;
import com.rupeek.hotelbooking.domain.common.Address;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.rupeek.hotelbooking.domain.property.Amenity.BREAKFAST;
import static com.rupeek.hotelbooking.domain.property.Amenity.GYM;
import static com.rupeek.hotelbooking.domain.property.Amenity.PARKING;
import static com.rupeek.hotelbooking.domain.property.Amenity.POOL;
import static com.rupeek.hotelbooking.domain.property.Amenity.RESTAURANT;
import static com.rupeek.hotelbooking.domain.property.Amenity.SPA;
import static com.rupeek.hotelbooking.domain.property.Amenity.WIFI;

/** Loads a standalone hotel and a two-property chain when started with the {@code demo} profile. */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final OnboardingService onboarding;

    public DemoDataSeeder(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @Override
    public void run(ApplicationArguments args) {
        report(onboarding.onboardOwner("Asha Residency", List.of(
                property("Asha Residency", "Koramangala", "Bengaluru", 3, Set.of(WIFI, BREAKFAST),
                        room("Standard Double", 2, 1, "2500"),
                        room("Family Suite", 4, 2, "4800")))));

        report(onboarding.onboardOwner("Lotus Hospitality Group", List.of(
                property("Lotus Grand", "Indiranagar", "Bengaluru", 5, Set.of(WIFI, POOL, SPA, GYM, RESTAURANT),
                        room("Deluxe King", 2, 5, "7500"),
                        room("Executive Suite", 3, 2, "12000")),
                property("Lotus Bay", "Bandra", "Mumbai", 4, Set.of(WIFI, POOL, PARKING),
                        room("Sea View Twin", 2, 4, "6500")))));
    }

    private static NewProperty property(String name, String locality, String city, int stars,
                                        Set<Amenity> amenities, NewRoomType... rooms) {
        return new NewProperty(name, new Address("", locality, city), stars, amenities, List.of(rooms));
    }

    private static NewRoomType room(String name, int occupancy, int units, String price) {
        return new NewRoomType(name, occupancy, units, Money.of(price, "INR"));
    }

    private static void report(OnboardingResult result) {
        result.properties().forEach(p -> log.info("Seeded property '{}' ({}) in {} -> id={} roomTypes={}",
                p.name(), result.owner().name(), p.address().city(), p.id(),
                p.roomTypes().stream().map(rt -> rt.name() + "=" + rt.id()).toList()));
    }
}
