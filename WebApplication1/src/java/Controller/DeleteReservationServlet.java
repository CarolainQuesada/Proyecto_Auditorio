package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that handles the deletion of a reservation by an administrator.
 *
 * <p>Mapped to {@code /deleteReservation}, this servlet processes HTTP GET
 * requests from the admin panel, forwards a {@code DELETE} command to the
 * backend socket server on {@code localhost:5000}, and returns a plain-text
 * result to the caller.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>No active session → HTTP 401, body {@code "unauthorized"}.</li>
 *   <li>Active session with a non-ADMIN role → HTTP 403, body
 *       {@code "forbidden"}.</li>
 * </ul>
 *
 * <p>The socket protocol sends a single line:
 * <pre>{@code
 * DELETE;<reservationId>
 * }</pre>
 * and expects the server to reply with {@code "deleted"} on success.
 *
 * <p>Response body values (plain text):
 * <ul>
 *   <li>{@code "ok"}    — The reservation was successfully deleted.</li>
 *   <li>{@code "error"} — The server did not confirm deletion or an
 *                         exception occurred.</li>
 * </ul>
 *
 * @see ConfirmReservationServlet
 * @see ListReservationsServlet
 */
@WebServlet("/deleteReservation")
public class DeleteReservationServlet extends HttpServlet {

    /**
     * Processes the deletion request for a reservation.
     *
     * <p>Validates session, role, and the mandatory {@code id} parameter,
     * then opens a TCP connection to the backend server and sends
     * {@code DELETE;<id>}. Writes {@code "ok"} or {@code "error"} as the
     * plain-text response body and sets the appropriate HTTP status code.
     *
     * @param req  the HTTP request; must include the {@code id} query parameter
     *             identifying the reservation to delete
     * @param resp the HTTP response; content type is set to
     *             {@code text/plain;charset=UTF-8}
     * @throws IOException if an I/O error occurs during socket communication
     *                     or while writing the response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            resp.setStatus(401);
            resp.getWriter().write("unauthorized");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
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

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("DELETE;" + id.trim());

            String responseServer = in.readLine();

            if (responseServer != null && responseServer.trim().equalsIgnoreCase("deleted")) {
                resp.getWriter().write("ok");
            } else {
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