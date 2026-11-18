package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.repository.PropertyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> properties = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        properties.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String id) {
        return Optional.ofNullable(properties.get(id));
    }

    @Override
    public List<Property> findByCity(String city) {
        return properties.values().stream()
                .filter(property -> property.address().isInCity(city))
                .toList();
    }
}
