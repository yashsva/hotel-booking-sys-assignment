package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/** One payment attempt for a booking, identified by the client's idempotency key. */
public final class Payment {

    private final String id;
    private final String bookingId;
    private final String idempotencyKey;
    private final String method;
    private final Money amount;
    private final String gatewayReference;
    private final String failureReason;
    private final Instant createdAt;

    private PaymentStatus status;
    private Money refundedAmount;

    private Payment(String id, String bookingId, String idempotencyKey, String method, Money amount,
                    PaymentResult result, Instant createdAt) {
        this.id = requireNonBlank(id, "payment.id");
        this.bookingId = requireNonBlank(bookingId, "bookingId");
        this.idempotencyKey = requireNonBlank(idempotencyKey, "idempotencyKey");
        this.method = requireNonBlank(method, "method");
        this.amount = requireNonNull(amount, "amount");
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.status = result.successful() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.gatewayReference = result.gatewayReference();
        this.failureReason = result.failureReason();
    }

    public static Payment record(String id, String bookingId, String idempotencyKey, String method,
                                 Money amount, PaymentResult result, Instant createdAt) {
        return new Payment(id, bookingId, idempotencyKey, method, amount, result, createdAt);
    }

    public synchronized void markRefunded(Money refund) {
        require(status == PaymentStatus.SUCCEEDED, "only a successful payment can be refunded");
        require(refund.compareTo(amount) <= 0, "refund cannot exceed the amount paid");
        this.refundedAmount = refund;
        this.status = PaymentStatus.REFUNDED;
    }

    public boolean isSuccessful() {
        return status() == PaymentStatus.SUCCEEDED;
    }

    public String id() { return id; }
    public String bookingId() { return bookingId; }
    public String idempotencyKey() { return idempotencyKey; }
    public String method() { return method; }
    public Money amount() { return amount; }
    public Optional<String> gatewayReference() { return Optional.ofNullable(gatewayReference); }
    public Optional<String> failureReason() { return Optional.ofNullable(failureReason); }
    public Instant createdAt() { return createdAt; }
    public synchronized PaymentStatus status() { return status; }
    public synchronized Optional<Money> refundedAmount() { return Optional.ofNullable(refundedAmount); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Payment other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
