package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that allows an authenticated CLIENT user to cancel one of their own
 * reservations.
 *
 * <p>Mapped to {@code /cancelReservation}, this servlet processes HTTP GET
 * requests from the user profile page. It forwards a
 * {@code CANCEL;<id>;<email>} command to the backend socket server on
 * {@code localhost:5000}. The server verifies ownership before deleting.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>No active session → HTTP 401, body {@code "unauthorized"}.</li>
 *   <li>ADMIN role → HTTP 403, body {@code "forbidden"}. Admins use
 *       {@code /deleteReservation} instead.</li>
 * </ul>
 *
 * <p>Required query parameter: {@code id} — the reservation ID to cancel.
 *
 * <p>Response body values (plain text):
 * <ul>
 *   <li>{@code "ok"}          — Cancelled successfully.</li>
 *   <li>{@code "not_found"}   — Reservation does not exist or belongs to
 *                               another user.</li>
 *   <li>{@code "not_allowed"} — Reservation is EXPIRED and cannot be
 *                               cancelled.</li>
 *   <li>{@code "error"}       — Server or socket error.</li>
 * </ul>
 *
 * @see MyReservationsServlet
 */
@WebServlet("/cancelReservation")
public class CancelReservationServlet extends HttpServlet {

    /**
     * Processes the cancellation request for the authenticated user's reservation.
     *
     * @param req  the HTTP request; must include the {@code id} query parameter
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
            resp.getWriter().write("unauthorized");
            return;
        }

        String role = session.getAttribute("role") != null
                ? session.getAttribute("role").toString() : "";

        if ("ADMIN".equalsIgnoreCase(role)) {
            resp.setStatus(403);
            resp.getWriter().write("forbidden");
            return;
        }

        String id = req.getParameter("id");

        if (id == null || id.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("error");
            return;
        }

        String email = session.getAttribute("emailUsuario").toString();

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("CANCEL;" + id.trim() + ";" + email);
            String responseServer = in.readLine();

            if (responseServer == null) {
                resp.setStatus(500);
                resp.getWriter().write("error");
                return;
            }

            switch (responseServer.trim()) {
                case "cancelled":
                    resp.getWriter().write("ok");
                    break;
                case "not_found":
                    resp.setStatus(404);
                    resp.getWriter().write("not_found");
                    break;
                case "not_allowed":
                    resp.setStatus(409);
                    resp.getWriter().write("not_allowed");
                    break;
                default:
                    resp.setStatus(500);
                    resp.getWriter().write("error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("error");
        }
    }
}