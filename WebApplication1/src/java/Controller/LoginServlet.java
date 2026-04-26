package Controller;

import service.UserService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

/**
 * Servlet that handles user authentication for the UNA Space Management System.
 *
 * <p>Mapped to {@code /login}, this servlet processes HTTP POST requests from
 * the login form. It validates the submitted credentials, delegates
 * authentication and registration to {@link UserService}, creates an HTTP
 * session on success, and redirects the user to the appropriate landing page
 * based on their assigned role.
 *
 * <p>Email validation rules:
 * <ul>
 *   <li>The email field must be non-null and non-empty.</li>
 *   <li>The domain must be {@code @una.ac.cr}; any other domain redirects to
 *       {@code index.html?error=email}.</li>
 * </ul>
 *
 * <p>Post-authentication redirects by role:
 * <ul>
 *   <li>{@code ADMIN}  → {@code admin.html}</li>
 *   <li>{@code CLIENT} → {@code dashboard.html}</li>
 *   <li>Unknown role   → session invalidated, redirect to
 *       {@code index.html?error=login}</li>
 * </ul>
 *
 * <p>HTTP GET requests are redirected to {@code index.html} to prevent
 * direct browser access to the servlet URL.
 *
 * <p>Session attributes set on successful login:
 * <ul>
 *   <li>{@code emailUsuario} — the authenticated user's email address.</li>
 *   <li>{@code role}         — the user's role in uppercase
 *                              ({@code ADMIN} or {@code CLIENT}).</li>
 * </ul>
 *
 * @see UserService
 * @see LogoutServlet
 * @see CurrentUserServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /**
     * Service layer responsible for authenticating or auto-registering users.
     */
    private final UserService userService = new UserService();

    /**
     * Processes the login form submission.
     *
     * <p>Validates the email domain and password presence, calls
     * {@link UserService#loginOrRegisterUser(String, String)}, and on success
     * creates a new session with the user's email and role before redirecting
     * to the appropriate page.
     *
     * @param request  the HTTP request; must include the {@code email} and
     *                 {@code password} parameters
     * @param response the HTTP response used to issue the redirect
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirect
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email    = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        email = email.trim().toLowerCase();

        // Enforce the institutional email domain restriction.
        if (!email.endsWith("@una.ac.cr")) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        password = password.trim();

        String role = userService.loginOrRegisterUser(email, password);

        if (role == null || "ERROR".equalsIgnoreCase(role)) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        role = role.toUpperCase();

        // Create a new session and store the authenticated user's attributes.
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
     * Redirects any direct GET access to the login page.
     *
     * <p>This prevents users from bookmarking or navigating directly to the
     * {@code /login} servlet URL in their browser.
     *
     * @param request  the HTTP request
     * @param response the HTTP response used to redirect to {@code index.html}
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirect
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }
}