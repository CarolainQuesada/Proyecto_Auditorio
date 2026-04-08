package concurrency;

import java.util.concurrent.locks.ReentrantLock;

public class CalendarLock {

    private static final ReentrantLock lock = new ReentrantLock(true);

    public static void lock() {
        lock.lock();
    }

    public static void unlock() {
        lock.unlock();
    }
}