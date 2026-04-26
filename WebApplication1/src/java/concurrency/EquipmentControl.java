package concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages concurrent access to shared auditorium equipment using per-equipment
 * {@link Semaphore}s and a global ordering lock to prevent deadlocks.
 *
 * <p>Three types of equipment are supported, each with a fixed maximum quantity:
 * <ul>
 *   <li><b>PROYECTOR</b> (projector) — 2 units</li>
 *   <li><b>SONIDO</b> (sound system) — 3 units</li>
 *   <li><b>MICROFONO</b> (microphone) — 5 units</li>
 * </ul>
 *
 * <p><b>Deadlock prevention:</b> When multiple equipment types must be acquired
 * atomically (via {@link #acquireMultiple}), a {@link ReentrantLock}
 * ({@code multiLock}) serializes all multi-acquire operations, and equipment is
 * always acquired in the fixed canonical order defined by {@link #LOCK_ORDER}.
 * This guarantees consistent lock ordering and eliminates circular-wait
 * conditions.
 *
 * <p><b>Fairness:</b> Each semaphore is created with fairness enabled
 * ({@code true}), so threads acquire permits in FIFO order.
 *
 * <p>This class is designed to be instantiated once (e.g., inside a service
 * layer) and shared across all concurrent requests.
 *
 * @see java.util.concurrent.Semaphore
 * @see java.util.concurrent.locks.ReentrantLock
 */
public class EquipmentControl {

    /**
     * Map from equipment type name (upper-case) to its controlling semaphore.
     * Keys: {@code "PROYECTOR"}, {@code "SONIDO"}, {@code "MICROFONO"}.
     */
    private final Map<String, Semaphore> equipmentSemaphores;

    /**
     * Mutex that serializes all multi-equipment acquire/release operations,
     * ensuring that availability checks and acquisitions are performed
     * atomically and in the canonical {@link #LOCK_ORDER} order.
     */
    private final ReentrantLock multiLock;

    /**
     * Canonical acquisition order used by {@link #acquireMultiple} and
     * {@link #releaseMultiple} to enforce a consistent lock ordering and
     * prevent deadlocks.
     */
    private static final String[] LOCK_ORDER = {"PROYECTOR", "SONIDO", "MICROFONO"};

    /**
     * Constructs an {@code EquipmentControl} instance and initializes the
     * semaphore for each equipment type with the permitted maximum quantity.
     */
    public EquipmentControl() {
        this.equipmentSemaphores = new HashMap<>();
        this.multiLock = new ReentrantLock();

        // Initialize each semaphore with its available unit count (fair mode).
        equipmentSemaphores.put("PROYECTOR", new Semaphore(2, true));
        equipmentSemaphores.put("MICROFONO", new Semaphore(5, true));
        equipmentSemaphores.put("SONIDO",    new Semaphore(3, true));
    }

    /**
     * Attempts to acquire the specified quantity of a single equipment type
     * without blocking.
     *
     * @param type     the equipment type name (case-insensitive);
     *                 must be one of {@code "PROYECTOR"}, {@code "SONIDO"},
     *                 or {@code "MICROFONO"}
     * @param quantity the number of units to acquire; must be positive and
     *                 within the maximum for the given type
     * @return {@code true} if the units were successfully reserved;
     *         {@code false} if the type is unknown or insufficient units are
     *         available
     */
    public boolean acquire(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem == null) return false;
        return sem.tryAcquire(quantity);
    }

    /**
     * Releases the specified quantity of a single equipment type back to the
     * semaphore.
     *
     * <p>No action is taken if the type is unknown.
     *
     * @param type     the equipment type name (case-insensitive)
     * @param quantity the number of units to release
     */
    public void release(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem != null) {
            sem.release(quantity);
        }
    }

    /**
     * Atomically checks availability for all required equipment types and, if
     * all are available, acquires them in the canonical {@link #LOCK_ORDER}.
     *
     * <p>The operation is protected by {@code multiLock} to prevent interleaved
     * acquisitions from other threads. If any equipment type has insufficient
     * units, the method returns {@code false} without acquiring anything
     * (all-or-nothing semantics).
     *
     * <p>If the thread is interrupted during acquisition, any already-acquired
     * permits are released and the method returns {@code false}.
     *
     * @param requirements a map from equipment type name (upper-case) to the
     *                     number of units required; types not in the map are
     *                     ignored
     * @return {@code true} if all requested units were successfully acquired;
     *         {@code false} if any type was unavailable or the thread was
     *         interrupted
     */
    public boolean acquireMultiple(Map<String, Integer> requirements) {
        multiLock.lock();
        try {
            // First pass: verify sufficient permits are available for every type.
            for (String equipment : LOCK_ORDER) {
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    if (equipmentSemaphores.get(equipment).availablePermits() < qty) {
                        return false;
                    }
                }
            }

            // Second pass: acquire permits in canonical order.
            for (String equipment : LOCK_ORDER) {
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    equipmentSemaphores.get(equipment).acquire(qty);
                }
            }
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Roll back any permits already acquired.
            releaseMultiple(requirements);
            return false;
        } finally {
            multiLock.unlock();
        }
    }

    /**
     * Releases all equipment units previously acquired by
     * {@link #acquireMultiple}, in <em>reverse</em> canonical order to mirror
     * a symmetric release strategy.
     *
     * <p>The operation is protected by {@code multiLock}.
     *
     * @param requirements a map from equipment type name (upper-case) to the
     *                     number of units to release; types not in the map are
     *                     ignored
     */
    public void releaseMultiple(Map<String, Integer> requirements) {
        multiLock.lock();
        try {
            // Release in reverse canonical order.
            for (int i = LOCK_ORDER.length - 1; i >= 0; i--) {
                String equipment = LOCK_ORDER[i];
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    equipmentSemaphores.get(equipment).release(qty);
                }
            }
        } finally {
            multiLock.unlock();
        }
    }

    /**
     * Returns the number of currently available units for the given equipment
     * type.
     *
     * @param type the equipment type name (case-insensitive)
     * @return the number of available permits, or {@code 0} if the type is
     *         unknown
     */
    public int availablePermits(String type) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        return sem != null ? sem.availablePermits() : 0;
    }
}