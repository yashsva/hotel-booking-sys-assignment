package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import com.rupeek.hotelbooking.domain.repository.OwnerRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<String, PropertyOwner> owners = new ConcurrentHashMap<>();

    @Override
    public PropertyOwner save(PropertyOwner owner) {
        owners.put(owner.id(), owner);
        return owner;
    }

    @Override
    public Optional<PropertyOwner> findById(String id) {
        return Optional.ofNullable(owners.get(id));
    }
}
