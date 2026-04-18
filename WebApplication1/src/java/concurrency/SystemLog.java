package concurrency;

public class SystemLog {
   
    public synchronized void log(String action, String details) {
        System.out.println("[LOG] " + action + ": " + details);
    }
}
