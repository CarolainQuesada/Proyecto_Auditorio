package concurrency;

import java.util.concurrent.Semaphore;

/**
 * Controls concurrent access to the auditorium's seating capacity using a
 * {@link Semaphore}.
 *
 * <p>The auditorium supports a maximum of {@value #MAX_CAPACITY} attendees.
 * Each reservation must acquire the required number of permits before being
 * persisted. Permits are released when the reservation is cancelled or expires.
 *
 * <p>The semaphore is initialized with <em>fairness</em> ({@code true}),
 * ensuring that threads acquire permits in arrival order (FIFO) to prevent
 * starvation.
 *
 * <p>All methods are {@code static} because a single shared semaphore governs
 * the entire auditorium; no per-instance state is needed.
 *
 * @see java.util.concurrent.Semaphore
 */
public class CapacityControl {

    /**
     * Maximum number of attendees (seats) the auditorium can accommodate.
     * This value defines the total number of semaphore permits available.
     */
    private static final int MAX_CAPACITY = 200;

    /**
     * Fair semaphore that models the available seating capacity.
     * Each permit represents one attendee seat.
     */
    private static final Semaphore semaphore = new Semaphore(MAX_CAPACITY, true);

    /**
     * Private constructor — this class is a static utility and should not be
     * instantiated.
     */
    private CapacityControl() {}

    /**
     * Validates that the requested number of attendees falls within the
     * acceptable range (1 to {@value #MAX_CAPACITY}).
     *
     * @param quantity the number of attendees to validate
     * @return {@code true} if {@code quantity} is between 1 and
     *         {@value #MAX_CAPACITY} (inclusive); {@code false} otherwise
     */
    public static boolean isValidCapacity(int quantity) {
        return quantity > 0 && quantity <= MAX_CAPACITY;
    }

    /**
     * Attempts to acquire the given number of permits from the semaphore
     * without blocking (non-blocking try).
     *
     * <p>Returns {@code false} immediately if the requested number of permits
     * is not currently available, or if the quantity is out of the valid range.
     *
     * @param permits the number of semaphore permits to acquire (i.e., the
     *                number of attendees to reserve seats for)
     * @return {@code true} if the permits were successfully acquired;
     *         {@code false} if the capacity is insufficient or the quantity
     *         is invalid
     */
    public static boolean acquire(int permits) {
        if (!isValidCapacity(permits)) {
            return false;
        }
        return semaphore.tryAcquire(permits);
    }

    /**
     * Releases the given number of permits back to the semaphore, making them
     * available for other reservations.
     *
     * <p>No action is taken if {@code permits} is outside the valid range.
     *
     * @param permits the number of permits to release; must be between 1 and
     *                {@value #MAX_CAPACITY} (inclusive)
     */
    public static void release(int permits) {
        if (permits > 0 && permits <= MAX_CAPACITY) {
            semaphore.release(permits);
        }
    }

    /**
     * Returns the maximum seating capacity of the auditorium.
     *
     * @return {@value #MAX_CAPACITY}
     */
    public static int getMaxCapacity() {
        return MAX_CAPACITY;
    }

    /**
     * Returns the number of semaphore permits currently available, which
     * corresponds to the remaining free seats in the auditorium.
     *
     * @return the number of available permits (0 to {@value #MAX_CAPACITY})
     */
    public static int availablePermits() {
        return semaphore.availablePermits();
    }
}