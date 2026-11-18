package com.rupeek.hotelbooking.application.concurrency;

import java.util.function.Supplier;

/**
 * Serialises work on one key (e.g. one booking) so read-check-write sequences such as
 * "pay" and "cancel" cannot interleave. In-memory today; a distributed lock later.
 */
public interface LockManager {

    <T> T withLock(String key, Supplier<T> action);
}
