package service;

import concurrency.EquipmentControl;
import dao.EquipmentDAO;
import java.util.Map;

/**
 * Service layer for managing equipment availability in reservations.
 *
 * <p>{@code EquipmentService} coordinates between the concurrency layer
 * ({@link EquipmentControl}) and the persistence layer ({@link EquipmentDAO})
 * to acquire and release equipment resources. All acquire/release operations
 * are first applied to the in-memory semaphores managed by
 * {@link EquipmentControl}, and the resulting available quantity is then
 * persisted to the database via {@link EquipmentDAO#updateAvailability}.
 *
 * <h2>Equipment types</h2>
 * <p>Valid type strings (case-insensitive) match the {@code name} column in
 * the {@code equipment} table, e.g. {@code "PROYECTOR"}, {@code "MICROFONO"},
 * {@code "SONIDO"}. Passing {@code "NINGUNO"} (or quantity {@code 0}) is a
 * valid no-op for reservations that do not require equipment.
 *
 * @see EquipmentControl
 * @see EquipmentDAO
 */
public class EquipmentService {

    /** Concurrency control for in-memory equipment semaphores. */
    private final EquipmentControl control;

    /** DAO used to persist availability changes to the database. */
    private final EquipmentDAO dao;

    /**
     * Constructs an {@code EquipmentService} with default
     * {@link EquipmentControl} and {@link EquipmentDAO} instances.
     */
    public EquipmentService() {
        this.control = new EquipmentControl();
        this.dao = new EquipmentDAO(); 
    }

    /**
     * Attempts to acquire the specified quantity of a single equipment type
     * for a reservation.
     *
     * <p>If {@code equipmentType} is {@code "NINGUNO"} (case-insensitive) or
     * {@code quantity} is {@code 0}, the method returns {@code true} immediately
     * without touching the semaphore or the database.
     *
     * <p>On success the new available quantity is persisted via
     * {@link EquipmentDAO#updateAvailability}.
     *
     * @param equipmentType the equipment type name (e.g. {@code "PROYECTOR"})
     * @param quantity      the number of units to reserve; must be positive
     * @param reservationId identifier of the reservation (used for logging)
     * @return {@code true} if the equipment was successfully acquired;
     *         {@code false} if there are insufficient units available
     */
    public boolean reserveEquipment(String equipmentType, int quantity, String reservationId) {
        if ("NINGUNO".equalsIgnoreCase(equipmentType) || quantity == 0) {
            return true;
        }

        boolean success = control.acquire(equipmentType, quantity);
        
        if (success) {
            int equipmentId = getEquipmentId(equipmentType);
            if (equipmentId > 0) {
                int newAvailable = control.availablePermits(equipmentType);
                dao.updateAvailability(equipmentId, newAvailable);
            }
            System.out.println("[EQUIPMENT] Reservado: " + equipmentType + " x" + quantity);
        }
        
        return success;
    }

    /**
     * Releases previously acquired units of a single equipment type back
     * to the pool.
     *
     * <p>If {@code equipmentType} is {@code "NINGUNO"} (case-insensitive) or
     * {@code quantity} is {@code 0}, the method returns immediately without
     * any action.
     *
     * <p>After releasing, the updated available quantity is persisted via
     * {@link EquipmentDAO#updateAvailability}.
     *
     * @param equipmentType the equipment type name
     * @param quantity      the number of units to release; must match the
     *                      amount originally acquired
     * @param reservationId identifier of the reservation (used for logging)
     */
    public void releaseEquipment(String equipmentType, int quantity, String reservationId) {
        if ("NINGUNO".equalsIgnoreCase(equipmentType) || quantity == 0) {
            return;
        }

        control.release(equipmentType, quantity);
        
        int equipmentId = getEquipmentId(equipmentType);
        if (equipmentId > 0) {
            int newAvailable = control.availablePermits(equipmentType);
            dao.updateAvailability(equipmentId, newAvailable);
        }
        
        System.out.println("[EQUIPMENT] Liberado: " + equipmentType + " x" + quantity);
    }

    /**
     * Atomically acquires multiple equipment types in a single operation.
     *
     * <p>The acquisition is all-or-nothing: if any type lacks sufficient
     * available units, no permits are consumed. On success, the available
     * quantity of each affected type is persisted to the database.
     *
     * @param requirements a map from equipment type name to the number of
     *                     units required; must not be {@code null}
     * @param reservationId identifier of the reservation (used for logging)
     * @return {@code true} if all requirements were satisfied; {@code false}
     *         if any type had insufficient availability (no permits are taken
     *         in this case)
     */
    public boolean reserveMultiple(Map<String, Integer> requirements, String reservationId) {
        boolean success = control.acquireMultiple(requirements);
        
        if (success) {
            for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
                int equipmentId = getEquipmentId(entry.getKey());
                if (equipmentId > 0) {
                    int newAvailable = control.availablePermits(entry.getKey());
                    dao.updateAvailability(equipmentId, newAvailable);
                }
            }
        }
        
        return success;
    }

    /**
     * Releases multiple equipment types simultaneously, returning all units
     * to their respective pools.
     *
     * <p>After releasing, the updated available quantity for each type is
     * persisted to the database.
     *
     * @param requirements a map from equipment type name to the number of
     *                     units to release; must not be {@code null}
     * @param reservationId identifier of the reservation (used for logging)
     */
    public void releaseMultiple(Map<String, Integer> requirements, String reservationId) {
        control.releaseMultiple(requirements);
        
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            int equipmentId = getEquipmentId(entry.getKey());
            if (equipmentId > 0) {
                int newAvailable = control.availablePermits(entry.getKey());
                dao.updateAvailability(equipmentId, newAvailable);
            }
        }
    }

    /**
     * Looks up the database ID for the given equipment type name.
     *
     * @param type the equipment type name (upper-cased before querying)
     * @return the equipment ID, or {@code -1} if not found
     */
    private int getEquipmentId(String type) {
        return dao.getEquipmentIdByName(type);
    }

    /**
     * Returns the current number of available permits for the specified
     * equipment type, as tracked by the in-memory semaphore.
     *
     * @param type the equipment type name
     * @return the current available quantity
     */
    public int getAvailability(String type) {
        return control.availablePermits(type);
    }
}