package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.PayRequest;
import com.rupeek.hotelbooking.api.dto.PaymentResponse;
import com.rupeek.hotelbooking.application.PaymentService;
import com.rupeek.hotelbooking.application.command.PayCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payments")
@Tag(name = "Payments", description = "Pay for a booking")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    @Operation(summary = "Pay for a booking with CARD, UPI or WALLET",
            description = "Send the same Idempotency-Key when retrying; the original result is returned and "
                    + "nothing is charged twice. Instruments starting with 'fail' are declined by the mock gateway.")
    public PaymentResponse pay(@PathVariable String bookingId,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @Valid @RequestBody PayRequest request) {
        return PaymentResponse.from(payments.pay(
                new PayCommand(bookingId, idempotencyKey, request.method(), request.details())));
    }
}
