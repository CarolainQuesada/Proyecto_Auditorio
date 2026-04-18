package service;

import concurrency.EquipmentControl;
import dao.EquipmentDAO;
import java.util.Map;

/**
 * Servicio de equipamiento - Coordina semáforos + BD
 * SIN parámetro Connection en constructor (compatible con ReservationService)
 */
public class EquipmentService {
    
    private final EquipmentControl control;
    private final EquipmentDAO dao;

    /**
     * Constructor SIN parámetros - compatible con ReservationService
     */
    public EquipmentService() {
        this.control = new EquipmentControl();
        this.dao = new EquipmentDAO(); // DAO maneja conexión internamente
    }

    /**
     * Reserva equipamiento para una reserva
     */
    public boolean reserveEquipment(String equipmentType, int quantity, String reservationId) {
        if ("NINGUNO".equalsIgnoreCase(equipmentType) || quantity == 0) {
            return true;
        }

        boolean success = control.acquire(equipmentType, quantity);
        
        if (success) {
            // Actualizar BD
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
     * Libera equipamiento
     */
    public void releaseEquipment(String equipmentType, int quantity, String reservationId) {
        if ("NINGUNO".equalsIgnoreCase(equipmentType) || quantity == 0) {
            return;
        }

        control.release(equipmentType, quantity);
        
        // Actualizar BD
        int equipmentId = getEquipmentId(equipmentType);
        if (equipmentId > 0) {
            int newAvailable = control.availablePermits(equipmentType);
            dao.updateAvailability(equipmentId, newAvailable);
        }
        
        System.out.println("[EQUIPMENT] Liberado: " + equipmentType + " x" + quantity);
    }

    /**
     * Reserva múltiple con prevención de deadlocks
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
     * Libera múltiples equipos
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

    private int getEquipmentId(String type) {
        return dao.getEquipmentIdByName(type);
    }

    public int getAvailability(String type) {
        return control.availablePermits(type);
    }
}