package concurrency;

import java.util.concurrent.Semaphore;

public class CapacityControl {

    private static final int MAX_CAPACITY = 200;
    
    private static final Semaphore semaphore = new Semaphore(MAX_CAPACITY, true);
    
    public static boolean acquire(int permits) {
        if (permits <= 0 || permits > MAX_CAPACITY) {
            return false;
        }
        return semaphore.tryAcquire(permits);
    }

    public static void release(int permits) {
        if (permits > 0) {
            semaphore.release(permits);
        }
    }

    public static int availablePermits() {
        return semaphore.availablePermits();
    }
}