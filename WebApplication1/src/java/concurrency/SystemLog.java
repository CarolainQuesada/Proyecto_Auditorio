package concurrency;

import dao.LogDAO;

/**
 * Thread-safe singleton that centralizes all system event logging.
 *
 * <p>Every significant action performed by the application (reservation
 * creation, confirmation, deletion, TTL expiry, etc.) is recorded through
 * this class. Each log entry is:
 * <ol>
 *   <li>Printed to standard output (for server-console visibility), and</li>
 *   <li>Persisted to the {@code log} database table via {@link LogDAO}.</li>
 * </ol>
 *
 * <p><b>Thread safety:</b> Both overloads of {@link #log} are declared
 * {@code synchronized}, ensuring that log entries are written atomically even
 * when multiple threads log concurrently.
 *
 * <p><b>Singleton pattern:</b> The single instance is created eagerly at class
 * loading time and returned by {@link #getInstance()}. No lazy initialization
 * or double-checked locking is required.
 *
 * <p>Usage example:
 * <pre>{@code
 * SystemLog.getInstance().log("user@una.ac.cr", "RESERVE_OK", "Reservation created for 2026-05-01");
 * }</pre>
 *
 * @see LogDAO
 */
public class SystemLog {

    // ── Singleton ──────────────────────────────────────────────────────────

    /**
     * Eagerly initialized singleton instance. Thread-safe by class-loading
     * guarantee.
     */
    private static final SystemLog INSTANCE = new SystemLog();

    /**
     * Returns the single application-wide {@code SystemLog} instance.
     *
     * @return the singleton {@code SystemLog}
     */
    public static SystemLog getInstance() {
        return INSTANCE;
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    /**
     * DAO used to persist log entries to the database.
     * Initialized once in the private constructor.
     */
    private final LogDAO logDAO;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * Private constructor that creates the underlying {@link LogDAO}.
     * Called only once during class initialization.
     */
    private SystemLog() {
        this.logDAO = new LogDAO();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records a log entry attributed to a specific user.
     *
     * <p>The current timestamp is formatted as {@code yyyy-MM-dd HH:mm:ss}
     * and prepended to the console output. The entry is also persisted to the
     * database asynchronously; database errors are swallowed and only printed
     * to {@code stderr} to avoid propagating persistence failures to callers.
     *
     * @param user    the email or identifier of the user who triggered the event;
     *                must not be {@code null}
     * @param action  a short upper-case action code (e.g., {@code "RESERVE_OK"},
     *                {@code "DELETE_OK"}); must not be {@code null}
     * @param details a human-readable description of the event; must not be
     *                {@code null}
     */
    public synchronized void log(String user, String action, String details) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("[LOG] [" + timestamp + "] [" + action + "] user="
                + user + " | " + details);

        try {
            logDAO.register(user, action, details);
        } catch (Exception e) {
            System.err.println("[LOG-DB ERROR] Could not persist log entry: " + e.getMessage());
        }
    }

    /**
     * Records a log entry attributed to the system itself (user = {@code "SYSTEM"}).
     *
     * <p>Convenience overload of {@link #log(String, String, String)} for
     * system-generated events such as TTL expiry jobs.
     *
     * @param action  a short upper-case action code
     * @param details a human-readable description of the event
     */
    public synchronized void log(String action, String details) {
        log("SYSTEM", action, details);
    }
}