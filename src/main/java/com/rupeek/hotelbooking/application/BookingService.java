package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.CreateBookingCommand;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryLedger;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.repository.BookingRepository;
import com.rupeek.hotelbooking.domain.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Creates bookings. A new booking holds one room for its stay until it is paid, fails payment or is cancelled. */
@Service
public class BookingService {

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final InventoryLedger inventory;
    private final PricingStrategy pricing;
    private final Clock clock;

    public BookingService(PropertyRepository properties, BookingRepository bookings, InventoryLedger inventory,
                          PricingStrategy pricing, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.inventory = inventory;
        this.pricing = pricing;
        this.clock = clock;
    }

    public Booking createBooking(CreateBookingCommand command) {
        command.stay().requireNotStartingBefore(LocalDate.now(clock));
        Property property = properties.findById(command.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", command.propertyId()));
        RoomType roomType = property.roomType(command.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type", command.roomTypeId()));

        Money amount = pricing.quote(roomType, command.stay(), command.guests());
        // Validate everything before touching inventory, so a bad request never holds a room.
        Booking booking = Booking.create(UUID.randomUUID().toString(), property.id(), roomType,
                command.guestName(), command.guests(), command.stay(), amount, clock.instant());

        inventory.reserve(roomType, command.stay());
        return bookings.save(booking);
    }

    public Booking getBooking(String bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }
}
