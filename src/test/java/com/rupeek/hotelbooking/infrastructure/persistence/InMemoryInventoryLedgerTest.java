package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.inventory.RoomUnavailableException;
import com.rupeek.hotelbooking.domain.property.RoomType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryInventoryLedgerTest {

    private static final LocalDate FEB_1 = LocalDate.of(2027, 2, 1);

    private final InMemoryInventoryLedger ledger = new InMemoryInventoryLedger();
    private final RoomType twoUnits = new RoomType("rt-2", "Twin", 2, 2, Money.of("1000", "INR"));
    private final RoomType oneUnit = new RoomType("rt-1", "Single", 1, 1, Money.of("800", "INR"));

    private static DateRange nights(int fromDay, int toDay) {
        return DateRange.of(FEB_1.plusDays(fromDay), FEB_1.plusDays(toDay));
    }

    @Test
    void reservingReducesAvailabilityForOverlappingStaysOnly() {
        ledger.reserve(twoUnits, nights(0, 3));

        assertThat(ledger.availableUnits(twoUnits, nights(0, 3))).isEqualTo(1);
        assertThat(ledger.availableUnits(twoUnits, nights(2, 5))).isEqualTo(1);
        assertThat(ledger.availableUnits(twoUnits, nights(3, 5))).isEqualTo(2); // checkout day is free
    }

    @Test
    void preventsDoubleBookingOfTheLastUnitForOverlappingDates() {
        ledger.reserve(oneUnit, nights(0, 3));

        assertThatThrownBy(() -> ledger.reserve(oneUnit, nights(2, 4))).isInstanceOf(RoomUnavailableException.class);
        ledger.reserve(oneUnit, nights(3, 5)); // back-to-back is allowed
    }

    @Test
    void aFailedReservationHoldsNothing() {
        ledger.reserve(oneUnit, nights(2, 3)); // only night 2 is taken

        assertThatThrownBy(() -> ledger.reserve(oneUnit, nights(0, 4))).isInstanceOf(RoomUnavailableException.class);
        assertThat(ledger.availableUnits(oneUnit, nights(0, 2))).isEqualTo(1);
        assertThat(ledger.availableUnits(oneUnit, nights(3, 4))).isEqualTo(1);
    }

    @Test
    void releaseMakesTheRoomAvailableAgain() {
        ledger.reserve(oneUnit, nights(0, 2));
        ledger.release(oneUnit.id(), nights(0, 2));

        assertThat(ledger.availableUnits(oneUnit, nights(0, 2))).isEqualTo(1);
        ledger.reserve(oneUnit, nights(0, 2));
    }

    @Test
    void concurrentReservationsForTheLastUnitLetExactlyOneWin() throws Exception {
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            attempts.add(pool.submit(() -> {
                start.await();
                try {
                    ledger.reserve(oneUnit, nights(0, 3));
                    return true;
                } catch (RoomUnavailableException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int winners = 0;
        for (Future<Boolean> attempt : attempts) {
            winners += attempt.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(ledger.availableUnits(oneUnit, nights(0, 3))).isZero();
    }
}
