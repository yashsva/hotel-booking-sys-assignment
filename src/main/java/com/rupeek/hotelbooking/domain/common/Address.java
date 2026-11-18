package com.rupeek.hotelbooking.domain.common;

import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonBlank;

public record Address(String line, String locality, String city) {

    public Address {
        line = line == null ? "" : line.trim();
        locality = requireNonBlank(locality, "locality");
        city = requireNonBlank(city, "city");
    }

    public boolean isInCity(String otherCity) {
        return city.equalsIgnoreCase(otherCity.trim());
    }

    public boolean isInLocality(String otherLocality) {
        return locality.equalsIgnoreCase(otherLocality.trim());
    }
}
