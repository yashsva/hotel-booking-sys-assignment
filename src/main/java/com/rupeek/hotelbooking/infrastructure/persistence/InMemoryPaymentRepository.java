package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, String> paymentIdByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        String existingId = paymentIdByIdempotencyKey.putIfAbsent(payment.idempotencyKey(), payment.id());
        if (existingId != null && !existingId.equals(payment.id())) {
            throw new DomainValidationException("Idempotency key already used: " + payment.idempotencyKey());
        }
        payments.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(paymentIdByIdempotencyKey.get(idempotencyKey)).map(payments::get);
    }
}
