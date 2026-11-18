package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;

import java.util.List;

public record OnboardingResult(PropertyOwner owner, List<Property> properties) {
}
