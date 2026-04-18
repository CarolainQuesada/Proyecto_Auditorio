package socket;

import service.ReservationService;
import java.io.*;
import java.net.Socket;

/**
 * Handler por cada cliente conectado.
 * Extiende Thread (requisito Fase II - hilos reales).
 */
public class ClientHandler extends Thread {
    
    private final Socket clientSocket;
    private final ReservationService reservationService;

    /**
     * Constructor del handler
     */
    public ClientHandler(Socket socket, ReservationService service) {
        this.clientSocket = socket;
        this.reservationService = service;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress();
        System.out.println("[SERVER] Cliente conectado: " + clientInfo);
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String command = in.readLine();
            
            if (command == null || command.isEmpty()) {
                out.println("ERROR: Comando vacío");
                return;
            }
            
            System.out.println("[SERVER] Comando recibido: " + command);
            
            String[] parts = command.split(";");
            String action = parts[0].toUpperCase();
            
            switch (action) {
                case "RESERVAR":
                    handleReserve(parts, out);
                    break;
                    
                case "EDITAR":
                    handleEdit(parts, out);
                    break;
                    
                case "CONFIRMAR":
                    handleConfirm(parts, out);
                    break;
                    
                case "ELIMINAR":
                    handleDelete(parts, out);
                    break;
                    
                case "LISTAR":
                    handleList(out);
                    break;
                    
                default:
                    out.println("ERROR: Comando no reconocido");
            }
            
        } catch (IOException e) {
            System.err.println("[SERVER] Error con cliente " + clientInfo + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("[SERVER] Cliente desconectado: " + clientInfo);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Maneja comando RESERVAR
     * Formato: RESERVAR;usuario;fecha;horaInicio;horaFin;cantidad;equipoType;equipoQty
     */
    private void handleReserve(String[] parts, PrintWriter out) {
        try {
            if (parts.length < 8) {
                out.println("ERROR: Parámetros insuficientes");
                return;
            }
            
            String usuario = parts[1];
            String fecha = parts[2];
            String horaInicio = parts[3];
            String horaFin = parts[4];
            int cantidad = Integer.parseInt(parts[5]);
            String equipoType = parts[6];
            int equipoQty = Integer.parseInt(parts[7]);

            String resultado = reservationService.createReservation(
                usuario, fecha, horaInicio, horaFin, cantidad, equipoType, equipoQty
            );
            
            out.println(resultado);
            System.out.println("[SERVER] Resultado reserva: " + resultado);
            
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Maneja comando EDITAR
     * Formato: EDITAR;id;fecha;horaInicio;horaFin;cantidad
     */
    private void handleEdit(String[] parts, PrintWriter out) {
        try {
            if (parts.length < 6) {
                out.println("ERROR: Parámetros insuficientes");
                return;
            }
            
            int id = Integer.parseInt(parts[1]);
            String fecha = parts[2];
            String horaInicio = parts[3];
            String horaFin = parts[4];
            int cantidad = Integer.parseInt(parts[5]);

            String resultado = reservationService.editReservation(id, fecha, horaInicio, horaFin, cantidad);
            out.println(resultado);
            
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Maneja comando CONFIRMAR
     * Formato: CONFIRMAR;id
     */
    private void handleConfirm(String[] parts, PrintWriter out) {
        try {
            int id = Integer.parseInt(parts[1]);
            String resultado = reservationService.confirmReservation(id);
            out.println(resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Maneja comando ELIMINAR
     * Formato: ELIMINAR;id
     */
    private void handleDelete(String[] parts, PrintWriter out) {
        try {
            int id = Integer.parseInt(parts[1]);
            String resultado = reservationService.deleteReservation(id);
            out.println(resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Maneja comando LISTAR
     * Formato: LISTAR
     */
    private void handleList(PrintWriter out) {
        try {
            String resultado = reservationService.listReservations();
            out.println(resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }
}