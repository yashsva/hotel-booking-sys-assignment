package com.rupeek.hotelbooking.domain.repository;

import com.rupeek.hotelbooking.domain.property.PropertyOwner;

import java.util.Optional;

public interface OwnerRepository {

    PropertyOwner save(PropertyOwner owner);

    Optional<PropertyOwner> findById(String id);
}
