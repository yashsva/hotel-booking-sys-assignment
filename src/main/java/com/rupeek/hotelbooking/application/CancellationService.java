package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.concurrency.LockManager;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.RefundDecision;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryLedger;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentResult;
import com.rupeek.hotelbooking.domain.payment.RefundFailedException;
import com.rupeek.hotelbooking.domain.repository.BookingRepository;
import com.rupeek.hotelbooking.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Cancels a booking: applies the refund policy (only paid bookings get money back),
 * refunds through the gateway, then frees the room so it is searchable and bookable again.
 */
@Service
public class CancellationService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final CancellationPolicy policy;
    private final PaymentGateway gateway;
    private final InventoryLedger inventory;
    private final LockManager locks;
    private final Clock clock;

    public CancellationService(BookingRepository bookings, PaymentRepository payments, CancellationPolicy policy,
                               PaymentGateway gateway, InventoryLedger inventory, LockManager locks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.policy = policy;
        this.gateway = gateway;
        this.inventory = inventory;
        this.locks = locks;
        this.clock = clock;
    }

    public CancellationOutcome cancel(String bookingId) {
        return locks.withLock(BookingLocks.key(bookingId), () -> {
            Booking booking = bookings.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
            // Fail fast before any money moves if the booking cannot be cancelled.
            booking.ensureCanTransitionTo(BookingStatus.CANCELLED);
            Instant now = clock.instant();

            RefundDecision decision = decideRefund(booking, now);
            if (decision.hasRefund()) {
                refund(booking, decision.refundAmount());
            }
            booking.cancel(decision.refundAmount(), now);
            bookings.save(booking);
            inventory.release(booking.roomTypeId(), booking.stay());
            return new CancellationOutcome(booking, decision);
        });
    }

    private RefundDecision decideRefund(Booking booking, Instant now) {
        if (booking.status() != BookingStatus.CONFIRMED) {
            return new RefundDecision(Money.zero(booking.amount().currency()), "Booking was not paid: nothing to refund");
        }
        return policy.evaluate(booking, now);
    }

    private void refund(Booking booking, Money amount) {
        String paymentId = booking.paymentId().orElseThrow();
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        PaymentResult result = gateway.refund(payment.gatewayReference().orElseThrow(), amount);
        if (!result.successful()) {
            throw new RefundFailedException(payment.id(), result.failureReason());
        }
        payment.markRefunded(amount);
        payments.save(payment);
    }
}
