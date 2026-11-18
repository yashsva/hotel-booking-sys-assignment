package com.rupeek.hotelbooking.domain.common;

/** Input that breaks a domain invariant (bad dates, too many guests, malformed payment details, ...). */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
