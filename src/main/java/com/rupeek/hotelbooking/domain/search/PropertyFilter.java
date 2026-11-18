package com.rupeek.hotelbooking.domain.search;

import java.util.function.Predicate;

/** An optional search refinement (price, amenities, rating, ...). Filters compose with {@link Predicate#and}. */
@FunctionalInterface
public interface PropertyFilter extends Predicate<RoomOffer> {
}
