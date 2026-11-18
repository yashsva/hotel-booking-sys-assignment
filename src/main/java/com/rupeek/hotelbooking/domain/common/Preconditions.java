package com.rupeek.hotelbooking.domain.common;

/** Tiny guard helpers so domain constructors stay one line per invariant. */
public final class Preconditions {

    private Preconditions() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainValidationException(message);
        }
    }

    public static <T> T requireNonNull(T value, String field) {
        require(value != null, field + " is required");
        return value;
    }

    public static String requireNonBlank(String value, String field) {
        require(value != null && !value.isBlank(), field + " must not be blank");
        return value.trim();
    }
}
