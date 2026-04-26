package Controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that exposes the role of the currently authenticated user.
 *
 * <p>Mapped to {@code /role}, this servlet handles HTTP GET requests and
 * returns the session-stored role as a plain-text response. It is used by
 * the frontend JavaScript on protected pages (such as {@code admin.html})
 * to verify that the current session has the required privileges before
 * rendering sensitive content.
 *
 * <p>Response values:
 * <ul>
 *   <li>The user's role string (e.g. {@code "ADMIN"} or {@code "CLIENT"})
 *       if a valid session with the {@code role} attribute exists.</li>
 *   <li>{@code "NO_SESSION"} if there is no active session or the
 *       {@code role} attribute is absent.</li>
 * </ul>
 *
 * <p>This endpoint is intentionally lightweight and stateless — it only
 * reads from an existing session and never creates one.
 *
 * @see CurrentUserServlet
 * @see LoginServlet
 */
@WebServlet("/role")
public class RoleServlet extends HttpServlet {

    /**
     * Returns the role of the currently authenticated user as plain text.
     *
     * <p>Reads the {@code role} attribute from the current HTTP session.
     * If no session or attribute is found, writes {@code "NO_SESSION"}
     * to the response body, which the frontend interprets as a signal
     * to redirect to the login page.
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
            resp.getWriter().write("NO_SESSION");
            return;
        }

        resp.getWriter().write(session.getAttribute("role").toString());
    }
}