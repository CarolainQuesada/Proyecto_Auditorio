package Controller;

import service.UserService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

/**
 * Servlet responsible for handling user authentication in the system.
 *
 * <p>Mapped to {@code /login}, this servlet receives login requests from
 * {@code index.html}, validates the submitted credentials, delegates the
 * authentication process to {@link UserService}, and creates an HTTP session
 * when the login is successful.
 *
 * <p>The access module works with users stored in the {@code users} database
 * table. The servlet does not create users automatically; if the email is not
 * registered, the user is redirected back to the login page with an appropriate
 * error message.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>The email field must not be empty.</li>
 *   <li>The email must belong to the institutional domain {@code @una.ac.cr}.</li>
 *   <li>The password field must not be empty.</li>
 * </ul>
 *
 * <p>Session attributes created after successful authentication:
 * <ul>
 *   <li>{@code emailUsuario} — stores the authenticated user's email.</li>
 *   <li>{@code role} — stores the user's role, such as {@code ADMIN} or {@code CLIENT}.</li>
 * </ul>
 *
 * <p>Redirection rules after login:
 * <ul>
 *   <li>{@code ADMIN} users are redirected to {@code admin.html}.</li>
 *   <li>{@code CLIENT} users are redirected to {@code dashboard.html}.</li>
 *   <li>Invalid or unknown roles invalidate the session and redirect to the login page.</li>
 * </ul>
 *
 * @see UserService
 * @see RegisterServlet
 * @see LogoutServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /**
     * Service used to validate user credentials and obtain the user's role.
     */
    private final UserService userService = new UserService();

    /**
     * Handles HTTP POST requests sent from the login form.
     *
     * <p>This method validates the email and password fields, normalizes the
     * email to lowercase, verifies that it belongs to the {@code @una.ac.cr}
     * domain, and delegates authentication to {@link UserService#loginUser}.
     *
     * <p>If authentication is successful, a new HTTP session is created and
     * the user's email and role are stored as session attributes. The user is
     * then redirected according to their role.
     *
     * <p>Possible redirects:
     * <ul>
     *   <li>{@code index.html?error=email} — invalid or empty email.</li>
     *   <li>{@code index.html?error=not_registered} — email does not exist in the database.</li>
     *   <li>{@code index.html?error=login} — invalid password or authentication error.</li>
     *   <li>{@code admin.html} — successful login with {@code ADMIN} role.</li>
     *   <li>{@code dashboard.html} — successful login with {@code CLIENT} role.</li>
     * </ul>
     *
     * @param request  the HTTP request containing {@code email} and {@code password}
     *                 parameters from the login form
     * @param response the HTTP response used to redirect the user according
     *                 to the authentication result
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirection
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@una.ac.cr")) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        password = password.trim();

        String result = userService.loginUser(email, password);

        if ("NOT_REGISTERED".equals(result)) {
            response.sendRedirect("index.html?error=not_registered");
            return;
        }

        if ("INVALID".equals(result) || "ERROR".equals(result)) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        String role = result.toUpperCase();

        HttpSession session = request.getSession(true);
        session.setAttribute("emailUsuario", email);
        session.setAttribute("role", role);

        if ("ADMIN".equals(role)) {
            response.sendRedirect("admin.html");
        } else if ("CLIENT".equals(role)) {
            response.sendRedirect("dashboard.html");
        } else {
            session.invalidate();
            response.sendRedirect("index.html?error=login");
        }
    }

    /**
     * Handles direct HTTP GET access to the login servlet.
     *
     * <p>Since authentication must be performed through the login form using
     * POST, direct access to {@code /login} is redirected to {@code index.html}.
     *
     * @param request  the HTTP request received by the servlet
     * @param response the HTTP response used to redirect the user to the login page
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirection
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }
}