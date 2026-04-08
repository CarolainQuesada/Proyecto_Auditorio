package concurrency;

import java.util.concurrent.Semaphore;

public class CapacityControl {

    private static final int MAX_CAPACITY = 100;
    private static final Semaphore semaphore = new Semaphore(MAX_CAPACITY, true);

    public static boolean acquire(int quantity) {

        if (quantity <= 0) {
            return false;
        }

        return semaphore.tryAcquire(quantity);
    }

    public static void release(int quantity) {

        if (quantity <= 0) {
            return;
        }

        semaphore.release(quantity);
    }

    public static int available() {
        return semaphore.availablePermits();
    }
}