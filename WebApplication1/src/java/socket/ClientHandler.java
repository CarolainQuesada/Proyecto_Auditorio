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

            if (command == null || command.trim().isEmpty()) {
                out.println("error");
                serverGUI.log("Empty command from: " + clientInfo);
                return;
            }

            serverGUI.log("Command from " + clientInfo + ": " + command);
            System.out.println("[SERVER] Command received: " + command);

            /*
             * Se usa split(";", -1) para NO perder campos vacíos.
             *
             * Ejemplo sin equipo:
             * RESERVE;juan@una.ac.cr;2026-04-30;08:00;10:00;80;;
             *
             * Con split normal Java elimina los campos vacíos finales.
             */
            String[] parts = command.split(";", -1);

            if (parts.length == 0 || parts[0].trim().isEmpty()) {
                out.println("error");
                serverGUI.log("Invalid command from: " + clientInfo);
                return;
            }

            String action = parts[0].trim().toUpperCase();

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
                    out.println("error");
                    serverGUI.log("Unknown command from " + clientInfo + ": " + action);
                    break;
            }

        } catch (IOException e) {
            serverGUI.log("Error with client " + clientInfo + ": " + e.getMessage());
            System.err.println("[SERVER] Error with client " + clientInfo + ": " + e.getMessage());

        } finally {
            try {
                clientSocket.close();
                serverGUI.log("Client disconnected: " + clientInfo);
                System.out.println("[SERVER] Client disconnected: " + clientInfo);

            } catch (IOException ignored) {
            }
        }
    }

    private void handleReserve(String[] parts, PrintWriter out) {
        try {
            /*
             * Formato esperado:
             * RESERVE;usuario;fecha;inicio;fin;cantidad;equipos;cantidades
             *
             * Ejemplo:
             * RESERVE;juan@una.ac.cr;2026-04-30;08:00;10:00;80;1,2,3;1,2,1
             */
            if (parts.length < 6) {
                out.println("error");
                serverGUI.log("RESERVE rejected: insufficient parameters");
                return;
            }

            String usuario = parts[1].trim();
            String fecha = parts[2].trim();
            String horaInicio = parts[3].trim();
            String horaFin = parts[4].trim();
            String cantidadStr = parts[5].trim();

            if (usuario.isEmpty() || fecha.isEmpty() || horaInicio.isEmpty()
                    || horaFin.isEmpty() || cantidadStr.isEmpty()) {
                out.println("error");
                serverGUI.log("RESERVE rejected: empty required fields");
                return;
            }

            int cantidad = Integer.parseInt(cantidadStr);

            String equipmentIdsStr = parts.length > 6 ? parts[6].trim() : "";
            String equipmentQtysStr = parts.length > 7 ? parts[7].trim() : "";

            List<ReservationEquipment> equipments = parseEquipments(equipmentIdsStr, equipmentQtysStr);

            if (equipments == null) {
                out.println("busy_equipment");
                serverGUI.log("RESERVE rejected: invalid equipment parameters");
                return;
            }

            serverGUI.log(
                    "Reserve: " + usuario
                    + " | " + fecha + " " + horaInicio + "-" + horaFin
                    + " | Qty: " + cantidad
                    + " | Equipments: " + equipments.size()
            );

            String resultado = reservationService.createReservationWithEquipment(
                    usuario,
                    fecha,
                    horaInicio,
                    horaFin,
                    cantidad,
                    equipments
            );

            out.println(resultado);
            serverGUI.log("Reserve result for " + usuario + ": " + resultado);

        } catch (NumberFormatException e) {
            out.println("quantity");
            serverGUI.log("RESERVE rejected: invalid numeric value");

        } catch (Exception e) {
            out.println("error");
            serverGUI.log("RESERVE error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<ReservationEquipment> parseEquipments(String equipmentIdsStr, String equipmentQtysStr) {
        List<ReservationEquipment> equipments = new ArrayList<>();

        if (equipmentIdsStr == null || equipmentIdsStr.trim().isEmpty()) {
            return equipments;
        }

        if (equipmentQtysStr == null || equipmentQtysStr.trim().isEmpty()) {
            return null;
        }

        String[] eqIds = equipmentIdsStr.split(",");
        String[] eqQtys = equipmentQtysStr.split(",");

        if (eqIds.length != eqQtys.length) {
            return null;
        }

        for (int i = 0; i < eqIds.length; i++) {
            try {
                int eqId = Integer.parseInt(eqIds[i].trim());
                int eqQty = Integer.parseInt(eqQtys[i].trim());

                if (!isValidEquipment(eqId)) {
                    return null;
                }

                if (eqQty <= 0 || eqQty > getEquipmentMax(eqId)) {
                    return null;
                }

                equipments.add(new ReservationEquipment(0, eqId, eqQty));

            } catch (NumberFormatException e) {
                return null;
            }
        }

        return equipments;
    }

    private void handleEdit(String[] parts, PrintWriter out) {
        try {
            /*
             * Formato:
             * EDIT;id;fecha;inicio;fin;cantidad;status
             */
            if (parts.length < 7) {
                out.println("error");
                serverGUI.log("EDIT rejected: insufficient parameters");
                return;
            }

            int id = Integer.parseInt(parts[1].trim());
            String fecha = parts[2].trim();
            String horaInicio = parts[3].trim();
            String horaFin = parts[4].trim();
            int cantidad = Integer.parseInt(parts[5].trim());
            String status = parts[6].trim().toUpperCase();

            if (fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty() || status.isEmpty()) {
                out.println("error");
                serverGUI.log("EDIT rejected: empty fields");
                return;
            }

            if (!"PENDING".equals(status)
                    && !"CONFIRMED".equals(status)
                    && !"EXPIRED".equals(status)) {
                out.println("error");
                serverGUI.log("EDIT rejected: invalid status " + status);
                return;
            }

            serverGUI.log("Edit request for reservation ID: " + id);

            String resultado = reservationService.editReservation(
                    id,
                    fecha,
                    horaInicio,
                    horaFin,
                    cantidad,
                    status
            );

            out.println(resultado);
            serverGUI.log("Edit result for ID " + id + ": " + resultado);

        } catch (NumberFormatException e) {
            out.println("quantity");
            serverGUI.log("EDIT rejected: invalid numeric value");

        } catch (Exception e) {
            out.println("error");
            serverGUI.log("EDIT error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConfirm(String[] parts, PrintWriter out) {
        try {
            /*
             * Formato:
             * CONFIRM;id
             */
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                out.println("error");
                serverGUI.log("CONFIRM rejected: missing id");
                return;
            }

            int id = Integer.parseInt(parts[1].trim());

            serverGUI.log("Confirm request for reservation ID: " + id);

            String resultado = reservationService.confirmReservation(id);

            out.println(resultado);
            serverGUI.log("Confirm result for ID " + id + ": " + resultado);

        } catch (NumberFormatException e) {
            out.println("error");
            serverGUI.log("CONFIRM rejected: invalid id");

        } catch (Exception e) {
            out.println("error");
            serverGUI.log("CONFIRM error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(String[] parts, PrintWriter out) {
        try {
            /*
             * Formato:
             * DELETE;id
             */
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                out.println("error");
                serverGUI.log("DELETE rejected: missing id");
                return;
            }

            int id = Integer.parseInt(parts[1].trim());

            serverGUI.log("Delete request for reservation ID: " + id);

            String resultado = reservationService.deleteReservation(id);

            out.println(resultado);
            serverGUI.log("Delete result for ID " + id + ": " + resultado);

        } catch (NumberFormatException e) {
            out.println("error");
            serverGUI.log("DELETE rejected: invalid id");

        } catch (Exception e) {
            out.println("error");
            serverGUI.log("DELETE error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleList(PrintWriter out) {
        try {
            serverGUI.log("List reservations request");

            String resultado = reservationService.listReservations();

            out.println(resultado);

        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
            serverGUI.log("LIST error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidEquipment(int equipmentId) {
        return equipmentId == 1 || equipmentId == 2 || equipmentId == 3;
    }

    private int getEquipmentMax(int equipmentId) {
        switch (equipmentId) {
            case 1:
                return 2; // Proyector
            case 2:
                return 5; // Micrófono
            case 3:
                return 3; // Sistema de sonido
            default:
                return 0;
        }
    }
}