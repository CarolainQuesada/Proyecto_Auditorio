package Controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * Servlet that exposes the email address of the currently authenticated user.
 *
 * <p>Mapped to {@code /currentUser}, this servlet handles HTTP GET requests
 * and returns a plain-text response used by the frontend to display the
 * logged-in user's identity and to guard pages that require an active session.
 *
 * <p>Response values:
 * <ul>
 *   <li>The user's institutional email (e.g. {@code usuario@una.ac.cr}) if
 *       a valid session with the {@code emailUsuario} attribute exists.</li>
 *   <li>{@code "NO_SESSION"} if there is no active session or the attribute
 *       is missing, signaling the frontend to redirect to the login page.</li>
 * </ul>
 *
 * <p>This endpoint is intentionally unauthenticated so that JavaScript on
 * any page can call it to determine session state before rendering.
 *
 * @see RoleServlet
 * @see LoginServlet
 */
@WebServlet("/currentUser")
public class CurrentUserServlet extends HttpServlet {

    /**
     * Returns the email of the currently authenticated user as plain text.
     *
     * <p>Reads the {@code emailUsuario} attribute from the current HTTP
     * session. If no session or attribute is found, writes {@code "NO_SESSION"}
     * to the response body instead.
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

        if (session == null || session.getAttribute("emailUsuario") == null) {
            resp.getWriter().write("NO_SESSION");
            return;
        }

        resp.getWriter().write(session.getAttribute("emailUsuario").toString());
    }
}