package Controller;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Servlet that handles administrative confirmation of pending reservations.
 *
 * <p>Mapped to {@code /confirmReservation}, this servlet accepts HTTP GET
 * requests from the admin panel and forwards a {@code CONFIRM} command to
 * the backend socket server running on {@code localhost:5000}.
 *
 * <p>Access is restricted to sessions with the {@code ADMIN} role:
 * <ul>
 *   <li>Unauthenticated requests are redirected to {@code index.html}.</li>
 *   <li>Non-admin authenticated requests are redirected to
 *       {@code dashboard.html?msg=unauthorized}.</li>
 * </ul>
 *
 * <p>The socket protocol expects a single line in the form:
 * <pre>{@code
 * CONFIRM;<reservationId>
 * }</pre>
 * and receives one of the following responses:
 * <ul>
 *   <li>{@code "confirmed"} — reservation was successfully confirmed.</li>
 *   <li>{@code "expired"}   — reservation had already expired and cannot
 *                             be confirmed.</li>
 *   <li>any other value     — treated as a generic error.</li>
 * </ul>
 *
 * <p>On completion, the admin is redirected to {@code admin.html} with an
 * appropriate {@code msg} query parameter ({@code confirmed}, {@code expired},
 * {@code error}, or {@code server}).
 *
 * @see AdminEquipmentServlet
 */
@WebServlet("/confirmReservation")
public class ConfirmReservationServlet extends HttpServlet {

    /**
     * Processes the confirmation request for a reservation.
     *
     * <p>Validates the session role, then opens a TCP connection to the
     * backend server and sends the {@code CONFIRM;<id>} command. The server
     * response is parsed and the admin is redirected accordingly.
     *
     * @param req  the HTTP request; must contain the {@code id} query parameter
     *             with the reservation identifier to confirm
     * @param resp the HTTP response used to issue the redirect
     * @throws IOException if an I/O error occurs during socket communication
     *                     or response redirect
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            resp.sendRedirect("index.html");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
            resp.sendRedirect("dashboard.html?msg=unauthorized");
            return;
        }

        if (id == null || id.trim().isEmpty()) {
            resp.sendRedirect("admin.html?msg=error");
            return;
        }

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("CONFIRM;" + id.trim());

            String responseServer = in.readLine();

            if (responseServer == null) {
                resp.sendRedirect("admin.html?msg=server");
                return;
            }

            String result = responseServer.trim().toLowerCase();

            if (result.contains("confirmed")) {
                resp.sendRedirect("admin.html?msg=confirmed");
            } else if (result.contains("expired")) {
                resp.sendRedirect("admin.html?msg=expired");
            } else {
                resp.sendRedirect("admin.html?msg=error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("admin.html?msg=server");
        }
    }
}