package concurrency;

import dao.LogDAO;

public class SystemLog {

    // ── SINGLETON ──────────────────────────────────────────────────
    private static final SystemLog INSTANCE = new SystemLog();

    public static SystemLog getInstance() {
        return INSTANCE;
    }

    private final LogDAO logDAO;

    private SystemLog() {
        this.logDAO = new LogDAO();
    }

    public synchronized void log(String user, String action, String details) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("[LOG] [" + timestamp + "] [" + action + "] user="
                + user + " | " + details);

        try {
            logDAO.register(user, action, details);
        } catch (Exception e) {
            System.err.println("[LOG-DB ERROR] No se pudo persistir: " + e.getMessage());
        }
    }

    public synchronized void log(String action, String details) {
        log("SYSTEM", action, details);
    }
}