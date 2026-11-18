package com.rupeek.hotelbooking.domain.inventory;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.property.RoomType;

/**
 * Tracks how many units of each room type are held per night.
 * Implementations must make {@link #reserve} atomic across all nights of the stay,
 * so two concurrent callers can never both take the last unit.
 */
public interface InventoryLedger {

    /** Holds one unit for every night of the stay, or throws {@link RoomUnavailableException} and holds nothing. */
    void reserve(RoomType roomType, DateRange stay);

    /** Returns one unit for every night of the stay to the pool. */
    void release(String roomTypeId, DateRange stay);

    /** Units free on every night of the stay (the minimum across the nights). */
    int availableUnits(RoomType roomType, DateRange stay);
}
