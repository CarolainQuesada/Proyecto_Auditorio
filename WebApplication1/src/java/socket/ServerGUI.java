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

public class ServerGUI extends JFrame {

    private final JTextArea logArea;
    private ServerSocket server;
    private volatile boolean running = false;

    private final ReservationService reservationService;
    private TTLMonitor ttlMonitor;

    private static final List<String> clients = new ArrayList<>();

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

    public synchronized void addClient(String user) {
        if (!clients.contains(user)) {
            clients.add(user);
        }

        log("Client connected: " + user);
    }

    public synchronized void removeClient(String user) {
        clients.remove(user);
        log("Client disconnected: " + user);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}