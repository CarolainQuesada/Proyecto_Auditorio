package Controller;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;

/**
 * Servlet that handles administrative editing of existing reservations.
 *
 * <p>Mapped to {@code /editReservation}, this servlet processes HTTP POST
 * requests submitted from the admin panel form. It validates the input,
 * then forwards an {@code EDIT} command to the backend socket server on
 * {@code localhost:5000} and redirects to the admin panel with a result code.
 *
 * <p>Access is restricted to sessions with the {@code ADMIN} role:
 * <ul>
 *   <li>Unauthenticated requests are redirected to {@code index.html}.</li>
 *   <li>Non-admin authenticated requests are redirected to
 *       {@code dashboard.html?msg=unauthorized}.</li>
 * </ul>
 *
 * <p>Client-side validations performed before the socket call:
 * <ul>
 *   <li>All required fields must be non-null and non-empty.</li>
 *   <li>{@code status} must be one of {@code PENDING}, {@code CONFIRMED},
 *       or {@code EXPIRED} (case-insensitive).</li>
 *   <li>Attendee {@code quantity} must be in the range [1, 200].</li>
 *   <li>{@code date} must not be in the past.</li>
 *   <li>{@code start_time} must be strictly earlier than {@code end_time}.</li>
 * </ul>
 *
 * <p>The socket protocol sends a single semicolon-delimited line:
 * <pre>{@code
 * EDIT;<id>;<date>;<start_time>;<end_time>;<quantity>;<status>
 * }</pre>
 * and the server responds with a status keyword that is mapped to a
 * redirect message code (e.g. {@code ok}, {@code busy}, {@code hour}).
 *
 * @see ReservationServlet
 * @see ConfirmReservationServlet
 */
@WebServlet("/editReservation")
public class EditReservationServlet extends HttpServlet {

    /**
     * Processes an edit request for an existing reservation.
     *
     * <p>Validates session, role, and all form parameters, then sends the
     * {@code EDIT} command to the socket server. On completion, the admin
     * is redirected to {@code admin.html} with a {@code msg} query parameter
     * indicating the outcome:
     * <ul>
     *   <li>{@code ok}       — Update applied successfully.</li>
     *   <li>{@code busy}     — Time slot conflict detected by the server.</li>
     *   <li>{@code hour}     — Invalid time range.</li>
     *   <li>{@code quantity} — Attendee count out of range.</li>
     *   <li>{@code past}     — Date is in the past.</li>
     *   <li>{@code server}   — Backend server unreachable or returned null.</li>
     *   <li>{@code error}    — Any other unexpected condition.</li>
     * </ul>
     *
     * @param request  the HTTP request; must include the parameters
     *                 {@code id}, {@code date}, {@code start_time},
     *                 {@code end_time}, {@code quantity}, and {@code status}
     * @param response the HTTP response used to issue the redirect
     * @throws IOException if an I/O error occurs during socket communication
     *                     or response redirect
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("emailUsuario") == null
                || session.getAttribute("role") == null) {

            response.sendRedirect("index.html");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect("dashboard.html?msg=unauthorized");
            return;
        }

        String id       = request.getParameter("id");
        String date     = request.getParameter("date");
        String startTime = request.getParameter("start_time");
        String endTime  = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");
        String status   = request.getParameter("status");

        try {
            // Validate that all required parameters are present and non-empty.
            if (id == null || id.trim().isEmpty()
                    || date == null || date.trim().isEmpty()
                    || startTime == null || startTime.trim().isEmpty()
                    || endTime == null || endTime.trim().isEmpty()
                    || quantity == null || quantity.trim().isEmpty()
                    || status == null || status.trim().isEmpty()) {

                response.sendRedirect("admin.html?msg=error");
                return;
            }

            id        = id.trim();
            date      = date.trim();
            startTime = startTime.trim();
            endTime   = endTime.trim();
            quantity  = quantity.trim();
            status    = status.trim().toUpperCase();

            // Only allow recognized status transitions.
            if (!"PENDING".equals(status)
                    && !"CONFIRMED".equals(status)
                    && !"EXPIRED".equals(status)) {

                response.sendRedirect("admin.html?msg=error");
                return;
            }

            int qty = Integer.parseInt(quantity);

            if (qty <= 0 || qty > 200) {
                response.sendRedirect("admin.html?msg=quantity");
                return;
            }

            // Reject past dates at the servlet level before hitting the server.
            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today           = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect("admin.html?msg=past");
                return;
            }

            if (startTime.compareTo(endTime) >= 0) {
                response.sendRedirect("admin.html?msg=hour");
                return;
            }

            try (
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                String command = "EDIT;"
                        + id + ";"
                        + date + ";"
                        + startTime + ";"
                        + endTime + ";"
                        + quantity + ";"
                        + status;

                out.println(command);

                String responseServer = in.readLine();

                if (responseServer == null) {
                    response.sendRedirect("admin.html?msg=server");
                    return;
                }

                String result = responseServer.trim().toLowerCase();

                if (result.contains("updated")) {
                    response.sendRedirect("admin.html?msg=ok");
                } else if (result.contains("hour")) {
                    response.sendRedirect("admin.html?msg=hour");
                } else if (result.contains("quantity")) {
                    response.sendRedirect("admin.html?msg=quantity");
                } else if (result.contains("busy_time") || result.contains("busy")) {
                    response.sendRedirect("admin.html?msg=busy");
                } else if (result.contains("past")) {
                    response.sendRedirect("admin.html?msg=past");
                } else {
                    response.sendRedirect("admin.html?msg=error");
                }
            }

        } catch (NumberFormatException e) {
            response.sendRedirect("admin.html?msg=quantity");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.html?msg=server");
        }
    }
}