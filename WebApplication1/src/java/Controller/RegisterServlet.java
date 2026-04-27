package Controller;

import service.UserService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

/**
 * Servlet responsible for handling new user registration in the system.
 *
 * <p>Mapped to {@code /register}, this servlet receives registration requests
 * from {@code register.html}, validates the submitted data through
 * {@link UserService}, and creates a new client user in the {@code users}
 * database table when the registration is successful.
 *
 * <p>This servlet is part of the access module and separates user registration
 * from login authentication. Existing users must authenticate through
 * {@code /login}, while new users must first create an account through this
 * servlet.
 *
 * <p>Registration rules:
 * <ul>
 *   <li>The email must not be empty.</li>
 *   <li>The email must belong to the institutional domain {@code @una.ac.cr}.</li>
 *   <li>The password must not be empty.</li>
 *   <li>The confirmation password must match the password.</li>
 *   <li>The password must meet the minimum length configured in {@link UserService}.</li>
 *   <li>The email must not already exist in the {@code users} table.</li>
 * </ul>
 *
 * <p>When a user is successfully registered, the account is created with the
 * {@code CLIENT} role. Administrator accounts are not created from this form.
 *
 * @see UserService
 * @see LoginServlet
 * @see LogoutServlet
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    /**
     * Service used to validate registration data and create client users.
     */
    private final UserService userService = new UserService();

    /**
     * Handles HTTP POST requests submitted from the registration form.
     *
     * <p>This method receives the email, password, and password confirmation
     * parameters from {@code register.html}. The email is normalized to
     * lowercase before being sent to the service layer.
     *
     * <p>The registration process is delegated to
     * {@link UserService#registerClient(String, String, String)}, which returns
     * a status code indicating the result of the operation.
     *
     * <p>Possible redirects:
     * <ul>
     *   <li>{@code index.html?registered=ok} — user was created successfully.</li>
     *   <li>{@code register.html?error=email} — invalid or non-institutional email.</li>
     *   <li>{@code register.html?error=password} — password fields are empty.</li>
     *   <li>{@code register.html?error=password_match} — passwords do not match.</li>
     *   <li>{@code register.html?error=password_short} — password does not meet minimum length.</li>
     *   <li>{@code register.html?error=exists} — email is already registered.</li>
     *   <li>{@code register.html?error=server} — unexpected error while registering.</li>
     * </ul>
     *
     * @param request  the HTTP request containing {@code email}, {@code password},
     *                 and {@code confirmPassword} parameters
     * @param response the HTTP response used to redirect the user according
     *                 to the registration result
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirection
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (email != null) {
            email = email.trim().toLowerCase();
        }

        String result = userService.registerClient(email, password, confirmPassword);

        switch (result) {
            case "CREATED":
                response.sendRedirect("index.html?registered=ok");
                break;

            case "EMAIL":
                response.sendRedirect("register.html?error=email");
                break;

            case "PASSWORD":
                response.sendRedirect("register.html?error=password");
                break;

            case "PASSWORD_MATCH":
                response.sendRedirect("register.html?error=password_match");
                break;

            case "PASSWORD_SHORT":
                response.sendRedirect("register.html?error=password_short");
                break;

            case "EXISTS":
                response.sendRedirect("register.html?error=exists");
                break;

            default:
                response.sendRedirect("register.html?error=server");
                break;
        }
    }

    /**
     * Handles direct HTTP GET access to the registration servlet.
     *
     * <p>When a user accesses {@code /register} directly from the browser,
     * this method redirects the request to {@code register.html}, where the
     * registration form is located.
     *
     * @param request  the HTTP request received by the servlet
     * @param response the HTTP response used to redirect the user to the
     *                 registration page
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during redirection
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("register.html");
    }
}