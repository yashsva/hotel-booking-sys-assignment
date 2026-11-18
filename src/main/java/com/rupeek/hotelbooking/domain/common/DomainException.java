package com.rupeek.hotelbooking.domain.common;

/**
 * Base type for all business-rule violations raised by the domain.
 * The API layer maps each concrete subtype to an HTTP status.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
