package socket;

import concurrency.CapacityControl;
import concurrency.TTLMonitor;
import service.ReservationService;

import javax.swing.*;
import java.awt.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing-based graphical interface for the reservation server.
 *
 * <p>{@code ServerGUI} extends {@link JFrame} and serves as the main entry
 * point for the server process. It is responsible for:
 * <ul>
 *   <li>Rendering a scrollable log area that displays real-time server events.</li>
 *   <li>Accepting TCP connections on port {@code 5000} and spawning a new
 *       {@link ClientHandler} thread for each one.</li>
 *   <li>Starting and stopping the {@link TTLMonitor} background thread that
 *       periodically expires stale pending reservations.</li>
 *   <li>Providing a thread-safe {@link #log(String)} method that appends
 *       messages to the GUI from any thread.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>The user clicks <b>Start Server</b> → {@link #startServer()} opens a
 *       {@link ServerSocket} on port 5000 and begins accepting connections.</li>
 *   <li>The user clicks <b>Stop</b> → {@link #stopServer()} interrupts the
 *       TTLMonitor and closes the server socket.</li>
 * </ol>
 *
 * @see ClientHandler
 * @see TTLMonitor
 * @see ReservationService
 */
public class ServerGUI extends JFrame {

    /** Scrollable text area that displays server log messages. */
    private final JTextArea logArea;

    /** The TCP server socket listening on port 5000. */
    private ServerSocket server;

    /**
     * Flag indicating whether the server accept-loop is running.
     * Declared {@code volatile} so the accept-loop thread observes
     * changes made by the EDT when the user clicks Stop.
     */
    private volatile boolean running = false;

    /** Service layer used by {@link ClientHandler} instances. */
    private final ReservationService reservationService;

    /** Background thread that expires old pending reservations. */
    private TTLMonitor ttlMonitor;

    /**
     * Shared list of currently connected client identifiers.
     * Access is guarded by {@code synchronized} methods.
     */
    private static final List<String> clients = new ArrayList<>();

    /**
     * Constructs the server GUI, initialises the {@link ReservationService},
     * and makes the window visible.
     *
     * <p>This constructor must be invoked on the Event Dispatch Thread (EDT).
     * Use {@link SwingUtilities#invokeLater(Runnable)} as shown in
     * {@link #main(String[])}.
     */
    public ServerGUI() {
        System.out.println("📊 Capacidad inicial: " + CapacityControl.availablePermits() + " / 200");

        this.reservationService = new ReservationService();

        setTitle("Reservation Server");
        setSize(650, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        JButton start = new JButton("Start Server");
        JButton stop = new JButton("Stop");

        JPanel panel = new JPanel();
        panel.add(start);
        panel.add(stop);

        add(panel, BorderLayout.SOUTH);

        start.addActionListener(e -> startServer());
        stop.addActionListener(e -> stopServer());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Opens the server socket on port {@code 5000} and enters the
     * accept-loop on a new daemon thread.
     *
     * <p>For each accepted connection a {@link ClientHandler} is created
     * and started. The method also triggers {@link #startTTLMonitor()}.
     * If the server is already running, the call is a no-op.
     */
    private void startServer() {
        if (running) {
            log("Server is already running");
            return;
        }

        Thread serverThread = new Thread(() -> {
            try {
                server = new ServerSocket(5000);
                running = true;

                log("Server started on port 5000");
                log("Capacity available: " + CapacityControl.availablePermits() + " / 200");

                startTTLMonitor();

                while (running) {
                    try {
                        Socket client = server.accept();
                        String ip = client.getInetAddress().getHostAddress();

                        log("New incoming connection from: " + ip);

                        ClientHandler handler = new ClientHandler(client, reservationService, this);
                        handler.start();

                    } catch (Exception e) {
                        if (running) {
                            log("Connection error: " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                log("Server error: " + e.getMessage());
                e.printStackTrace();

            } finally {
                running = false;
                log("Server thread finished");
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Starts the {@link TTLMonitor} daemon thread if it is not already alive.
     *
     * <p>The monitor periodically marks pending reservations as expired when
     * they exceed their TTL. It is started automatically by {@link #startServer()}.
     */
    private void startTTLMonitor() {
        if (ttlMonitor != null && ttlMonitor.isAlive()) {
            log("TTLMonitor is already running");
            return;
        }

        ttlMonitor = new TTLMonitor(this);
        ttlMonitor.setDaemon(true);
        ttlMonitor.start();

        log("TTLMonitor started");
    }

    /**
     * Stops the server by closing the {@link ServerSocket} and interrupting
     * the {@link TTLMonitor}.
     *
     * <p>Sets {@link #running} to {@code false} before closing the socket so
     * the accept-loop exits cleanly without logging a spurious connection error.
     */
    private void stopServer() {
        try {
            running = false;

            if (ttlMonitor != null && ttlMonitor.isAlive()) {
                ttlMonitor.interrupt();
                log("TTLMonitor stopped");
            }

            if (server != null && !server.isClosed()) {
                server.close();
            }

            log("Server stopped");

        } catch (Exception e) {
            log("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers a client as connected and logs the event.
     *
     * <p>Duplicate entries are silently ignored. This method is
     * {@code synchronized} to protect the shared {@link #clients} list.
     *
     * @param user a string identifying the client (e.g. IP address or email)
     */
    public synchronized void addClient(String user) {
        if (!clients.contains(user)) {
            clients.add(user);
        }

        log("Client connected: " + user);
    }

    /**
     * Unregisters a client and logs the disconnection event.
     *
     * <p>This method is {@code synchronized} to protect the shared
     * {@link #clients} list.
     *
     * @param user the identifier of the client to remove
     */
    public synchronized void removeClient(String user) {
        clients.remove(user);
        log("Client disconnected: " + user);
    }

    /**
     * Appends a message to the log area on the Event Dispatch Thread.
     *
     * <p>Safe to call from any thread. The caret is scrolled to the end
     * after each append so the latest message is always visible.
     *
     * @param message the text to append; a newline is added automatically
     */
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Application entry point. Creates the {@link ServerGUI} on the EDT.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}