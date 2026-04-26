package service;

import dao.UserDAO;
import model.User;

/**
 * Service layer for user authentication and self-registration.
 *
 * <p>{@code UserService} provides a single entry point,
 * {@link #loginOrRegisterUser(String, String)}, that handles three scenarios
 * in order:
 * <ol>
 *   <li><b>Hardcoded admin</b> — credentials matching the built-in admin
 *       account return {@code "ADMIN"} immediately without touching the
 *       database.</li>
 *   <li><b>Existing user login</b> — if the email/password pair matches a
 *       database record the user's normalised role is returned.</li>
 *   <li><b>Auto-registration</b> — if the email is not found in the database
 *       a new {@code CLIENT} account is created and the user is logged in
 *       immediately.</li>
 * </ol>
 *
 * <h2>Role values returned</h2>
 * <ul>
 *   <li>{@code "ADMIN"}  — administrator account.</li>
 *   <li>{@code "CLIENT"} — regular client account (mapped from the Spanish
 *       database value {@code "CLIENTE"} via {@link #normalizeRole(String)}).</li>
 *   <li>{@code "ERROR"}  — authentication or registration failed.</li>
 * </ul>
 *
 * @see UserDAO
 * @see model.User
 */
public class UserService {

    /** DAO used for database authentication and user creation. */
    private final UserDAO dao = new UserDAO();

    /** Email address of the built-in administrator account. */
    private static final String ADMIN_EMAIL = "admin@una.ac.cr";

    /** Password of the built-in administrator account. */
    private static final String ADMIN_PASSWORD = "123";

    /**
     * Authenticates an existing user or registers a new one if the email is
     * not yet in the database.
     *
     * <p>The lookup order is:
     * <ol>
     *   <li>Check against the hardcoded admin credentials.</li>
     *   <li>Attempt a database login with the given email and password.</li>
     *   <li>If no user is found and the email is also not registered, create
     *       a new {@code CLIENT} user and log them in.</li>
     *   <li>If the email exists but the password is wrong, return
     *       {@code "ERROR"}.</li>
     * </ol>
     *
     * @param email    the user's email address; comparison is case-insensitive
     *                 for the admin check, exact-match for database queries
     * @param password the plain-text password
     * @return {@code "ADMIN"}, {@code "CLIENT"}, or {@code "ERROR"}
     */
    public String loginOrRegisterUser(String email, String password) {

        try {
            if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD.equals(password)) {
                return "ADMIN";
            }

            User user = dao.login(email, password);

            if (user != null) {
                return normalizeRole(user.getRole());
            }

            User existingUser = dao.findByEmail(email);

            if (existingUser != null) {
                return "ERROR";
            }

            boolean created = dao.createClientUser(email, password);

            if (!created) {
                return "ERROR";
            }

            User newUser = dao.login(email, password);

            if (newUser != null) {
                return normalizeRole(newUser.getRole());
            }

            return "ERROR";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Normalises a raw role string from the database to one of the
     * application-level role constants.
     *
     * <p>The Spanish value {@code "CLIENTE"} is mapped to {@code "CLIENT"}.
     * The values {@code "ADMIN"} and {@code "CLIENT"} are returned as-is.
     * Any other value (including {@code null}) returns {@code "ERROR"}.
     *
     * @param role the raw role string as stored in the database; may be
     *             {@code null}
     * @return {@code "ADMIN"}, {@code "CLIENT"}, or {@code "ERROR"}
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "ERROR";
        }

        role = role.toUpperCase();

        if ("CLIENTE".equals(role)) {
            return "CLIENT";
        }

        if ("ADMIN".equals(role) || "CLIENT".equals(role)) {
            return role;
        }

        return "ERROR";
    }
}