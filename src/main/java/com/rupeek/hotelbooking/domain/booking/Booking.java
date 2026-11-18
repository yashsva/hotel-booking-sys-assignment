package com.rupeek.hotelbooking.domain.booking;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/**
 * A guest's reservation of one room of a {@link RoomType} for a {@link DateRange}.
 * State only changes through the intent methods below, each guarded by {@link BookingStatus}.
 */
public final class Booking {

    private final String id;
    private final String propertyId;
    private final String roomTypeId;
    private final String guestName;
    private final int guests;
    private final DateRange stay;
    private final Money amount;
    private final Instant createdAt;

    private BookingStatus status;
    private String paymentId;
    private Money refundAmount;
    private Instant cancelledAt;

    private Booking(String id, String propertyId, RoomType roomType, String guestName, int guests,
                    DateRange stay, Money amount, Instant createdAt) {
        this.id = requireNonBlank(id, "booking.id");
        this.propertyId = requireNonBlank(propertyId, "propertyId");
        requireNonNull(roomType, "roomType");
        this.guestName = requireNonBlank(guestName, "guestName");
        require(guests > 0, "guests must be at least 1");
        require(roomType.canAccommodate(guests),
                "room type " + roomType.name() + " fits at most " + roomType.maxOccupancy() + " guests");
        this.roomTypeId = roomType.id();
        this.guests = guests;
        this.stay = requireNonNull(stay, "stay");
        this.amount = requireNonNull(amount, "amount");
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    public static Booking create(String id, String propertyId, RoomType roomType, String guestName,
                                 int guests, DateRange stay, Money amount, Instant now) {
        return new Booking(id, propertyId, roomType, guestName, guests, stay, amount, now);
    }

    public synchronized void confirm(String paymentId) {
        transitionTo(BookingStatus.CONFIRMED);
        this.paymentId = requireNonBlank(paymentId, "paymentId");
    }

    public synchronized void markPaymentFailed() {
        transitionTo(BookingStatus.PAYMENT_FAILED);
    }

    public synchronized void cancel(Money refund, Instant at) {
        transitionTo(BookingStatus.CANCELLED);
        this.refundAmount = requireNonNull(refund, "refund");
        this.cancelledAt = requireNonNull(at, "cancelledAt");
    }

    public synchronized void ensureCanTransitionTo(BookingStatus next) {
        status.ensureCanTransitionTo(next);
    }

    private void transitionTo(BookingStatus next) {
        status.ensureCanTransitionTo(next);
        status = next;
    }

    public String id() { return id; }
    public String propertyId() { return propertyId; }
    public String roomTypeId() { return roomTypeId; }
    public String guestName() { return guestName; }
    public int guests() { return guests; }
    public DateRange stay() { return stay; }
    public Money amount() { return amount; }
    public Instant createdAt() { return createdAt; }
    public synchronized BookingStatus status() { return status; }
    public synchronized Optional<String> paymentId() { return Optional.ofNullable(paymentId); }
    public synchronized Optional<Money> refundAmount() { return Optional.ofNullable(refundAmount); }
    public synchronized Optional<Instant> cancelledAt() { return Optional.ofNullable(cancelledAt); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Booking other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
