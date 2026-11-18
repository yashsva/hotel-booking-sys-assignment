package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Address;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/** A bookable hotel. Immutable once onboarded; always belongs to exactly one {@link PropertyOwner}. */
public final class Property {

    private final String id;
    private final String ownerId;
    private final String name;
    private final Address address;
    private final int starRating;
    private final Set<Amenity> amenities;
    private final List<RoomType> roomTypes;

    public Property(String id, String ownerId, String name, Address address, int starRating,
                    Set<Amenity> amenities, List<RoomType> roomTypes) {
        this.id = requireNonBlank(id, "property.id");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.name = requireNonBlank(name, "property.name");
        this.address = requireNonNull(address, "address");
        require(starRating >= 1 && starRating <= 5, "starRating must be between 1 and 5");
        this.starRating = starRating;
        this.amenities = amenities == null || amenities.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(amenities));
        require(roomTypes != null && !roomTypes.isEmpty(), "a property needs at least one room type");
        require(hasUniqueNames(roomTypes), "room type names must be unique within a property");
        this.roomTypes = List.copyOf(roomTypes);
    }

    private static boolean hasUniqueNames(List<RoomType> roomTypes) {
        Set<String> names = new HashSet<>();
        return roomTypes.stream().allMatch(rt -> names.add(rt.name().toLowerCase()));
    }

    public Optional<RoomType> roomType(String roomTypeId) {
        return roomTypes.stream().filter(rt -> rt.id().equals(roomTypeId)).findFirst();
    }

    public boolean hasAllAmenities(Collection<Amenity> required) {
        return amenities.containsAll(required);
    }

    public String id() { return id; }
    public String ownerId() { return ownerId; }
    public String name() { return name; }
    public Address address() { return address; }
    public int starRating() { return starRating; }
    public Set<Amenity> amenities() { return amenities; }
    public List<RoomType> roomTypes() { return roomTypes; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Property other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
