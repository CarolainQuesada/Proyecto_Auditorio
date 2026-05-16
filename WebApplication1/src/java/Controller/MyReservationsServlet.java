package Controller;

import java.io.IOException;
import service.ReservationService;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that retrieves all reservations belonging to the currently
 * authenticated CLIENT user.
 *
 * <p>Mapped to {@code /myReservations}, this servlet handles HTTP GET
 * requests and queries the reservation service directly. The raw
 * pipe-delimited response is passed directly to the frontend for rendering.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>No active session → HTTP 401, body {@code "ERROR: unauthorized"}.</li>
 *   <li>Active session with ADMIN role → HTTP 403, body
 *       {@code "ERROR: forbidden"}.</li>
 * </ul>
 *
 * <p>Response format (same as LIST but only for the requesting user):
 * <pre>{@code
 * id,date,startTime,endTime,quantity,status,equipment|...
 * }</pre>
 *
 * @see CancelReservationServlet
 */
@WebServlet("/myReservations")
public class MyReservationsServlet extends HttpServlet {

    private final ReservationService reservationService = new ReservationService();

    /**
     * Fetches and returns the authenticated user's reservations as a
     * plain-text pipe-delimited response.
     *
     * @param req  the HTTP request; no parameters required beyond the session
     * @param resp the HTTP response; content type is {@code text/plain;charset=UTF-8}
     * @throws IOException if an I/O error occurs during socket communication
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("emailUsuario") == null) {
            resp.setStatus(401);
            resp.getWriter().write("ERROR: unauthorized");
            return;
        }

        String role = session.getAttribute("role") != null
                ? session.getAttribute("role").toString() : "";

        if ("ADMIN".equalsIgnoreCase(role)) {
            resp.setStatus(403);
            resp.getWriter().write("ERROR: forbidden");
            return;
        }

        String email = session.getAttribute("emailUsuario").toString();

        try {
            String response = reservationService.listReservationsByUser(email);
            resp.getWriter().write(response != null ? response : "");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("ERROR: server");
        }
    }
}
