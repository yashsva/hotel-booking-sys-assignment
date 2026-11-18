package com.rupeek.hotelbooking.domain.repository;

import com.rupeek.hotelbooking.domain.payment.Payment;

import java.util.Optional;

public interface PaymentRepository {

    /** Saves the payment; fails if another payment already uses its idempotency key. */
    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
