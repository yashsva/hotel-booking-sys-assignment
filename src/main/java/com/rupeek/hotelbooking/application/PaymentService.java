package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.PayCommand;
import com.rupeek.hotelbooking.application.concurrency.LockManager;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryLedger;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentProcessor;
import com.rupeek.hotelbooking.domain.payment.PaymentRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentResult;
import com.rupeek.hotelbooking.domain.repository.BookingRepository;
import com.rupeek.hotelbooking.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

/**
 * Takes payment for a booking and lets the outcome drive the booking:
 * success confirms it, failure marks it failed and frees the room.
 * Requests are idempotent per {@code Idempotency-Key}: a retry returns the original result
 * without charging again.
 */
@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PaymentProcessorRegistry processors;
    private final InventoryLedger inventory;
    private final LockManager locks;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, PaymentRepository payments, PaymentProcessorRegistry processors,
                          InventoryLedger inventory, LockManager locks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.processors = processors;
        this.inventory = inventory;
        this.locks = locks;
        this.clock = clock;
    }

    public PaymentOutcome pay(PayCommand command) {
        requireNonBlank(command.idempotencyKey(), "Idempotency-Key");
        return locks.withLock(BookingLocks.key(command.bookingId()), () -> {
            Booking booking = loadBooking(command.bookingId());
            Optional<Payment> previous = payments.findByIdempotencyKey(command.idempotencyKey());
            if (previous.isPresent()) {
                return replay(previous.get(), booking);
            }
            booking.ensureCanTransitionTo(BookingStatus.CONFIRMED);
            PaymentProcessor processor = processors.processorFor(command.method());

            PaymentResult result = processor.process(
                    new PaymentRequest(booking.id(), booking.amount(), command.details()));
            Payment payment = payments.save(Payment.record(UUID.randomUUID().toString(), booking.id(),
                    command.idempotencyKey(), processor.method(), booking.amount(), result, clock.instant()));

            applyOutcome(booking, payment);
            bookings.save(booking);
            return new PaymentOutcome(payment, booking, false);
        });
    }

    private PaymentOutcome replay(Payment previous, Booking booking) {
        if (!previous.bookingId().equals(booking.id())) {
            throw new DomainValidationException("Idempotency-Key was already used for a different booking");
        }
        return new PaymentOutcome(previous, booking, true);
    }

    private void applyOutcome(Booking booking, Payment payment) {
        if (payment.isSuccessful()) {
            booking.confirm(payment.id());
        } else {
            booking.markPaymentFailed();
            inventory.release(booking.roomTypeId(), booking.stay());
        }
    }

    private Booking loadBooking(String bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }
}
