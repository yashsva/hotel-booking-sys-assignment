package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.PropertyOwner;

import java.util.List;

public record OwnerResponse(String id, String name, boolean multiProperty, List<String> propertyIds) {

    public static OwnerResponse from(PropertyOwner owner) {
        return new OwnerResponse(owner.id(), owner.name(), owner.isMultiProperty(), owner.propertyIds());
    }
}
