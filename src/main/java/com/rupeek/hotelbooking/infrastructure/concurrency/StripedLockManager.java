package com.rupeek.hotelbooking.infrastructure.concurrency;

import com.rupeek.hotelbooking.application.concurrency.LockManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** A fixed pool of locks, keys hashed onto it: bounded memory, no lock-per-key leak. */
@Component
public class StripedLockManager implements LockManager {

    private static final int STRIPES = 256;

    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];

    public StripedLockManager() {
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks[Math.floorMod(key.hashCode(), STRIPES)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
