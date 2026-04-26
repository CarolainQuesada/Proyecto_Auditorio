package Controller;

import dao.LogDAO;
import model.Log;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that returns the full system audit log to authorized administrators.
 *
 * <p>Mapped to {@code /listLogs}, this servlet handles HTTP GET requests from
 * the admin panel's bitácora section. It fetches all log entries via
 * {@link LogDAO} and serializes them as a pipe-delimited plain-text response
 * that the frontend parses into a sortable, filterable table.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>No active session → HTTP 401, body {@code "unauthorized"}.</li>
 *   <li>Active session with a non-ADMIN role → HTTP 403, body
 *       {@code "forbidden"}.</li>
 * </ul>
 *
 * <p>Response format (plain text, {@code UTF-8}):
 * <pre>{@code
 * <id>~<createdAt>~<user>~<action>~<description>|<id>~...
 * }</pre>
 * Each log entry is separated by {@code "|"} and its fields are separated
 * by {@code "~"}. An empty body is returned when there are no log entries.
 * On DAO failure, the response body begins with {@code "ERROR:"} followed by
 * the exception message.
 *
 * @see LogDAO
 * @see model.Log
 */
@WebServlet("/listLogs")
public class ListLogsServlet extends HttpServlet {

    /**
     * Returns all system audit log entries as a pipe-delimited plain-text response.
     *
     * <p>Validates session and role, then delegates to {@link LogDAO#getAll()}
     * to retrieve the entries. Each {@link model.Log} is serialized as:
     * {@code id~createdAt~user~action~description|}.
     *
     * @param req  the HTTP request; no parameters are required
     * @param resp the HTTP response; content type is set to
     *             {@code text/plain;charset=UTF-8}
     * @throws IOException if an I/O error occurs while writing the response
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

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equalsIgnoreCase(role)) {
            resp.setStatus(403);
            resp.getWriter().write("forbidden");
            return;
        }

        try {
            LogDAO dao = new LogDAO();
            List<Log> logs = dao.getAll();
            StringBuilder sb = new StringBuilder();

            for (Log log : logs) {
                sb.append(log.getId()).append("~")
                  .append(log.getCreatedAt()).append("~")
                  .append(log.getUser()).append("~")
                  .append(log.getAction()).append("~")
                  .append(log.getDescription())
                  .append("|");
            }

            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("ERROR:" + e.getMessage());
        }
    }
}