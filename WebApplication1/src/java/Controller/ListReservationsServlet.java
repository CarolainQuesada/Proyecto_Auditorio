package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that retrieves all reservations from the backend server for the
 * admin panel.
 *
 * <p>Mapped to {@code /listReservations}, this servlet handles HTTP GET
 * requests and forwards a {@code LIST} command to the backend socket server
 * on {@code localhost:5000}. The raw response is passed directly to the
 * frontend, which parses and renders it as the reservations table.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>No active session → HTTP 401, body {@code "ERROR: unauthorized"}.</li>
 *   <li>Active session with a non-ADMIN role → HTTP 403, body
 *       {@code "ERROR: forbidden"}.</li>
 * </ul>
 *
 * <p>The socket protocol sends a single line:
 * <pre>{@code
 * LIST
 * }</pre>
 * and expects the server to reply with a pipe-delimited list of reservations.
 * Each entry uses comma separation for its fields:
 * <pre>{@code
 * <id>,<date>,<startTime>,<endTime>,<quantity>,<status>,<user>,<equipment>|...
 * }</pre>
 * An empty string is written if the server response is {@code null}.
 * On socket failure, HTTP 500 is set and the body begins with
 * {@code "ERROR: server"}.
 *
 * @see DeleteReservationServlet
 * @see ConfirmReservationServlet
 * @see EditReservationServlet
 */
@WebServlet("/listReservations")
public class ListReservationsServlet extends HttpServlet {

    /**
     * Fetches and returns all reservations as a plain-text pipe-delimited response.
     *
     * <p>Validates session and role, opens a TCP socket to the backend, sends
     * the {@code LIST} command, and writes the server's reply directly to the
     * HTTP response body.
     *
     * @param req  the HTTP request; no parameters are required
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
            resp.getWriter().write("ERROR: unauthorized");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {            resp.setStatus(403);
            resp.getWriter().write("ERROR: forbidden");
            return;
        }

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("LIST");
            String responseServer = in.readLine();

            if (responseServer == null) {
                responseServer = "";
            }

            resp.getWriter().write(responseServer);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("ERROR: server");
        }
    }
}