package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.payment.PaymentProcessor;
import com.rupeek.hotelbooking.domain.payment.UnsupportedPaymentMethodException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks up the processor for a payment method. Every {@link PaymentProcessor} bean registers itself. */
@Component
public class PaymentProcessorRegistry {

    private final Map<String, PaymentProcessor> processorsByMethod;

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        this.processorsByMethod = processors.stream()
                .collect(Collectors.toUnmodifiableMap(p -> normalise(p.method()), Function.identity()));
    }

    public PaymentProcessor processorFor(String method) {
        PaymentProcessor processor = method == null ? null : processorsByMethod.get(normalise(method));
        if (processor == null) {
            throw new UnsupportedPaymentMethodException(method, supportedMethods());
        }
        return processor;
    }

    public Set<String> supportedMethods() {
        return new TreeSet<>(processorsByMethod.keySet());
    }

    private static String normalise(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }
}
