package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.command.NewProperty;
import com.rupeek.hotelbooking.application.command.NewRoomType;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.repository.OwnerRepository;
import com.rupeek.hotelbooking.domain.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;

/**
 * Onboards properties. There is one path for everyone: a standalone hotel is an owner
 * onboarded with one property, a chain is an owner onboarded with several.
 */
@Service
public class OnboardingService {

    private final OwnerRepository owners;
    private final PropertyRepository properties;

    public OnboardingService(OwnerRepository owners, PropertyRepository properties) {
        this.owners = owners;
        this.properties = properties;
    }

    public OnboardingResult onboardOwner(String ownerName, List<NewProperty> newProperties) {
        require(newProperties != null && !newProperties.isEmpty(), "an owner must be onboarded with at least one property");
        PropertyOwner owner = new PropertyOwner(newId(), ownerName);
        // Build every property first so one invalid property rejects the whole request.
        List<Property> built = newProperties.stream().map(np -> buildProperty(owner.id(), np)).toList();
        built.forEach(owner::addProperty);
        owners.save(owner);
        built.forEach(properties::save);
        return new OnboardingResult(owner, built);
    }

    public Property addProperty(String ownerId, NewProperty newProperty) {
        PropertyOwner owner = getOwner(ownerId);
        Property property = buildProperty(owner.id(), newProperty);
        owner.addProperty(property);
        properties.save(property);
        owners.save(owner);
        return property;
    }

    public PropertyOwner getOwner(String ownerId) {
        return owners.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
    }

    public Property getProperty(String propertyId) {
        return properties.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
    }

    private Property buildProperty(String ownerId, NewProperty np) {
        require(np.roomTypes() != null, "a property needs at least one room type");
        List<RoomType> roomTypes = np.roomTypes().stream().map(this::buildRoomType).toList();
        return new Property(newId(), ownerId, np.name(), np.address(), np.starRating(), np.amenities(), roomTypes);
    }

    private RoomType buildRoomType(NewRoomType rt) {
        return new RoomType(newId(), rt.name(), rt.maxOccupancy(), rt.totalUnits(), rt.basePricePerNight());
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
