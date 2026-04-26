package concurrency;

import dao.ReservationDAO;
import socket.ServerGUI;

/**
 * Background daemon thread that periodically expires {@code PENDING}
 * reservations that have exceeded the configured Time-To-Live (TTL) window.
 *
 * <p>Once started, this thread runs an infinite loop that:
 * <ol>
 *   <li>Calls {@link ReservationDAO#cleanExpired(int)} to mark all
 *       {@code PENDING} reservations older than {@value #TTL_MINUTES} minutes
 *       as {@code EXPIRED} in the database.</li>
 *   <li>Logs the number of expired reservations to both the server GUI and
 *       the {@link SystemLog}.</li>
 *   <li>Sleeps for 60 seconds before the next check.</li>
 * </ol>
 *
 * <p>The thread should be started as a daemon (via
 * {@code ttlMonitor.setDaemon(true)}) so that it does not prevent JVM
 * shutdown. It can be stopped by interrupting it from the owning
 * {@code ServerGUI}.
 *
 * <p>Example usage (from {@code ServerGUI}):
 * <pre>{@code
 * TTLMonitor monitor = new TTLMonitor(gui);
 * monitor.setDaemon(true);
 * monitor.start();
 * }</pre>
 *
 * @see ReservationDAO#cleanExpired(int)
 * @see SystemLog
 */
public class TTLMonitor extends Thread {

    /**
     * Reference to the server GUI used for real-time log output.
     */
    private final ServerGUI gui;

    /**
     * Number of minutes after which a {@code PENDING} reservation is
     * automatically transitioned to {@code EXPIRED} status.
     */
    private static final int TTL_MINUTES = 10;

    /**
     * Constructs a new {@code TTLMonitor} bound to the given server GUI.
     *
     * @param gui the {@link ServerGUI} instance used to display log messages;
     *            must not be {@code null}
     */
    public TTLMonitor(ServerGUI gui) {
        this.gui = gui;
    }

    /**
     * Main loop of the TTL monitor thread.
     *
     * <p>Runs indefinitely until the thread is interrupted (e.g., when the
     * server is stopped). On each iteration:
     * <ul>
     *   <li>Invokes {@link ReservationDAO#cleanExpired(int)} with the
     *       configured TTL.</li>
     *   <li>Logs the count of expired reservations if any were found.</li>
     *   <li>Sleeps for 60 000 ms (1 minute) before the next cycle.</li>
     * </ul>
     *
     * <p>If interrupted during sleep, the thread sets the interrupt flag and
     * exits the loop cleanly.
     */
    @Override
    public void run() {
        while (true) {
            try {
                gui.log("TTL running... expiration threshold: " + TTL_MINUTES + " minutes");

                int expired = new ReservationDAO().cleanExpired(TTL_MINUTES);

                if (expired > 0) {
                    gui.log("TTL expired reservations: " + expired);

                    SystemLog.getInstance().log(
                            "SYSTEM",
                            "TTL_EXPIRED",
                            "Reservations automatically expired: " + expired
                    );
                }

                // Wait 1 minute before the next expiry check.
                Thread.sleep(60_000);

            } catch (InterruptedException e) {
                gui.log("TTL monitor stopped");
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                gui.log("TTL error: " + e.getMessage());
            }
        }
    }
}