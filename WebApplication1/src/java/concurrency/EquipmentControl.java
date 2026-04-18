package concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Control concurrente del equipamiento audiovisual mediante semáforos.
 * Cumple con Fase II: semáforos contadores + fairness
 */
public class EquipmentControl {
    
    private final Map<String, Semaphore> equipmentSemaphores;
    private final java.util.concurrent.locks.ReentrantLock multiLock;
    
    // Orden jerárquico para prevención de deadlocks
    private static final String[] LOCK_ORDER = {"PROYECTOR", "SONIDO", "MICROFONO"};

    public EquipmentControl() {
        this.equipmentSemaphores = new HashMap<>();
        this.multiLock = new java.util.concurrent.locks.ReentrantLock();
        
        // Inicializar semáforos (fairness = true para evitar inanición)
        equipmentSemaphores.put("PROYECTOR", new Semaphore(2, true));
        equipmentSemaphores.put("MICROFONO", new Semaphore(5, true));
        equipmentSemaphores.put("SONIDO", new Semaphore(3, true));
    }

    /**
     * Adquiere equipamiento (no bloqueante)
     */
    public boolean acquire(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem == null) return false;
        return sem.tryAcquire(quantity);
    }

    /**
     * Libera equipamiento
     */
    public void release(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem != null) {
            sem.release(quantity);
        }
    }

    /**
     * Reserva MÚLTIPLES equipos atómicamente (previene deadlocks)
     * Siempre adquiere en orden: PROYECTOR → SONIDO → MICROFONO
     */
    public boolean acquireMultiple(Map<String, Integer> requirements) {
        multiLock.lock();
        try {
            // Fase 1: Verificar disponibilidad
            for (String equipment : LOCK_ORDER) {
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    if (equipmentSemaphores.get(equipment).availablePermits() < qty) {
                        return false;
                    }
                }
            }
            
            // Fase 2: Adquirir en orden jerárquico
            for (String equipment : LOCK_ORDER) {
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    equipmentSemaphores.get(equipment).acquire(qty);
                }
            }
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseMultiple(requirements);
            return false;
        } finally {
            multiLock.unlock();
        }
    }

    /**
     * Libera múltiples equipos (orden inverso)
     */
    public void releaseMultiple(Map<String, Integer> requirements) {
        multiLock.lock();
        try {
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
     * Consulta disponibilidad
     */
    public int availablePermits(String type) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        return sem != null ? sem.availablePermits() : 0;
    }
}