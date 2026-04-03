package socket;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import concurrency.TTLMonitor;

public class ServidorGUI extends JFrame {

    private JTextArea logArea;
    private ServerSocket server;
    private boolean corriendo = false;

    private static final List<String> clientes = new ArrayList<>();

    public ServidorGUI() {

        setTitle("Servidor de Reservas");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        JButton iniciar = new JButton("Iniciar Servidor");
        JButton detener = new JButton("Detener");

        JPanel panel = new JPanel();
        panel.add(iniciar);
        panel.add(detener);

        add(panel, BorderLayout.SOUTH);

        iniciar.addActionListener(e -> iniciarServidor());
        detener.addActionListener(e -> detenerServidor());

        setVisible(true);
    }

    private void iniciarServidor() {

        if (corriendo) {
            log("️ El servidor ya está corriendo");
            return;
        }

        new Thread(() -> {
            try {
                server = new ServerSocket(5000);
                corriendo = true;

                log("Servidor iniciado en puerto 5000");

               
                new TTLMonitor(this).start();

                while (corriendo) {
                    Socket cliente = server.accept();

                    log("Nueva conexión entrante");

                 
                    new FlujoCliente(cliente, this).start();
                }

            } catch (Exception e) {
                log(" Error servidor: " + e.getMessage());
            }
        }).start();
    }

   
    private void detenerServidor() {
        try {
            corriendo = false;

            if (server != null && !server.isClosed()) {
                server.close();
            }

            log("Servidor detenido");

        } catch (Exception e) {
            log(" Error al detener: " + e.getMessage());
        }
    }

public synchronized void agregarCliente(String usuario) {
    clientes.add(usuario);
    log(" Cliente conectado: " + usuario);
}

public synchronized void eliminarCliente(String usuario) {
    clientes.remove(usuario);
    log(" Cliente desconectado: " + usuario);
}

    public void log(String mensaje) {
        SwingUtilities.invokeLater(() -> logArea.append(mensaje + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServidorGUI::new);
    }
}