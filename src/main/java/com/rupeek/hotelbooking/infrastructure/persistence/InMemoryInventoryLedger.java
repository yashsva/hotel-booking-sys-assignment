package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.inventory.InventoryLedger;
import com.rupeek.hotelbooking.domain.inventory.RoomUnavailableException;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a per-night count of held units for each room type. Every room type has its own
 * monitor, so bookings for different room types never contend, while two bookings for the
 * same room type are serialised: check all nights, then hold all nights, as one step.
 */
@Component
public class InMemoryInventoryLedger implements InventoryLedger {

    private final Map<String, RoomTypeCalendar> calendars = new ConcurrentHashMap<>();

    @Override
    public void reserve(RoomType roomType, DateRange stay) {
        RoomTypeCalendar calendar = calendarFor(roomType.id());
        synchronized (calendar) {
            if (calendar.freeUnits(roomType.totalUnits(), stay) < 1) {
                throw new RoomUnavailableException(roomType.id(), stay);
            }
            stay.nightsOfStay().forEach(calendar::hold);
        }
    }

    @Override
    public void release(String roomTypeId, DateRange stay) {
        RoomTypeCalendar calendar = calendarFor(roomTypeId);
        synchronized (calendar) {
            stay.nightsOfStay().forEach(calendar::free);
        }
    }

    @Override
    public int availableUnits(RoomType roomType, DateRange stay) {
        RoomTypeCalendar calendar = calendarFor(roomType.id());
        synchronized (calendar) {
            return calendar.freeUnits(roomType.totalUnits(), stay);
        }
    }

    private RoomTypeCalendar calendarFor(String roomTypeId) {
        return calendars.computeIfAbsent(roomTypeId, id -> new RoomTypeCalendar());
    }

    /** Units held per night for one room type. Not thread-safe on its own; guarded by its monitor. */
    private static final class RoomTypeCalendar {

        private final Map<LocalDate, Integer> heldPerNight = new HashMap<>();

        int freeUnits(int totalUnits, DateRange stay) {
            int maxHeld = stay.nightsOfStay().stream()
                    .mapToInt(night -> heldPerNight.getOrDefault(night, 0))
                    .max()
                    .orElse(0);
            return totalUnits - maxHeld;
        }

        void hold(LocalDate night) {
            heldPerNight.merge(night, 1, Integer::sum);
        }

        void free(LocalDate night) {
            heldPerNight.computeIfPresent(night, (n, held) -> held > 1 ? held - 1 : null);
        }
    }
}
