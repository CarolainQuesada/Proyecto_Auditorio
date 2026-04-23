package socket;

import service.ReservationService;
import model.ReservationEquipment;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final ReservationService reservationService;
    private final ServerGUI serverGUI;

    public ClientHandler(Socket socket, ReservationService service, ServerGUI gui) {
        this.clientSocket = socket;
        this.reservationService = service;
        this.serverGUI = gui;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress();
        serverGUI.log("Client connected: " + clientInfo);
        System.out.println("[SERVER] Client connected: " + clientInfo);

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String command = in.readLine();

            if (command == null || command.isEmpty()) {
                out.println("ERROR: Empty command");
                serverGUI.log("Empty command from: " + clientInfo);
                return;
            }

            serverGUI.log("Command from " + clientInfo + ": " + command);
            System.out.println("[SERVER] Command received: " + command);

            String[] parts = command.split(";");
            String action = parts[0].toUpperCase();

            switch (action) {
                case "RESERVE":
                    handleReserve(parts, out);
                    break;
                case "EDIT":
                    handleEdit(parts, out);
                    break;
                case "CONFIRM":
                    handleConfirm(parts, out);
                    break;
                case "DELETE":
                    handleDelete(parts, out);
                    break;
                case "LIST":
                    handleList(out);
                    break;
                default:
                    out.println("ERROR: Unknown command");
                    serverGUI.log("Unknown command from " + clientInfo + ": " + action);
            }

        } catch (IOException e) {
            serverGUI.log("Error with client " + clientInfo + ": " + e.getMessage());
            System.err.println("[SERVER] Error with client " + clientInfo + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                serverGUI.log("Client disconnected: " + clientInfo);
                System.out.println("[SERVER] Client disconnected: " + clientInfo);
            } catch (IOException ignored) {}
        }
    }

    private void handleReserve(String[] parts, PrintWriter out) {
        try {
            if (parts.length < 6) {
                out.println("ERROR: Insufficient parameters");
                return;
            }

            String usuario = parts[1];
            String fecha = parts[2];
            String horaInicio = parts[3];
            String horaFin = parts[4];
            int cantidad = Integer.parseInt(parts[5]);
            
            String equipmentIdsStr = (parts.length > 6) ? parts[6] : "";
            String equipmentQtysStr = (parts.length > 7) ? parts[7] : "";
            
            List<ReservationEquipment> equipments = new ArrayList<>();
            if (!equipmentIdsStr.isEmpty()) {
                String[] eqIds = equipmentIdsStr.split(",");
                String[] eqQtys = equipmentQtysStr.split(",");
                
                for (int i = 0; i < eqIds.length; i++) {
                    try {
                        int eqId = Integer.parseInt(eqIds[i].trim());
                        int eqQty = (i < eqQtys.length) ? Integer.parseInt(eqQtys[i].trim()) : 1;
                        
                        if (eqQty > 0 && eqId > 0) {
                            equipments.add(new ReservationEquipment(0, eqId, eqQty));
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar valores inválidos
                        System.err.println("[ClientHandler] Invalid equipment param: " + e.getMessage());
                    }
                }
            }

            serverGUI.log("Reserve: " + usuario + " | " + fecha + " " + horaInicio + "-" + horaFin + " | Qty: " + cantidad + " | Equipments: " + equipments.size());

            String resultado = reservationService.createReservationWithEquipment(
                usuario, fecha, horaInicio, horaFin, cantidad, equipments
            );

            out.println(resultado);
            serverGUI.log("Result for " + usuario + ": " + resultado);

        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEdit(String[] parts, PrintWriter out) {
        try {
            if (parts.length < 7) {
                out.println("ERROR: Insufficient parameters");
                return;
            }

            int id = Integer.parseInt(parts[1]);
            String fecha = parts[2];
            String horaInicio = parts[3];
            String horaFin = parts[4];
            int cantidad = Integer.parseInt(parts[5]);
            String status = parts[6];

            serverGUI.log("Edit request for reservation ID: " + id);

            String resultado = reservationService.editReservation(id, fecha, horaInicio, horaFin, cantidad, status);
            out.println(resultado);
            serverGUI.log("Edit result for ID " + id + ": " + resultado);

        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleConfirm(String[] parts, PrintWriter out) {
        try {
            int id = Integer.parseInt(parts[1]);
            serverGUI.log("Confirm request for reservation ID: " + id);
            String resultado = reservationService.confirmReservation(id);
            out.println(resultado);
            serverGUI.log("Confirm result for ID " + id + ": " + resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleDelete(String[] parts, PrintWriter out) {
        try {
            int id = Integer.parseInt(parts[1]);
            serverGUI.log("Delete request for reservation ID: " + id);
            String resultado = reservationService.deleteReservation(id);
            out.println(resultado);
            serverGUI.log("Delete result for ID " + id + ": " + resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleList(PrintWriter out) {
        try {
            serverGUI.log("List reservations request");
            String resultado = reservationService.listReservations();
            out.println(resultado);
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }
}