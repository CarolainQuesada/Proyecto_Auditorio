package socket;

import service.ReservationService;
import model.ReservationEquipment;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles a single client connection on the reservation server.
 *
 * <p>Each instance of {@code ClientHandler} is created by {@link ServerGUI}
 * when a new TCP connection is accepted, and runs on its own thread (extends
 * {@link Thread}). The handler reads exactly one command line from the client,
 * dispatches it to the appropriate {@code handle*} method, writes the result
 * back, and then closes the socket.
 *
 * <h2>Supported commands</h2>
 * <ul>
 *   <li>{@code RESERVE} — create a new reservation, optionally with equipment.</li>
 *   <li>{@code EDIT}    — update date, time, quantity, or status of an existing reservation.</li>
 *   <li>{@code CONFIRM} — confirm a pending reservation within its TTL window.</li>
 *   <li>{@code DELETE}  — permanently remove a reservation and its equipment links.</li>
 *   <li>{@code LIST}    — retrieve all non-expired reservations.</li>
 * </ul>
 *
 * <h2>Wire format</h2>
 * <p>Commands are semicolon-delimited strings sent as a single line, for example:
 * <pre>
 * RESERVE;juan@una.ac.cr;2026-04-30;08:00;10:00;80;1,2;1,2
 * </pre>
 * The command is parsed with {@code split(";", -1)} to preserve trailing
 * empty fields when no equipment is provided.
 *
 * @see ServerGUI
 * @see ReservationService
 */
public class ClientHandler extends Thread {

    /** The socket for this client connection. */
    private final Socket clientSocket;

    /** Service layer used to execute reservation business logic. */
    private final ReservationService reservationService;

    /** Reference to the server GUI for logging. */
    private final ServerGUI serverGUI;

    /**
     * Constructs a new {@code ClientHandler} for the given client socket.
     *
     * @param socket  the accepted client socket; must not be {@code null}
     * @param service the reservation service to delegate operations to
     * @param gui     the server GUI used for logging
     */
    public ClientHandler(Socket socket, ReservationService service, ServerGUI gui) {
        this.clientSocket = socket;
        this.reservationService = service;
        this.serverGUI = gui;
    }

    /**
     * Entry point of the client-handler thread.
     *
     * <p>Reads one command line, routes it to the appropriate handler method,
     * and ensures the socket is closed when processing finishes (or if an
     * exception is thrown).
     */
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

    /**
     * Processes a {@code RESERVE} command sent by the client.
     *
     * <p>Expected format:
     * <pre>
     * RESERVE;usuario;fecha;inicio;fin;cantidad[;equipmentIds[;equipmentQtys]]
     * </pre>
     * Fields {@code equipmentIds} and {@code equipmentQtys} are optional
     * comma-separated lists. If both are absent the reservation is created
     * without equipment.
     *
     * <p>Possible responses written to {@code out}:
     * <ul>
     *   <li>{@code ok}               — reservation created successfully.</li>
     *   <li>{@code busy_equipment}   — one or more equipment items are unavailable or invalid.</li>
     *   <li>{@code quantity}         — a numeric field could not be parsed.</li>
     *   <li>{@code error}            — any other failure (missing fields, service error, etc.).</li>
     * </ul>
     *
     * @param parts the command split by {@code ";"}, index 0 is the action token
     * @param out   the writer connected to the client socket
     */
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

    /**
     * Parses the equipment portion of a {@code RESERVE} command into a list
     * of {@link ReservationEquipment} objects.
     *
     * <p>Both {@code equipmentIdsStr} and {@code equipmentQtysStr} must be
     * comma-separated strings of equal length. Each equipment ID is validated
     * via {@link #isValidEquipment(int)} and each quantity must be a positive
     * integer that does not exceed the maximum returned by
     * {@link #getEquipmentMax(int)}.
     *
     * @param equipmentIdsStr  comma-separated equipment IDs, or empty/null if none
     * @param equipmentQtysStr comma-separated quantities matching {@code equipmentIdsStr}
     * @return a (possibly empty) list of equipment items, or {@code null} if
     *         the input is malformed, IDs are invalid, or quantities are out of range
     */
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

    /**
     * Processes an {@code EDIT} command sent by the client.
     *
     * <p>Expected format:
     * <pre>
     * EDIT;id;fecha;inicio;fin;cantidad;status
     * </pre>
     * The {@code status} field must be one of {@code PENDING}, {@code CONFIRMED},
     * or {@code EXPIRED} (case-insensitive on input, normalized to upper-case).
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code ok}       — update applied successfully.</li>
     *   <li>{@code quantity} — a numeric field could not be parsed.</li>
     *   <li>{@code error}    — missing/empty fields, invalid status, or service failure.</li>
     * </ul>
     *
     * @param parts the command split by {@code ";"}, index 0 is the action token
     * @param out   the writer connected to the client socket
     */
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

    /**
     * Processes a {@code CONFIRM} command sent by the client.
     *
     * <p>Expected format:
     * <pre>
     * CONFIRM;id
     * </pre>
     * The reservation can only be confirmed while its status is {@code PENDING}
     * and within the TTL window defined by the persistence layer.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code ok}    — reservation confirmed.</li>
     *   <li>{@code error} — missing or non-numeric ID, or service failure.</li>
     * </ul>
     *
     * @param parts the command split by {@code ";"}, index 0 is the action token
     * @param out   the writer connected to the client socket
     */
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

    /**
     * Processes a {@code DELETE} command sent by the client.
     *
     * <p>Expected format:
     * <pre>
     * DELETE;id
     * </pre>
     * Deletion is transactional: the associated {@code reservation_equipment}
     * rows are removed before the reservation itself.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code ok}    — reservation deleted.</li>
     *   <li>{@code error} — missing or non-numeric ID, or service failure.</li>
     * </ul>
     *
     * @param parts the command split by {@code ";"}, index 0 is the action token
     * @param out   the writer connected to the client socket
     */
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

    /**
     * Processes a {@code LIST} command sent by the client.
     *
     * <p>Expected format:
     * <pre>
     * LIST
     * </pre>
     * Returns all non-expired reservations ordered by date and start time as
     * a newline-separated string produced by {@link ReservationService#listReservations()}.
     *
     * @param out the writer connected to the client socket
     */
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

    /**
     * Returns {@code true} if the given equipment ID is recognised by this
     * server.
     *
     * <p>Valid IDs: {@code 1} (Proyector), {@code 2} (Micrófono),
     * {@code 3} (Sistema de sonido).
     *
     * @param equipmentId the ID to validate
     * @return {@code true} if valid; {@code false} otherwise
     */
    private boolean isValidEquipment(int equipmentId) {
        return equipmentId == 1 || equipmentId == 2 || equipmentId == 3;
    }

    /**
     * Returns the maximum number of units that can be requested for a given
     * equipment type in a single reservation.
     *
     * <ul>
     *   <li>ID 1 (Proyector)         → 2 units</li>
     *   <li>ID 2 (Micrófono)         → 5 units</li>
     *   <li>ID 3 (Sistema de sonido) → 3 units</li>
     * </ul>
     *
     * @param equipmentId the equipment ID
     * @return the maximum allowed quantity, or {@code 0} for unknown IDs
     */
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