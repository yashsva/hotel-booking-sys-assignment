package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.BookingResponse;
import com.rupeek.hotelbooking.api.dto.CancellationResponse;
import com.rupeek.hotelbooking.api.dto.CreateBookingRequest;
import com.rupeek.hotelbooking.application.BookingService;
import com.rupeek.hotelbooking.application.CancellationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Book, view and cancel")
public class BookingController {

    private final BookingService bookings;
    private final CancellationService cancellations;

    public BookingController(BookingService bookings, CancellationService cancellations) {
        this.bookings = bookings;
        this.cancellations = cancellations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold a room for a stay; the booking stays PENDING_PAYMENT until paid")
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookings.createBooking(request.toCommand()));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get a booking and its current state")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookings.getBooking(bookingId));
    }

    @PostMapping("/{bookingId}/cancellation")
    @Operation(summary = "Cancel a booking, refund per the cancellation policy and release the room")
    public CancellationResponse cancel(@PathVariable String bookingId) {
        return CancellationResponse.from(cancellations.cancel(bookingId));
    }
}
