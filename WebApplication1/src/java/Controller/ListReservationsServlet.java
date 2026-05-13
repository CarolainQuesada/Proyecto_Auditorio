package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import util.Config;

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

        if (!"ADMIN".equalsIgnoreCase(role)) {
            resp.setStatus(403);
            resp.getWriter().write("ERROR: forbidden");
            return;
        }

        try {
        String socketHost = Config.getSocketHost();
        int socketPort = Config.getSocketPort();

        System.out.println("[ListReservations] ⏱️ Conectando a: " + socketHost + ":" + socketPort);
        long startConnect = System.currentTimeMillis();

        Socket socket = new Socket();
        socket.connect(
            new java.net.InetSocketAddress(socketHost, socketPort),
            5000  
        );

        System.out.println("[ListReservations] ✅ Conectado en " + (System.currentTimeMillis() - startConnect) + "ms");

        socket.setSoTimeout(15000); 

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("[ListReservations] 📤 Enviando comando LIST...");
            long startSend = System.currentTimeMillis();

            out.println("LIST");
            out.flush();  

            System.out.println("[ListReservations] 📥 Esperando respuesta...");
            String responseServer = in.readLine();

            System.out.println("[ListReservations] ✅ Respuesta recibida en " + (System.currentTimeMillis() - startSend) + "ms");
            System.out.println("[ListReservations] 📦 Tamaño respuesta: " + (responseServer != null ? responseServer.length() : 0) + " caracteres");

            if (responseServer == null) {
                responseServer = "";
            }

            resp.getWriter().write(responseServer);
        }

        socket.close();

    } catch (java.net.ConnectException e) {
        System.err.println("[ListReservations] ❌ ERROR: No se pudo conectar. ¿ServerGUI está corriendo?");
        e.printStackTrace();
        resp.setStatus(503);
        resp.getWriter().write("ERROR: server unavailable");

    } catch (java.net.SocketTimeoutException e) {
        System.err.println("[ListReservations] ❌ ERROR: ServerGUI no respondió en 15 segundos. Revisa si la BD está lenta o hay bloqueo.");
        resp.setStatus(504);
        resp.getWriter().write("ERROR: server timeout");

    } catch (Exception e) {
        System.err.println("[ListReservations] ❌ ERROR: " + e.getMessage());
        e.printStackTrace();
        resp.setStatus(500);
        resp.getWriter().write("ERROR: server");
    }
    }
}