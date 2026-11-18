package com.rupeek.hotelbooking.infrastructure.search;

import com.rupeek.hotelbooking.domain.common.DomainValidationException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Shared parsing for raw query parameters, so each factory only names its keys. */
final class FilterParameters {

    private FilterParameters() {
    }

    static Optional<String> single(Map<String, List<String>> parameters, String key) {
        return Optional.ofNullable(parameters.get(key))
                .flatMap(values -> values.stream().filter(v -> v != null && !v.isBlank()).findFirst())
                .map(String::trim);
    }

    /** Supports both {@code ?k=a&k=b} and {@code ?k=a,b}. */
    static List<String> multi(Map<String, List<String>> parameters, String key) {
        return parameters.getOrDefault(key, List.of()).stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    static Optional<BigDecimal> decimal(Map<String, List<String>> parameters, String key) {
        return single(parameters, key).map(value -> parse(key, value, BigDecimal::new));
    }

    static Optional<Integer> integer(Map<String, List<String>> parameters, String key) {
        return single(parameters, key).map(value -> parse(key, value, Integer::valueOf));
    }

    static <T> T parse(String key, String value, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new DomainValidationException("Invalid value '" + value + "' for parameter '" + key + "'");
        }
    }
}
