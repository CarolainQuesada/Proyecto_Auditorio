package Controller;

import java.io.IOException;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

/**
 * Servlet that terminates the current user session and redirects to the
 * login page.
 *
 * <p>Mapped to {@code /logout}, this servlet handles HTTP GET requests
 * triggered by the "Cerrar sesión" / "Salir" links present in both the
 * admin panel and the user dashboard.
 *
 * <p>If an active session exists, it is immediately invalidated, clearing
 * all attributes (including {@code emailUsuario} and {@code role}).
 * The user is then unconditionally redirected to {@code index.html},
 * regardless of whether a session was found.
 *
 * <p>This servlet does not perform any role check, as logging out must
 * always be permitted to any caller.
 *
 * @see LoginServlet
 * @see CurrentUserServlet
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /**
     * Invalidates the current HTTP session (if any) and redirects to the
     * login page.
     *
     * @param req  the HTTP request; no parameters are required
     * @param resp the HTTP response used to redirect to {@code index.html}
     * @throws IOException if an I/O error occurs during the redirect
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        resp.sendRedirect("index.html");
    }
}