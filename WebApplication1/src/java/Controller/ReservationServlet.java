package Controller;

import util.SocketClient;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet that handles new reservation creation submitted from the user dashboard.
 *
 * <p>Mapped to {@code /ReservationServlet}, this servlet processes HTTP POST
 * requests from the calendar-based reservation form. It validates all input,
 * collects requested audiovisual equipment, and forwards a {@code RESERVE}
 * command to the backend socket server via {@link SocketClient}.
 *
 * <p>Access requires an authenticated session with a valid {@code emailUsuario}
 * attribute. Unauthenticated requests are redirected to {@code index.html}.
 *
 * <p>Client-side validations performed before the socket call:
 * <ul>
 *   <li>All core fields ({@code date}, {@code start_time}, {@code end_time},
 *       {@code quantity}) must be non-null and non-empty.</li>
 *   <li>{@code date} must not be in the past.</li>
 *   <li>{@code start_time} must be strictly earlier than {@code end_time}.</li>
 *   <li>{@code quantity} must be in the range [1, 200].</li>
 *   <li>Each selected equipment item's quantity must be in the range
 *       [1, max] where max is defined by {@link #getEquipmentMax(int)}.</li>
 * </ul>
 *
 * <p>The socket command sent to the backend has the following format:
 * <pre>{@code
 * RESERVE;<user>;<date>;<start_time>;<end_time>;<quantity>;<equipmentIds>;<equipmentQtys>
 * }</pre>
 * where {@code equipmentIds} and {@code equipmentQtys} are comma-separated
 * lists that may be empty if no equipment was requested.
 *
 * <p>On completion, the user is redirected to {@code dashboard.html} with a
 * {@code msg} query parameter:
 * <ul>
 *   <li>{@code ok}             — Reservation created successfully.</li>
 *   <li>{@code busy}           — Time slot conflict.</li>
 *   <li>{@code busy_capacity}  — Auditorium capacity exceeded.</li>
 *   <li>{@code busy_equipment} — Requested equipment not available.</li>
 *   <li>{@code hour}           — Invalid time range.</li>
 *   <li>{@code quantity}       — Attendee count out of range.</li>
 *   <li>{@code past}           — Date is in the past.</li>
 *   <li>{@code server}         — Backend server unreachable.</li>
 *   <li>{@code error}          — Any other unexpected condition.</li>
 * </ul>
 *
 * @see SocketClient
 * @see EditReservationServlet
 */
@WebServlet("/ReservationServlet")
public class ReservationServlet extends HttpServlet {

    /**
     * Processes a new reservation request from the user dashboard.
     *
     * <p>Validates session, all form fields, and equipment quantities.
     * Builds the {@code RESERVE} command and sends it through
     * {@link SocketClient#sendCommand(String)}. Redirects to
     * {@code dashboard.html} with the appropriate {@code msg} code.
     *
     * @param request  the HTTP request; must include {@code date},
     *                 {@code start_time}, {@code end_time}, {@code quantity},
     *                 and optionally {@code equipment[]} and
     *                 {@code eqQty_<id>} parameters
     * @param response the HTTP response used to issue the redirect
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirect
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("emailUsuario") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String user = (String) session.getAttribute("emailUsuario");

        String date     = request.getParameter("date");
        String start    = request.getParameter("start_time");
        String end      = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");

        try {
            if (date == null || date.trim().isEmpty()
                    || start == null || start.trim().isEmpty()
                    || end == null || end.trim().isEmpty()
                    || quantity == null || quantity.trim().isEmpty()) {

                response.sendRedirect("dashboard.html?msg=error");
                return;
            }

            // Reject past dates before reaching the socket server.
            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today           = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect("dashboard.html?msg=past");
                return;
            }

            if (start.compareTo(end) >= 0) {
                response.sendRedirect("dashboard.html?msg=hour");
                return;
            }

            int qty = Integer.parseInt(quantity);

            if (qty <= 0 || qty > 200) {
                response.sendRedirect("dashboard.html?msg=quantity");
                return;
            }

        } catch (Exception e) {
            response.sendRedirect("dashboard.html?msg=error");
            return;
        }

        // Build equipment lists from the multi-value "equipment" parameter.
        String[] selectedEquipments = request.getParameterValues("equipment");

        List<String> equipmentIds  = new ArrayList<>();
        List<String> equipmentQtys = new ArrayList<>();

        if (selectedEquipments != null) {
            for (String equipmentIdStr : selectedEquipments) {
                try {
                    int equipmentId  = Integer.parseInt(equipmentIdStr);
                    int maxAllowed   = getEquipmentMax(equipmentId);

                    if (maxAllowed == 0) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    String qtyParam = request.getParameter("eqQty_" + equipmentId);

                    if (qtyParam == null || qtyParam.trim().isEmpty()) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    int equipmentQuantity = Integer.parseInt(qtyParam.trim());

                    if (equipmentQuantity <= 0 || equipmentQuantity > maxAllowed) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    equipmentIds.add(String.valueOf(equipmentId));
                    equipmentQtys.add(String.valueOf(equipmentQuantity));

                } catch (NumberFormatException e) {
                    response.sendRedirect("dashboard.html?msg=busy_equipment");
                    return;
                }
            }
        }

        String equipmentStr = String.join(",", equipmentIds);
        String eqQtyStr     = String.join(",", equipmentQtys);

        String command = String.format(
                "RESERVE;%s;%s;%s;%s;%s;%s;%s",
                user, date, start, end, quantity, equipmentStr, eqQtyStr
        );

        System.out.println("[Servlet] Enviando: " + command);

        String resultado = SocketClient.sendCommand(command);

        if (resultado == null) {
            response.sendRedirect("dashboard.html?msg=server");
            return;
        }

        resultado = resultado.trim().toLowerCase();

        if (resultado.equals("created")) {
            response.sendRedirect("dashboard.html?msg=ok");
            return;
        }

        // Map server response codes to frontend message codes.
        String msg = "error";

        if      (resultado.equals("busy_time"))      msg = "busy";
        else if (resultado.equals("busy_capacity"))  msg = "busy_capacity";
        else if (resultado.equals("busy_equipment")) msg = "busy_equipment";
        else if (resultado.equals("hour"))           msg = "hour";
        else if (resultado.equals("quantity"))       msg = "quantity";
        else if (resultado.equals("past"))           msg = "past";

        response.sendRedirect("dashboard.html?msg=" + msg);
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