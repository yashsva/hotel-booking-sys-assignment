package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.CreateBookingCommand;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.inventory.RoomUnavailableException;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.rupeek.hotelbooking.application.TestHotel.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final TestHotel hotel = new TestHotel();
    private final Property solo = hotel.singleRoomHotel();
    private final RoomType standard = solo.roomTypes().getFirst();

    private CreateBookingCommand command(int guests, int daysFromToday, int nights) {
        return new CreateBookingCommand(solo.id(), standard.id(), "Asha", guests, stay(daysFromToday, nights));
    }

    @Test
    void createsAPendingBookingPricedForTheStayAndHoldsTheRoom() {
        Booking booking = hotel.bookings.createBooking(command(2, 3, 2));

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.amount()).isEqualTo(Money.of("4000", "INR"));
        assertThat(hotel.inventory.availableUnits(standard, stay(3, 2))).isZero();
        assertThat(hotel.bookings.getBooking(booking.id())).isSameAs(booking);
    }

    @Test
    void preventsDoubleBookingForOverlappingDates() {
        hotel.bookings.createBooking(command(2, 3, 3));

        assertThatThrownBy(() -> hotel.bookings.createBooking(command(1, 5, 2)))
                .isInstanceOf(RoomUnavailableException.class);
        hotel.bookings.createBooking(command(1, 6, 2)); // starts on the previous check-out day
    }

    @Test
    void invalidRequestsNeverHoldInventory() {
        assertThatThrownBy(() -> hotel.bookings.createBooking(command(3, 3, 2)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> hotel.bookings.createBooking(command(2, -1, 2)))
                .isInstanceOf(DomainValidationException.class);

        assertThat(hotel.inventory.availableUnits(standard, stay(-1, 6))).isEqualTo(1);
    }

    @Test
    void unknownPropertyOrRoomTypeIsNotFound() {
        assertThatThrownBy(() -> hotel.bookings.createBooking(
                new CreateBookingCommand("nope", standard.id(), "Asha", 1, stay(3, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> hotel.bookings.createBooking(
                new CreateBookingCommand(solo.id(), "nope", "Asha", 1, stay(3, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> hotel.bookings.getBooking("nope")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void simultaneousBookingsOfTheLastRoomProduceExactlyOneBooking() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            attempts.add(() -> {
                try {
                    hotel.bookings.createBooking(command(1, 3, 2));
                    return true;
                } catch (RoomUnavailableException e) {
                    return false;
                }
            });
        }
        long successes = 0;
        for (Future<Boolean> result : pool.invokeAll(attempts)) {
            successes += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
    }
}
