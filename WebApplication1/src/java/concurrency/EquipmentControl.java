package concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class EquipmentControl {
    
    private final Map<String, Semaphore> equipmentSemaphores;
    private final java.util.concurrent.locks.ReentrantLock multiLock;
    
    private static final String[] LOCK_ORDER = {"PROYECTOR", "SONIDO", "MICROFONO"};

    public EquipmentControl() {
        this.equipmentSemaphores = new HashMap<>();
        this.multiLock = new java.util.concurrent.locks.ReentrantLock();
        
        equipmentSemaphores.put("PROYECTOR", new Semaphore(2, true));
        equipmentSemaphores.put("MICROFONO", new Semaphore(5, true));
        equipmentSemaphores.put("SONIDO", new Semaphore(3, true));
    }

    public boolean acquire(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem == null) return false;
        return sem.tryAcquire(quantity);
    }

    public void release(String type, int quantity) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        if (sem != null) {
            sem.release(quantity);
        }
    }

    public boolean acquireMultiple(Map<String, Integer> requirements) {
        multiLock.lock();
        try {
            for (String equipment : LOCK_ORDER) {
                if (requirements.containsKey(equipment)) {
                    int qty = requirements.get(equipment);
                    if (equipmentSemaphores.get(equipment).availablePermits() < qty) {
                        return false;
                    }
                }
            }
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

    public int availablePermits(String type) {
        Semaphore sem = equipmentSemaphores.get(type.toUpperCase());
        return sem != null ? sem.availablePermits() : 0;
    }
}