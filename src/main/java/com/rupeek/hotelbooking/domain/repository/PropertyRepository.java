package com.rupeek.hotelbooking.domain.repository;

import com.rupeek.hotelbooking.domain.property.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String id);

    List<Property> findByCity(String city);
}
