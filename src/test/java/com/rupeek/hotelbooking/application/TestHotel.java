package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.CreateBookingCommand;
import com.rupeek.hotelbooking.application.command.NewProperty;
import com.rupeek.hotelbooking.application.command.NewRoomType;
import com.rupeek.hotelbooking.application.command.PayCommand;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.common.Address;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.PaymentResult;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.infrastructure.concurrency.StripedLockManager;
import com.rupeek.hotelbooking.infrastructure.payment.CardPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.payment.MockPaymentGateway;
import com.rupeek.hotelbooking.infrastructure.payment.UpiPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.payment.WalletPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryInventoryLedger;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryOwnerRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryPropertyRepository;
import com.rupeek.hotelbooking.infrastructure.policy.BaseNightlyPricing;
import com.rupeek.hotelbooking.infrastructure.policy.TieredCancellationPolicy;
import com.rupeek.hotelbooking.infrastructure.search.AmenitiesFilterFactory;
import com.rupeek.hotelbooking.infrastructure.search.PriceRangeFilterFactory;
import com.rupeek.hotelbooking.infrastructure.search.StarRatingFilterFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The whole application wired by hand with the in-memory adapters and a fixed clock.
 * Tests exercise real collaborators instead of mocks, so they pin down behaviour, not calls.
 */
public class TestHotel {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    /** "Now" in every test: 10 Jan 2027, 12:00 IST. */
    public static final Instant NOW = LocalDate.of(2027, 1, 10).atTime(12, 0).atZone(ZONE).toInstant();
    public static final LocalDate TODAY = LocalDate.of(2027, 1, 10);

    public final Clock clock = Clock.fixed(NOW, ZONE);
    public final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    public final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    public final InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
    public final InMemoryInventoryLedger inventory = new InMemoryInventoryLedger();
    public final CountingGateway gateway = new CountingGateway();
    public final BaseNightlyPricing pricing = new BaseNightlyPricing();

    public final OnboardingService onboarding = new OnboardingService(new InMemoryOwnerRepository(), properties);
    public final SearchService search = new SearchService(properties, inventory, pricing,
            List.of(new PriceRangeFilterFactory(), new AmenitiesFilterFactory(), new StarRatingFilterFactory()), clock);
    public final BookingService bookings = new BookingService(properties, bookingRepository, inventory, pricing, clock);
    public final PaymentService payments = new PaymentService(bookingRepository, paymentRepository,
            new PaymentProcessorRegistry(List.of(new CardPaymentProcessor(gateway), new UpiPaymentProcessor(gateway),
                    new WalletPaymentProcessor(gateway))),
            inventory, new StripedLockManager(), clock);
    public final CancellationService cancellations = new CancellationService(bookingRepository, paymentRepository,
            new TieredCancellationPolicy(ZONE, LocalTime.NOON, 48, 24, 50), gateway, inventory,
            new StripedLockManager(), clock);

    // ---- builders -------------------------------------------------------------------------

    public static NewRoomType room(String name, int occupancy, int units, String pricePerNight) {
        return new NewRoomType(name, occupancy, units, Money.of(pricePerNight, "INR"));
    }

    public static NewProperty property(String name, String city, int stars, Set<Amenity> amenities, NewRoomType... rooms) {
        return new NewProperty(name, new Address("1 Main Road", "Central", city), stars, amenities, List.of(rooms));
    }

    public Property onboard(NewProperty property) {
        return onboarding.onboardOwner(property.name() + " Owner", List.of(property)).properties().getFirst();
    }

    /** A 3-star Bengaluru hotel with a single unit of one room type: handy for availability tests. */
    public Property singleRoomHotel() {
        return onboard(property("Solo Inn", "Bengaluru", 3, Set.of(Amenity.WIFI), room("Standard", 2, 1, "2000")));
    }

    public static DateRange stay(int daysFromToday, int nights) {
        LocalDate checkIn = TODAY.plusDays(daysFromToday);
        return DateRange.of(checkIn, checkIn.plusDays(nights));
    }

    public Booking book(Property property, DateRange stay) {
        RoomType roomType = property.roomTypes().getFirst();
        return bookings.createBooking(new CreateBookingCommand(property.id(), roomType.id(), "Guest", 2, stay));
    }

    public PaymentOutcome payByCard(Booking booking, String cardToken) {
        return payments.pay(new PayCommand(booking.id(), UUID.randomUUID().toString(), "CARD",
                Map.of("cardToken", cardToken)));
    }

    /** Real mock gateway that also counts charges, to prove idempotency. */
    public static class CountingGateway extends MockPaymentGateway {
        public final AtomicInteger charges = new AtomicInteger();
        public final AtomicInteger refunds = new AtomicInteger();

        @Override
        public PaymentResult charge(String instrument, Money amount, String reference) {
            charges.incrementAndGet();
            return super.charge(instrument, amount, reference);
        }

        @Override
        public PaymentResult refund(String gatewayReference, Money amount) {
            refunds.incrementAndGet();
            return super.refund(gatewayReference, amount);
        }
    }
}
