package Controller;

import concurrency.SystemLog;
import dao.ReservationEquipmentDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/equipment")
public class AdminEquipmentServlet extends HttpServlet {

    private final ReservationEquipmentDAO equipmentDAO = new ReservationEquipmentDAO();
    private final SystemLog log = SystemLog.getInstance();

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