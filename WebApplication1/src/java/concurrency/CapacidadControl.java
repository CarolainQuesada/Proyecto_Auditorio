package concurrency;

import java.util.concurrent.Semaphore;

public class CapacidadControl {

    private static final int MAX_CAPACIDAD = 100;
    private static final Semaphore sem = new Semaphore(MAX_CAPACIDAD, true);

    public static boolean adquirir(int cantidad) {

        if (cantidad <= 0) {
            return false;
        }

        return sem.tryAcquire(cantidad);
    }

    public static void liberar(int cantidad) {

        if (cantidad <= 0) return;

        sem.release(cantidad);
    }

    public static int disponibles() {
        return sem.availablePermits();
    }
}