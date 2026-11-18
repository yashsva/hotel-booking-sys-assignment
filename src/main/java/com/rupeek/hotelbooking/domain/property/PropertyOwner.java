package com.rupeek.hotelbooking.domain.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

/**
 * An account that owns one or more properties. A standalone hotel is simply an owner
 * with a single property and a hotel chain is an owner with many, so both use the same
 * model and the same onboarding path.
 */
public final class PropertyOwner {

    private final String id;
    private final String name;
    private final List<String> propertyIds = new ArrayList<>();

    public PropertyOwner(String id, String name) {
        this.id = requireNonBlank(id, "owner.id");
        this.name = requireNonBlank(name, "owner.name");
    }

    public synchronized void addProperty(Property property) {
        require(property.ownerId().equals(id), "property belongs to a different owner");
        require(!propertyIds.contains(property.id()), "property already registered: " + property.id());
        propertyIds.add(property.id());
    }

    public synchronized List<String> propertyIds() {
        return List.copyOf(propertyIds);
    }

    public synchronized boolean isMultiProperty() {
        return propertyIds.size() > 1;
    }

    public String id() { return id; }
    public String name() { return name; }

    @Override
    public boolean equals(Object o) {
        return o instanceof PropertyOwner other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
