package concurrency;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a global fair reentrant lock used to serialize all calendar-related
 * operations (create, edit, delete, and overlap checks for reservations).
 *
 * <p>Using a single shared lock ensures that no two threads can simultaneously
 * modify the reservation calendar, preventing race conditions such as
 * double-bookings or conflicting updates.
 *
 * <p>The lock is initialized with <em>fairness</em> enabled ({@code true}),
 * which guarantees that threads acquire the lock in the order they requested
 * it (FIFO), avoiding starvation under high concurrency.
 *
 * <p>Usage pattern:
 * <pre>{@code
 * CalendarLock.lock();
 * try {
 *     // critical section: check overlaps and create/edit reservation
 * } finally {
 *     CalendarLock.unlock();
 * }
 * }</pre>
 *
 * @see java.util.concurrent.locks.ReentrantLock
 */
public class CalendarLock {

    /**
     * The underlying fair reentrant lock instance shared across the entire
     * application. Declared {@code static final} so that all callers operate
     * on the same lock object.
     */
    private static final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Private constructor — this class is a utility class and should not be
     * instantiated.
     */
    private CalendarLock() {}

    /**
     * Acquires the calendar lock, blocking the calling thread until the lock
     * becomes available.
     *
     * <p>Must always be paired with a call to {@link #unlock()} in a
     * {@code finally} block to ensure the lock is released even if an
     * exception is thrown.
     */
    public static void lock() {
        lock.lock();
    }

    /**
     * Releases the calendar lock, allowing other waiting threads to proceed.
     *
     * <p>This method must only be called by the thread that holds the lock.
     * Calling it without a prior {@link #lock()} call will throw an
     * {@link IllegalMonitorStateException}.
     */
    public static void unlock() {
        lock.unlock();
    }
}