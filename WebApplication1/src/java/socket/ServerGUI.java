package socket;

import javax.swing.*;
import java.awt.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import concurrency.TTLMonitor;
import service.ReservationService;

public class ServerGUI extends JFrame {

    private JTextArea logArea;
    private ServerSocket server;
    private boolean running = false;
    private final ReservationService reservationService;
    private static final List<String> clients = new ArrayList<>();

    public ServerGUI() {
        
        this.reservationService = new ReservationService();
        setTitle("Reservation Server");
        setSize(500, 400);
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

        new Thread(() -> {
            try {
                server = new ServerSocket(5000);
                running = true;

                log("Server started on port 5000");

                new TTLMonitor(this).start();

                while (running) {
                    try {
                        Socket client = server.accept();
                        log("New incoming connection");
                        new ClientHandler(client, reservationService).start();
                    } catch (Exception e) {
                        if (running) {
                            log("Connection error: " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                log("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void stopServer() {
        try {
            running = false;

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
        clients.add(user);
        log("Client connected: " + user);
    }

    public synchronized void removeClient(String user) {
        clients.remove(user);
        log("Client disconnected: " + user);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}