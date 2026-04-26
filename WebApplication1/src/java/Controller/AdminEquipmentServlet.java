package Controller;

import concurrency.SystemLog;
import dao.ReservationEquipmentDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Servlet responsible for all administrative equipment management operations
 * on existing reservations.
 *
 * <p>Mapped to {@code /admin/equipment}, this servlet handles HTTP POST
 * requests from the admin panel to add, update, delete, or bulk-remove
 * audiovisual equipment items associated with a given reservation.
 *
 * <p>Access is restricted to sessions with the {@code ADMIN} role.
 * Unauthenticated requests are redirected to {@code ../index.html};
 * non-admin authenticated requests are redirected to
 * {@code ../dashboard.html?msg=unauthorized}.
 *
 * <p>Supported {@code action} parameter values:
 * <ul>
 *   <li>{@code "add"}       — Assigns a new equipment item to a reservation.</li>
 *   <li>{@code "update"}    — Changes the quantity of an already-assigned item.</li>
 *   <li>{@code "delete"}    — Removes one specific equipment item.</li>
 *   <li>{@code "deleteAll"} — Removes all equipment items from a reservation.</li>
 * </ul>
 *
 * <p>All operations are persisted via {@link ReservationEquipmentDAO} and
 * audited through {@link SystemLog}.
 *
 * @see ReservationEquipmentDAO
 * @see SystemLog
 */
@WebServlet("/admin/equipment")
public class AdminEquipmentServlet extends HttpServlet {

    /**
     * DAO used to persist equipment changes on reservations.
     */
    private final ReservationEquipmentDAO equipmentDAO = new ReservationEquipmentDAO();

    /**
     * Singleton audit log used to record all equipment management actions.
     */
    private final SystemLog log = SystemLog.getInstance();

    /**
     * Handles POST requests for equipment management on reservations.
     *
     * <p>Validates the session and role, then dispatches to the appropriate
     * operation based on the {@code action} request parameter:
     * <ul>
     *   <li>{@code "deleteAll"} — Removes all equipment from the reservation.</li>
     *   <li>{@code "delete"}    — Removes the specified equipment item.</li>
     *   <li>{@code "update"}    — Updates the quantity of an existing item.</li>
     *   <li>{@code "add"}       — Adds a new equipment item with a given quantity.</li>
     * </ul>
     *
     * <p>On success, redirects to {@code ../admin.html?msg=equipment_updated}
     * or {@code ../admin.html?msg=equipment_deleted}. On failure, redirects
     * to {@code ../admin.html?msg=error} or {@code ../admin.html?msg=busy_equipment}.
     *
     * @param request  the HTTP request containing {@code action},
     *                 {@code reservation_id}, {@code equipment_id}, and
     *                 optionally {@code quantity} parameters
     * @param response the HTTP response used to issue the redirect
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirect
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            response.sendRedirect("../index.html");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect("../dashboard.html?msg=unauthorized");
            return;
        }

        String adminUser = session.getAttribute("emailUsuario") != null
                ? session.getAttribute("emailUsuario").toString()
                : "ADMIN";

        String action = request.getParameter("action");
        String reservationIdParam = request.getParameter("reservation_id");

        try {
            if (action == null || reservationIdParam == null || reservationIdParam.trim().isEmpty()) {
                response.sendRedirect("../admin.html?msg=error");
                return;
            }

            int reservationId = Integer.parseInt(reservationIdParam);

            // --- deleteAll: remove every equipment item from the reservation ---
            if ("deleteAll".equalsIgnoreCase(action)) {
                boolean ok = equipmentDAO.deleteByReservationId(reservationId);

                if (ok) {
                    log.log(adminUser, "EQUIPMENT_DELETE_ALL_OK",
                            "Reserva=" + reservationId + ";todos_los_equipos_eliminados");

                    response.sendRedirect("../admin.html?msg=equipment_deleted");
                } else {
                    log.log(adminUser, "EQUIPMENT_DELETE_ALL_ERROR",
                            "No se pudieron eliminar todos los equipos de reserva=" + reservationId);

                    response.sendRedirect("../admin.html?msg=error");
                }

                return;
            }

            String equipmentIdParam = request.getParameter("equipment_id");

            if (equipmentIdParam == null || equipmentIdParam.trim().isEmpty()) {
                response.sendRedirect("../admin.html?msg=error");
                return;
            }

            int equipmentId = Integer.parseInt(equipmentIdParam);

            if (!isValidEquipment(equipmentId)) {
                log.log(adminUser, "EQUIPMENT_ERROR",
                        "Equipo inválido id=" + equipmentId + " reserva=" + reservationId);
                response.sendRedirect("../admin.html?msg=error");
                return;
            }

            // --- delete: remove a single equipment item from the reservation ---
            if ("delete".equalsIgnoreCase(action)) {
                boolean ok = equipmentDAO.delete(reservationId, equipmentId);

                if (ok) {
                    log.log(adminUser, "EQUIPMENT_DELETE_OK",
                            "Reserva=" + reservationId + ";equipo=" + equipmentId);

                    response.sendRedirect("../admin.html?msg=equipment_deleted");
                } else {
                    log.log(adminUser, "EQUIPMENT_DELETE_ERROR",
                            "No se pudo eliminar equipo=" + equipmentId + " reserva=" + reservationId);

                    response.sendRedirect("../admin.html?msg=error");
                }

                return;
            }

            // --- add / update: set or change a quantity for an equipment item ---
            if ("update".equalsIgnoreCase(action) || "add".equalsIgnoreCase(action)) {
                String quantityParam = request.getParameter("quantity");

                if (quantityParam == null || quantityParam.trim().isEmpty()) {
                    response.sendRedirect("../admin.html?msg=error");
                    return;
                }

                int quantity = Integer.parseInt(quantityParam);
                int maxAllowed = getEquipmentMax(equipmentId);

                if (quantity <= 0 || quantity > maxAllowed) {
                    log.log(adminUser, "EQUIPMENT_REJECT",
                            "Cantidad inválida equipo=" + equipmentId
                                    + ";cantidad=" + quantity
                                    + ";max=" + maxAllowed
                                    + ";reserva=" + reservationId);

                    response.sendRedirect("../admin.html?msg=busy_equipment");
                    return;
                }

                boolean ok;

                if ("update".equalsIgnoreCase(action)) {
                    ok = equipmentDAO.updateQuantity(reservationId, equipmentId, quantity);
                } else {
                    ok = equipmentDAO.addOrUpdate(reservationId, equipmentId, quantity);
                }

                if (ok) {
                    log.log(adminUser,
                            "update".equalsIgnoreCase(action) ? "EQUIPMENT_UPDATE_OK" : "EQUIPMENT_ADD_OK",
                            "Reserva=" + reservationId
                                    + ";equipo=" + equipmentId
                                    + ";cantidad=" + quantity);

                    response.sendRedirect("../admin.html?msg=equipment_updated");
                } else {
                    log.log(adminUser, "EQUIPMENT_SAVE_ERROR",
                            "No se pudo guardar equipo=" + equipmentId
                                    + " reserva=" + reservationId);

                    response.sendRedirect("../admin.html?msg=error");
                }

                return;
            }

            response.sendRedirect("../admin.html?msg=error");

        } catch (Exception e) {
            e.printStackTrace();

            log.log(adminUser, "EQUIPMENT_ERROR",
                    "Excepción administrando equipamiento: " + e.getMessage());

            response.sendRedirect("../admin.html?msg=server");
        }
    }

    /**
     * Determines whether the given equipment ID corresponds to a valid,
     * recognized audiovisual item in the system.
     *
     * <p>Valid IDs:
     * <ul>
     *   <li>{@code 1} — Proyector</li>
     *   <li>{@code 2} — Micrófono</li>
     *   <li>{@code 3} — Sistema de sonido</li>
     * </ul>
     *
     * @param equipmentId the numeric equipment identifier to validate
     * @return {@code true} if the ID is recognized; {@code false} otherwise
     */
    private boolean isValidEquipment(int equipmentId) {
        return equipmentId == 1 || equipmentId == 2 || equipmentId == 3;
    }

    /**
     * Returns the maximum quantity allowed for a given equipment type.
     *
     * <p>Inventory limits:
     * <ul>
     *   <li>{@code 1} (Proyector)         — 2 units</li>
     *   <li>{@code 2} (Micrófono)         — 5 units</li>
     *   <li>{@code 3} (Sistema de sonido) — 3 units</li>
     * </ul>
     *
     * @param equipmentId the numeric equipment identifier
     * @return the maximum allowed quantity, or {@code 0} if the ID is unknown
     */
    private int getEquipmentMax(int equipmentId) {
        switch (equipmentId) {
            case 1: return 2; // Proyector
            case 2: return 5; // Micrófono
            case 3: return 3; // Sistema de sonido
            default: return 0;
        }
    }
}