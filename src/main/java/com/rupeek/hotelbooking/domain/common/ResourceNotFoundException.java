package com.rupeek.hotelbooking.domain.common;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }
}
